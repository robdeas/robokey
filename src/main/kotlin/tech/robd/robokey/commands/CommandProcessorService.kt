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
package tech.robd.robokey.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import tech.robd.robokey.AppConfig
import tech.robd.robokey.Logable
import tech.robd.robokey.keyboards.ArduinoService
import tech.robd.robokey.keyboards.KeyboardInterface
import tech.robd.robokey.keyboards.LocalRobotKeyboardService
import tech.robd.robokey.setupLogs

/**
 * CommandProcessorService manages command processors for both the local keyboard and the Arduino,
 * providing methods to enqueue commands like stopping, resetting, and resuming typing.
 * It also dynamically selects the appropriate processor based on the application mode.
 *
 * @param arduinoService The service for interacting with the Arduino keyboard.
 * @param localRobotKeyboardService The service for interacting with the local robot keyboard.
 * @param appConfig The application configuration which contains mode and other settings.
 */
@Service
class CommandProcessorService(
    private val arduinoService: ArduinoService?,
    private val localRobotKeyboardService: LocalRobotKeyboardService?,
    private val appConfig: AppConfig,
) {
    companion object : Logable {
        private val log = setupLogs
    }

    // Lazy initialisation of keyboardService to ensure it's set up after Spring components are ready
    private val keyboardService by lazy { getCommandProcessor() }

    // Command processor for local keyboard
    private val localCommandProcessor =
        localRobotKeyboardService?.let {
            CommandProcessor(
                keyboardService = it,
                appConfig = appConfig,
            )
        }

    // Command processor for Arduino
    private val arduinoCommandProcessor =
        arduinoService?.let {
            CommandProcessor(
                keyboardService = it,
                appConfig = appConfig,
            )
        }

    // Coroutine scope for command processing, tied to the service's lifecycle
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Enqueue a stop command to the local keyboard processor to stop typing and clear any command buffers.
     */
    fun stopTyping() {
        scope.launch {
            try {
                keyboardService.enqueueCommand("STOP")
            } catch (e: Exception) {
                log.info("Error while enqueuing STOP command: ${e.message}")
            }
        }
    }

    /**
     * Enqueue a pause command to the local keyboard processor to stop typing but not clear any command buffers.
     */
    fun pauseTyping() {
        scope.launch {
            try {
                keyboardService.enqueueCommand("PAUSE")
            } catch (e: Exception) {
                log.info("Error while enqueuing PAUSE command: ${e.message}")
            }
        }
    }

    /**
     * Enqueue a reset command to the Arduino or LocalRobot processor to reset the Arduino keyboard or reinitialize the awt robot.
     */
    fun resetKeyboardProcessor() {
        scope.launch {
            try {
                if (keyboardService.isUsingArduino() || keyboardService.isUsingLocalRobot()) {
                    keyboardService.enqueueCommand("RESET")
                }
            } catch (e: Exception) {
                log.info("Error while enqueuing RESET command: ${e.message}")
            }
        }
    }

    /**
     * Enqueue a resume command to the local keyboard processor to resume typing.
     */
    fun resumeTyping() {
        scope.launch {
            try {
                keyboardService.enqueueCommand("RESUME")
            } catch (e: Exception) {
                log.info("Error while enqueuing RESUME command: ${e.message}")
            }
        }
    }

    /**
     * Dynamically selects and returns the appropriate CommandProcessor based on the current app mode.
     *
     * The modes are determined by the appConfig.mode:
     * - "HARDWARE" or "PHYSICAL": Returns the Arduino processor.
     * - "VIRTUAL" or "LOCAL": Returns the local keyboard processor.
     * - "DUMMY" or "LOG": Returns a dummy processor for logging or testing.
     *
     * @return The selected CommandProcessor based on the current mode.
     */
    fun getCommandProcessor(): CommandProcessor =
        when (appConfig.mode.uppercase()) {
            "HARDWARE", "PHYSICAL" ->
                arduinoCommandProcessor
                    ?: throw IllegalStateException("ArduinoService is not available.")

            "VIRTUAL", "LOCAL" ->
                localCommandProcessor
                    ?: throw IllegalStateException("LocalRobotKeyboardService is not available.")

            "DUMMY", "LOG" -> {
                // Return a dummy processor for logging or testing purposes
                val dummyKeyboardService =
                    object : KeyboardInterface {
                        override suspend fun sendCommandData(commands: List<String>) {
                            log.info("DummyKeyboardService received commands: $commands")
                        }

                        override suspend fun stopAndClearQueue() {
                            log.info("DummyKeyboardService stopAndClearQueue called")
                        }

                        override suspend fun resume() {
                            log.info("DummyKeyboardService resume called")
                        }
                    }
                CommandProcessor(
                    keyboardService = dummyKeyboardService,
                    appConfig = appConfig,
                )
            }

            else -> throw IllegalStateException("Unknown config mode: ${appConfig.mode}")
        }
}
