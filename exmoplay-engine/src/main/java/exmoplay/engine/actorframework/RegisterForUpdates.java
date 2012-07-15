/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.07.2011
 */
package exmoplay.engine.actorframework;

public class RegisterForUpdates {

    public final Class messageType;
    public final MessageSendable receiver;

    public RegisterForUpdates(Class messageType, MessageSendable receiver) {
        super();
        this.messageType = messageType;
        this.receiver = receiver;
    }
}
