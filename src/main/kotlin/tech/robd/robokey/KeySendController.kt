/*
 * Copyright (C) 2024 Rob Deas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.  

 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.  
 */
package tech.robd.robokey

import kotlinx.coroutines.reactor.mono
import org.springframework.core.io.ClassPathResource
import org.springframework.util.FileCopyUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.core.io.Resource
import tech.robd.robokey.commands.CommandProcessorService

/**
 * REST controller that handles incoming HTTP requests related to command processing and system control.
 *
 * This controller provides multiple endpoints for interacting with the system, including sending commands,
 * controlling typing (stop/resume), and resetting the system. It integrates with the `CommandProcessorService`
 * to process commands asynchronously using coroutines and Reactor's `Mono` for reactive responses.
 *
 * @param commandProcessorService The service responsible for managing the command processor and executing commands.
 */
@RestController
class KeySendController(val commandProcessorService: CommandProcessorService) {

    companion object : Logable {
        private val log = setupLogs

        const val PING_RESPONSE = "OK"
    }

    @GetMapping("/ping")
    fun ping(): Mono<String> {
        return Mono.just(PING_RESPONSE)
    }

    @GetMapping("/command")
    fun sendCommand(@RequestParam("text") text: String): Mono<String> {
        return Mono.defer {
            Mono.fromCallable {
                URLDecoder.decode(text, StandardCharsets.UTF_8.toString())
            }.subscribeOn(Schedulers.boundedElastic()) // Run the decoding in a boundedElastic thread
        }.flatMap { decodedText ->
            mono {
                commandProcessorService.getCommandProcessor().enqueueCommand(decodedText)
                "Command sent: $decodedText"
            }
        }
    }


    @GetMapping("/text")
    fun sendText(@RequestParam("text") text: String): Mono<String> {
        return Mono.defer {
            Mono.fromCallable {
                URLDecoder.decode(text, StandardCharsets.UTF_8.toString())
            }.subscribeOn(Schedulers.boundedElastic()) // Run the decoding in a boundedElastic thread
        }.flatMap { decodedText ->
            mono {
                commandProcessorService.getCommandProcessor().enqueueCommand("TEXT:$decodedText")
                "Command sent: $decodedText"
            }
        }
    }

    @GetMapping("/stop")
    fun stopTyping(): Mono<String> {
        return Mono.fromCallable {
            commandProcessorService.stopTyping()
            "Typing stopped."
        }
    }

    @GetMapping("/reset")
    fun resetSystem(): Mono<String> {
        return Mono.fromCallable {
            commandProcessorService.resetKeyboardProcessor()  // This works for both local and Arduino
            "System reset."
        }
    }

    @GetMapping("/resume")
    fun resumeTyping(): Mono<String> {
        return Mono.fromCallable {
            commandProcessorService.resumeTyping()
            "Typing resumed."
        }
    }

    @GetMapping("/help")
    fun help(): Mono<String> {
        return Mono.fromCallable {
            val resource: Resource = ClassPathResource("help.md")
            val markdownContent = resource.inputStream.use { inputStream ->
                InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                    FileCopyUtils.copyToString(reader)
                }
            }
            val parser = Parser.builder().build()
            val document = parser.parse(markdownContent)
            val renderer = HtmlRenderer.builder().build()
            renderer.render(document)
        }.subscribeOn(Schedulers.boundedElastic())
    }

//    // Helper method for decoding URL-encoded text
//    private fun decodeText(text: String): Mono<String> {
//        return Mono.fromCallable {
//            URLDecoder.decode(text, StandardCharsets.UTF_8.toString())
//        }.subscribeOn(Schedulers.boundedElastic())
//    }
}
