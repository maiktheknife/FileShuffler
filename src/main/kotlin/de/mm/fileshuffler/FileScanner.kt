package de.mm.fileshuffler

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.util.*

/**
 * Created by Max on 05.08.2017.
 */
object FileScanner {

	private val LOGGER = LoggerFactory.getLogger(FileScanner::class.java)

	fun getFiles(paths: Map<File, Boolean>, filter: FileFilter): Queue<File> {
		LOGGER.debug("getFilesRec from {}", paths)
		val files = paths.keys
				.filter { paths[it]!! }
				.flatMap { getFilesRec(it, filter) }
		Collections.shuffle(files)
		return LinkedList(files)
	}

	private fun getFilesRec(path: File, filter: FileFilter): List<File> {
		LOGGER.debug("getFilesRec from {}", path.absolutePath)

		val files = path.listFiles(filter)
		if (files != null) {
			val (a, b) = files.partition { it.isDirectory }
			return a.flatMap { getFilesRec(it, filter) }
					.plus(b)
		} else {
			LOGGER.info("Path $path not available")
			return listOf()
		}
	}

}