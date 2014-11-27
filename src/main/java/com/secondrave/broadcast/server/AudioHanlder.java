package com.secondrave.broadcast.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.joda.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by benstpierre on 14-10-25.
 */
public class AudioHanlder extends AbstractHandler {


    private List<TimedAudioChunk> timedChunks = Lists.newArrayList();

    public AudioHanlder() {
        runCacheFairy();
        final Timer timer = new Timer();
        final int taskFairySchedule = 5 * 1000;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runCacheFairy();
            }
        }, taskFairySchedule, taskFairySchedule);
    }

    public synchronized void runCacheFairy() {
        final Iterator<TimedAudioChunk> it = timedChunks.iterator();
        final Instant now = Instant.now();

        Instant furthestCached = now; // keep track of what latest sample is
        while (it.hasNext()) {
            final TimedAudioChunk currentChunk = it.next();
            //Delete anything where start time is before "now" (no point in keeping it)
            if (currentChunk.playAt.isBefore(now)) {
                it.remove();
                continue;
            }
            if (furthestCached.isBefore(currentChunk.playAt)) {
                furthestCached = currentChunk.playAt;
            }
        }
    }


    private void printDebug(Instant instant) {
        final List<DebugEntry> debugEntries = Lists.newArrayList();
        {
            DebugEntry debugNow = new DebugEntry();
            debugNow.instant = Instant.now();
            debugNow.name = "Instant.now()";
            debugEntries.add(debugNow);
        }
        {
            DebugEntry requestedInstant = new DebugEntry();
            requestedInstant.instant = instant;
            requestedInstant.name = "Requested Instant";
            debugEntries.add(requestedInstant);
        }
        for (int i = 0; i < timedChunks.size(); i++) {
            TimedAudioChunk chunk = timedChunks.get(i);
            DebugEntry requestedInstant = new DebugEntry();
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
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        TimedAudioChunk toDownload = null;

        if (request.getParameter("INDEX") != null) {
            toDownload = timedChunks.get(Integer.valueOf(request.getParameter("INDEX")));
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
            for (TimedAudioChunk timedAudioChunk : timedChunks) {
                if (timedAudioChunk.playAt.isEqual(instant) || timedAudioChunk.playAt.isAfter(instant)) {
                    toDownload = timedAudioChunk;
                    break;
                }
            }
        }
        httpServletResponse.setHeader("PLAYAT", String.valueOf(toDownload.playAt.getMillis()));
        httpServletResponse.setHeader("PLAYLENGTH", String.valueOf(toDownload.audioChunk.getLengthMS()));
        httpServletResponse.setContentType("audio/wav");
        httpServletResponse.setContentLength(toDownload.audioChunk.getAudioData().length);
        ByteStreams.copy(new ByteArrayInputStream(toDownload.audioChunk.getAudioData()), httpServletResponse.getOutputStream());
    }

    private static class TimedAudioChunk {
        Instant playAt;
        AudioChunk audioChunk;

        @Override
        public String toString() {
            return "TimedAudioChunk{" +
                    "playAt=" + playAt +
                    ", audioChunk=" + audioChunk +
                    '}';
        }
    }

    class DebugEntry {
        Instant instant;
        String name;
    }

}
