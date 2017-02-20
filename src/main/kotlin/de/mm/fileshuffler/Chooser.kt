package de.mm.fileshuffler

import de.mm.fileshuffler.filter.ExtensionFilter
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileFilter
import java.io.FileWriter
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
	private var isNotificationEnabled: Boolean
	private val movieFilter: ExtensionFilter
	private val musicFilter: ExtensionFilter
	private lateinit var trayIcon: TrayIcon
	private lateinit var pathChooser: Menu

	init {
		this.paths = mutableMapOf()
		this.paths.put(File(System.getProperty("user.dir")), true)
		this.content = LinkedList()
		this.isScanningNecessary = true

		val preferences = loadPreferences()
		this.isNotificationEnabled = preferences.getProperty(PREFERENCE_NOTIFICATION, "true").toBoolean()
		this.movieFilter = ExtensionFilter(preferences.getProperty(PREFERENCE_FILTER_MOVIE, "").split(",;. \t\n"))
		this.musicFilter = ExtensionFilter(preferences.getProperty(PREFERENCE_FILTER_MUSIC, "").split(",;. \t\n"))
		this.filter = this.movieFilter
		initSystemTray()
	}

	private fun loadPreferences(): Properties {
		return Properties().apply {
			load(Thread.currentThread().contextClassLoader.getResourceAsStream(PREFERENCE_PATH))
		}
	}

	/*
	 * scan
	 */

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
		return paths.keys
				.filter { paths[it]!! }
				.flatMap { getFiles(it, filter) }
	}

	private fun getFiles(path: File, filter: FileFilter): List<File> {
		LOGGER.debug("getFiles from {}", path.absolutePath)
		val (a, b) = path.listFiles(filter)
				.partition { it.isDirectory }

		return a.flatMap { getFiles(it, filter) }
				.plus(b)
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

		val playItem = MenuItem("Play a new one").apply {
			addActionListener { shuffle() }
		}

		// path

		val pathAdder = MenuItem("add Path").apply {
			addActionListener { addPath() }
		}

		pathChooser = Menu("choose Path")
		updateChooserIconBoxes()

		// filter

		val movieItem = MenuItem("Movies").apply {
			addActionListener { setFilter(movieFilter) }
		}

		val musicItem = MenuItem("Music").apply {
			addActionListener { setFilter(musicFilter) }
		}

		val filterChooser = Menu("choose FilterTyp").apply {
			add(movieItem)
			add(musicItem)
		}

		// settings

		val notificationItem = CheckboxMenuItem("enable messages", isNotificationEnabled).apply {
			addItemListener { e -> toggleNotification(state) }
		}

		val settingsChooser = Menu("Settings").apply {
			add(notificationItem)
		}

		// exit

		val exitItem = MenuItem("Exit").apply {
			addActionListener { exitProgram() }
		}

		popup.apply {
			add(playItem)
			addSeparator()
			add(pathAdder)
			add(pathChooser)
			add(filterChooser)
			addSeparator()
			add(settingsChooser)
			addSeparator()
			add(exitItem)
		}

		trayIcon = TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH), "FileShuffler", popup)
		try {
			SystemTray.getSystemTray().add(trayIcon)
			displaySystemTrayMessage("ready for orders")
		} catch (ignored: AWTException) {
		}

	}

	private fun toggleNotification(state: Boolean) {
		LOGGER.debug("toggleNotification: '{}'", state)
		isNotificationEnabled = state
		loadPreferences().apply {
			setProperty(PREFERENCE_NOTIFICATION, state.toString())
			store(FileWriter(PREFERENCE_PATH), "")
		}
	}

	private fun displaySystemTrayMessage(message: String) {
		LOGGER.debug("displaySystemTrayMessage: '{}' {}", message, isNotificationEnabled)
		if (isNotificationEnabled) {
			trayIcon.displayMessage("FileShuffler", message, TrayIcon.MessageType.INFO)
		}
	}

	/*
	 * exit
	 */

	private fun exitProgram() {
		LOGGER.debug("exitProgram")
		SystemTray.getSystemTray().remove(trayIcon)
		System.exit(0)
	}

	companion object {
		private val LOGGER = LoggerFactory.getLogger(Chooser::class.java)
		private val PREFERENCE_PATH = "preferences.properties"
		private val PREFERENCE_NOTIFICATION = "notification.enabled"
		private val PREFERENCE_FILTER_MUSIC = "filter.music"
		private val PREFERENCE_FILTER_MOVIE = "filter.movie"
	}

}