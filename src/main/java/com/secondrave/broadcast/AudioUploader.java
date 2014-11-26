package com.secondrave.broadcast;

import javax.sound.sampled.*;
import java.util.Vector;

/**
 * Created by benstpierre on 14-11-26.
 */
public class AudioUploader {

    private final Mixer selectedMixer;

    public AudioUploader(Mixer selectedMixer) {
        this.selectedMixer = selectedMixer;
    }

    public void start() {
        try {
            final AudioFormat af = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100.0F,
                    16,
                    1,
                    2,
                    44100.0F,
                    true);
            final TargetDataLine rofl = AudioSystem.getTargetDataLine(af, selectedMixer.getMixerInfo());
            rofl.toString();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }


    public void stop() {

    }
}
