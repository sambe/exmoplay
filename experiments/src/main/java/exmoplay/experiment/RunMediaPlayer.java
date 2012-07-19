/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 29, 2012
 */
package exmoplay.experiment;

import java.io.File;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import com.xuggle.ferry.JNIMemoryManager;
import com.xuggle.ferry.JNIMemoryManager.MemoryModel;

import exmoplay.access.MediaAnalyzer;
import exmoplay.access.MediaInfo;
import exmoplay.engine.MediaPlayer;
import exmoplay.engine.ui.ControlBar;
import exmoplay.engine.ui.VideoScreen;
import exmoplay.experiment.util.SimplePanelFrame;

public class RunMediaPlayer {

    public static void main(String[] args) throws Exception {
        JNIMemoryManager.setMemoryModel(MemoryModel.NATIVE_BUFFERS);
        MediaPlayer mediaPlayer = new MediaPlayer();

        VideoScreen screen = mediaPlayer.createScreen();
        ControlBar controlBar = mediaPlayer.createControlBar();
        mediaPlayer.setActiveScreen(screen);

        //mediaPlayer.openVideo(new File("/home/sberner/Desktop/10-21.04.09.flv"));
        File videoFile = new File("/home/sberner/media/salsavids/m2/MOV00356.MP4");
        MediaInfo mediaInfo = MediaAnalyzer.analyze(videoFile);
        mediaPlayer.openVideo(videoFile, mediaInfo);

        javax.swing.JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0,gap 0", "[fill]", "[fill][20, fill]"));
        panel.add(screen, "wrap,push");
        panel.add(controlBar, "");
        int width = 640;
        int height = 480 + 20;
        SimplePanelFrame frame = new SimplePanelFrame(panel, width + 20, height + 20 + 65);
    }
}
