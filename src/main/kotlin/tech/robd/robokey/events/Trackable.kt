package tech.robd.robokey.events

import tech.robd.robokey.Logable
import tech.robd.robokey.setupLogs

/**
 * Interface for events or objects that can be tracked within the RoboKey system.
 *
 * `Trackable` provides a common structure for tracking event details, requiring implementing
 * classes to define an `uuid` and a `getEventDetails` method for consistent identification.
 * It extends the `Logable` interface to leverage basic logging functionality and includes a
 * `logEventDetails` function to log detailed event information, facilitating monitoring and
 * diagnostics across the application.
 *
 * Key Properties and Methods:
 * - `uuid`: A unique identifier for the trackable event, allowing it to be distinctly referenced
 *   in logs and during processing.
 * - `getEventDetails()`: Abstract method to retrieve a detailed description of the event, supporting
 *   customized details per implementing class.
 * - `logEventDetails()`: Logs the event's details using the application's standard logger, making it
 *   easy to monitor and trace events in the system logs.
 *
 * Example Usage:
 * ```
 * val event = CustomTrackableEvent(...)
 * event.logEventDetails() // Logs details of the trackable event with ID and custom details
 * ```
 */
interface Trackable : Logable {
    val eventId: String

    /**
     * Provides detailed information about the trackable event.
     *
     * Implementing classes should provide a string that describes the event in detail,
     * allowing for comprehensive logging and debugging.
     *
     * @return A string containing the event's detailed description.
     */
    fun getEventDetails(): String

    /**
     * Logs details of the `Trackable` event.
     *
     * This function logs the details of the event using the `setupLogs` logger. It calls
     * `getEventDetails()` to retrieve the event's description and includes both the `uuid`
     * and event details in the log entry. Useful for tracking and auditing purposes.
     */
    fun logEventDetails() {
        val logger: org.slf4j.Logger = setupLogs
        logger.info("Tracking event: ID = $eventId, Details = ${getEventDetails()}")
    }
}
