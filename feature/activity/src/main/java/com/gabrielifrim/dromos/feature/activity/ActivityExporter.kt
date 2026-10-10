package com.gabrielifrim.dromos.feature.activity

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.TrackPoint
import com.gabrielifrim.dromos.core.gpx.GpxWriter
import com.gabrielifrim.dromos.core.gpx.KmlWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class ExportFormat(val extension: String, val mimeType: String) {
    GPX("gpx", "application/gpx+xml"),
    KML("kml", "application/vnd.google-earth.kml+xml"),
}

/** « dromos-2026-10-10-0830.gpx » : trié par date dans n'importe quel dossier. */
fun exportFileName(activity: Activity, format: ExportFormat): String =
    "dromos-" + FILE_DATE.format(activity.startedAt.atZone(ZoneId.systemDefault())) + "." + format.extension

private val FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")

/** Écrit une sortie en GPX ou KML, vers un fichier choisi ou un fichier à partager. */
class ActivityExporter @Inject constructor(@ApplicationContext private val context: Context) {

    /** Vers un document créé par le sélecteur de fichiers. */
    suspend fun save(activity: Activity, name: String, points: List<TrackPoint>, format: ExportFormat, uri: Uri) =
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Document inaccessible : $uri")
            write(activity, name, points, format, stream)
        }

    /** Vers le cache, exposé aux autres applications par le FileProvider de ce module. */
    suspend fun shareable(activity: Activity, name: String, points: List<TrackPoint>, format: ExportFormat): Uri =
        withContext(Dispatchers.IO) {
            // Un seul fichier à la fois : le précédent partage est écrasé, rien ne s'accumule.
            val directory = File(context.cacheDir, "exports").apply { deleteRecursively(); mkdirs() }
            val file = File(directory, exportFileName(activity, format))
            write(activity, name, points, format, file.outputStream())
            FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
        }

    private fun write(activity: Activity, name: String, points: List<TrackPoint>, format: ExportFormat, stream: OutputStream) {
        stream.bufferedWriter().use { out ->
            when (format) {
                ExportFormat.GPX -> GpxWriter.write(activity, name, points, out)
                ExportFormat.KML -> KmlWriter.write(activity, name, points, out)
            }
        }
    }
}
