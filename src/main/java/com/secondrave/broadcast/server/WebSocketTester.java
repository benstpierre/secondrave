package com.secondrave.broadcast.server;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.secondrave.protos.SecondRaveProtos;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by benstpierre on 14-12-03.
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebSocketTester {


    private final CountDownLatch closeLatch;

    @SuppressWarnings("unused")
    private Session session;
    private int count;

    public WebSocketTester() {
        this.closeLatch = new CountDownLatch(1);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.printf("Got connect: %s%n", session);
        this.session = session;
    }


    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @OnWebSocketMessage
    public void onBinaryMethod(byte data[], int offset,
                               int length) {
        try {
            SecondRaveProtos.AudioPiece piece = SecondRaveProtos.AudioPiece.parseFrom(ByteString.copyFrom(data, offset, length));
            this.baos.write(piece.getAudioData().toByteArray());
            count++;
            if (count > 10) {
                playAudio();
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 1, 2, 44100.0f, false);

    private void playAudio() {
        try {
            byte audio[] = baos.toByteArray();
            InputStream input = new ByteArrayInputStream(audio);
            AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();


            int bufferSize = (int) format.getSampleRate()
                    * format.getFrameSize();
            byte buffer[] = new byte[bufferSize];

            int count;
            while ((count =
                    ais.read(buffer, 0, buffer.length)) != -1) {
                if (count > 0) {
                    line.write(buffer, 0, count);
                }
            }
            line.drain();
            line.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String destUri = "ws://10.0.1.13:8080/events/";
        if (args.length > 0) {
            destUri = args[0];
        }
        WebSocketClient client = new WebSocketClient();
        client.getPolicy().setMaxBinaryMessageSize(100000);
        WebSocketTester socket = new WebSocketTester();
        try {
            client.start();
            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);
            System.out.printf("Connecting to : %s%n", echoUri);
            socket.awaitClose(500, TimeUnit.SECONDS);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
