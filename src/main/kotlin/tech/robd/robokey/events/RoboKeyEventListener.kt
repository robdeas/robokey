package tech.robd.robokey.events

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tech.robd.robokey.Logable
import tech.robd.robokey.setupLogs
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Listener for handling `RoboKeyEvent` instances in the RoboKey application.
 *
 * `RoboKeyEventListener` implements `RoboKeyEventListenerInterface` and responds to
 * `RoboKeyEvent` occurrences. By using the `@EventListener` annotation, this class
 * automatically subscribes to events published within the application context, allowing
 * it to process each event as it occurs. This listener prints a message indicating the
 * event type and associated data and is designed to handle event-related logic, such as
 * triggering further actions or updating states.
 *
 * It counts events but does not store the actual events.
 * Note the count will be reset on application startup.
 *
 * Key Annotations:
 * - `@Component`: Registers this listener as a Spring bean, making it available for dependency
 *   injection and event handling within the application context.
 * - `@EventListener`: Declares that the `handleRoboKeyEvent` method should be invoked whenever
 *   a `RoboKeyEvent` is published, allowing for automatic event-driven responses.
 *
 * Example Usage:
 * ```
 * // When a RoboKeyEvent is published, this listener will automatically process it:
 * val event = RoboKeyEvent(...)
 * applicationContext.publishEvent(event) // Triggers handleRoboKeyEvent
 * ```
 *
 * @see RoboKeyEvent
 * @see RoboKeyEventListenerInterface
 */
@Component
class RoboKeyEventListener : RoboKeyEventListenerInterface {
    companion object : Logable {
        private val log = setupLogs
    }

    private val objectMapper = jacksonObjectMapper()

    // Map to store usage count of each command type
    private val commandUsageCount = ConcurrentHashMap<EventCommand, Int>()

    /**
     * Handles a `RoboKeyEvent` instance by responding to the event's type and data.
     *
     * This method is triggered automatically whenever a `RoboKeyEvent` is published in the
     * application context. It logs a message to the console with the event's type and data,
     * and additional production logic can be implemented here to process the event as required.
     *
     * @param event The `RoboKeyEvent` instance containing the type and data for the event.
     */
    @EventListener
    override fun handleRoboKeyEvent(event: RoboKeyEvent) {
        logReceivedMessage("Event occurred of type: ${event.eventType} with data: ${event.eventData}")
        val parentContext = event.parentEvent
        if (parentContext.eventSourceActor != EventSourceActor.MICROCONTROLLER) {
            if (parentContext is EventBatch) {
                parentContext.contexts.forEach {
                    incrementCommandCount(it.eventCommand)
                }
            } else {
                incrementCommandCount(parentContext.eventCommand)
            }
        }
        // Optional: Log the command usage counts for immediate feedback
        println("Current command usage counts: $commandUsageCount")
    }

    /**
     * Logs a received message from the Arduino with an appropriate log level.
     *
     * The method determines the log level by parsing the JSON content of the message.
     * It defaults to DEBUG level if the log level is not specified or is invalid.
     *
     * @param line The message received from the Arduino in JSON format.
     */
    fun logReceivedMessage(line: String) {
        synchronized(System.out as OutputStream) {
            val level = getLogLevelFromJson(line)

            when (level) {
                "DEBUG" -> log.debug(line)
                "INFO" -> log.info(line)
                "ERROR" -> log.error(line)
                else -> log.debug(line) // default if level is missing or invalid
            }
            System.out.flush()
        }
    }

    /**
     * Extracts the log level from a JSON-formatted string.
     *
     * The method reads the JSON content from the provided string and attempts to extract the log level.
     * If the string is not a valid JSON or the log level field is missing, the method returns `null`.
     *
     * @param line The JSON-formatted string from which to extract the log level.
     * @return The extracted log level as a string, or `null` if the log level cannot be determined.
     */
    private fun getLogLevelFromJson(line: String): String? =
        try {
            val jsonNode = objectMapper.readTree(line)
            jsonNode["level"]?.asText()
        } catch (e: Exception) {
            null // return null if not JSON
        }

    private fun incrementCommandCount(command: EventCommand) {
        commandUsageCount.merge(command, 1, Int::plus) // Increment count atomically
    }

    // Method to get the current usage count of each command (useful for logging or analysis)
    fun getCommandUsageCount(): Map<EventCommand, Int> = commandUsageCount
}
