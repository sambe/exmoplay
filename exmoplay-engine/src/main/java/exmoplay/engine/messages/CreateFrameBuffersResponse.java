/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package exmoplay.engine.messages;

import java.util.Collections;
import java.util.List;

import exmoplay.access.MediaFrame;

public class CreateFrameBuffersResponse {

    public final List<MediaFrame> frameBuffers;

    public CreateFrameBuffersResponse(List<MediaFrame> frameBuffers) {
        this.frameBuffers = Collections.unmodifiableList(frameBuffers);
    }
}
