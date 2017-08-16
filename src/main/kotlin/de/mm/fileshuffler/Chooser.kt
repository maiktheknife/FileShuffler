package de.mm.fileshuffler

import de.mm.fileshuffler.filter.ExtensionFilter
import de.mm.fileshuffler.service.PreferenceService
import de.mm.fileshuffler.service.RecentFolderService
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileFilter
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane

class Chooser {
	private val paths: MutableMap<File, Boolean> = mutableMapOf()
	private val movieFilter: ExtensionFilter
	private val musicFilter: ExtensionFilter
	private val preferenceService = PreferenceService()
	private var filter: FileFilter
	private var isScanningNecessary: Boolean
	private var isNotificationEnabled: Boolean
	private var mode: Mode
	private var trayIcon: TrayIcon? = null
	private lateinit var pathChoicer: Menu
	private lateinit var modeItem: MenuItem

	init {
		this.isNotificationEnabled = preferenceService.isNotificationEnabled()
		this.movieFilter = ExtensionFilter(preferenceService.getMovieExtensions())
		this.musicFilter = ExtensionFilter(preferenceService.getMusicExtensions())
		this.filter = this.movieFilter
		this.paths.putAll(RecentFolderService.getRecentFolders().map { Pair(it, true) })
		this.isScanningNecessary = true
		this.mode = Mode.RANDOM
		initSystemTray(this.mode)
	}

	/*
	 * scan
	 */

	private fun shuffle() {
		LOGGER.debug("shuffle with scanning: '{}'", isScanningNecessary)
		if (isScanningNecessary) {
			mode.init(paths, filter)
			isScanningNecessary = false
		}

		val file = mode.pop()
		if (file == null) {
			displayDialog("Nichts passendes gefunden :( (Filter?)")
		} else {
			handleFile(file)
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
				displayDialog("Fehler beim Handeln\n: ${e.message}", JOptionPane.ERROR_MESSAGE)
			}
		} else {
			LOGGER.warn("handleFile '{}' konnte nicht geöffnent werden (Desktop not supported", f)
			displayDialog("File:\n$f\n wurde gewürfelt, konnte aber nicht geöffnent werden", JOptionPane.WARNING_MESSAGE)
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
		pathChoicer.removeAll()
		paths.keys.forEach {
			val checkboxMenuItem = CheckboxMenuItem(it.name, paths[it]!!)
			checkboxMenuItem.addItemListener { _ -> togglePath(it, checkboxMenuItem.state) }
			pathChoicer.add(checkboxMenuItem)
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

	private fun initSystemTray(mode: Mode) {
		LOGGER.debug("initSystemTray")

		// play
		val playItem = MenuItem("Play a new one").apply {
			addActionListener { shuffle() }
		}

		// mode
		modeItem = MenuItem().apply {
			addActionListener { toggleMode(mode.other()) }
			label = when (mode) {
				Mode.QUEUE -> "Mode (use random))"
				Mode.RANDOM -> "Mode (use queue)"
			}
		}

		// path
		pathChoicer = Menu("choose")
		updateChooserIconBoxes()

		val pathChooser = Menu("Path").apply {
			add(MenuItem("add").apply {
				addActionListener { addPath() }
			})
			add(pathChoicer)
		}

		// filter
		val filterChooser = Menu("FilterTyp").apply {
			add(MenuItem("Movies").apply {
				addActionListener { setFilter(movieFilter) }
			})
			add(MenuItem("Music").apply {
				addActionListener { setFilter(musicFilter) }
			})
		}

		// queue
		val queueChooser = Menu("Queue").apply {
			add(MenuItem("Reload").apply {
				addActionListener {
					LOGGER.debug("refill queue")
					mode.reload(paths, filter)
				}
			})
		}

		// settings
		val settingsChooser = Menu("Settings").apply {
			add(CheckboxMenuItem("enable messages", isNotificationEnabled).apply {
				addItemListener { _ -> toggleNotification(state) }
			})
		}

		// exit
		val exitItem = MenuItem("Exit").apply {
			addActionListener { exitProgram() }
		}

		val popup = PopupMenu().apply {
			add(playItem)
			addSeparator()
			add(modeItem)
			if (mode === Mode.QUEUE) {
				add(queueChooser)
			}
			add(pathChooser)
			add(filterChooser)
			addSeparator()
			add(settingsChooser)
			addSeparator()
			add(exitItem)
		}

		val diceImage = Thread.currentThread().contextClassLoader.getResourceAsStream("images/dice.png")
		val trayIconImage = ImageIO.read(diceImage)
		if (trayIcon != null){
			SystemTray.getSystemTray().remove(trayIcon)
		}
		trayIcon = TrayIcon(trayIconImage.getScaledInstance(TrayIcon(trayIconImage).size.width, -1, Image.SCALE_SMOOTH), "FileShuffler", popup)
		SystemTray.getSystemTray().add(trayIcon)
		displaySystemTrayMessage("ready for orders")

	}

	private fun toggleNotification(state: Boolean) {
		LOGGER.debug("toggleNotification: '{}'", state)
		isNotificationEnabled = state
		preferenceService.setNotificationEnabled(state)
	}

	private fun toggleMode(mode: Mode) {
		LOGGER.debug("toggleMode to: '{}'", mode)
		this.mode = mode
		modeItem.label = when (mode) {
			Mode.QUEUE -> "Mode (use random))"
			Mode.RANDOM -> "Mode (use queue)"
		}
		initSystemTray(mode)
	}

	private fun displaySystemTrayMessage(message: String) {
		LOGGER.debug("displaySystemTrayMessage: '{}' {}", message, isNotificationEnabled)
		if (isNotificationEnabled) {
			trayIcon?.displayMessage("FileShuffler", message, TrayIcon.MessageType.INFO)
		}
	}

	private fun displayDialog(message: String, mode: Int = JOptionPane.INFORMATION_MESSAGE) {
		LOGGER.debug("displayDialog '{}'", message)
		JOptionPane.showMessageDialog(null, message, "Error", mode)
	}

	/*
	 * exit
	 */

	private fun exitProgram() {
		LOGGER.debug("exitProgram")
		mode.shutdown()
		preferenceService.storePreferences()
		RecentFolderService.storeRecentFolders(paths.keys)
		SystemTray.getSystemTray().remove(trayIcon)
		System.exit(0)
	}

	companion object {
		private val LOGGER = LoggerFactory.getLogger(Chooser::class.java)
	}

}