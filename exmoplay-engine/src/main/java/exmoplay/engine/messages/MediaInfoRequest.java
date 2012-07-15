/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.03.2011
 */
package exmoplay.engine.messages;

import exmoplay.engine.actorframework.MessageSendable;

public class MediaInfoRequest extends ResponseRequest {

    public MediaInfoRequest(MessageSendable responseTo) {
        super(responseTo);
    }
}
