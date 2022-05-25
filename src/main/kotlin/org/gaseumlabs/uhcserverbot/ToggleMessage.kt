package org.gaseumlabs.uhcserverbot

import net.dv8tion.jda.api.entities.*
import org.gaseumlabs.uhcserverbot.toggle.Toggle
import java.io.File
import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter

class ToggleMessage(
	private val dataName: String,
	private val channel: TextChannel,
	private val messageText: String,
	private val reactToggles: ArrayList<Toggle>
) {
	var message: Message? = null
		private set

	private val filename: String
		get() = "data/$dataName.txt"

	fun doToggle(emote: Emote, guild: Guild, member: Member) {
		reactToggles.find { reactToggle -> reactToggle.emote.idLong == emote.idLong }
			?.doToggle(guild, member)
	}

	fun generateMessage() {
		channel.sendMessage(generateMessageText(messageText)).queue { newMessage: Message ->
			linkReactions(newMessage, reactToggles)
			message = newMessage

			saveMessageData()
		}
	}

	private fun generateMessageText(header: String): String {
		return reactToggles.fold("$header\n") { acc, reactToggle ->
			"${acc}${reactToggle.messageLine}\n"
		}
	}

	private fun updateMessage(originalMessage: Message) {
		originalMessage.editMessage(generateMessageText(messageText)).queue()
		linkReactions(originalMessage, reactToggles)

		message = originalMessage
	}

	private fun linkReactions(message: Message, reactToggles: ArrayList<Toggle>) {
		message.clearReactions().queue {
			reactToggles.forEach { reactToggle ->
				message.addReaction(reactToggle.emote).queue()
			}
		}
	}

	/**
	 * read from disk the last stored messageID that the bot created
	 * getMessage function holds null when no message existed
	 */
	private fun readMessageData(getMessage: (Message?) -> Unit) {
		val file = File(filename)
		if (!file.exists()) return getMessage(null)

		val reader = BufferedReader(FileReader(file))
		val messageID = reader.readLine()
		reader.close()

		if (messageID == NO_DATA) return getMessage(null)

		channel.retrieveMessageById(messageID).queue(getMessage) { getMessage(null) }
	}

	/**
	 * save to disk what readMessageData reads
	 */
	private fun saveMessageData() {
		val writer = FileWriter(File(filename))
		val writeMessage = message

		writer.write(writeMessage?.id ?: NO_DATA)
		writer.close()
	}

	companion object {
		private const val NO_DATA = "NULL"

		fun setupDir() {
			val dataDir = File("data")
			if (!dataDir.exists()) dataDir.mkdir()
		}
	}

	init {
		readMessageData { retrieved: Message? ->
			if (retrieved == null) {
				generateMessage()
			} else {
				updateMessage(retrieved)
			}
		}
	}
}
