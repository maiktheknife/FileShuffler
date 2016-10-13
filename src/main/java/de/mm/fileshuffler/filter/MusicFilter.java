package de.mm.fileshuffler.filter;

import java.io.File;
import java.io.FileFilter;

public class MusicFilter implements FileFilter {

	@Override
	public boolean accept(File path) {
		
		if (path.isDirectory()) {
			return true;
		}
		
		String[] parts = path.getPath().split("\\.");
		String ending = parts[parts.length - 1];
		
		switch (ending) {
			case "mp3":
				return true;
			default:
				return false;
		}
	}
	
	@Override
	public String toString() {
		return "MusicFilter";
	}

}
