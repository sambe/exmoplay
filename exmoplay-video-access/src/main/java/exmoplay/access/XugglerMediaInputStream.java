/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.12.2011
 */
package exmoplay.access;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.sound.sampled.AudioFormat;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IAudioSamples.Format;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IPixelFormat.Type;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IStreamCoder.Direction;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import exmoplay.access.MediaInfo.AudioSamplesInfo;
import exmoplay.access.MediaInfo.VideoPictureInfo;

public class XugglerMediaInputStream {
    private static final boolean DEBUG = true;
    private static final boolean TRACE = true;

    private final File file;

    private final IContainer container;
    private IStream audioStream;
    private IStream videoStream;
    private int audioStreamIndex = -1;
    private int videoStreamIndex = -1;
    private IStreamCoder originalAudioCoder;
    private final IStreamCoder originalVideoCoder;
    private IStreamCoder audioCoder;
    private IStreamCoder videoCoder;
    private final IVideoResampler videoResampler;
    private final IVideoPicture resamplingTempPic;
    private final IConverter javaImageConverter;

    private final PacketSource packetSource;

    private final IAudioSamples samples;

    private final AudioFormat audioFormat;
    private final VideoFormat videoFormat;
    private final double exactAudioFramesSampleNum;
    private final long audioFramesSampleNum;
    private final double frameTime;
    private final double audioPacketTimeStep;

    private long intendedAudioPosition = -1;
    private long intendedVideoPosition = -1;
    private long officialVideoPosition = 0;
    private long targetVideoTimestamp = -1;
    private long targetAudioTimestamp = -1;
    private int targetAudioBytePos = -1;
    private IPacket audioPacketReadPartially = null;
    private IPacket videoPacketReadPartially = null;
    private int audioPacketOffset = 0;
    private int videoPacketOffset = 0;
    private int samplesBytePos = 0;
    private int bytesPerSample;

    private List<IVideoPicture> createdVideoPictures = new ArrayList<IVideoPicture>();

    private final MediaInfo mediaInfo;

    private final IPacket emptyPacket = IPacket.make();

    public static class PacketSource {
        private IContainer container;
        private Queue<IPacket> audioPackets = new LinkedList<IPacket>();
        private Queue<IPacket> videoPackets = new LinkedList<IPacket>();
        private int audioStreamIndex;
        private int videoStreamIndex;

        public PacketSource(IContainer container, int audioStreamIndex, int videoStreamIndex) {
            this.container = container;
            this.audioStreamIndex = audioStreamIndex;
            this.videoStreamIndex = videoStreamIndex;
        }

        public void reset() {
            for (IPacket packet : audioPackets)
                packet.delete();
            audioPackets.clear();
            for (IPacket packet : videoPackets)
                packet.delete();
            videoPackets.clear();
        }

        private IPacket readPacketOfStream(int index, int otherIndex, Queue<IPacket> otherPackets) {
            while (true) {
                IPacket p = IPacket.make();
                if (container.readNextPacket(p) < 0) {
                    p.delete();
                    return null;
                }
                if (p.getStreamIndex() == index) {
                    return p;
                } else if (p.getStreamIndex() == otherIndex) {
                    otherPackets.add(p);
                } else {
                    p.delete();
                }
            }
        }

        /**
         * @return the next audio packet or null if end of file or error
         */
        public IPacket readNextAudioPacket() {
            if (audioPackets.isEmpty())
                return readPacketOfStream(audioStreamIndex, videoStreamIndex, videoPackets);
            else
                return audioPackets.poll();
        }

        /**
         * @return the next video packet or null if end of file or error
         */
        public IPacket readNextVideoPacket() {
            if (videoPackets.isEmpty())
                return readPacketOfStream(videoStreamIndex, audioStreamIndex, audioPackets);
            else
                return videoPackets.poll();
        }

        public IPacket peekNextVideoPacket() {
            if (videoPackets.isEmpty()) {
                IPacket packet = readPacketOfStream(videoStreamIndex, audioStreamIndex, audioPackets);
                videoPackets.add(packet);
                return packet;
            } else
                return videoPackets.peek();
        }
    }

    public XugglerMediaInputStream(File file) throws IOException {
        this(file, MediaAnalyzer.analyze(file));
    }

