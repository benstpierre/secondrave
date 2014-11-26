package com.secondrave.broadcast;

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


    final MenuItem startServer = new MenuItem("Start Server");
    final MenuItem stopServer = new MenuItem("Stop Server");
    final MenuItem aboutItem = new MenuItem("About");
    final MenuItem exitItem = new MenuItem("Exit");


    final Menu sourceMenu = new Menu("Source");
    private ArrayList<Mixer> mixers = new ArrayList<Mixer>();
    private Map<CheckboxMenuItem, Mixer> sourceMap = new HashMap<CheckboxMenuItem, Mixer>(20);

    private Mixer selectedMixer;
    private AudioUploader audioUploader;

    public static void main(String[] args) {
        new Program().doTray();
    }

    private void doTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(createImage("/images/headphones.png", "tray icon"));
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
        this.audioUploader = new AudioUploader(selectedMixer);
        this.audioUploader.start();
    }

    private void stopServer() {
        this.audioUploader.stop();
        this.audioUploader = null;
    }

    private void showAbout() {

    }

    private void quitApplication() {
        stopServer();
        System.exit(0);
    }

    private void populateAudioInputList() {
        //Check this out for interest
        //http://www.java-forum.org/spiele-multimedia-programmierung/94699-java-sound-api-zuordnung-port-mixer-input-mixer.html
        final String newLine = System.getProperty("line.separator");


        final AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        final float rate = 44100.0f;
        final int channels = 2;
        final int frameSize = 4;
        final int sampleSize = 16;
        final boolean bigEndian = true;

        final AudioFormat audioFormat = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8)
                * channels, rate, bigEndian);

        final DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

        TargetDataLine targetDataLine;

        //Go through the System audio mixers
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer targetMixer = AudioSystem.getMixer(mixerInfo);
                targetMixer.open();


                //Check if it supports the desired format
                if (targetMixer.isLineSupported(info)) {
                    mixers.add(targetMixer);

//                    System.out.println(mixerInfo.getName() + " supports recording @" + audioFormat);
//                    //now go back and start again trying to match a mixer to a port
//                    //the only way I figured how is by matching name, because
//                    //the port mixer name is the same as the actual mixer with "Port " in front of it
//                    // there MUST be a better way
//
//
//                    for (Mixer.Info mifo : AudioSystem.getMixerInfo()) {
//                        final String port_string = "Port ";
//                        if ((port_string + mixerInfo.getName()).equals(mifo.getName())) {
//                            System.out.println("Matched Port to Mixer:" + mixerInfo.getName());
//                            final Mixer portMixer = AudioSystem.getMixer(mifo);
//                            portMixer.open();
//                            portMixer.isLineSupported(info);
//                            //now check the mixer has the right input type eg LINE_IN
//
//
//
//                            if (portMixer.isLineSupported(info)) {
//                                //OK we have a supported Port Type for the Mixer
//                                //This has all matched (hopefully)
//                                //now just get the record line
//                                //There should be at least 1 line, usually 32 and possible unlimited
//                                // which would be "AudioSystem.Unspecified" if we ask the mixer
//                                //but I haven't checked any of this
//                                targetDataLine = (TargetDataLine) targetMixer.getLine(info);
//                                System.out.println("Got TargetDataLine from :" + targetMixer.getMixerInfo().getName());
//                                return;
//                            }
//                        }
//                    }
//                    System.out.println(newLine);
                }
                targetMixer.close();
            } catch (Exception e) {
                e.printStackTrace();
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