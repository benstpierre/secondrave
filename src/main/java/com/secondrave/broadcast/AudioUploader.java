package com.secondrave.broadcast;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Target;
import java.util.Vector;

/**
 * Created by benstpierre on 14-11-26.
 */
public class AudioUploader {

    private final Mixer selectedMixer;
    private final DataLine.Info dataLineInfo;
    private final AudioFormat audioFormat;

    private TargetDataLine targetDataLine;

    public AudioUploader(Mixer selectedMixer, DataLine.Info dataLineInfo, AudioFormat audioFormat) {
        this.selectedMixer = selectedMixer;
        this.dataLineInfo = dataLineInfo;
        this.audioFormat = audioFormat;
    }

    public void start() {
        try {
            selectedMixer.open();
            this.targetDataLine = (TargetDataLine) selectedMixer.getLine(dataLineInfo);
            this.targetDataLine.open(audioFormat, this.targetDataLine.getBufferSize());
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
        while (count < 50) {
            if ((numBytesRead = targetDataLine.read(data, 0, bufferLengthInBytes)) == -1) {
                break;
            }
            out.write(data, 0, numBytesRead);
            count++;
        }

        // we reached the end of the stream.
        // stop and close the line.
        targetDataLine.stop();
        targetDataLine.close();
        targetDataLine = null;

        // stop and close the output stream
        try {
            out.flush();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        playAudio(out);
    }


    private void playAudio(ByteArrayOutputStream out) {
        try {
            byte audio[] = out.toByteArray();
            InputStream input = new ByteArrayInputStream(audio);
            final AudioFormat format = audioFormat;
            final AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());
            final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            Runnable runner = new Runnable() {
                int bufferSize = (int) format.getSampleRate()
                        * format.getFrameSize();
                byte buffer[] = new byte[bufferSize];

                public void run() {
                    try {
                        int count;
                        while ((count = ais.read(
                                buffer, 0, buffer.length)) != -1) {
                            if (count > 0) {
                                line.write(buffer, 0, count);
                            }
                        }
                        line.drain();
                        line.close();
                    } catch (IOException e) {
                        System.err.println("I/O problems: " + e);
                        System.exit(-3);
                    }
                }
            };
            Thread playThread = new Thread(runner);
            playThread.start();
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e);
            System.exit(-4);
        }
    }


    public void stop() {

    }
}