    public XugglerMediaInputStream(File file, MediaInfo mediaInfo) throws IOException {
        this.file = file;
        this.mediaInfo = mediaInfo;
        // open the media file
        container = IContainer.make();
        int resultCode = container.open(new RandomAccessFile(file, "r"), IContainer.Type.READ, null);
        if (resultCode < 0)
            throw new BadVideoException("error opening container (code = " + resultCode);

        // retrieve some information about the media file
        int numStreams = container.getNumStreams();
        if (DEBUG) {
            long duration = container.getDuration();
            System.out.println("number of streams in container: " + numStreams);
            System.out.println("duration: " + duration);
        }

        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (DEBUG) {
                System.out.println("Stream " + i + " is of type " + coder.getCodecType());
            }
            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                if (DEBUG) {
                    System.out.println("audio sample rate: " + coder.getSampleRate());
                    System.out.println("channels: " + coder.getChannels());
                    System.out.println("format: " + coder.getCodec().getLongName());
                }
                audioStream = stream;
                audioStreamIndex = i;
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                if (DEBUG) {
                    System.out.println("width: " + coder.getWidth());
                    System.out.println("height: " + coder.getHeight());
                    System.out.println("format: " + coder.getCodec().getLongName());
                }
                videoStream = stream;
                videoStreamIndex = i;
            } else {
                // ignore other streams
            }
        }

        // There are videos without audio
        //if (audioStream == null)
        //    throw new BadVideoException("no audio stream found");
        if (videoStream == null)
            throw new BadVideoException("no video stream found");

        if (audioStream != null)
            originalAudioCoder = audioStream.getStreamCoder();
        originalVideoCoder = videoStream.getStreamCoder();

        initDecoders();

        videoFormat = createVideoFormat(originalVideoCoder, mediaInfo.videoFrameRate);
        frameTime = 1000.0 / videoFormat.getFrameRate();

        if (audioStream != null) {
            audioFormat = createJavaSoundFormat(originalAudioCoder);
            exactAudioFramesSampleNum = originalAudioCoder.getSampleRate() / videoFormat.getFrameRate();
            audioFramesSampleNum = (long) Math.ceil(exactAudioFramesSampleNum);
            bytesPerSample = audioFormat.getSampleSizeInBits() / 8 * audioFormat.getChannels();

            int audioPacketSize = mediaInfo.audioFrameSize;
            int audioSampleRate = originalAudioCoder.getSampleRate();
            audioPacketTimeStep = (double) audioPacketSize / bytesPerSample / (double) audioSampleRate;

            if (DEBUG) {
                System.out.println("audio samples per video frame: " + audioFramesSampleNum);
            }
        } else {
            audioFormat = null;
            exactAudioFramesSampleNum = 0;
            audioFramesSampleNum = 0;
            bytesPerSample = 0;
            audioPacketTimeStep = 0.0;
        }

        // initialize resampler
        if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
            // if the video stream is not in BGR24, we'll have to convert it
            videoResampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(),
                    IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
            if (videoResampler == null)
                throw new BadVideoException("Could not create video sampler. Probably unsupported color space");
            resamplingTempPic = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(),
                    videoCoder.getHeight());
        } else {
            videoResampler = null;
            resamplingTempPic = null;
        }

        // initialize java image converter
        javaImageConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24,
                IPixelFormat.Type.BGR24,
                videoCoder.getWidth(), videoCoder.getHeight());

        // initialize buffers
        if (audioStream != null)
            samples = IAudioSamples.make(mediaInfo.audioFrameSize, audioCoder.getChannels());
        else
            samples = null;

        packetSource = new PacketSource(container, audioStreamIndex, videoStream.getIndex());
    }

    private void initDecoders() {
        if (audioCoder != null && audioCoder.isOpen())
            audioCoder.close();
        if (videoCoder != null && videoCoder.isOpen())
            videoCoder.close();

        if (originalAudioCoder != null) {
            audioCoder = IStreamCoder.make(Direction.DECODING, originalAudioCoder);
            int audioStreamOpenResultCode = audioCoder.open(null, null);
            if (audioStreamOpenResultCode < 0)
                throw new IllegalStateException("Could not open audio stream (error " + audioStreamOpenResultCode + ")");
        }

        videoCoder = IStreamCoder.make(Direction.DECODING, originalVideoCoder);
        int videoStreamOpenResultCode = videoCoder.open(null, null);
        if (videoStreamOpenResultCode < 0)
            throw new IllegalStateException("Could not open video stream (error " + videoStreamOpenResultCode + ")");
    }

    private static AudioFormat createJavaSoundFormat(IStreamCoder audioCoder) {
        float sampleRate = (float) audioCoder.getSampleRate();
        int sampleSize = (int) IAudioSamples.findSampleBitDepth(audioCoder.getSampleFormat());
        int channels = audioCoder.getChannels();
        boolean signed = true; /* xuggler defaults to signed 16 bit samples */
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
    }

    private static VideoFormat createVideoFormat(IStreamCoder videoCoder, double frameRate) {
        String encoding = videoCoder.getCodec().getName();
        Dimension size = new Dimension(videoCoder.getWidth(), videoCoder.getHeight());
        // taking frameRate as a parameter (from MediaInfo) because real frame rate is often not exactly this
        //double frameRate = videoCoder.getFrameRate().getValue();
        return new VideoFormat(encoding, size, frameRate);
    }

    public void readFrame(MediaFrame mf) {
        mf.endOfMedia = false;
        long DEBUG_startMillis = 0;
        if (DEBUG)
            DEBUG_startMillis = System.currentTimeMillis();
        int skippedVideoFrames = 0;
        int skippedAudioFrames = 0;

        // loop through the packets
        int audioBytePos = 0;

        boolean audioComplete = false;
        while (!audioComplete && targetAudioTimestamp != -1) {
            IPacket audioPacket = null;
            if (audioPacketReadPartially != null) {
                audioPacket = audioPacketReadPartially;
            } else {
                audioPacket = packetSource.readNextAudioPacket();
                // end of media (or error)
                if (audioPacket == null)
                    break;
            }
            //long packetTimestamp = (long) (audioPacket.getTimeStamp() * mediaInfo.audioPacketTimeBase * 1000.0);

            //System.out.println("original timestamps: packet.pts = " + audioPacket.getPts() + "; samples.pts = "
            //        + samples.getPts() + "; packet.timestamp = " + audioPacket.getTimeStamp()
            //        + "; samples.timestamp = " + samples.getTimeStamp());

            while (audioPacketOffset < audioPacket.getSize()) {
                // only decode packet if previous decoded data has been copied fully
                if (samplesBytePos == 0) {
                    if (samples.isComplete()) {
                        //System.err.println("Samples was already complete!");
                        // only in case it was completed before, it needs to be reset
                        samples.setComplete(false, -1, -1, -1, Format.FMT_NONE, -1);
                    }
                    int bytesDecoded = audioCoder.decodeAudio(samples, audioPacket, audioPacketOffset);
                    if (bytesDecoded < 0) {
                        // TODO recovery doesn't seem to work here, maybe would require to filter out this frame (finding them in MediaAnalyzer)
                        if (bytesDecoded == MediaAnalyzer.MP2_HEADER_MISSING) {
                            audioPacketOffset = audioPacket.getSize(); // skip processing of packet
                            break;
                        }
                        throw new IllegalStateException("could not decode audio. Error code " + bytesDecoded);
                    }
                    audioPacketOffset += bytesDecoded;
                }
                if (audioPacketOffset < audioPacket.getSize()) {
                    audioPacketReadPartially = audioPacket;
                } else {
                    audioPacketReadPartially = null;
                    audioPacketOffset = 0; // setting to 0, because we know it will be copied completely afterwards (TODO we don't really know that isComplete() will return true)
                }

                if (samples.isComplete()) {
                    //long samplesNum = (long) Math.round(samples.getTimeStamp() * mediaInfo.samplesTimeBase
                    //        / audioPacketTimeStep);
                    // first skip audio samples that are before the "intendedPosition" (position that was set with setPosition)
                    if (intendedAudioPosition != -1 && samples.getTimeStamp() < targetAudioTimestamp) {
                        // reset buffer, so it can start filling again (because we're skipping until intendedPosition)
                        //System.out.println("Skipping " + samples.getNumSamples() + " audio samples. ("
                        //        + samplesTimestamp + ")");
                        //samples.setComplete(false, audioFramesSampleNum, audioCoder.getSampleRate(),
                        //        audioCoder.getChannels(), audioCoder.getSampleFormat(), packet.getPts());
                        skippedAudioFrames++;
                        if (audioPacketOffset == 0) // because the packet was already completely processed (see if/else further up)
                            break;
                        continue;
                    }
                    if (intendedAudioPosition != -1) {
                        intendedAudioPosition = -1; // to reset (already skipped frames as necessary) 
                        samplesBytePos = targetAudioBytePos;
                    }
                    // CAUTION packetTimestamp can be wrong (the one from the next packet), because content of samples can be
                    // leftover from a packet that has been completely decoded, but the decoded samples have not all been used yet.
                    //System.out.println("Got " + samples.getNumSamples() + " audio samples! (" + samplesTimestamp + ")");
                    int audioFrameSize = calculateAudioFrameSize(officialVideoPosition);
                    mf.audio.size = audioFrameSize;
                    byte[] dest = mf.audio.audioData;
                    int bytesToCopy = Math.min(samples.getSize() - samplesBytePos, audioFrameSize - audioBytePos);
                    if (bytesToCopy < 0) {
                        System.err.println("ERROR: XugglerMediaInputStream: audio bytesToCopy < 0");
                        System.err.println("samples.getSize() = " + samples.getSize());
                        System.err.println("samplesBytePos = " + samplesBytePos);
                        System.err.println("audioFrameSize = " + audioFrameSize);
                        System.err.println("audioBytePos = " + audioBytePos);
                        bytesToCopy = 0;
                    }
                    try {
                        samples.get(samplesBytePos, dest, audioBytePos, bytesToCopy);
                    } catch (IndexOutOfBoundsException e) {
                        throw e;
                    }
                    samplesBytePos += bytesToCopy;
                    audioBytePos += bytesToCopy;
                    // if "samples" could not be fully copied because the current frame is already filled up
                    if (audioBytePos == audioFrameSize) {
                        audioBytePos = 0; // because it is full now
                        audioComplete = true;
                        break;
                    } else {
                        if (samplesBytePos != samples.getSize())
                            System.err.println("ERROR: internal inconsistency: target array not full, but still uncopied bytes available");
                        samplesBytePos = 0; // because it has been fully copied over
                    }
                }

                // means that the packet was completely read (see if/else further up)
                if (audioPacketOffset == 0)
                    break;
            }
        }

        // clear rest of audio samples in case not enough was written.
        if (!audioComplete) {
            for (int i = audioBytePos; i < mf.audio.size; i++) {
                mf.audio.audioData[i] = 0;
            }
        }

        IVideoPicture picture;
        if (videoResampler == null)
            picture = mf.video.videoPicture;
        else
            picture = resamplingTempPic;

        boolean videoComplete = false;

        while (!videoComplete && targetVideoTimestamp != -1) {
            IPacket videoPacket;
            if (videoPacketReadPartially != null) {
                videoPacket = videoPacketReadPartially;
            } else {
                videoPacket = packetSource.readNextVideoPacket();
                if (videoPacket == null)
                    break;
            }
            if (videoPacket != null) {
                //long packetTimestamp = (long) (videoPacket.getTimeStamp() * videoPacket.getTimeBase().getValue() * 1000.0);

                while (videoPacketOffset < videoPacket.getSize()) {
                    if (picture.isComplete()) {
                        picture.setComplete(false, Type.NONE, -1, -1, -1);
                    }
                    int bytesDecoded = videoCoder.decodeVideo(picture, videoPacket, videoPacketOffset);
                    if (bytesDecoded < 0) {
                        throw new RuntimeException("could not decode video. Error code " + bytesDecoded);
                    }
                    videoPacketOffset += bytesDecoded;

                    if (videoPacketOffset < videoPacket.getSize()) {
                        videoPacketReadPartially = videoPacket;
                    } else {
                        videoPacketReadPartially = null;
                        videoPacketOffset = 0;
                    }

                    /*
                     * Some decoders will consume data in a packet, but will not be able to construct
                     * a full video picture yet.  Therefore you should always check if you
                     * got a complete picture from the decoder
                     */
                    if (picture.isComplete()) {
                        // FIXME for some reason the time base of either packet or picture is wrong by a factor of 10
                        //       picture 1.133333 <-- wrong
                        //       packet 0.135 <-- correct
                        //long pictureTimestamp = (long) (picture.getTimeStamp() * mediaInfo.pictureTimeBase * 1000L);

                        // first skip video frames that are before the "intendedPosition" (position that was set with setPosition)
                        //if (intendedVideoPosition != -1 && pictureTimestamp + frameTime / 2 < intendedVideoPosition) {
                        if (intendedVideoPosition != -1 && picture.getTimeStamp() < targetVideoTimestamp) {
                            // reset buffer (because we're skipping frames until intendedPosition)
                            //System.out.println("Skipping a video picture. (" + pictureTimestamp + ")");
                            //picture.setComplete(false, videoCoder.getPixelType(), videoCoder.getWidth(),
                            //        videoCoder.getHeight(), 0);
                            skippedVideoFrames++;
                            if (videoPacketOffset == 0) // because 0 means that it was processed completely (see if/else further up)
                                break;
                            continue;
                        }
                        intendedVideoPosition = -1;

                        IVideoPicture newPic;
                        /*
                         * If the resampler is not null, that means we didn't get the
                         * video in BGR24 format and
                         * need to convert it into BGR24 format.
                         */
                        if (videoResampler != null) {
                            // we must resample
                            newPic = mf.video.videoPicture;
                            int errorCode = videoResampler.resample(newPic, picture);
                            if (errorCode < 0) {
                                IError error = IError.make(errorCode);
                                throw new RuntimeException("could not resample video from: "
                                        + file + ", due to: " + error.getDescription() + " (" + errorCode + ")");
                            }
                        } else {
                            newPic = picture;
                        }

                        // TODO this security check should not be necessary, remove later
                        //if (newPic.getPixelType() != IPixelFormat.Type.BGR24)
                        //    throw new RuntimeException("could not decode video" +
                        //            " as BGR 24 bit data in: " + file);

                        mf.video.bufferedImage = javaImageConverter.toImage(newPic);

                        //System.out.println("Received a video picture (" + pictureTimestamp + ")");
                        videoComplete = true;
                        break;
                    }

                    // means that the packet was completely read (see if/else further up)
                    if (audioPacketOffset == 0)
                        break;

                }
            }
        }

        if (!videoComplete) {
            mf.endOfMedia = true;
        } else {
            mf.timestamp = mf.video.videoPicture.getTimeStamp() * mediaInfo.pictureTimeBase * 1000L;
            officialVideoPosition = (long) (mf.timestamp + frameTime);
        }

        if (TRACE) {
            System.out.println("TRACE: skipped video frames: " + skippedVideoFrames + "; skipped audio frames: "
                    + skippedAudioFrames);
        }
        if (DEBUG) {
            long DEBUG_endMillis = System.currentTimeMillis();
            System.err.println("DEBUG: read media frame in " + (DEBUG_endMillis - DEBUG_startMillis) + "ms");
        }
    }

    private int calculateAudioFrameSize(long millis) {
        long frameNum = (long) Math.round(millis / 1000.0 * videoFormat.getFrameRate());
        long startAudioBytePos = Math.round(frameNum * exactAudioFramesSampleNum) * bytesPerSample;
        long endAudioBytePos = Math.round((frameNum + 1) * exactAudioFramesSampleNum) * bytesPerSample;
        return (int) (endAudioBytePos - startAudioBytePos);
    }

    public long setPosition(long millis) {
        //TODO possibly implement alternative seeking method that preserves audio packet in front of key video packet (1. search backwards to video keyframe, 2. search backwards to audio keyframe)
        //     maybe check how seeking is done in Xuggler and maybe propose to mailing list (after checking for discussions of course)
        long micros = (millis - (long) frameTime * 3) * 1000; // TODO remove *3 again (was needed to test missing skipping)
        if (micros < 0)
            micros = 0;
        /*IIndexEntry indexEntry = videoStream.findTimeStampEntryInIndex(micros, 0);
        long targetMicros;
        if (indexEntry != null)
            targetMicros = indexEntry.getTimeStamp();
        else
            targetMicros = micros;*/
        long targetMicros = mediaInfo.findRelevantKeyframeTimestamp(micros);

        // seek in microseconds (try to find the last position before or at the specified position)
        long minTimestamp = Math.max(0, targetMicros - 10000000);
        long targetTimestamp = targetMicros;
        long maxTimestamp = targetMicros;
        long statusCode = container.seekKeyFrame(-1, minTimestamp, targetTimestamp, maxTimestamp, 0);
        if (statusCode < 0)
            throw new IllegalStateException("Seek to position " + millis + "ms (actual " + targetTimestamp
                    + " microseconds)failed with code " + statusCode
                    + " in file " + file);
        if (videoPacketReadPartially != null)
            videoPacketReadPartially.delete();
        videoPacketReadPartially = null;
        if (audioPacketReadPartially != null)
            audioPacketReadPartially.delete();
        audioPacketReadPartially = null;
        audioPacketOffset = 0;
        videoPacketOffset = 0;
        //long initialFrame = (long) Math.floor(targetMicros / 1000000.0 * videoFormat.getFrameRate());
        long finalFrame = (long) Math.round(millis / 1000.0 * videoFormat.getFrameRate());
        VideoPictureInfo videoInfo = mediaInfo.findVideoPictureInfoByFrameNumber(finalFrame);
        if (videoInfo != null)
            targetVideoTimestamp = videoInfo.timestamp;
        else
            targetVideoTimestamp = -1;
        //targetVideoFramesToSkip = (int) (finalFrame - initialFrame);
        double exactFrameTime = 1000.0 * finalFrame / videoFormat.getFrameRate();

        packetSource.reset();
        //emptyDecoders(); did not work, now doing the following instead
        initDecoders();
        samplesBytePos = 0;
        intendedVideoPosition = millis;
        officialVideoPosition = millis;
        if (audioStream != null) {
            intendedAudioPosition = millis;
            int audioFrameSize = mediaInfo.audioFrameSize;
            long targetSamplePos = (long) Math.round(finalFrame * exactAudioFramesSampleNum) * bytesPerSample;
            AudioSamplesInfo targetSample = mediaInfo.findAudioSamplesInfoContainingOffset(targetSamplePos);
            targetAudioTimestamp = targetSample.timestamp; //(long) Math.floor(targetSample / audioFrameSize);
            targetAudioBytePos = (int) (targetSamplePos - targetSample.samplesOffset); //(int) (targetSample % audioFrameSize);
            if (targetAudioBytePos > targetSample.samplesLength)
                targetAudioTimestamp = -1;
        }
        return millis;
    }

    private void emptyDecoders() {
        // call decoders with an empty packet until they return an error to pull out any decoded stuff that is still hanging around
        int result = 0;
        int videoFramesPulled = -1;
        IPacket packet = packetSource.peekNextVideoPacket();
        IVideoPicture picture = resamplingTempPic;
        while (result >= 0 && packet.getTimeStamp() != picture.getTimeStamp()) {
            int offset = 0;
            while (result >= 0 && offset < packet.getSize()) {
                result = videoCoder.decodeVideo(resamplingTempPic, packet, offset);
                if (result >= 0)
                    offset += result;
                if (picture.isComplete())
                    break;
            }
            videoFramesPulled++;
        }
        if (DEBUG && videoFramesPulled > 0)
            System.out.println("DEBUG: pulled " + videoFramesPulled + " old video frames out of decoder!");
    }

    public long getPosition() {
        /*if (intendedVideoPosition != -1)
            return intendedVideoPosition;*/
        // This doesn't work because frames are pulled out with some delay from the decoder, so the next packet does not have to correspond to the next frame
        /*IPacket packet = packetSource.peekNextVideoPacket();
        return (long) (packet.getTimeStamp() * mediaInfo.videoPacketTimeBase * 1000.0);*/
        // This didn't work because it is based on the last frame, in obscure cases a frame can be skipped, in which case it shows the wrong timestamp
        return officialVideoPosition;
    }

    public long getDuration() {
        return (long) (mediaInfo.numberOfVideoFrames / videoFormat.getFrameRate() * 1000000L);
    }

    public VideoFormat getVideoFormat() {
        return videoFormat;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public MediaFrame createFrame() {
        AudioBuffer audio = new AudioBuffer();
        audio.audioData = new byte[(int) audioFramesSampleNum * bytesPerSample];
        VideoBuffer video = new VideoBuffer();
        video.videoPicture = IVideoPicture.make(IPixelFormat.Type.BGR24, videoCoder.getWidth(),
                videoCoder.getHeight());
        createdVideoPictures.add(video.videoPicture.copyReference());
        return new MediaFrame(audio, video);
    }

    public void close() {
        audioCoder.close();
        videoCoder.close();
        container.close();
        for (IVideoPicture p : createdVideoPictures)
            p.delete();
    }
}
