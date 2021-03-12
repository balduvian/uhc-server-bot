package org.codeland.toggle

import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member

abstract class Toggle(val emote: Emote) {
	abstract fun doToggle(guild: Guild, member: Member)

	abstract val messageLine: String
}
