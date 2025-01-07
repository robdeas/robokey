package tech.robd.robokey

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.util.FileCopyUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tech.robd.robokey.commands.CommandProcessorService
import tech.robd.robokey.events.EventCommand
import tech.robd.robokey.events.EventSourceActor
import tech.robd.robokey.events.EventsProvider
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * REST controller that handles incoming HTTP requests related to command processing and system control.
 *
 * This controller provides multiple endpoints for interacting with the system, including sending commands,
 * controlling typing (stop/resume), and resetting the system. It integrates with the `CommandProcessorService`
 * to process commands asynchronously using coroutines.
 *
 * @param commandProcessorService The service responsible for managing the command processor and executing commands.
 */
@RestController
class KeySendController(
    private val commandProcessorService: CommandProcessorService,
    private val eventsProvider: EventsProvider,
) {
    companion object : Logable {
        private val log = setupLogs

        const val PING_RESPONSE = "OK"
    }

    @GetMapping("/ping")
    suspend fun ping(): String {
        val parentEvent =
            eventsProvider.createRootParentEventContext(
                eventSourceActor = EventSourceActor.WEB,
                eventCommand = EventCommand.IS_ALIVE,
            )
        log.info("Ping received, tracking with event ID: ${parentEvent.uuid}")
        return PING_RESPONSE
    }

    @GetMapping("/command")
    suspend fun sendCommand(
        @RequestParam("text") text: String,
    ): String {
        val decodedText = decodeText(text)
        val parentEvent =
            eventsProvider.createRootParentEventContext(
                eventSourceActor = EventSourceActor.WEB,
                eventCommand = Utils.extractCommandFromString(decodedText),
                eventValue = decodedText,
            )
        commandProcessorService.getCommandProcessor().enqueueCommand(decodedText, parentEvent)
        return "Command sent: $decodedText"
    }

    @GetMapping("/text")
    suspend fun sendText(
        @RequestParam("text") text: String,
    ): String {
        val decodedText = decodeText(text)
        val parentEvent =
            eventsProvider.createRootParentEventContext(
                eventSourceActor = EventSourceActor.WEB,
                eventCommand = EventCommand.TEXT,
                eventValue = decodedText,
            )
        commandProcessorService.getCommandProcessor().enqueueCommand("TEXT:$decodedText", parentEvent)
        return "Command sent: $decodedText"
    }

    @GetMapping("/stop")
    suspend fun stopTyping(): String {
        val parentEvent =
            eventsProvider.createRootParentEventContext(
                eventSourceActor = EventSourceActor.WEB,
                eventCommand = EventCommand.STOP_TYPING,
            )
        commandProcessorService.stopTyping(parentEvent)
        return "Typing stopped."
    }

    @GetMapping("/reset")
    suspend fun resetSystem(): String {
        val parentEvent =
            eventsProvider.createRootParentEventContext(
                eventSourceActor = EventSourceActor.WEB,
                eventCommand = EventCommand.RESET_KEYBOARD,
            )
        commandProcessorService.resetKeyboardProcessor(parentEvent)
        return "System reset."
    }

    @GetMapping("/resume")
    suspend fun resumeTyping(): String {
        val parentEvent =
            eventsProvider.createRootParentEventContext(
                eventSourceActor = EventSourceActor.WEB,
                eventCommand = EventCommand.RESUME_TYPING,
            )
        commandProcessorService.resumeTyping(parentEvent)
        return "Typing resumed."
    }

    @GetMapping("/help")
    suspend fun help(): String =
        withContext(Dispatchers.IO) {
            val resource: Resource = ClassPathResource("help.md")
            val markdownContent =
                resource.inputStream.use { inputStream ->
                    InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                        FileCopyUtils.copyToString(reader)
                    }
                }
            val parser = Parser.builder().build()
            val document = parser.parse(markdownContent)
            val renderer = HtmlRenderer.builder().build()
            renderer.render(document)
        }

    private suspend fun decodeText(text: String): String =
        withContext(Dispatchers.IO) {
            URLDecoder.decode(text, StandardCharsets.UTF_8.toString())
        }
}
