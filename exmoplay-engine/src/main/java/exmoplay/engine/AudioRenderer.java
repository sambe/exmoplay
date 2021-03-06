/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 26.12.2010
 */
package exmoplay.engine;

import java.util.LinkedList;
import java.util.Queue;

import javax.sound.sampled.AudioFormat;

import exmoplay.access.AudioBuffer;
import exmoplay.engine.actorframework.Actor;
import exmoplay.engine.audio.AudioProvider;
import exmoplay.engine.audio.AudioProvider.AudioState;
import exmoplay.engine.audio.JoalAudioProvider;
import exmoplay.engine.messages.AudioSyncEvent;
import exmoplay.engine.messages.CachedFrame;
import exmoplay.engine.messages.ControlCommand;
import exmoplay.engine.messages.SetSpeed;

public class AudioRenderer extends Actor {
    private static final boolean DEBUG = false;
    private static final boolean TRACE = false;

    private static final double MIN_SPEED = 0.25;

    private AudioFormat audioFormat;
    private AudioProvider audioProvider;
    private final Queue<CachedFrame> frameQueue = new LinkedList<CachedFrame>();
    private int bufferPos = 0;
    private int bytesToWrite = -1;
    private double speed = 1.0;
    private byte[] speedCorrectedBuffer = null;
    private long speedCorrectedBufferSeqNum = -1;

    private final Actor syncTarget;

    public AudioRenderer(Actor errorHandler, AudioFormat audioFormat, Actor syncTarget) {
        super(errorHandler, 50000000, Priority.MAX);
        this.audioFormat = audioFormat;
        this.syncTarget = syncTarget;
    }

    @Override
    protected void init() throws Exception {
        // create sound channel
        //audioProvider = new JavaSoundAudioProvider();
        audioProvider = new JoalAudioProvider();
        audioProvider.open(audioFormat);

        // add listener for synchronization events
        if (syncTarget != null) {
            audioProvider.addAudioStateListener(new AudioProvider.AudioStateListener() {

                @Override
                public void updateState(AudioState audioState) {
                    if (audioState.equals(AudioState.PLAYING)) {
                        syncTarget.send(new AudioSyncEvent(AudioSyncEvent.Type.START));
                    } else if (audioState.equals(AudioState.STOPPED)) {
                        syncTarget.send(new AudioSyncEvent(AudioSyncEvent.Type.STOP));
                    }
                }
            });
        }
    }

    @Override
    protected void act(Object message) {
        if (message instanceof CachedFrame) {
            //System.out.println("AudioRenderer: receiving media frame");
            handleMediaFrame((CachedFrame) message);
        } else if (message instanceof ControlCommand) {
            handleControlCommand((ControlCommand) message);
        } else if (message instanceof SetSpeed) {
            handleSetSpeed((SetSpeed) message);
        } else {
            throw new IllegalArgumentException("unknown type of message: " + message.getClass().getName());
        }
    }

    private void handleControlCommand(ControlCommand c) {
        if (DEBUG) {
            System.out.println("AudioRenderer: receiving ControlCommand: " + c.command);
        }
        switch (c.command) {
        case START:
            if (DEBUG) {
                System.err.println("AudioRenderer: starting...");
            }
            audioProvider.start();
            break;
        case STOP:
            audioProvider.stop();
            break;
        case FLUSH:
            // recycle all frames in the queue
            for (CachedFrame cf : frameQueue) {
                cf.recycle();
            }
            frameQueue.clear();
            audioProvider.flush();
            break;
        case CLOSE:
            // stops the actor (which will close the line)
            stop();
            break;
        }
    }

    @Override
    protected void idle() {
        //System.out.println("AudioRenderer: started writing to buffer");
        fillAudioBuffer();
        //System.out.println("AudioRenderer: finished writing to buffer");

        super.idle();
    }

    private void checkBufferSize(byte[] frameBuffer, double absSpeed) {
        if (speedCorrectedBuffer == null || speedCorrectedBuffer.length < frameBuffer.length / absSpeed) {
            // calculating with the min possible speed, or lower if actual is lower
            double minSpeed = Math.min(absSpeed, MIN_SPEED);
            int length = (int) Math.ceil(frameBuffer.length / minSpeed);
            speedCorrectedBuffer = new byte[length];
        }

    }

