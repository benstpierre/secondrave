package com.secondrave.broadcast.server;

import org.joda.time.Instant;

/**
 * Created by benstpierre on 14-10-27.
 */
public class AudioChunk {

    private int lengthMS;
    private byte[] audioData;
    Instant playAt;

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

    public Instant getPlayAt() {
        return playAt;
    }

    public void setPlayAt(Instant playAt) {
        this.playAt = playAt;
    }

    @Override
    public String toString() {
        return "AudioChunk{" +
                "lengthMS=" + lengthMS +
                ", audioDataLength=" + audioData.length +
                ", playAt=" + playAt +
                '}';
    }
}
