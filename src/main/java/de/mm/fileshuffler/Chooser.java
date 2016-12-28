package de.mm.fileshuffler;

import de.mm.fileshuffler.filter.MovieFilter;
import de.mm.fileshuffler.filter.MusicFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class Chooser {
	private static final Logger LOGGER = LoggerFactory.getLogger(Chooser.class);

	private Map<File, Boolean> paths;
	private FileFilter filter;
	private Queue<File> content;
	private boolean isScanningNecessary;
	private TrayIcon trayIcon;
	private Menu pathChooser;

	public Chooser() {
		this.paths = new HashMap<>();
		this.paths.put(new File(System.getProperty("user.dir")), true);
		this.filter = new MovieFilter();
		this.isScanningNecessary = true;
		initSystemTray();
	}

	private void shuffle() {
		LOGGER.debug("shuffle with scanning: '{}'", isScanningNecessary);
		if (isScanningNecessary) {
			List<File> files = scanFolders(paths, filter);
			isScanningNecessary = false;
			Collections.shuffle(files);
			content = new LinkedList<>(files);
			handleFile(content.poll());
		}

		if (content.isEmpty()) {
			JOptionPane.showMessageDialog(null, "Nichts passendes gefunden :( (Filter?)", ":(", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private List<File> scanFolders(Map<File, Boolean> paths, FileFilter filter) {
		List<File> files = new ArrayList<>();
		for (File path : paths.keySet()) {
			if (paths.get(path)) {
				files.addAll(getFiles(path, filter));
			}
		}
		return files;
	}

	private List<File> getFiles(File path, FileFilter filter) {
		LOGGER.debug("getFiles from {}", path.getAbsolutePath());
		List<File> content = new ArrayList<>();
		for (File file : path.listFiles(filter)) {
			if (file.isDirectory()) {
				content.addAll(getFiles(file, filter));
			} else {
				content.add(file);
			}
		}
		return content;
	}

	private void handleFile(File f) {
		LOGGER.debug("handleFile: '{}'", f.getAbsolutePath());
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(f);
				displaySystemTrayMessage(f.getName());
			} catch (IOException e) {
				LOGGER.error("handleFile '{}' konnte nicht geöffnent werden", e);
				JOptionPane.showMessageDialog(null, "Fehler beim Handeln\n " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			LOGGER.warn("handleFile '{}' konnte nicht geöffnent werden (Desktop not supported", f);
			JOptionPane.showMessageDialog(null, "File:\n" + f + "\n wurde gewürfelt, konnte aber nicht geöffnent werden", "Error", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	/*
	 * Path
	 */

	private void addPath() {
		LOGGER.debug("addPath");
		JFileChooser chooser = new JFileChooser(paths.keySet().iterator().next());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File path = chooser.getSelectedFile();
			LOGGER.debug("addPath '{}'", path);
			this.paths.put(path, true);
			this.isScanningNecessary = true;
			displaySystemTrayMessage("Path: " + path.getPath());
			updateChooserIconBoxes();
		}
	}

	private void updateChooserIconBoxes() {
		LOGGER.debug("updateChooserIconBoxes");
		pathChooser.removeAll();
		for (File path : paths.keySet()) {
			CheckboxMenuItem checkboxMenuItem = new CheckboxMenuItem(path.getName(), paths.get(path));
			checkboxMenuItem.addItemListener(e -> togglePath(path, checkboxMenuItem.getState()));
			pathChooser.add(checkboxMenuItem);
		}
	}

	private void togglePath(File path, boolean shouldAdd) {
		LOGGER.debug("togglePath {} {}", path, shouldAdd);
		isScanningNecessary = true;
		if (shouldAdd) {
			paths.put(path, shouldAdd);
			displaySystemTrayMessage("add path: " + path);
		} else {
			paths.remove(path);
			displaySystemTrayMessage("remove path: " + path);
		}
	}

	private void setFilter(FileFilter filter) {
		LOGGER.debug("setFilter to '{}'", filter);
		this.filter = filter;
		this.isScanningNecessary = true;
		displaySystemTrayMessage("Filter: " + filter.toString());
	}
	
	/*
	 * SystemTray
	 */

	private void initSystemTray() {
		LOGGER.debug("initSystemTray");
		BufferedImage trayIconImage;
		try {
			InputStream s = Thread.currentThread().getContextClassLoader().getResourceAsStream("images/dice.png");
			trayIconImage = ImageIO.read(s);
		} catch (IOException e) {
			LOGGER.error("error while reading dice image", e);
			return;
		}
		int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
		PopupMenu popup = new PopupMenu();

		MenuItem playItem = new MenuItem("Play a new one");
		playItem.addActionListener(e -> shuffle());

		// path

		MenuItem pathAdder = new MenuItem("add Path");
		pathAdder.addActionListener(e -> addPath());

		pathChooser = new Menu("choose Path");
		updateChooserIconBoxes();

		// filter

		MenuItem movieItem = new MenuItem("Movies");
		movieItem.addActionListener(e -> setFilter(new MovieFilter()));

		MenuItem musicItem = new MenuItem("Music");
		musicItem.addActionListener(e -> setFilter(new MusicFilter()));

		Menu filterChooser = new Menu("choose FilterTyp");
		filterChooser.add(movieItem);
		filterChooser.add(musicItem);

		// exit

		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(e -> exitProgram());

		popup.add(playItem);
		popup.addSeparator();
		popup.add(pathAdder);
		popup.add(pathChooser);
		popup.add(filterChooser);
		popup.addSeparator();
		popup.add(exitItem);

		trayIcon = new TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH), "FileShuffler", popup);
		try {
			SystemTray.getSystemTray().add(trayIcon);
			displaySystemTrayMessage("ready for orders");
		} catch (AWTException ignored) {
		}

	}

	private void displaySystemTrayMessage(String message) {
		LOGGER.debug("displaySystemTrayMessage: '{}'", message);
		if (trayIcon != null) {
			trayIcon.displayMessage("FileShuffler", message, TrayIcon.MessageType.INFO);
		}
	}

	private void clearSystemTray() {
		LOGGER.debug("clearSystemTray");
		if (!SystemTray.isSupported()) {
			return;
		}
		SystemTray.getSystemTray().remove(trayIcon);
	}

	/*
	 * exit
	 */

	private void exitProgram() {
		LOGGER.debug("exitProgram");
		clearSystemTray();
		System.exit(0);
	}

}