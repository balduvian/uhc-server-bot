package org.codeland

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.*
import java.io.File
import java.io.BufferedReader
import java.io.FileReader
import org.codeland.toggle.RoleToggle
import java.awt.Color
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.codeland.toggle.Toggle
import java.lang.Exception

class UHCServerBot(
	val token: String,
	val guildID: String,
	val channelID: String,
	val iconPath: String

) : ListenerAdapter() {
	val jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MEMBERS).build()
	val selfID: Long = jda.selfUser.idLong

	lateinit var toggleMessages: ArrayList<ToggleMessage>

	init {
		jda.addEventListener(this)
	}

	companion object {
		fun createUHCServerBot(dataFilename: String): UHCServerBot {
			val file = File(dataFilename)
			val reader = BufferedReader(FileReader(file))

			val token = reader.readLine()
			val guildID = reader.readLine()
			val channelID = reader.readLine()
			val iconPath = reader.readLine()

			reader.close()

			return UHCServerBot(token, guildID, channelID, iconPath)
		}
	}

	private fun setupRoleToggle(guild: Guild, data: DataFile.DataReturn): RoleToggle {
		val roleList = guild.getRolesByName(data.roleName, false)
		val role = if (roleList.isEmpty()) {
			guild.createRole().setColor(Color(data.color)).setName(data.roleName).complete()
		} else {
			roleList[0]
		}

		val emoteList = guild.getEmotesByName(data.roleName, false)
		val emote = if (emoteList.isEmpty()) {
			guild.createEmote(data.roleName, Icon.from(File(data.imagePath))).complete()
		} else {
			emoteList[0]
		}

		return RoleToggle(role, emote)
	}

	fun setActivity(activityMessage: String) {
		jda.presence.activity = Activity.playing(activityMessage)
	}

	override fun onReady(event: ReadyEvent) {
		/* set up the bot's appearance */
		jda.selfUser.manager.setAvatar(Icon.from(File(iconPath))).complete()
		setActivity("UHC Server")

		/* setup the toggle messages */
		val guild = jda.getGuildById(guildID) ?: throw Exception("No guild by ID $guildID")
		val toggleChannel = guild.getTextChannelById(channelID) ?: throw Exception("No channel by ID $channelID")

		ToggleMessage.setupDir()

		val toggles = ArrayList(DataFile.getData("roles")?.map { data ->
			setupRoleToggle(guild, data) as Toggle
		})

		toggleMessages = arrayListOf(
			ToggleMessage("uhcRoles", toggleChannel, "React to this message to toggle your roles", toggles)
		)
	}

	/**
	 * gets a toggle message by the id provided
	 */
	private fun findToggleMessage(id: Long): ToggleMessage? {
		return toggleMessages.find { toggleMessage ->
			toggleMessage.message?.idLong == id
		}
	}

	override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
		val currentMessage = findToggleMessage(event.messageIdLong) ?: return
		val user = event.user
		/* don't do anything when bot is adding initial reactions */
		if (user.idLong == selfID) return

		/* reset the reaction for this user */
		event.reaction.removeReaction(event.user).queue()
		if (!user.isBot) currentMessage.doToggle(event.reactionEmote.emote, event.guild, event.member)
	}

	override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
		/* regenerate the message if it was deleted */
		findToggleMessage(event.messageIdLong)?.generateMessage()
	}

	override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
		val memberRoles = event.guild.getRolesByName("member", true)
		if (memberRoles.isEmpty()) return println("Member role does not exist")

		event.guild.addRoleToMember(event.member, memberRoles[0]).queue()
	}
}
