package com.secondrave.broadcast.server;

import com.secondrave.protos.SecondRaveProtos;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.jetlang.channels.Channel;
import org.jetlang.core.Callback;
import org.jetlang.core.Disposable;
import org.jetlang.fibers.Fiber;

public class SoundSocket extends WebSocketAdapter implements Callback<SecondRaveProtos.AudioPiece> {

    private Disposable subscription;

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        final Pair<Fiber, Channel<SecondRaveProtos.AudioPiece>> channelInfo = AudioServer.currentChannelInfo();
        final Fiber fiber = channelInfo.getValueOne();
        final Channel<SecondRaveProtos.AudioPiece> channel = channelInfo.getValueTwo();
        this.subscription = channel.subscribe(fiber, this);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        subscription.dispose();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
        subscription.dispose();
    }

    @Override
    public void onMessage(SecondRaveProtos.AudioPiece message) {
        getRemote().sendBytesByFuture(message.toByteString().asReadOnlyByteBuffer());
    }
}