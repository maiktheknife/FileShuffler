package de.mm.fileshuffler

import javax.swing.*
import java.awt.*

/**
 * Created by Max on 13.10.2016.
 */
object Main {

    @JvmStatic
    fun main(args: Array<String>) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (ignored: ClassNotFoundException) {
        } catch (ignored: InstantiationException) {
        } catch (ignored: IllegalAccessException) {
        } catch (ignored: UnsupportedLookAndFeelException) {
        }

        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "SystemTray is not supported", "Error", JOptionPane.ERROR_MESSAGE)
        } else {
            Chooser()
        }

    }
}
