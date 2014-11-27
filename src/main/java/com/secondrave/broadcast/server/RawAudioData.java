package com.secondrave.broadcast.server;

/**
 * Created by benstpierre on 14-10-27.
 */
public class RawAudioData {


    private boolean isLastChunk;
    private byte[] pcmData;

    public byte[] getPcmData() {
        return pcmData;
    }

    public void setPcmData(byte[] pcmData) {
        this.pcmData = pcmData;
    }

    public boolean isLastChunk() {
        return isLastChunk;
    }

    public void setLastChunk(boolean isLastChunk) {
        this.isLastChunk = isLastChunk;
    }
}
