package de.mm.fileshuffler;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Max on 13.10.2016.
 */
public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {}

        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "SystemTray is not supported", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            new Chooser();
        }

    }
}
