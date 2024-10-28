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
@file:Suppress("ktlint:standard:no-wildcard-imports")

package tech.robd.robokey.commands

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.reactor.awaitSingleOrNull
import tech.robd.robokey.AppConfig
import tech.robd.robokey.Logable
import tech.robd.robokey.keyboards.ArduinoCommand
import tech.robd.robokey.keyboards.ArduinoService
import tech.robd.robokey.keyboards.KeyboardInterface
import tech.robd.robokey.keyboards.LocalRobotKeyboardService
import tech.robd.robokey.setupLogs

/**
 * The `CommandProcessor` is responsible for managing and processing commands for a keyboard service,
 * whether it's an Arduino-based keyboard or a local robot keyboard.
 *
 * It handles both queued commands (such as sending text or other actions to the keyboard)
 * and immediate commands like "STOP", "RESET", "PAUSE", and "RESUME". Commands can be enqueued, processed, paused, or canceled
 * depending on the state of the system.
 *
 * The class is coroutine-based, using channels to queue commands and a `CoroutineScope` to manage the lifecycle of tasks.
 *
 * @param keyboardService The service for interacting with the keyboard, which must implement the `KeyboardInterface`.
 * @param appConfig The application configuration containing properties like the serial COM port for Arduino communication.
 */
class CommandProcessor(
    internal val keyboardService: KeyboardInterface,
    private val appConfig: AppConfig,
) {
    companion object : Logable {
        private val log = setupLogs
    }

    /**
     * A `Channel` for queuing commands. This channel has an unlimited buffer, meaning commands will be queued until processed.
     */
    private val commandChannel = Channel<String>(Channel.UNLIMITED)

    /**
     * A list of commands that can interrupt queued commands. These commands have higher priority and are processed immediately.
     */
    private val immediateCommands = PriorityKeyboardCommand.getAllTextValues()

    /**
     * A `CoroutineScope` for managing the lifecycle of command processing.
     *
     * The scope uses `Dispatchers.IO` for I/O-bound operations and a `SupervisorJob` to ensure that if one command fails, it does not cancel the entire processing.
     */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Flags for managing pause and stop states
    private var isPaused = false
    private var isStopped = false

    init {
        processCommands()
    }

    /**
     * Enqueues a command to be processed by the command processor.
     *
     * The command is added to the `commandChannel`, which will process the commands sequentially. If the system
     * is paused or stopped, the command will remain in the queue until the system resumes.
     *
     * @param command The command to enqueue for processing.
     */
    suspend fun enqueueCommand(command: String) {
        log.info("Enqueuing command: $command")
        commandChannel.send(command)
    }

    /**
     * Processes commands from the queue in sequence.
     *
     * Commands are taken from the `commandChannel` one by one. If a command is recognized as an immediate command
     * (e.g., "STOP", "RESET"), it will be handled with higher priority. Otherwise, the command is treated as a normal queued command.
     */
    private fun processCommands() {
        scope.launch {
            for (command in commandChannel) {
                log.info("Processing queued command: $command")
                if (immediateCommands.contains(command.uppercase())) {
                    handleImmediateCommand(command)
                } else {
                    handleQueuedCommand(command)
                }
            }
        }
    }

    /**
     * Handles the processing of queued commands, such as sending text to the keyboard.
     *
     * If the system is paused or stopped, the command will be queued but not sent until the system resumes.
     * Commands are assigned timeouts depending on their type (e.g., short commands like `CMD_SET_PRESS_LENGTH` get shorter timeouts).
     *
     * @param command The queued command to be processed.
     */
    private suspend fun handleQueuedCommand(command: String) {
        if (isStopped) {
            log.info("System is stopped. Ignoring command: $command")
            return
        }

        if (isPaused) {
            log.info("System is paused. Command queued but not sent: $command")
            return
        }

        // Assign timeouts based on command type
        val commandTimeout =
            when {
                command.startsWith("CMD_SET_PRESS_LENGTH") -> 1000L
                command.startsWith("lorem") -> 5000L // Longer timeout for large text
                else -> 3000L
            }

        try {
            val arduinoCommand = ArduinoCommand(command, commandTimeout)
            log.debug("Sending command to keyboard with timeout [$commandTimeout]: $command")
            (keyboardService as? ArduinoService)
                ?.sendRawDataToArduino(appConfig.comPort!!, listOf(arduinoCommand))
                ?.awaitSingleOrNull()
            log.info("Command successfully sent: $command")
        } catch (e: Exception) {
            log.warn("Error sending command: ${e.message}")
        }
    }

    /**
     * Handles immediate commands such as "STOP", "RESET", "PAUSE", and "RESUME".
     *
     * These commands interrupt the queued commands and are processed with higher priority. They control the system's state
     * (e.g., stopping the system, pausing commands, or resetting the keyboard service).
     *
     * @param command The immediate command to be processed.
     */
    private suspend fun handleImmediateCommand(command: String) {
        log.info("Processing immediate command: $command")
        when (command.uppercase()) {
            "STOP" -> {
                log.info("Stopping typing and clearing the command queue.")
                setIsStopped(true)
                clearCommandQueue()
                if (keyboardService is ArduinoService) {
                    val stopCommand = ArduinoCommand("CMD:STOP", 1000)
                    keyboardService.sendRawDataToArduino(appConfig.comPort!!, listOf(stopCommand)).awaitSingleOrNull()
                    log.info("Stop command successfully sent to Arduino.")
                    delay(500)
                }
            }

            "RESET" -> {
                when (keyboardService) {
                    is ArduinoService -> {
                        log.info("Sending reset command to Arduino.")
                        try {
                            val resetCommand = ArduinoCommand("CMD:RESET", 1000)
                            keyboardService
                                .sendRawDataToArduino(appConfig.comPort!!, listOf(resetCommand))
                                .awaitSingleOrNull()
                            log.info("Reset command successfully sent to Arduino.")
                        } catch (e: Exception) {
                            log.info("Error sending reset command to Arduino: ${e.message}")
                        }
                    }

                    is LocalRobotKeyboardService -> {
                        log.info("Resetting local robot keyboard.")
                        try {
                            // Call the resetRobot method to create a new Robot instance
                            keyboardService.resetRobot()
                            log.info("Local robot keyboard successfully reset.")
                        } catch (e: Exception) {
                            log.info("Error resetting local robot keyboard: ${e.message}")
                        }
                    }

                    else -> {
                        log.info("Unsupported keyboard service for reset.")
                        setIsStopped(true)
                        clearCommandQueue()
                    }
                }
            }

            "PAUSE" -> {
                log.info("Pausing operation.")
                isPaused = true
                if (keyboardService is ArduinoService) {
                    val pauseCommand = ArduinoCommand("CMD:PAUSE", 1000) // Send pause command to Arduino
                    keyboardService.sendRawDataToArduino(appConfig.comPort!!, listOf(pauseCommand)).awaitSingleOrNull()
                    log.info("Pause command successfully sent to Arduino.")
                }
            }

            "RESUME" -> {
                log.info("Resuming operation.")
                isPaused = false
                setIsStopped(false)
            }
        }
    }

    /**
     * Clears the command queue by discarding all queued commands.
     */
    private fun clearCommandQueue() {
        while (true) {
            val result = commandChannel.tryReceive()
            if (result.isSuccess) {
                val command = result.getOrNull()
                log.info("Clearing command from queue: $command")
            } else {
                break
            }
        }
    }

    /**
     * Sets whether the system is in a stopped state. When stopped, no further commands will be processed.
     *
     * @param stopped If true, the system will stop processing commands.
     */
    private suspend fun setIsStopped(stopped: Boolean) {
        isStopped = stopped
        if (stopped) {
            if (keyboardService is ArduinoService) {
                log.info(
                    "Arduino service needs to stop. (It will only handle priority commands to allow the Arduino to receive the stop command.)",
                )
            }
            keyboardService.stopAndClearQueue()
        } else {
            keyboardService.resume()
        }
    }

    /**
     * Shuts down the `CommandProcessor`, canceling all coroutines and closing the command channel.
     */
    fun shutdown() {
        scope.cancel()
        commandChannel.close()
        log.info("CommandProcessor has been shut down.")
    }

    // Utility functions to check the type of keyboard service in use
    fun isUsingArduino() = keyboardService is ArduinoService

    fun isUsingLocalRobot() = keyboardService is LocalRobotKeyboardService
}
