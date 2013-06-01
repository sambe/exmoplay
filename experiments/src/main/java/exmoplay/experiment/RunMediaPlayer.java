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

        File videoFile = new File("/home/sberner/Desktop/10-21.04.09.flv"); // audio out of sync (600ms ahead)
        //File videoFile = new File("/home/sberner/media/salsavids/m2/MOV00356.MP4"); // audio out of sync (600ms ahead)
        //File videoFile = new File("/home/sberner/media/salsavids/m2/MOV00347.MP4"); // repeating segements of pictures after some time (e.g. 30 seconds)
        //File videoFile = new File("/home/samuel/Desktop/Wildlife.wmv"); // retrieval too slow, otherwise working fine, sync not easily testable
        //File videoFile = new File("/home/samuel/Desktop/test2.mp4");
        //File videoFile = new File("/home/sberner/media/films/clips/GeorgeWBush.avi"); // audio out of sync (2000ms ahead)
        //File videoFile = new File("/home/sberner/media/films/clips/clinton-final-days.mov"); // audio only in fragments, most missing, finished way too early
        //File videoFile = new File("/home/sberner/media/films/clips/BushandBoredKid.avi"); // audio out of sync (2000ms ahead)
        //File videoFile = new File("/home/sberner/media/films/Johnny and Lucy.wmv"); // audio is too quickly finished (5-10% before end), maybe not in sync
        //File videoFile = new File("/home/sberner/media/films/shakira-dont_bother_(at_mtv_ema_2005).mpg"); //does not work, stops processing after very few seconds (MP2_HEADER_MISSING) 
        //File videoFile = new File("/home/sberner/media/films/DJ.Bobo.-.Chihuahua.2002.mpeg"); // audio 700ms ahead, but works well and with that better than VLC for once
        //File videoFile = new File("/home/sberner/media/films/101001_01_EFG_Unternehmensfilm_COM_960x540.wmv");

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
