/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 01.08.2011
 */
package exmoplay.engine.messages;

import exmoplay.engine.Controller;

public class StateUpdate {
    public final Controller.State state;

    public StateUpdate(Controller.State state) {
        this.state = state;
    }
}
