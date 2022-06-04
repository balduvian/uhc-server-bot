package org.gaseumlabs.uhcserverbot

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigInteger
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

fun main() {
	println("UHC Server Bot starting!")

	val botConfig = BotConfig.fromFile(BotConfig.BOT_FILE_NAME)
		?: return println("Invalid config file")

	//UHCServerBot(botConfig)
//
	//val scanner = Scanner(System.`in`)
	//while (true) {
	//	val line = scanner.nextLine()
//
	//	if (line == "restart") {
	//		val runningDir = System.getProperty("user.dir")
	//		Runtime.getRuntime().exec("cmd.exe /c start cmd.exe /k \"cd \"$runningDir\" & run.bat\"")
	//		exitProcess(0)
	//	}
	//}

	val gson = GsonBuilder().create()

	fun validate(ed: String?, timestamp: String?): Boolean {
		if (ed == null || timestamp == null) return false

		val spec = X509EncodedKeySpec(BigInteger(botConfig.publicKey, 16).toByteArray(), "Ed25519")
		val kf = KeyFactory.getInstance("EdDSA")
		val key = kf.generatePublic(spec)

		val sig = Signature.getInstance("Ed25519")
		sig.initVerify(key)
		return sig.verify(BigInteger(ed, 16).toByteArray())
	}

	embeddedServer(Netty, port = 12312) {
		routing {
			get("/") {
				call.respondText("Hello, world!")
			}
			post("/api/interactions") {
				if (!validate(call.request.header("X-Signature-Ed25519"), call.request.header("X-Signature-Timestamp"))) {
					return@post call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
				}

				val json = JsonParser.parseReader(call.receiveStream().reader())

				if (json !is JsonObject) return@post call.respondText("bad", status = HttpStatusCode.BadRequest)
				val type = json.get("type").asInt

				when (type) {
					1 -> {
						val obj = JsonObject()
						obj.addProperty("type", 1)
						call.respondText(gson.toJson(obj))
					}
					else -> {
						call.respondText("bad", status = HttpStatusCode.BadRequest)
					}
				}
			}
		}
	}.start(wait = true)
}
