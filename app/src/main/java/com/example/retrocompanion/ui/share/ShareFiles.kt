package com.example.retrocompanion.ui.share

import java.io.File

/**
 * Where shared card images live: one dedicated folder inside the app cache, which is also the
 * only path the FileProvider exposes (see res/xml/share_file_paths.xml). Plain java.io so it's
 * unit testable.
 */
object ShareFiles {
    const val DIR_NAME = "share_cards"

    /** A card must outlive the share sheet, so only files older than this are deleted. */
    const val MAX_AGE_MILLIS = 60L * 60 * 1000

    /** Creates the share folder if needed and removes cards left over from earlier shares. */
    fun prepareDir(cacheDir: File, nowMillis: Long = System.currentTimeMillis()): File {
        val dir = File(cacheDir, DIR_NAME)
        dir.mkdirs()
        deleteStale(dir, nowMillis)
        return dir
    }

    /** Deletes the PNG cards in [dir] last modified more than [maxAgeMillis] ago. */
    fun deleteStale(dir: File, nowMillis: Long, maxAgeMillis: Long = MAX_AGE_MILLIS): Int {
        var deleted = 0
        dir.listFiles { file -> file.isFile && file.extension == "png" }?.forEach { file ->
            if (nowMillis - file.lastModified() > maxAgeMillis && file.delete()) deleted++
        }
        return deleted
    }

    fun newCardFile(dir: File, nowMillis: Long = System.currentTimeMillis()): File =
        File(dir, "card-$nowMillis.png")
}
