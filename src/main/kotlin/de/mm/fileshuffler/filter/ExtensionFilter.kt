package de.mm.fileshuffler.filter

import java.io.File
import java.io.FileFilter

class ExtensionFilter(val endings: List<String>) : FileFilter {

	override fun accept(path: File): Boolean {

		if (path.isDirectory) {
			return true
		}

		val parts = path.path.split("\\.".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
		val ending = parts[parts.size - 1]

		return endings.contains(ending)
	}

}
 