    private int copyToSpeedScaledBuffer(byte[] idata, int idataSize, byte[] odata, long seqNum) {
        //byte[] idata = (byte[]) inputBuffer.array();
        //byte[] odata = (byte[]) outputBuffer.array();

        boolean forward = speed > 0.0;
        double absSpeed = Math.abs(speed);

        // determine length
        int frameBytes = audioFormat.getFrameSize();
        int inputFrames = idataSize / frameBytes;
        long start = (long) Math.floor(seqNum * inputFrames / absSpeed);
        long end = (long) Math.floor((seqNum + 1) * inputFrames / absSpeed);
        int outputFrames = (int) (end - start);
        int outputBytes = outputFrames * frameBytes;

        // scale
        //outputBuffer.position(0);
        //outputBuffer.limit(outputBytes);
        for (int i = 0; i < outputFrames; i++) {
            // currently only picking nearest, could be linear or polynomial interpolation
            int nearestInputFrame = i * inputFrames / outputFrames;
            int ibase = nearestInputFrame * frameBytes;
            int obase = forward ? i * frameBytes : (outputFrames - 1 - i) * frameBytes;
            for (int j = 0; j < frameBytes; j++) {
                odata[obase + j] = idata[ibase + j];
                //outputBuffer.put(obase + j, inputBuffer.get(ibase + j));
            }
        }
        return outputBytes;
    }

    private void fillAudioBuffer() {
        // TODO if speed != 1.0, we need to copy the data in a different fashion (approximation -> later offer different modes of approximation (e.g. nearest, average, polynomial)
        int available;
        while (frameQueue.peek() != null && (available = audioProvider.getWritableBytes()) != 0) {
            if (!audioProvider.isOpen()) {
                throw new IllegalStateException(
                        "Audio Provider was not started, but is already delivered with audio data.");
            }
            AudioBuffer audio = frameQueue.peek().frame.audio;
            byte[] audioBuffer = audio.getAudioData();
            int audioSize = audio.getSize();

            // if speed != 1.0 replace with speed corrected buffer
            checkBufferSize(audioBuffer, Math.abs(speed));
            if (speed == 1.0) {
                bytesToWrite = audioSize;
            } else {
                long seqNum = frameQueue.peek().seqNum;
                if (seqNum != speedCorrectedBufferSeqNum) {
                    bytesToWrite = copyToSpeedScaledBuffer(audioBuffer, audioSize, speedCorrectedBuffer, seqNum);
                    speedCorrectedBufferSeqNum = seqNum;
                }
                audioBuffer = speedCorrectedBuffer;
            }

            // select part of buffer to write to line out
            int offset = bufferPos;
            int length = bytesToWrite - bufferPos;
            if (length > available) {
                length = available;
            }
            //System.out.println("writing: length = " + length + "; bufferPos = " + bufferPos + "; offset = " + offset
            //        + "; buffer.length = " + audioBuffer.getLength() + "; buffer.offset = " + audioBuffer.getOffset());
            //System.out.println("AudioRenderer: before writing to audio provider: available = " + available + "; length = "
            //        + length);
            int bytesWritten = audioProvider.write(audioBuffer, offset, length);
            //System.out.println("AudioRenderer: after else {writing to audio provider: bytesWritten: " + bytesWritten);
            //System.out.println("written " + bytesWritten + " bytes to audio line");

            bufferPos += bytesWritten;
            if (bufferPos == bytesToWrite) {
                //System.err.println("DEBUG: Finished playing audio for frame "
                //        + frameQueue.peek().seqNum);
                CachedFrame frame = frameQueue.peek();
                if (TRACE) {
                    System.out.println("TRACE: " + frame.seqNum + ": " + bytesWritten
                            + " bytes in audio buffer; video buffer eom?: " + frame.frame.isEndOfMedia());
                }
                frameQueue.poll().recycle();
                bufferPos = 0;
            }
        }
    }

    private void handleMediaFrame(CachedFrame frame) {
        if (!frame.frame.isEndOfMedia()) {
            frameQueue.add(frame);
        } else {
            frame.recycle();
        }
    }

    private void handleSetSpeed(SetSpeed message) {
        speed = message.newSpeed;
    }

    @Override
    protected void destruct() {
        if (audioProvider.isOpen()) {
            audioProvider.close();
        }
    }
}
