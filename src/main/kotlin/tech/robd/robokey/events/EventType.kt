package tech.robd.robokey.events

/**
 * Enum representing various types of events that can occur in the RoboKey application.
 *
 * Each event type signifies a specific action, state change, or step within the application
 * workflow, allowing the system to categorize and handle events appropriately.
 *
 * Usage:
 * - Enables differentiation of events based on their nature or role within the system.
 * - Supports logging, filtering, and handling based on the specific event type.
 *
 * Example:
 * ```
 * when (eventType) {
 *     EventType.COMMAND_SENT -> // Handle command being sent
 *     EventType.KEYBOARD_EVENT -> // Handle keyboard-related event
 *     ...
 * }
 * ```
 */
enum class EventType {
    COMMAND_SENT,
    COMMAND_RECEIVED,
    KEYBOARD_EVENT,
    CLI_COMMAND,
    START_COMMAND_EVENT,
    START_BATCH_EVENT,
    COMMAND_RECOGNIZED, // Command has been parsed and recognised
    DATA_SENT_TO_ARDUINO, // Data has been sent to the Arduino for processing
    DATA_RECEIVED_FROM_ARDUINO,
    ARDUINO_PROCESSING_COMPLETE, // Arduino has completed processing the command
    COMMANDS_QUEUED,
}
