package com.secondrave.broadcast.server;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.joda.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by benstpierre on 14-10-25.
 */
public class AudioServer extends AbstractHandler implements Runnable {


    private List<AudioChunk> audioChunks = Lists.newArrayList();
    private Server server;

    public synchronized void pushAudioData(AudioChunk audioChunk) {
        audioChunks.add(audioChunks.size(), audioChunk);
        cleanupStaleAudio();
    }

    private void cleanupStaleAudio() {
        final Instant now = Instant.now();
        Iterables.removeIf(audioChunks, new Predicate<AudioChunk>() {
            @Override
            public boolean apply(AudioChunk audioChunk) {
                return audioChunk.playAt.isBefore(now);
            }
        });
    }

    private void printDebug(Instant instant) {
        final List<DebugEntry> debugEntries = Lists.newArrayList();
        {
            final DebugEntry debugNow = new DebugEntry();
            debugNow.instant = Instant.now();
            debugNow.name = "Instant.now()";
            debugEntries.add(debugNow);
        }
        {
            final DebugEntry requestedInstant = new DebugEntry();
            requestedInstant.instant = instant;
            requestedInstant.name = "Requested Instant";
            debugEntries.add(requestedInstant);
        }
        for (int i = 0; i < audioChunks.size(); i++) {
            final AudioChunk chunk = audioChunks.get(i);
            final DebugEntry requestedInstant = new DebugEntry();
            requestedInstant.instant = chunk.playAt;
            requestedInstant.name = chunk.toString();
            debugEntries.add(requestedInstant);
        }
        Collections.sort(debugEntries, Ordering.from(new Comparator<DebugEntry>() {
            @Override
            public int compare(DebugEntry o1, DebugEntry o2) {
                return o1.instant.compareTo(o2.instant);
            }
        }));
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("START");
        for (DebugEntry debugEntry : debugEntries) {
            System.out.println(debugEntry.name + " " + debugEntry.instant.toDateTime());
        }
        System.out.println("START");
        System.out.println("");
        System.out.println("");
        System.out.println("");
    }

    @Override
    public synchronized void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        AudioChunk toDownload = null;

        if (request.getParameter("INDEX") != null) {
            toDownload = audioChunks.get(Integer.valueOf(request.getParameter("INDEX")));
        }


        final String strNewestSampleAfterInstant = request.getHeader("NEWEST_SAMPLE_AFTER_INSTANT");
        Instant instant;
        try {
            final Long longNewestSampleAfterInstant = Long.valueOf(strNewestSampleAfterInstant);
            instant = new Instant(longNewestSampleAfterInstant);
        } catch (NumberFormatException ex) {
            instant = Instant.now();
        }

        printDebug(instant);

        if (toDownload == null) {
            for (AudioChunk timedAudioChunk : audioChunks) {
                if (timedAudioChunk.playAt.isEqual(instant) || timedAudioChunk.playAt.isAfter(instant)) {
                    toDownload = timedAudioChunk;
                    break;
                }
            }
        }
        httpServletResponse.setHeader("PLAYAT", String.valueOf(toDownload.playAt.getMillis()));
        httpServletResponse.setHeader("PLAYLENGTH", String.valueOf(toDownload.getLengthMS()));
        httpServletResponse.setContentType("audio/wav");
        httpServletResponse.setContentLength(toDownload.getAudioData().length);
        ByteStreams.copy(new ByteArrayInputStream(toDownload.getAudioData()), httpServletResponse.getOutputStream());
    }

    @Override
    public void run() {
        this.server = new Server(8080);
        try {
            server.setHandler(this);
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestStop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class DebugEntry {
        Instant instant;
        String name;
    }

}
