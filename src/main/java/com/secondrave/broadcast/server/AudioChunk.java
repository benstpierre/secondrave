package com.secondrave.broadcast.server;

import org.joda.time.Duration;
import org.joda.time.Instant;

/**
 * Created by benstpierre on 14-10-27.
 */
public class AudioChunk {

    private byte[] audioData;
    private Instant playAt;
    private Duration duration;

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
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
                "duration=" + duration +
                ", audioDataLength=" + audioData.length +
                ", playAt=" + playAt +
                '}';
    }

    public Instant getEndAt() {
        return getPlayAt().plus(duration);
    }
}
