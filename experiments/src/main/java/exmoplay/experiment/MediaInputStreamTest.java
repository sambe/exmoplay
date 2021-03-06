/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package exmoplay.experiment;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JPanel;

import exmoplay.access.MediaFrame;
import exmoplay.access.VideoFormat;
import exmoplay.access.XugglerMediaInputStream;
import exmoplay.engine.actorframework.Actor;
import exmoplay.engine.actorframework.Actor.Priority;
import exmoplay.engine.messages.MediaError;
import exmoplay.experiment.util.SimplePanelFrame;

public class MediaInputStreamTest {

    private static volatile long newLocationToSet = -1;

    public static void main(String[] args) throws Exception {
        File videoFile = new File("/home/sberner/Desktop/10-21.04.09.flv");
        XugglerMediaInputStream is = new XugglerMediaInputStream(videoFile);

        // create screen
        VideoFormat videoFormat = is.getVideoFormat();
        JPanel screen = new JPanel();
        screen.setLayout(null);
        int width = videoFormat.getSize().width;
        int height = videoFormat.getSize().height;
        JButton setPositionButton = new JButton("Set Position");
        setPositionButton.setBounds(10, height + 10, 180, 30);
        screen.add(setPositionButton);
        SimplePanelFrame frame = new SimplePanelFrame(screen, width + 20, height + 20 + 65);

        // add action on button click
        setPositionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newLocationToSet = 0;
            }
        });

        // create sound channel
        AudioFormat audioFormat = is.getAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();
        Actor errorHandler = new Actor(null, -1, Priority.NORM) {
            @Override
            protected void act(Object message) {
                if (message instanceof MediaError) {
                    MediaError me = (MediaError) message;
                    System.err.println(me.message);
                    me.exception.printStackTrace();
                } else {
                    System.err.println(message);
                }
            }
        };
        errorHandler.start();
        Actor recycler = new Actor(errorHandler, -1, Priority.NORM) {
            @Override
            protected void act(Object message) {
                // do nothing
            }
        };
        recycler.start();
        //AudioRenderer audioRenderer = new AudioRenderer(errorHandler, is.getAudioFormat(), null);
        //audioRenderer.start();
        boolean startedPlayback = false;

        // init buffers
        MediaFrame[] mediaFrames = new MediaFrame[100];
        for (int i = 0; i < mediaFrames.length; i++) {
            mediaFrames[i] = is.createFrame();
        }

        for (int i = 0; i < 100; i++) {

            if (newLocationToSet != -1) {
                double actualPosition = is.setPosition(newLocationToSet);
                System.out.println("Set to " + actualPosition + " seconds");
                newLocationToSet = -1;
                //audioRenderer.send(new ControlCommand(Command.STOP));
                startedPlayback = false;
                //audioRenderer.send(new ControlCommand(Command.FLUSH));
            }

            // preprocess frames
            for (int j = 0; j < 100; j++) {
                is.readFrame(mediaFrames[j]);

                /*// reverse audio
                Buffer buffer = mediaFrames[j].audio.getBuffer();
                int samplesize = audioFormat.getSampleSizeInBits() / 8;
                int offset = buffer.getOffset();
                int length = buffer.getLength();
                byte[] audioData = (byte[]) buffer.getData();
                for (int k = offset; k * 2 < length; k += samplesize) {
                    int k2 = length - k - samplesize;
                    for (int l = 0; l < samplesize; l++) {
                        byte tmp = audioData[k + l];
                        audioData[k + l] = audioData[k2 + l];
                        audioData[k2 + l] = tmp;
                    }
                }*/
            }

            // end of media
            if (mediaFrames[0].isEndOfMedia()) {
                break;
            }

            if (!startedPlayback) {
                //audioRenderer.send(new ControlCommand(Command.START));
                startedPlayback = true;
            }

            // play frames backwards
            int audioFramePos = 0;
            //int audioFramePos = 99;
            int audioPos = 0;
            /*for (int j = 0; j < 100; j++) {
                // TODO insert into media buffer
                CachedFrame cf = new CachedFrame(0, null);
                cf.frame = mediaFrames[j];
                audioRenderer.send(cf);
            }*/

            for (int j = 0; j < 100; j++) {
                //for (int j = 99; j >= 0; j--) {

                // display a video frame
                Graphics g = screen.getGraphics();
                g.drawImage(mediaFrames[j].video.getImage(), 0, 0, width, height, null);

                // fill audio frames into buffer, until it would block
                for (int k = audioFramePos; k < 100; k++) {
                    //for (int k = audioFramePos; k >= 0; k--) {
                    byte[] audioBuffer = mediaFrames[k].audio.getAudioData();
                    int offset = 0 + audioPos;
                    int length = mediaFrames[k].audio.getSize() - audioPos;
                    int available = line.available();
                    boolean onlyPartFree = length > available;
                    if (onlyPartFree) {
                        length = available;
                    }
                    int bytesWritten = 0;
                    if (available != 0) {
                        bytesWritten = line.write(audioBuffer, offset, length);
                        //System.out.println("written " + bytesWritten + " bytes to audio line");
                    }
                    if (onlyPartFree) {
                        if (audioBuffer.length - audioPos > 1) {
                            audioFramePos = k;
                            audioPos = audioPos + bytesWritten;
                        } else {
                            audioFramePos = k + 1;
                            audioPos = 0;
                        }

                        break;
                    }
                    audioPos = 0;
                    audioFramePos = k + 1;
                }
                Thread.sleep(60);
            }
            Thread.sleep(2000);
        }

        // close screen
        frame.setVisible(false);

        //// close sound channel
        //line.close();
        //audioRenderer.stop();

        // close media input stream
        is.close();

        System.exit(0);
    }
}
