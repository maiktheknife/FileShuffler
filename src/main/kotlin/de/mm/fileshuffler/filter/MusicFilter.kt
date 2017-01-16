package de.mm.fileshuffler.filter

import java.io.File
import java.io.FileFilter

class MusicFilter : FileFilter {

	override fun accept(path: File): Boolean {

		if (path.isDirectory) {
			return true
		}

		val parts = path.path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val ending = parts[parts.size - 1]

		when (ending) {
			"mp3" -> return true
			else -> return false
		}
	}

	override fun toString(): String {
		return "MusicFilter"
	}

}
