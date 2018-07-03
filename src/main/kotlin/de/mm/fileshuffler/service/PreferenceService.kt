package de.mm.fileshuffler.service

import java.io.FileWriter
import java.util.*

/**
 * Created by Max on 09.05.2017.
 */
class PreferenceService {
	private val properties = Properties().apply {
		load(Thread.currentThread().contextClassLoader.getResourceAsStream(PREFERENCE_FILE_NAME))
	}

	fun storePreferences() {
		properties.store(FileWriter(PREFERENCE_FILE_NAME), null)
	}

	fun getMovieExtensions() = properties.getProperty(PREFERENCE_FILTER_MOVIE, "").split(SPLIT_REGEX)

	fun getMusicExtensions() = properties.getProperty(PREFERENCE_FILTER_MUSIC, "").split(SPLIT_REGEX)

	companion object {
		private val SPLIT_REGEX = "[,;. \t\n]".toRegex()
		private const val PREFERENCE_FILE_NAME = "fileshuffler.properties"
		private const val PREFERENCE_FILTER_MUSIC = "filter.music"
		private const val PREFERENCE_FILTER_MOVIE = "filter.movie"
	}

}