/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 11.09.2011
 */
package exmoplay.engine.messages;

import exmoplay.engine.actorframework.MessageSendable;

public class StatusRequest extends ResponseRequest {

    public final Explicit<Long> position;
    public final Explicit<Long> positionMin;
    public final Explicit<Long> positionMax;
    public final Explicit<Double> speed;
    public final Explicit<Boolean> mute;

    public StatusRequest(MessageSendable responseTo) {
        this(null, null, null, null, null, responseTo);
    }

    public StatusRequest(Long position, MessageSendable responseTo) {
        this(new Explicit<Long>(position), null, null, null, null, responseTo);
    }

    public StatusRequest(Long minPosition, Long maxPosition, MessageSendable responseTo) {
        this(null, new Explicit<Long>(minPosition), new Explicit<Long>(maxPosition), null, null, responseTo);
    }

    public StatusRequest(Long position, Long minPosition, Long maxPosition, MessageSendable responseTo) {
        this(new Explicit<Long>(position), new Explicit<Long>(minPosition), new Explicit<Long>(maxPosition), null,
                null, responseTo);
    }

    public StatusRequest(Boolean mute, MessageSendable responseTo) {
        this(null, null, null, null, new Explicit<Boolean>(mute), responseTo);
    }

    public StatusRequest(Explicit<Long> position, Explicit<Long> positionMin, Explicit<Long> positionMax,
            Explicit<Double> speed, Explicit<Boolean> mute, MessageSendable responseTo) {
        super(responseTo);
        this.position = position;
        this.positionMin = positionMin;
        this.positionMax = positionMax;
        this.speed = speed;
        this.mute = mute;
    }
}
