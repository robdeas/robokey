package tech.robd.robokey.events

/**
 * Enum representing the sources of events in the RoboKey system.
 *
 * Each enum constant defines a specific origin for events, allowing the system
 * to identify and handle events based on their source. This categorization
 * can help with filtering, logging, and debugging event flows.
 *
 * Usage:
 * - Enables the event system to distinguish events by their source.
 * - Useful in scenarios where certain events should be handled differently
 *   depending on their origin.
 *
 * Example:
 * ```
 * when (eventSource) {
 *     EventSourceActor.WEB -> // Handle user-originated WEB events
 *     EventSourceActor.ERROR_MANAGER -> // Handle system-originated error events
 *     ...
 * }
 * ```
 */
enum class EventSourceActor {
    WEB, // From the REST Controller
    COMMAND_LINE, // from the commandline interface
    FILE_WATCHER, // From the file watcher
    GUI, // from the GUI
    MICROCONTROLLER, // An event generated on the microcontroller NOT part of the flow of another command
    ERROR_MANAGER, // some error that cannot be assigned to a parent
    SYSTEM_SETUP, // commands that are issued on system setup
    UNKNOWN, // if an event cannot be categorised
}
