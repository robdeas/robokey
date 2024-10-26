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

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tech.robd.robokey.*
import tech.robd.robokey.tasks.TaskPoolManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.*
import kotlin.coroutines.coroutineContext

/**
 * FileMonitorCommandHandler monitors a specified file for changes and processes new lines of input as commands.
 * It uses a coroutine to monitor the file and execute commands via the CommandProcessorService.
 *
 * @param taskPoolManager The task pool manager responsible for managing background tasks.
 * @param appConfig The application configuration that includes file watcher settings.
 * @param commandProcessorService The service used to process commands.
 */
@Component
class FileMonitorCommandHandler(
    private val taskPoolManager: TaskPoolManager,
    private val appConfig: AppConfig,
    private val commandProcessorService: CommandProcessorService,
    private val systemExitHandler: SystemExitHandler,
) {
    private val logger = LoggerFactory.getLogger(FileMonitorCommandHandler::class.java)

    companion object : Logable {
        private val log = setupLogs
    }

    private var monitoringJob: Job? = null

    /**
     * Starts the file monitoring process by launching a coroutine that listens for file modifications.
     */
    fun start() {
        val filePath = appConfig.fileWatcher.winFilePath
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                monitorFile(filePath)
            } catch (e: Exception) {
                log.error("Exception occurred while monitoring file", e)
            }
        }
    }

    /**
     * Stops the file monitoring process and shuts down the task pool manager.
     */
    fun stop() {
        monitoringJob?.cancel()
        taskPoolManager.shutdown()
    }

    /**
     * Monitors the specified file for new content and processes each new line as a command.
     *
     * @param filePath The path of the file to monitor.
     * @throws Exception If any I/O or coroutine-related exception occurs.
     */
    @Throws(Exception::class)
    private suspend fun monitorFile(filePath: String) {
        val path = Paths.get(filePath)

        // Ensure the file exists
        try {
            if (Files.notExists(path)) {
                withContext(Dispatchers.IO) {
                    Files.createDirectories(path.parent)
                } // Create parent directories if they don't exist
                withContext(Dispatchers.IO) {
                    Files.createFile(path)
                }
                log.info("File created successfully: $filePath")
            } else {
                log.info("File already exists: $filePath")
            }
        } catch (e: IOException) {
            log.error("An error occurred while creating the file: ${e.message}", e)
        }

        // Determine the appropriate charset for reading the file
        val charset: Charset = findCharset(filePath)
        log.info("Opening file with charset: ${charset.name()}")

        // Open the file and start monitoring for new lines
        withContext(Dispatchers.IO) {
            Files.newInputStream(path)
        }.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, charset)).use { reader ->
                // Skip the existing lines in the file
                while (reader.readLine() != null) {
                    // Just skip over the existing content
                }

                // Set up a WatchService to monitor file modifications
                val watchService = FileSystems.getDefault().newWatchService()
                try {
                    path.parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

                    while (coroutineContext.isActive) {  // Keep monitoring as long as coroutine is active
                        val key = watchService.take()
                        for (event in key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY &&
                                event.context().toString() == path.fileName.toString()
                            ) {
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    log.info("New line added: $line , characters=${Utils.getByteValuesAsHexString(line!!)}")
                                    handleCommand(command = line!!)  // Process the new line as a command
                                }
                            }
                        }
                        key.reset()  // Reset the watch key to continue receiving events
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()  // Handle interruption properly
                    log.error("File monitoring interrupted.", e)
                } finally {
                    watchService.close()
                }
            }
        }
    }

    /**
     * Detects the appropriate charset for reading the specified file. It checks the configuration settings and falls
     * back to a default charset if needed.
     *
     * @param filePath The path of the file to monitor.
     * @return The detected or configured charset to be used for reading the file.
     */
    private fun findCharset(filePath: String): Charset {
        log.info("Detecting charset for file: $filePath")
        val detectedCharset = Utils.detectCharset(filePath)
        log.info("Detected charset: $detectedCharset")

        return runCatching {
            val requestedCharset = appConfig.fileWatcher.charset.uppercase().trim()
            if (requestedCharset != "AUTO") {
                if (Charset.availableCharsets().containsKey(requestedCharset)) {
                    log.info("Using configured charset: $requestedCharset")
                    Charset.forName(requestedCharset)
                } else {
                    log.warn("Requested charset ($requestedCharset) not found. Using default.")
                    getFallbackCharset()
                }
            } else {
                log.info("Using auto-detected charset: $detectedCharset")
                detectedCharset?.let { Charset.forName(it) } ?: getFallbackCharset()
            }
        }.getOrElse {
            log.error("Error determining charset: ${it.message}", it)
            StandardCharsets.UTF_8
        }
    }

    /**
     * Returns a fallback charset if the primary charset detection fails.
     *
     * @return The fallback charset to be used.
     */
    private fun getFallbackCharset(): Charset {
        val fallbackCharset = appConfig.fileWatcher.fallBackCharset.uppercase().trim()
        return if (fallbackCharset != "AUTO") {
            runCatching {
                Charset.forName(fallbackCharset)
            }.getOrElse {
                log.warn("Fallback charset ($fallbackCharset) not found. Using UTF-8.")
                StandardCharsets.UTF_8
            }
        } else {
            StandardCharsets.UTF_8
        }
    }

    /**
     * Handles commands read from the monitored file. Commands like "EXIT" will stop the monitoring and shut down the application.
     *
     * @param command The command read from the file to be processed.
     */
    private suspend fun handleCommand(command: String) {
        log.info("Processing command: $command")
        when (command.uppercase().trim()) {
            "EXIT" -> {
                log.info("EXIT command received. Shutting down.")
                stop()
                systemExitHandler.exitProcess(1)
            }

            else -> {
                // Forward regular commands to the CommandProcessorService
                commandProcessorService.getCommandProcessor().enqueueCommand(command)
            }
        }
    }
}
