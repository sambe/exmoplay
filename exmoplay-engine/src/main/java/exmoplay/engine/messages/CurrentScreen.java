/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.04.2011
 */
package exmoplay.engine.messages;

import exmoplay.engine.ui.VideoScreen;

public class CurrentScreen {

    public final VideoScreen screen;

    public CurrentScreen(VideoScreen screen) {
        this.screen = screen;
    }
}
