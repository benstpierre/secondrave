package com.secondrave.broadcast.server;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-11-26.
 */
public class AudioCapture implements Runnable {

    private final Mixer selectedMixer;
    private final DataLine.Info dataLineInfo;
    private final AudioFormat audioFormat;
    private final AudioServer audioServer;

    private AtomicBoolean keepGoing = new AtomicBoolean(true);

    public AudioCapture(Mixer selectedMixer, DataLine.Info dataLineInfo, AudioFormat audioFormat, AudioServer audioServer) {
        this.selectedMixer = selectedMixer;
        this.dataLineInfo = dataLineInfo;
        this.audioFormat = audioFormat;
        this.audioServer = audioServer;
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
        final int frameSizeInBytes = audioFormat.getFrameSize();
        final int bufferLengthInFrames = targetDataLine.getBufferSize() / 8;
        final int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
        byte[] data = new byte[bufferLengthInBytes];
        int numBytesRead;

        targetDataLine.start();

        int count = 0;
        while (this.keepGoing.get()) {
            if ((numBytesRead = targetDataLine.read(data, 0, bufferLengthInBytes)) == -1) {
                break;
            }
            count++;
            if (count == 10) {
                out.write(data, 0, numBytesRead);
                audioServer.pushAudioData(out.toByteArray());
                out.reset();
                count = 0;
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
}
