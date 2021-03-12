package org.codeland

import java.io.File
import java.io.BufferedReader
import java.io.FileReader
import java.util.ArrayList

object DataFile {
	data class DataReturn(val roleName: String, val imagePath: String, val color: Int)

	fun getData(directoryPath: String): ArrayList<DataReturn>? {
		/* files required to create RoleToggleData exist in the directory */
		val files = File(directoryPath).listFiles()

		return ArrayList(files.mapNotNull { file ->
			val filename = file.toPath().fileName.toString()

			/* every ___.txt file is a RoleToggleData entry */
			if (filename.endsWith(".txt")) {
				val roleName = filename.substring(0, filename.length - 4)

				/* get the color of the role from the .txt file */
				val reader = BufferedReader(FileReader(file))
				val color = reader.readLine().toInt(16)
				reader.close()

				/* get the associated image from an image file with the same name */
				val imagePath = files.find { file2 ->
					file2.toPath().fileName.toString() == "$roleName.png"
				}?.absolutePath ?: return null

				DataReturn(roleName, imagePath, color)

			} else {
				null
			}
		})
	}
}