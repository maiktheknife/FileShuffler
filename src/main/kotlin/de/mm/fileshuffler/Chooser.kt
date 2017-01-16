package de.mm.fileshuffler

import de.mm.fileshuffler.filter.MovieFilter
import de.mm.fileshuffler.filter.MusicFilter
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane

class Chooser {

	private val paths: MutableMap<File, Boolean>
	private var filter: FileFilter
	private var content: Queue<File>
	private var isScanningNecessary: Boolean
	private lateinit var trayIcon: TrayIcon
	private lateinit var pathChooser: Menu

	init {
		this.paths = HashMap<File, Boolean>()
		this.paths.put(File(System.getProperty("user.dir")), true)
		this.content = LinkedList()
		this.filter = MovieFilter()
		this.isScanningNecessary = true
		initSystemTray()
	}

	private fun shuffle() {
		LOGGER.debug("shuffle with scanning: '{}'", isScanningNecessary)
		if (isScanningNecessary) {
			val files = scanFolders(paths, filter)
			isScanningNecessary = false
			Collections.shuffle(files)
			content = LinkedList(files)
		}

		if (content.isEmpty()) {
			JOptionPane.showMessageDialog(null, "Nichts passendes gefunden :( (Filter?)", ":(", JOptionPane.INFORMATION_MESSAGE)
		} else {
			handleFile(content.poll())
		}
	}

	private fun scanFolders(paths: Map<File, Boolean>, filter: FileFilter): List<File> {
		val files = ArrayList<File>()
		for (path in paths.keys) {
			if (paths[path]!!) {
				files.addAll(getFiles(path, filter))
			}
		}
		return files
	}

	private fun getFiles(path: File, filter: FileFilter): List<File> {
		LOGGER.debug("getFiles from {}", path.absolutePath)
		val content = ArrayList<File>()
		for (file in path.listFiles(filter)!!) {
			if (file.isDirectory) {
				content.addAll(getFiles(file, filter))
			} else {
				content.add(file)
			}
		}
		return content
	}

	private fun handleFile(f: File) {
		LOGGER.debug("handleFile: '{}'", f.absolutePath)
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(f)
				displaySystemTrayMessage(f.name)
			} catch (e: IOException) {
				LOGGER.error("handleFile '{}' konnte nicht geöffnent werden", e)
				JOptionPane.showMessageDialog(null, "Fehler beim Handeln\n " + e.message, "Error", JOptionPane.ERROR_MESSAGE)
			}

		} else {
			LOGGER.warn("handleFile '{}' konnte nicht geöffnent werden (Desktop not supported", f)
			JOptionPane.showMessageDialog(null, "File:\n$f\n wurde gewürfelt, konnte aber nicht geöffnent werden", "Error", JOptionPane.WARNING_MESSAGE)
		}
	}

	/*
	 * Path
	 */

	private fun addPath() {
		LOGGER.debug("addPath")
		val chooser = JFileChooser(paths.keys.iterator().next())
		chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
		val returnVal = chooser.showOpenDialog(null)
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			val path = chooser.selectedFile
			LOGGER.debug("addPath '{}'", path)
			this.paths.put(path, true)
			this.isScanningNecessary = true
			displaySystemTrayMessage("Path: " + path.path)
			updateChooserIconBoxes()
		}
	}

	private fun updateChooserIconBoxes() {
		LOGGER.debug("updateChooserIconBoxes")
		pathChooser.removeAll()
		for (path in paths.keys) {
			val checkboxMenuItem = CheckboxMenuItem(path.name, paths[path]!!)
			checkboxMenuItem.addItemListener { e -> togglePath(path, checkboxMenuItem.state) }
			pathChooser.add(checkboxMenuItem)
		}
	}

	private fun togglePath(path: File, shouldAdd: Boolean) {
		LOGGER.debug("togglePath {} {}", path, shouldAdd)
		isScanningNecessary = true
		if (shouldAdd) {
			paths.put(path, shouldAdd)
			displaySystemTrayMessage("add path: " + path)
		} else {
			paths.remove(path)
			displaySystemTrayMessage("remove path: " + path)
		}
	}

	private fun setFilter(filter: FileFilter) {
		LOGGER.debug("setFilter to '{}'", filter)
		this.filter = filter
		this.isScanningNecessary = true
		displaySystemTrayMessage("Filter: " + filter.toString())
	}

	/*
	 * SystemTray
	 */

	private fun initSystemTray() {
		LOGGER.debug("initSystemTray")
		val trayIconImage: BufferedImage
		try {
			val s = Thread.currentThread().contextClassLoader.getResourceAsStream("images/dice.png")
			trayIconImage = ImageIO.read(s)
		} catch (e: IOException) {
			LOGGER.error("error while reading dice image", e)
			return
		}

		val trayIconWidth = TrayIcon(trayIconImage).size.width
		val popup = PopupMenu()

		val playItem = MenuItem("Play a new one")
		playItem.addActionListener { e -> shuffle() }

		// path

		val pathAdder = MenuItem("add Path")
		pathAdder.addActionListener { e -> addPath() }

		pathChooser = Menu("choose Path")
		updateChooserIconBoxes()

		// filter

		val movieItem = MenuItem("Movies")
		movieItem.addActionListener { e -> setFilter(MovieFilter()) }

		val musicItem = MenuItem("Music")
		musicItem.addActionListener { e -> setFilter(MusicFilter()) }

		val filterChooser = Menu("choose FilterTyp")
		filterChooser.add(movieItem)
		filterChooser.add(musicItem)

		// exit

		val exitItem = MenuItem("Exit")
		exitItem.addActionListener { e -> exitProgram() }

		popup.add(playItem)
		popup.addSeparator()
		popup.add(pathAdder)
		popup.add(pathChooser)
		popup.add(filterChooser)
		popup.addSeparator()
		popup.add(exitItem)

		trayIcon = TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH), "FileShuffler", popup)
		try {
			SystemTray.getSystemTray().add(trayIcon)
			displaySystemTrayMessage("ready for orders")
		} catch (ignored: AWTException) {
		}

	}

	private fun displaySystemTrayMessage(message: String) {
		LOGGER.debug("displaySystemTrayMessage: '{}'", message)
		trayIcon.displayMessage("FileShuffler", message, TrayIcon.MessageType.INFO)
	}

	private fun clearSystemTray() {
		LOGGER.debug("clearSystemTray")
		SystemTray.getSystemTray().remove(trayIcon)
	}

	/*
	 * exit
	 */

	private fun exitProgram() {
		LOGGER.debug("exitProgram")
		clearSystemTray()
		System.exit(0)
	}

	companion object {
		private val LOGGER = LoggerFactory.getLogger(Chooser::class.java)
	}

}