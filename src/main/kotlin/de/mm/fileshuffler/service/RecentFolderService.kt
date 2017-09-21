package de.mm.fileshuffler.service;

import org.apache.commons.io.FileUtils
import java.io.File

/**
 * Created by Max on 09.05.2017.
 */
object RecentFolderService {

	private const val ENCODING = "UTF-8"
	private const val RECENT_FILES_NAME = "fileshuffler.recent"

	fun getRecentFolders(): Set<File> {
		val file = File(RECENT_FILES_NAME)
		if (file.exists() && file.canRead()) {
			return FileUtils
					.readLines(file, ENCODING)
					.map { File(it) }
					.toSet()
		} else {
			return setOf()
		}
	}

	fun storeRecentFolders(folders: Set<File>) = FileUtils.writeLines(File(RECENT_FILES_NAME), ENCODING, folders)

}
