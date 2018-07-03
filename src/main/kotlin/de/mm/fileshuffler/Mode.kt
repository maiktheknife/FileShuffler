package de.mm.fileshuffler

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileFilter
import java.util.*

/**
 * Created by Max on 05.08.2017.
 */
enum class Mode {

	RANDOM {
		override fun init(paths: Map<File, Boolean>, filter: FileFilter) {
			queue = FileScanner.getFiles(paths, filter)
		}

		override fun reload(paths: Map<File, Boolean>, filter: FileFilter) {
			init(paths, filter)
		}

		override fun other() = QUEUE

		override fun shutdown() {}
	},

	QUEUE {
		private val ENCODING = "UTF-8"
		private val QUEUE_FILE_NAME = "fileshuffler.queue"

		override fun init(paths: Map<File, Boolean>, filter: FileFilter) {
			val file = File(QUEUE_FILE_NAME)
			if (file.exists() && file.canRead()) {
				queue = LinkedList(FileUtils
						.readLines(file, ENCODING)
						.map { File(it) })
				if (queue.isEmpty()) {
					queue = refillQueue(paths, filter)
				}
			} else {
				queue = refillQueue(paths, filter)
			}
			storeQueueFile(queue)
		}

		override fun reload(paths: Map<File, Boolean>, filter: FileFilter) {
			queue = refillQueue(paths, filter)
			storeQueueFile(queue)
		}

		private fun refillQueue(paths: Map<File, Boolean>, filter: FileFilter): Queue<File> {
			return LinkedList(FileScanner.getFiles(paths, filter))
		}

		override fun other() = RANDOM

		override fun shutdown() = storeQueueFile(queue)

		private fun storeQueueFile(queue: Collection<File>) = FileUtils.writeLines(File(QUEUE_FILE_NAME), ENCODING, queue)

	};

	protected var queue: Queue<File> = LinkedList()

	abstract fun init(paths: Map<File, Boolean>, filter: FileFilter)

	abstract fun reload(paths: Map<File, Boolean>, filter: FileFilter)

	abstract fun other(): Mode

	fun pop(): File? = queue.poll()

	abstract fun shutdown()

}