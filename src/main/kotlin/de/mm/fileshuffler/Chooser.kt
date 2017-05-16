package de.mm.fileshuffler

import de.mm.fileshuffler.filter.ExtensionFilter
import de.mm.fileshuffler.service.FolderService
import de.mm.fileshuffler.service.PreferenceService
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
	private val paths: MutableMap<File, Boolean> = mutableMapOf()
	private var filter: FileFilter
	private var content: Queue<File>
	private var isScanningNecessary: Boolean
	private var isNotificationEnabled: Boolean
	private val movieFilter: ExtensionFilter
	private val musicFilter: ExtensionFilter
	private val preferenceService = PreferenceService()
	private lateinit var trayIcon: TrayIcon
	private lateinit var pathChooser: Menu

	init {
		this.isNotificationEnabled = preferenceService.isNotificationEnabled()
		this.movieFilter = ExtensionFilter(preferenceService.getMovieExtensions())
		this.musicFilter = ExtensionFilter(preferenceService.getMusicExtensions())
		this.filter = this.movieFilter
		this.paths.putAll(FolderService.loadFolders().map { Pair(it, true) })
		this.content = LinkedList()
		this.isScanningNecessary = true

		initSystemTray()
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

		val files = path.listFiles(filter)
		if (files != null) {
			val (a, b) = files.partition { it.isDirectory }
			return a.flatMap { getFiles(it, filter) }
					.plus(b)
		} else {
			LOGGER.info("Path $path not available")
			return listOf()
		}
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
		val chooser = JFileChooser()
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
		preferenceService.setNotificationEnabled(state)
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
		preferenceService.storePreferences()
		FolderService.storeFolders(paths.map { it.key }.toSet())
		SystemTray.getSystemTray().remove(trayIcon)
		System.exit(0)
	}

	companion object {
		private val LOGGER = LoggerFactory.getLogger(Chooser::class.java)
	}

}