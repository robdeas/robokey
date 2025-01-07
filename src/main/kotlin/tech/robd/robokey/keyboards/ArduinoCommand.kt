package tech.robd.robokey.keyboards

import tech.robd.robokey.events.EventGroup

/**
 * Represents a command to be sent to the Arduino, including the command string and a timeout for processing.
 *
 * @param command The command string to be sent to the Arduino.
 * @param timeout The maximum time (in milliseconds) to wait for a response before timing out.
 */
data class ArduinoCommand(
    val command: String,
    val timeout: Long,
    // there is probably a parent, but it might just be a setup command
    val parentEventContext: EventGroup? = null,
)
