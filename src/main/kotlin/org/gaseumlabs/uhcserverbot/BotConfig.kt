package org.gaseumlabs.uhcserverbot

import java.io.File

class BotConfig(
	val token: String,
	var guildId: String,
	var toggleChannelId: String,
) {
	companion object {
		const val BOT_FILE_NAME = "./bot.properties"

		val propertyNames = arrayOf(
			"token",
			"guildId",
			"toggleChannelId",
		)

		fun fromFile(filename: String): BotConfig? {
			val file = File(filename)

			val lines = file.readLines()

			val propertyValues = Array<String?>(propertyNames.size) { null }

			for (line in lines) {
				val eqIndex = line.indexOf('=')
				if (eqIndex == -1) continue

				val namePart = line.substring(0, eqIndex).trim()

				val propertyIndex = propertyNames.indexOfFirst { propertyName ->
					propertyName.equals(namePart, true)
				}
				if (propertyIndex == -1) continue

				propertyValues[propertyIndex] = line.substring(eqIndex + 1).trim()
			}

			if (propertyValues.any { it == null }) return null

			return BotConfig(
				propertyValues[0]!!,
				propertyValues[1]!!,
				propertyValues[2]!!,
			)
		}
	}
}
