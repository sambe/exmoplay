/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 07.06.2009
 */
package exmoplay.experiment.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

import javax.swing.JFrame;

@SuppressWarnings("serial")
public class SimplePanelFrame extends JFrame {

    public SimplePanelFrame(Component container, int width, int height) {
        this(container, width, height, true);
    }

    public SimplePanelFrame(final Component container, int width, int height, boolean exitOnClose) {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(container, BorderLayout.CENTER);
        if (exitOnClose)
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        else
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(width, height);
        setVisible(true);
    }
}
