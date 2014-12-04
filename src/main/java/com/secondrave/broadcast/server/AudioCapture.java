package com.secondrave.broadcast.server;

import com.google.protobuf.ByteString;
import com.secondrave.protos.SecondRaveProtos;
import org.jetlang.channels.MemoryChannel;
import org.joda.time.Duration;
import org.joda.time.Instant;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-11-26.
 */
public class AudioCapture implements Runnable {

    private final MemoryChannel<SecondRaveProtos.AudioPiece> channel;
    private final Mixer selectedMixer;
    private final DataLine.Info dataLineInfo;
    private final AudioFormat audioFormat;

    final Executor executor = Executors.newFixedThreadPool(1);

    private AtomicBoolean keepGoing = new AtomicBoolean(true);

    public AudioCapture(MemoryChannel<SecondRaveProtos.AudioPiece> channel, Mixer selectedMixer, DataLine.Info dataLineInfo, AudioFormat audioFormat) {
        this.channel = channel;
        this.selectedMixer = selectedMixer;
        this.dataLineInfo = dataLineInfo;
        this.audioFormat = audioFormat;
    }

    public void requestStop() {
        this.keepGoing.set(false);
    }

    @Override
    public void run() {
        final TargetDataLine targetDataLine;
        try {
            selectedMixer.open();
            targetDataLine = (TargetDataLine) selectedMixer.getLine(dataLineInfo);
            targetDataLine.open(audioFormat, targetDataLine.getBufferSize());
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final int bufferSize = (int) audioFormat.getSampleRate();
        final byte[] data = new byte[bufferSize];
        int numBytesRead;

        targetDataLine.start();

        int count = 0;
        Instant currentStart = Instant.now().plus(Duration.standardSeconds(2));
        while (this.keepGoing.get()) {
            numBytesRead = targetDataLine.read(data, 0, bufferSize);
            if (numBytesRead == -1) {
                break;
            }
            out.write(data, 0, numBytesRead);
            count++;
            if (count == 1) {
                final byte[] arrData = out.toByteArray();
                pushAudioData(arrData, currentStart);
                out.reset();
                count = 0;
                currentStart = currentStart.plus(Duration.millis((int) (arrData.length / 44.1 / 2)));
            }
        }

        // we reached the end of the stream.
        // requestStop and close the line.
        targetDataLine.stop();
        targetDataLine.close();

        // requestStop and close the output stream
        try {
            out.flush();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void stop() {
        this.keepGoing.set(false);
    }

    private void pushAudioData(final byte[] arrData, final Instant currentSampleStartsAtInstant) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final SecondRaveProtos.AudioPiece audioPiece = SecondRaveProtos.AudioPiece.newBuilder()
                        .setAudioData(ByteString.copyFrom(arrData))
                        .setDuration((int) (arrData.length / 44.1 / 2))
                        .setPlayAt(currentSampleStartsAtInstant.getMillis())
                        .build();
                channel.publish(audioPiece);
            }
        });
    }
}
