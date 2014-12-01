package com.secondrave.broadcast.server;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.joda.time.Duration;
import org.joda.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by benstpierre on 14-10-25.
 */
public class AudioServer extends AbstractHandler implements Runnable {


    private List<AudioChunk> audioChunks = Lists.newArrayList();
    private Server server;

    Instant lastEnd = Instant.now();

    public synchronized void pushAudioData(AudioChunk audioChunk) {
        String time = String.valueOf(audioChunk.getPlayAt().getMillis());
        time = time.substring(time.length() - 1 - 7, time.length());


        System.err.println("Start=" + time + " Length=" + audioChunk.getDuration().getMillis() + "Error=" + (audioChunk.getPlayAt().getMillis() - lastEnd.getMillis()));
        lastEnd = audioChunk.getEndAt();
        audioChunks.add(audioChunks.size(), audioChunk);
        cleanupStaleAudio();
        //printDebug(null);
    }

    private void cleanupStaleAudio() {
        final Instant tenSecondsAgo = Instant.now().minus(Duration.standardSeconds(30));
        Iterables.removeIf(audioChunks, new Predicate<AudioChunk>() {
            @Override
            public boolean apply(AudioChunk audioChunk) {
                return audioChunk.getPlayAt().isBefore(tenSecondsAgo);
            }
        });
    }

    private void printDebug(Instant instant) {
        final List<DebugEntry> debugEntries = Lists.newArrayList();
        {
            final DebugEntry debugNow = new DebugEntry();
            debugNow.instant = Instant.now();
            debugNow.name = "Instant.now()     ";
            debugEntries.add(debugNow);
        }
        if (instant != null) {
            final DebugEntry requestedInstant = new DebugEntry();
            requestedInstant.instant = instant;
            requestedInstant.name = "Requested Instant";
            debugEntries.add(requestedInstant);
        }
        for (final AudioChunk chunk : audioChunks) {
            final DebugEntry requestedInstant = new DebugEntry();
            requestedInstant.instant = chunk.getPlayAt();
            requestedInstant.name = "AudioChunk        ";
            debugEntries.add(requestedInstant);
        }
        Collections.sort(debugEntries, Ordering.from(new Comparator<DebugEntry>() {
            @Override
            public int compare(DebugEntry o1, DebugEntry o2) {
                return o1.instant.compareTo(o2.instant);
            }
        }));
        System.out.println("");
        System.out.println("START");
        for (DebugEntry debugEntry : debugEntries) {
            String time = String.valueOf(debugEntry.instant.getMillis());
            time = time.substring(time.length() - 1 - 5, time.length() - 1);
            System.out.println(debugEntry.name + " " + time);
        }
        System.out.println("END");
        System.out.println("");
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        final String strNewestSampleAfterInstant = request.getHeader("NEWEST_SAMPLE_AFTER_INSTANT");
        Instant instant;
        try {
            final Long longNewestSampleAfterInstant = Long.valueOf(strNewestSampleAfterInstant);
            instant = new Instant(longNewestSampleAfterInstant);
        } catch (NumberFormatException ex) {
            instant = null;
        }
        if (instant != null) {
            final AudioChunk toDownload = toDownload(instant);
            System.err.print(toDownload == null ? "X" : "✓\n");
            if (toDownload == null) {
                httpServletResponse.setHeader("TRYAGAIN", "true");
                httpServletResponse.getWriter().println("TRY AGAIN");
            } else {
                httpServletResponse.setHeader("PLAYAT", String.valueOf(toDownload.getPlayAt().getMillis()));
                httpServletResponse.setHeader("PLAYLENGTH", String.valueOf(toDownload.getDuration().getMillis()));
                httpServletResponse.setContentType("audio/wav");
                httpServletResponse.setContentLength(toDownload.getAudioData().length);
                ByteStreams.copy(new ByteArrayInputStream(toDownload.getAudioData()), httpServletResponse.getOutputStream());
            }
        }
    }

    private AudioChunk toDownload(Instant instant) {
        AudioChunk toDownload = null;
        //printDebug(instant);
        synchronized (this) {
            for (AudioChunk timedAudioChunk : audioChunks) {
                if (timedAudioChunk.getPlayAt().isEqual(instant) || instant.isBefore(timedAudioChunk.getPlayAt())) {
                    toDownload = timedAudioChunk;
                    break;
                }
            }
        }
        return toDownload;
    }

    @Override
    public void run() {
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(200);
        this.server = new Server(threadPool);

        // HTTP connector
        final ServerConnector http = new ServerConnector(server);
        http.setHost("10.0.1.13");
        http.setPort(8080);
        http.setIdleTimeout(30000);

        // Set the connector
        server.addConnector(http);

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
