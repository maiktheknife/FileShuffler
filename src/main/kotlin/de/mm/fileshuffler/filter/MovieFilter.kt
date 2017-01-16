package de.mm.fileshuffler.filter

import java.io.File
import java.io.FileFilter

class MovieFilter : FileFilter {

	override fun accept(path: File): Boolean {

		if (path.isDirectory) {
			return true
		}

		val parts = path.path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val ending = parts[parts.size - 1]

		when (ending) {
			"mkv", "mp4", "avi", "wmv", "mov", "mpg", "mpg2" -> return true
			else -> return false
		}
	}

	override fun toString(): String {
		return "MovieFilter"
	}
}
 