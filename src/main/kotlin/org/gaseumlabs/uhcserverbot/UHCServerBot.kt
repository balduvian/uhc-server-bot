package org.gaseumlabs.uhcserverbot

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.*
import java.io.File
import org.gaseumlabs.uhcserverbot.toggle.RoleToggle
import java.awt.Color
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.gaseumlabs.uhcserverbot.toggle.Toggle

class UHCServerBot(val config: BotConfig) : ListenerAdapter() {
	val jda = JDABuilder.createDefault(config.token)
		.enableIntents(GatewayIntent.GUILD_MEMBERS)
		.enableIntents(GatewayIntent.GUILD_EMOJIS)
		.enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS)
		.build()
	val selfID: Long = jda.selfUser.idLong

	lateinit var toggleMessages: ArrayList<ToggleMessage>

	init {
		jda.addEventListener(this)
	}

	private fun setupRoleToggle(guild: Guild, data: DataFile.DataReturn): RoleToggle {
		/* emotes cannot have special characters in their name, unlike roles */
		val roleName = data.roleName
		val emoteName = data.roleName.filter { it.lowercaseChar() in 'a'..'z' }

		val existingRole = guild.getRolesByName(data.roleName, false)
		val existingEmote = guild.getEmotesByName(emoteName, false)

		return RoleToggle(
			if (existingRole.isEmpty()) {
				guild.createRole().setColor(Color(data.color)).setName(roleName).complete()
			} else {
				existingRole[0]
			},
			if (existingEmote.isEmpty()) {
				guild.createEmote(emoteName, Icon.from(File(data.imagePath))).complete()
			} else {
				existingEmote[0]
			}
		)
	}

	fun setActivity(activityMessage: String) {
		jda.presence.activity = Activity.playing(activityMessage)
	}

	override fun onReady(event: ReadyEvent) {
		setActivity("UHC Server")

		/* setup the toggle messages */
		val guild = jda.getGuildById(config.guildId)
			?: throw Exception("No guild by ID ${config.guildId}")
		val toggleChannel = guild.getTextChannelById(config.toggleChannelId)
			?: throw Exception("No channel by ID ${config.toggleChannelId}")

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

	override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
		val currentMessage = findToggleMessage(event.messageIdLong) ?: return
		val user = event.user ?: return
		val member = event.member ?: return
		/* don't do anything when bot is adding initial reactions */
		if (user.idLong == selfID) return

		/* reset the reaction for this user */
		event.reaction.removeReaction(user).queue()
		if (!user.isBot) currentMessage.doToggle(event.reactionEmote.emote, event.guild, member)
	}

	override fun onMessageDelete(event: MessageDeleteEvent) {
		/* regenerate the message if it was deleted */
		findToggleMessage(event.messageIdLong)?.generateMessage()
	}

	override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
		val memberRoles = event.guild.getRolesByName("uhc", true)
		if (memberRoles.isEmpty()) return println("uhc role does not exist")

		event.guild.addRoleToMember(event.member, memberRoles[0]).queue()
	}
}
