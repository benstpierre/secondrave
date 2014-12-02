package com.secondrave.broadcast.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.fibers.ThreadFiber;

/**
 * Created by benstpierre on 14-10-25.
 */
public class AudioServer implements Runnable {


    private final ThreadFiber fiber;
    private final MemoryChannel<AudioChunk> channel;
    private Server server;

    public AudioServer(MemoryChannel<AudioChunk> channel, ThreadFiber fiber) {
        this.channel = channel;
        this.fiber = fiber;
    }

    @Override
    public void run() {
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(200);
        this.server = new Server(threadPool);

        // HTTP connector
        {
            final ServerConnector http = new ServerConnector(server);
            http.setHost("10.0.1.13");
            http.setPort(8080);
            http.setIdleTimeout(30000);
            server.addConnector(http);
        }

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        {
            final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);
            // Add a websocket to a specific path spec
            {
                final ServletHolder holderEvents = new ServletHolder("ws-events", new WebSocketServlet() {
                    @Override
                    public void configure(WebSocketServletFactory factory) {
                        factory.register(SoundSocket.class);
                    }
                });
                context.addServlet(holderEvents, "/events/*");
            }
        }

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestStop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
