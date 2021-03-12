package org.codeland

import java.util.*
import kotlin.system.exitProcess

fun main() {
	println("UHC Server Bot starting!")

	UHCServerBot.createUHCServerBot("bot.txt")

	val scanner = Scanner(System.`in`)
	while (true) {
		val line = scanner.nextLine()

		if (line == "restart") {
			val runningDir = System.getProperty("user.dir")
			Runtime.getRuntime().exec("cmd.exe /c start cmd.exe /k \"cd \"$runningDir\" & run.bat\"")
			exitProcess(67)
		}
	}
}
