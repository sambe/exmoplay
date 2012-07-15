/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package exmoplay.engine.messages;

import exmoplay.engine.actorframework.MessageSendable;

public abstract class ResponseRequest {

    public final MessageSendable responseTo;

    public ResponseRequest(MessageSendable responseTo) {
        this.responseTo = responseTo;
    }

}
