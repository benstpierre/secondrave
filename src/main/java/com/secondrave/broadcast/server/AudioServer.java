package com.secondrave.broadcast.server;

import org.eclipse.jetty.server.Server;

/**
 * Created by benstpierre on 14-11-26.
 */
public class AudioServer {


    public void startJetty() {
        final Server server = new Server(8080);
        try {
            server.setHandler(new AudioHanlder());
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}