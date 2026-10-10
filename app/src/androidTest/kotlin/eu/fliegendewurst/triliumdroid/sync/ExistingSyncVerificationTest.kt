package eu.fliegendewurst.triliumdroid.sync

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Read-only verification of a user-authorized sync; requires an external hash manifest. */
class ExistingSyncVerificationTest {
	@Test
	fun verifiesLargestAttachmentsAndCounts() {
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		val context = instrumentation.targetContext
		val manifestPath = InstrumentationRegistry.getArguments().getString("manifest")
		org.junit.Assume.assumeNotNull(manifestPath)
		val manifest = JSONObject(File(manifestPath!!).readText())
		val db = SQLiteDatabase.openDatabase(context.getDatabasePath("Document.db").path, null, SQLiteDatabase.OPEN_READONLY)
		try {
			val expected = manifest.getJSONArray("blobs")
			for (i in 0 until expected.length()) {
				val blob = expected.getJSONObject(i)
				val id = blob.getString("blobId")
				val size = blob.getLong("size")
				db.rawQuery("SELECT length(content) FROM blobs WHERE blobId=?", arrayOf(id)).use {
					assertTrue("Missing large blob", it.moveToFirst())
					assertEquals(size, it.getLong(0))
				}
				val digest = MessageDigest.getInstance("SHA-256")
				var offset = 1L
				while (offset <= size) {
					db.rawQuery("SELECT substr(content, ?, 1048576) FROM blobs WHERE blobId=?", arrayOf(offset.toString(), id)).use {
						assertTrue(it.moveToFirst())
						val bytes = it.getBlob(0)
						assertTrue(bytes.isNotEmpty())
						digest.update(bytes)
						offset += bytes.size
					}
				}
				assertEquals(blob.getString("sha256"), digest.digest().joinToString("") { "%02x".format(it) })
				instrumentation.sendStatus(0, Bundle().apply { putString("stream", "Verified attachment: $size bytes, SHA-256 matched\n") })
			}
			for (table in listOf("notes", "blobs")) {
				db.rawQuery("SELECT count(*) FROM $table", null).use {
					it.moveToFirst()
					assertEquals("$table count", manifest.getJSONObject("counts").getInt(table), it.getInt(0))
				}
			}
		} finally {
			db.close()
		}
	}
}
