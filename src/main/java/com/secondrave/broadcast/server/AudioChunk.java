package com.secondrave.broadcast.server;

/**
 * Created by benstpierre on 14-10-27.
 */
public class AudioChunk {

    private int lengthMS;
    private byte[] audioData;

    public int getLengthMS() {
        return lengthMS;
    }

    public void setLengthMS(int lengthMS) {
        this.lengthMS = lengthMS;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }

    @Override
    public String toString() {
        return "AudioChunk{" +
                "lengthMS=" + lengthMS +
                ", mp3AudioDataLengtj=" + audioData.length +
                '}';
    }
}
