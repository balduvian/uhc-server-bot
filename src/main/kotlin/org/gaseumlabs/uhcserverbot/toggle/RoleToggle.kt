package org.gaseumlabs.uhcserverbot.toggle

import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role

class RoleToggle(private val role: Role, emote: Emote) : Toggle(emote) {
	override fun doToggle(guild: Guild, member: Member) {
		if (member.roles.contains(role)) {
			guild.removeRoleFromMember(member, role).queue()
		} else {
			guild.addRoleToMember(member, role).queue()
		}
	}

	override val messageLine
		get() = "<:${emote.name}:${emote.id}> : <@&${role.id}>"
}
