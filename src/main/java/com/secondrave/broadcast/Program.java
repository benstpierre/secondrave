package com.secondrave.broadcast;

import com.secondrave.broadcast.server.AudioCapture;
import com.secondrave.broadcast.server.AudioServer;
import com.secondrave.protos.SecondRaveProtos;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.fibers.ThreadFiber;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by benstpierre on 14-11-26.
 */
public class Program implements ActionListener, ItemListener {

    final AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, false);
    final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);


    final MenuItem startServer = new MenuItem("Start Server");
    final MenuItem stopServer = new MenuItem("Stop Server");
    final MenuItem aboutItem = new MenuItem("About");
    final MenuItem exitItem = new MenuItem("Exit");


    final Menu sourceMenu = new Menu("Source");
    private ArrayList<Mixer> mixers = new ArrayList<Mixer>();
    private Map<CheckboxMenuItem, Mixer> sourceMap = new HashMap<CheckboxMenuItem, Mixer>(20);

    private Mixer selectedMixer;
    private AudioCapture audioCapture;
    private AudioServer audioServer;
    private MemoryChannel<SecondRaveProtos.AudioPiece> channel;
    private ThreadFiber fiber;
    private TrayIcon trayIcon;

    public static void main(String[] args) {
        new Program().doTray();
    }

    private void doTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        this.trayIcon = new TrayIcon(createImage("/images/headphones.png", "Tray icon"));
        final SystemTray tray = SystemTray.getSystemTray();

        // Create a pop-up menu components

        exitItem.addActionListener(this);
        startServer.addActionListener(this);
        stopServer.addActionListener(this);
        aboutItem.addActionListener(this);

        //Add components to pop-up menu
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(startServer);
        popup.add(stopServer);
        popup.addSeparator();
        popup.add(sourceMenu);

        populateAudioInputList();

        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
        setInitialState();
    }

    private void setInitialState() {
        this.startServer.setEnabled(false);
        this.stopServer.setEnabled(false);
    }

    //Obtain the image URL
    protected static Image createImage(String path, String description) {
        final URL imageURL = Program.class.getResource(path);

        if (imageURL == null) {
            System.err.println("Resource not found: " + path);
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (startServer == e.getSource()) {
            startServer();
        } else if (stopServer == e.getSource()) {
            stopServer();
        } else if (aboutItem == e.getSource()) {
            showAbout();
        } else if (exitItem == e.getSource()) {
            quitApplication();
        }
    }

    private void startServer() {
        //Jetlang setup code
        {
            this.fiber = new ThreadFiber();
            this.fiber.start();
            this.channel = new MemoryChannel<SecondRaveProtos.AudioPiece>();
        }
        //Start audio capture thread for capture
        {
            this.audioCapture = new AudioCapture(channel, selectedMixer, dataLineInfo, audioFormat);
            new Thread(audioCapture).start();
        }
        //Start audio server
        {
            this.audioServer = new AudioServer(channel, fiber);
            new Thread(audioServer).start();
        }
        //Change Tray Icon to red
        {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Program.this.startServer.setEnabled(false);
                    Program.this.stopServer.setEnabled(true);
                    Program.this.trayIcon.setImage(createImage("/images/headphones-onair.png", "Tray icon - On Air"));
                }
            });

        }
    }

    private void stopServer() {
        if (this.fiber != null) {
            this.fiber.dispose();
            this.fiber = null;
        }
        if (this.audioCapture != null) {
            this.audioCapture.requestStop();
            this.audioCapture = null;
        }
        if (this.audioServer != null) {
            this.audioServer.requestStop();
            this.audioServer = null;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Program.this.startServer.setEnabled(true);
                Program.this.stopServer.setEnabled(false);
                Program.this.trayIcon.setImage(createImage("/images/headphones.png", "Tray icon - On Air"));
            }
        });

    }

    private void showAbout() {

    }

    private void quitApplication() {
        stopServer();
        System.exit(0);
    }

    private void populateAudioInputList() {
        //Go through the System audio mixers
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = null;
            try {
                mixer = AudioSystem.getMixer(mixerInfo);
                mixer.open();
                //Check if it supports the desired format
                if (mixer.isLineSupported(dataLineInfo)) {
                    mixers.add(mixer);
                }
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            } finally {
                if (mixer != null) {
                    mixer.close();
                }
            }
        }
        addMixersToGui();
    }

    private void addMixersToGui() {
        for (Mixer mixer : mixers) {
            final CheckboxMenuItem menuItem = new CheckboxMenuItem(mixer.getMixerInfo().getName());
            sourceMenu.add(menuItem);
            sourceMap.put(menuItem, mixer);
            menuItem.addItemListener(this);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        final Mixer selectedMixer = sourceMap.get(e.getSource());
        if (selectedMixer != null) {
            this.selectedMixer = selectedMixer;
            for (Map.Entry<CheckboxMenuItem, Mixer> entry : sourceMap.entrySet()) {
                entry.getKey().setState(entry.getValue() == this.selectedMixer);
            }
            this.startServer.setEnabled(true);
        }
    }

}