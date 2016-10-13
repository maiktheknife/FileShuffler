package de.mm.fileshuffler;

import de.mm.fileshuffler.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;

public class Chooser {
	private static final Logger LOGGER = LoggerFactory.getLogger(Chooser.class);

	private File path;
	private FileFilter filter;
	private List<File> content;
	private boolean isScanningNecessary;
	private TrayIcon trayIcon;
	
	public Chooser() {
		this.path = new File(System.getProperty("user.dir"));
		this.filter = new MovieFilter();
		this.isScanningNecessary = true;
		initSystemTray();
	}
	
	private void shuffle(){
		LOGGER.debug("shuffle with scanning: '{}'", isScanningNecessary);
		if (isScanningNecessary) {
			content = getFiles(path, filter);
			isScanningNecessary = false;
		}
		
		if (content.isEmpty()) {
			JOptionPane.showMessageDialog(null, "Nichts passendes gefunden :( (Filter?)", ":(", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		Collections.shuffle(content);
		handleFile(content.get(0));
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
		}else {
			LOGGER.warn("handleFile '{}' konnte nicht geöffnent werden (Desktop not supported", f);
			JOptionPane.showMessageDialog(null, "File:\n" + f + "\n wurde gewürfelt, konnte aber nicht geöffnent werden", "Error", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	/*
	 * Setter
	 */

	private void setPath(File path) {
		LOGGER.debug("setPath to '{}'", path);
		this.path = path;
		this.isScanningNecessary = true;
		displaySystemTrayMessage("Path: " + path.getPath());
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
		SystemTray tray = SystemTray.getSystemTray();
		
		BufferedImage trayIconImage;
		try {
			InputStream s = Thread.currentThread().getContextClassLoader().getResourceAsStream("images/dice.png");
			trayIconImage = ImageIO.read(s);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
		
        PopupMenu popup = new PopupMenu();
        
        MenuItem defaultItem = new MenuItem("Play a new one");
        defaultItem.addActionListener(e -> shuffle());
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> exitProgram());
        
        MenuItem pathChooser = new MenuItem("choose Path");
        pathChooser.addActionListener(e -> {
	        JFileChooser chooser = new JFileChooser(path);
	        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	        int returnVal = chooser.showOpenDialog(null);

            if(returnVal == JFileChooser.APPROVE_OPTION) {
               File f = chooser.getSelectedFile();
               setPath(f);
            }
        });
        
        MenuItem movieItem = new MenuItem("Movies");
		movieItem.addActionListener(e -> setFilter(new MovieFilter()));

		MenuItem musicItem = new MenuItem("Music");
		musicItem.addActionListener(e -> setFilter(new MusicFilter()));

		Menu filterChooser = new Menu("choose FilterTyp");
		filterChooser.add(movieItem);
        filterChooser.add(musicItem);
        
        popup.add(defaultItem);
        popup.addSeparator();
        popup.add(pathChooser);
        popup.add(filterChooser);
        popup.addSeparator();
        popup.add(exitItem);
        
        trayIcon = new TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH), "FileShuffler", popup);
        
        try {
            tray.add(trayIcon);
            displaySystemTrayMessage("ready for orders");
        } catch (AWTException ignored) {}

	}

	private void displaySystemTrayMessage(String message){
		LOGGER.debug("displaySystemTrayMessage: '{}'", message);
		if (trayIcon != null) {
			trayIcon.displayMessage("FileShuffler", message, TrayIcon.MessageType.INFO);
		}
	}

	private void clearSystemTray(){
		LOGGER.debug("clearSystemTray");
		if (!SystemTray.isSupported()) {
			return;
		}
		SystemTray.getSystemTray().remove(trayIcon);
	}

	private void exitProgram() {
		LOGGER.debug("exitProgram");
		clearSystemTray();
		System.exit(0);
	}

}