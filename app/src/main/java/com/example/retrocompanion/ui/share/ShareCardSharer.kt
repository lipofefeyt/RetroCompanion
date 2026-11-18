package com.example.retrocompanion.ui.share

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Renders a card to a PNG in the app cache and hands it to the Android share sheet. */
object ShareCardSharer {

    fun authority(context: Context) = "${context.packageName}.shareprovider"

    /**
     * @throws java.io.IOException (or other exceptions) if the image can't be created or written;
     * callers should catch and tell the user. A missing or failed icon is not an error: the
     * card is drawn with a placeholder tile.
     */
    suspend fun share(context: Context, content: ShareCardContent, palette: ShareCardPalette) {
        val appContext = context.applicationContext
        val file = withContext(Dispatchers.IO) {
            val icon = loadIcon(appContext, content.iconUrl)
            val bitmap = ShareCardRenderer.render(content, palette, icon)
            try {
                val dir = ShareFiles.prepareDir(appContext.cacheDir)
                ShareFiles.newCardFile(dir).also { writePng(bitmap, it) }
            } finally {
                bitmap.recycle()
            }
        }

        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share").apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun writePng(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw java.io.IOException("Could not encode the share image.")
            }
        }
    }

    /** Loads the icon as a software bitmap (so it can be drawn on a Canvas), or null on any failure. */
    private suspend fun loadIcon(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        return try {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            val result = context.imageLoader.execute(request) as? SuccessResult
            (result?.drawable as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {
            null
        }
    }
}
