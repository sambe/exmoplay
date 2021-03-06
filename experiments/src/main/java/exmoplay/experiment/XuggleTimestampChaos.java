/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.03.2012
 */
package exmoplay.experiment;

import java.util.LinkedList;
import java.util.Queue;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

public class XuggleTimestampChaos {

    private static int audioIndex;
    private static int videoIndex;

    private static Queue<PacketRecord> audioPackets = new LinkedList<PacketRecord>();
    private static Queue<PacketRecord> videoPackets = new LinkedList<PacketRecord>();

    private static class PacketRecord {
        public final IPacket packet;
        public final long timestamp;
        public final long pts;
        public final long dts;
        public final long position;

        public PacketRecord(IPacket packet, long timestamp, long pts, long dts, long position) {
            this.packet = packet;
            this.timestamp = timestamp;
            this.pts = pts;
            this.dts = dts;
            this.position = position;
        }
    }

    public static void main(String[] args) {
        IContainer container = IContainer.make();

        int resultCode = container.open(args[0], IContainer.Type.READ, null);
        if (resultCode < 0)
            throw new IllegalStateException("error opening file (code = " + resultCode);

        // retrieve some information about the media file
        int numStreams = container.getNumStreams();
        long duration = container.getDuration();

        System.out.println("number of streams in container: " + numStreams);
        System.out.println("duration: " + duration);

        IStream audioStream = null;
        IStream videoStream = null;

        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            System.out.println("Stream " + i + " is of type " + coder.getCodecType());
            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                System.out.println("audio sample rate: " + coder.getSampleRate());
                System.out.println("channels: " + coder.getChannels());
                System.out.println("format: " + coder.getCodec().getLongName());
                audioStream = stream;
                audioIndex = stream.getIndex();
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                System.out.println("width: " + coder.getWidth());
                System.out.println("height: " + coder.getHeight());
                System.out.println("format: " + coder.getCodec().getLongName());
                System.out.println("frame rate: " + coder.getFrameRate());
                videoStream = stream;
                videoIndex = stream.getIndex();
            } else {

            }
        }

        if (audioStream == null)
            throw new IllegalStateException("no audio stream found");
        if (videoStream == null)
            throw new IllegalStateException("no video stream found");

        // retrieve audio & video
        IStreamCoder audioCoder = audioStream.getStreamCoder();
        IStreamCoder videoCoder = videoStream.getStreamCoder();

        int audioStreamOpenResultCode = audioCoder.open();
        if (audioStreamOpenResultCode < 0)
            throw new IllegalStateException("Could not open audio stream (error " + audioStreamOpenResultCode + ")");

        for (int k = 0; k < 10; k++) {
            for (int i = 0; i < 10; i++) {
                readPacket(container);
            }
            if (k % 2 == 0) {
                while (!audioPackets.isEmpty()) {
                    checkPacket(audioPackets.poll());
                }
            } else {
                while (!videoPackets.isEmpty()) {
                    checkPacket(videoPackets.poll());
                }
            }
        }

    }

    private static IPacket localPacket = IPacket.make();

    private static void readPacket(IContainer container) {
        container.readNextPacket(localPacket);
        IPacket packet = IPacket.make(localPacket, true);
        PacketRecord record = new PacketRecord(packet, packet.getTimeStamp(), packet.getPts(), packet.getDts(),
                packet.getPosition());
        if (packet.getStreamIndex() == audioIndex)
            audioPackets.add(record);
        else if (packet.getStreamIndex() == videoIndex)
            videoPackets.add(record);
    }

    private static void checkPacket(PacketRecord r) {
        IPacket p = r.packet;
        long timestamp = p.getTimeStamp();
        long pts = p.getPts();
        long dts = p.getDts();
        long position = p.getPosition();

        if (timestamp != r.timestamp || pts != r.pts || dts != r.dts || position != r.position) {
            System.out.println("Differences detected (now, before): timestamp: " + timestamp + ", " + r.timestamp
                    + "; pts: " + pts + ", " + r.pts + "; dts: " + dts + ", " + r.dts + "; position: " + position
                    + ", " + r.position);
        }
    }
}
