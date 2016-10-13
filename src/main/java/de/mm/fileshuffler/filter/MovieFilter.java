package de.mm.fileshuffler.filter;

import java.io.File;
import java.io.FileFilter;

public class MovieFilter implements FileFilter {
	
	@Override
	public boolean accept(File path) {
		
		if (path.isDirectory()) {
			return true;
		}
		
		String[] parts = path.getPath().split("\\.");
		String ending = parts[parts.length - 1];
		
		switch (ending) {
			case "mkv":
			case "mp4":
			case "avi":
			case "wmv":
			case "mov":
			case "mpg":
			case "mpg2":
				return true;
			default:
				return false;
		}
	}
	
	@Override
	public String toString() {
		return "MovieFilter";
	}
}
 