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

	fun setNotificationEnabled(value: Boolean) {
		properties.setProperty(PREFERENCE_NOTIFICATION, value.toString())
	}

	fun isNotificationEnabled() = properties.getProperty(PREFERENCE_NOTIFICATION, "true").toBoolean()

	fun getMovieExtensions() = properties.getProperty(PREFERENCE_FILTER_MOVIE, "").split(SPLIT_REGEX.toRegex())

	fun getMusicExtensions() = properties.getProperty(PREFERENCE_FILTER_MUSIC, "").split(SPLIT_REGEX.toRegex())

	companion object {
		private const val SPLIT_REGEX = "[,;. \t\n]"
		private const val PREFERENCE_FILE_NAME = "fileshuffler.properties"
		private const val PREFERENCE_NOTIFICATION = "notification.enabled"
		private const val PREFERENCE_FILTER_MUSIC = "filter.music"
		private const val PREFERENCE_FILTER_MOVIE = "filter.movie"
	}

}