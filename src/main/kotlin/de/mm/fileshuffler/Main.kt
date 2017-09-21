package de.mm.fileshuffler

import java.awt.SystemTray
import javax.swing.JOptionPane
import javax.swing.UIManager

/**
 * Created by Max on 13.10.2016.
 */
object Main {

	@JvmStatic
	fun main(args: Array<String>) {

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

		if (!SystemTray.isSupported()) {
			JOptionPane.showMessageDialog(null, "SystemTray is not supported", "Error", JOptionPane.ERROR_MESSAGE)
		} else {
			Chooser()
		}

	}
}
