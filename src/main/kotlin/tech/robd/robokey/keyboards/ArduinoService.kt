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

package tech.robd.robokey.keyboards

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import purejavacomm.CommPortIdentifier
import purejavacomm.SerialPort
import reactor.core.publisher.Mono
import tech.robd.robokey.AppConfig
import tech.robd.robokey.Logable
import tech.robd.robokey.Utils
import tech.robd.robokey.commands.PriorityKeyboardCommand
import tech.robd.robokey.events.*
import tech.robd.robokey.setupLogs
import java.io.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeoutException

/**
 * Service responsible for communicating with the Arduino via a serial port.
 *
 * The `ArduinoService` manages the opening and closing of the serial connection, sending commands to the Arduino,
 * and reading responses from it. It uses coroutines for asynchronous command processing and handles states such
 * as pause, stop, and busy to ensure proper synchronisation when interacting with the Arduino.
 *
 * @param appConfig The application configuration that provides settings such as the COM port and keyboard behaviour.
 */
@Service
class ArduinoService(
    val appConfig: AppConfig,
    private val eventsProvider: EventsProvider,
) : KeyboardInterface {
    companion object : Logable {
        private val log = setupLogs
    }

    private val objectMapper = jacksonObjectMapper()

    private var commPort: SerialPort? = null
    private var writeableSerialPort: OutputStream? = null
    private var readableSerialPort: InputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isPaused = false
    private var isStopped = false
    private var isBusy = false
    private val commandQueue = LinkedBlockingQueue<ArduinoCommand>()

    init {
        // Ensure resources are cleaned up on application shutdown
        Runtime.getRuntime().addShutdownHook(
            Thread {
                closeSerialConnection()
            },
        )
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
                "DEBUG" -> log.debug("Received from Arduino: $line")
                "INFO" -> log.info("Received from Arduino: $line")
                "ERROR" -> log.error("Received from Arduino: $line")
                else -> log.debug("Received from Arduino: $line") // default if level is missing or invalid
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

    /**
     * Sets up the Arduino with default commands based on the application configuration.
     *
     * This method sends a series of setup commands to the Arduino to configure it for keyboard interaction.
     * Commands include setting initial delays, jitter options, and key press durations.
     */
    fun setupArduinoDefaults() {
        val setupCommands = mutableListOf<ArduinoCommand>()
        val eventBatch =
            eventsProvider.createEventBatch(
                eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                batchContents = "SET UP system delay and jitter",
            )
        val comPort = appConfig.comPort
        if (comPort != null) {
//            setupCommands.add(
//                ArduinoCommand(
//                    "CMD_ECHO:OFF",
//                    1000,
//                    eventsProvider.createParentEventContextForBatch(
//                        batch = eventBatch,
//                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
//                        eventCommand = EventCommand.SETUP,
//                    ),
//                ),
//            )
            // Assign timeouts to each command based on expected duration
            setupCommands.add(
                ArduinoCommand(
                    "CMD_SET_INITIAL_DELAY:${appConfig.keyboard.initialDelay}",
                    3000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )
            setupCommands.add(
                ArduinoCommand(
                    if (appConfig.keyboard.press.jitter) "CMD_KEY_JITTER_ON" else "CMD_KEY_JITTER_OFF",
                    2000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )
            setupCommands.add(
                ArduinoCommand(
                    "CMD_SET_PRESS_LENGTH:${appConfig.keyboard.press.time}",
                    1000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )
            setupCommands.add(
                ArduinoCommand(
                    "CMD_SET_KEY_JITTER_VALUE:${appConfig.keyboard.press.jitterMax}",
                    1000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )
            setupCommands.add(
                ArduinoCommand(
                    if (appConfig.keyboard.interval.jitter) "CMD_DELAY_JITTER_ON" else "CMD_DELAY_JITTER_OFF",
                    2000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )
            setupCommands.add(
                ArduinoCommand(
                    "CMD_SET_DELAY:${appConfig.keyboard.interval.time}",
                    2000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )
            setupCommands.add(
                ArduinoCommand(
                    "CMD_SET_DELAY_JITTER_VALUE:${appConfig.keyboard.interval.jitterMax}",
                    2000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )
            setupCommands.add(
                ArduinoCommand(
                    if (appConfig.keyboard.allJitter) "CMD_JITTER_ON" else "CMD_JITTER_OFF",
                    2000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )
            setupCommands.add(
                ArduinoCommand(
                    if (appConfig.keyboard.output) "CMD_OUTPUT_ON" else "CMD_OUTPUT_OFF",
                    1000,
                    eventsProvider.createParentEventContextForBatch(
                        batch = eventBatch,
                        eventSourceActor = EventSourceActor.SYSTEM_SETUP,
                        eventCommand = EventCommand.SETUP,
                    ),
                ),
            )

            queueCommands(setupCommands)
        } else {
            log.info("Error: COM port not configured.")
        }
    }

    /**
     * Opens or retrieves an existing serial connection to the Arduino on the specified port.
     *
     * This method attempts to open the serial port and configure it with the appropriate parameters. If the port
     * is already open, it uses the existing connection. It also starts reading from the serial port asynchronously.
     *
     * @param portName The name of the serial port (e.g. "COM3").
     * @return A `Mono` signaling the success or failure of the connection setup.
     */
    fun getOrOpenSerialConnection(portName: String): Mono<Void> {
        return Mono.create { sink ->
            try {
                val portId = CommPortIdentifier.getPortIdentifier(portName)
                if (portId.isCurrentlyOwned) {
                    if (commPort != null) {
                        log.info("Using existing serial connection.")
                        sink.success()
                    } else {
                        sink.error(IllegalStateException("Port is currently `in` use"))
                        return@create
                    }
                }

                commPort = portId.open("SerialPortApp", 2000) as SerialPort
                commPort?.setSerialPortParams(
                    115200, // Baud rate
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE,
                )

                writeableSerialPort = commPort?.outputStream
                readableSerialPort = commPort?.inputStream

                // Start reading from the serial port asynchronously
                scope.launch {
                    readFromSerialPort()
                }

                setupArduinoDefaults()
                sink.success()
            } catch (e: Exception) {
                sink.error(e)
            }
        }
    }

    /**
     * Sends a list of commands to the Arduino.
     *
     * This method first opens the serial connection if it is not already open, then sends the commands sequentially.
     * If the system is stopped, it only sends priority commands. It also handles waiting for the Arduino to be ready
     * if the busy state is enabled `in` the configuration.
     *
     * @param portName The name of the serial port.
     * @param commands A list of `ArduinoCommand` objects to send.
     * @return A `Mono` signaling the success or failure of the command transmission.
     */
    fun sendRawDataToArduino(
        portName: String,
        commands: List<ArduinoCommand>,
    ): Mono<Void> {
        return getOrOpenSerialConnection(portName).then(
            Mono.create { sink ->
                scope.launch {
                    try {
                        for (cmd in commands) {
                            if (cmd.parentEventContext is EventBatch) {
                                val parentEvent =
                                    eventsProvider.createParentEventContextForBatch(
                                        batch = cmd.parentEventContext,
                                        eventSourceActor = cmd.parentEventContext.eventSourceActor,
                                        eventCommand = Utils.extractCommandFromString(cmd.command),
                                    )

                                eventsProvider.createRoboKeyEvent(
                                    eventSourceActor =
                                        cmd.parentEventContext.eventSourceActor
                                            ?: EventSourceActor.UNKNOWN,
                                    parentEvent = parentEvent,
                                    eventType = EventType.DATA_SENT_TO_ARDUINO,
                                    eventData = "Data sent to Arduino",
                                )
                            }
                            if (isStopped) {
                                if (isPriorityArduinoCommand(cmd.command)) {
                                    writeableSerialPort?.flush()
                                    writeableSerialPort?.write((commandWithParentUuid(cmd) + "\n").toByteArray())
                                    writeableSerialPort?.flush()
                                }
                            } else {
                                // Wait for Arduino to be free if useBusyState is enabled
                                if (appConfig.keyboard.useBusyState) {
                                    withTimeoutOrNull(cmd.timeout) {
                                        while (isBusy) {
                                            log.info("Waiting for Arduino to be ready for command: ${cmd.command}")
                                            delay(100) // Check every 100ms
                                        }
                                    } ?: run {
                                        log.info("Timeout waiting for Arduino to be free for command: ${cmd.command}")
                                        sink.error(
                                            TimeoutException("Timeout waiting for Arduino to become free for command: ${cmd.command}"),
                                        )
                                        return@launch
                                    }
                                }
                                writeableSerialPort?.write((commandWithParentUuid(cmd) + "\n").toByteArray())
                                writeableSerialPort?.flush()
                                delay(1000) // Non-blocking delay between sending lines
                            }
                        }
                        sink.success()
                    } catch (e: Exception) {
                        sink.error(e)
                    }
                }
            },
        )
    }

    private fun commandWithParentUuid(cmd: ArduinoCommand): String {
        val eventId = cmd.parentEventContext?.uuid
        val commandToSend =
            if (!eventId.isNullOrBlank() && eventId.length > 1) {
                "id=$eventId,${cmd.command}"
            } else {
                cmd.command
            }
        return commandToSend
    }

    /**
     * Checks whether the command is a priority command, which should be sent even if the system is stopped.
     *
     * Priority commands include commands like "STOP", "RESET", and "PAUSE".
     *
     * @param line The command line to check.
     * @return True if the command is a priority command; false otherwise.
     */
    private fun isPriorityArduinoCommand(line: String): Boolean {
        val trimmedLine = line.trim().uppercase()
        return trimmedLine in PriorityKeyboardCommand.getAllTextValues() ||
            trimmedLine.startsWith("CMD:") &&
            trimmedLine.substring(4) in PriorityKeyboardCommand.getAllTextValues()
    }

    /**
     * Reads data from the Arduino asynchronously.
     *
     * This method reads lines of data from the Arduino, processes them, and updates the busy status based on the response.
     *
     */
    private suspend fun readFromSerialPort() {
        delay(2000) // Wait for the serial connection to be ready
        val buffer = ByteArray(1024)
        val stringBuilder = StringBuilder()
        try {
            withContext(Dispatchers.IO) {
                val defaultRootParentEvent =
                    eventsProvider.createRootParentEventContext(
                        eventSourceActor = EventSourceActor.MICROCONTROLLER,
                    )
                while (true) {
                    val bytesRead = readableSerialPort?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val response = String(buffer, 0, bytesRead)
                        stringBuilder.append(response)

                        var lineEndIndex: Int
                        while (stringBuilder.indexOf("\n").also { lineEndIndex = it } != -1) {
                            val line = stringBuilder.substring(0, lineEndIndex).trim()
                            stringBuilder.delete(0, lineEndIndex + 1) // Remove the processed line

                            synchronized(System.out) {
                                logReceivedMessage(line)
                                System.out.flush()
                            }

                            // Create an event for the received data,
                            // cant tie it back to the original event
                            // until the whole JSON is read and parsed.
                            eventsProvider.createRoboKeyEvent(
                                eventSourceActor = EventSourceActor.MICROCONTROLLER,
                                parentEvent = defaultRootParentEvent, // TODO Replace to provide a meaningful context
                                eventType = EventType.DATA_RECEIVED_FROM_ARDUINO,
                                eventData = line,
                            )

                            // Parse the response to check the ready status
                            if (line.contains("\"ready\": \"yes\"", ignoreCase = true)) {
                                isBusy = false
                            } else if (line.contains("\"ready\": \"no\"", ignoreCase = true)) {
                                isBusy = true
                            }
                        }
                    } else if (bytesRead == -1) {
                        break // End of stream reached
                    }
                }
            }
        } catch (e: IOException) {
            log.info("IO Error reading from serial port: ${e.message}")
        } catch (e: Exception) {
            log.info("Unexpected error: ${e.message}")
        } finally {
            try {
                withContext(Dispatchers.IO) {
                    readableSerialPort?.close()
                }
            } catch (e: IOException) {
                log.info("Error closing input stream: ${e.message}")
            }
        }
    }

    /**
     * Closes the serial connection and releases resources.
     */
    private fun closeSerialConnection() {
        try {
            writeableSerialPort?.close()
            readableSerialPort?.close()
            commPort?.close()
            scope.cancel() // Cancel the coroutine scope
        } catch (e: Exception) {
            log.info("Error closing serial connection: ${e.message}")
        }
    }

    /**
     * Sends a list of commands to the Arduino for processing.
     *
     * This method converts a list of strings into `ArduinoCommand` objects with default timeouts, then queues them for processing.
     *
     * @param commands A list of command strings to send to the Arduino.
     */
    override suspend fun sendCommandData(
        commands: List<String>,
        commandEventContext: CommandEventContext,
    ) {
        val arduinoCommands = commands.map { ArduinoCommand(it, 3000, commandEventContext) } // Example timeout
        queueCommands(arduinoCommands)
    }

    /**
     * Stops the system and clears the command queue, resetting any ongoing operations.
     */
    override suspend fun stopAndClearQueue() {
        isStopped = true
        commandQueue.clear()
    }

    /**
     * Resumes the system, allowing command processing to continue.
     */
    override suspend fun resume() {
        isStopped = false
    }

    /**
     * Queues a list of `ArduinoCommand` objects for processing.
     *
     * This method adds the commands to the queue and starts processing them asynchronously.
     *
     * @param commands A list of `ArduinoCommand` objects to queue.
     */
    private fun queueCommands(commands: List<ArduinoCommand>) {
        val someParentEventContext: EventGroup =
            commands.firstOrNull()?.parentEventContext ?: eventsProvider.createEventBatch()

        // Publish an event for queued commands
        eventsProvider.createRoboKeyEvent(
            eventSourceActor = someParentEventContext.eventSourceActor ?: EventSourceActor.UNKNOWN,
            eventType = EventType.COMMANDS_QUEUED,
            eventData = "Queued ${commands.size} commands",
            parentEvent = someParentEventContext,
        )
        commandQueue.addAll(commands)
        processCommandQueue()
    }

    /**
     * Processes the command queue by sending each command to the Arduino.
     *
     * This method runs asynchronously and sends commands from the queue one by one, respecting the system's state (e.g., pause, stop, busy).
     */
    private fun processCommandQueue() {
        scope.launch {
            while (commandQueue.isNotEmpty()) {
                val command = commandQueue.poll()
                if (command != null) {
                    sendRawDataToArduino(appConfig.comPort!!, listOf(command)).subscribe()
                }
            }
        }
    }
}
