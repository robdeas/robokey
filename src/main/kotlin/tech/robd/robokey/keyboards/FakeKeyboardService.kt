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
package tech.robd.robokey.keyboards

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tech.robd.robokey.AppConfig
import tech.robd.robokey.Logable
import tech.robd.robokey.setupLogs
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * FakeKeyboardService simulates a keyboard service by logging commands to a file. This is useful for testing
 * or when you need a mock implementation of the KeyboardInterface without interacting with physical hardware.
 *
 * @param appConfig The application configuration that contains settings like the log file path.
 */
@Service
class FakeKeyboardService(val appConfig: AppConfig) : KeyboardInterface {
    companion object : Logable {
        private val log = setupLogs
    }

    // State flags for pausing and stopping operations
    private var isPaused = false
    private var isStopped = false

    /**
     * Logs the provided lines to the specified file path. Ensures that directories are created if they do not exist.
     *
     * @param filePath The path of the file where the lines will be logged.
     * @param lines The list of strings to be logged to the file.
     * @return A Mono that completes when the file has been written.
     */
    private fun sendLogData(filePath: String, lines: List<String>): Mono<Void> {
        return Mono.fromCallable {
            val file = File(filePath)

            // Ensure parent directories exist
            Files.createDirectories(Paths.get(file.parent))

            // Write lines to file
            file.bufferedWriter().use { writer ->
                for (line in lines) {
                    writer.write(line)
                    writer.newLine()
                }
            }
            log.info("Logged lines to $filePath")
        }
            .doOnError { e -> log.info("Error logging data to file: ${e.message}") }
            .subscribeOn(Schedulers.boundedElastic())  // Ensure file IO happens on a boundedElastic thread pool
            .then()  // Return Mono<Void> to signal completion
    }

    /**
     * Public method to log data to a file.
     *
     * @param filePath The file path where the data will be logged.
     * @param lines The list of strings to log.
     * @return A Mono that completes after the data has been logged.
     */
    fun logRawDataToFile(filePath: String, lines: List<String>): Mono<Void> {
        return sendLogData(filePath, lines)
    }

    /**
     * Sends a list of commands by logging them to the file specified in the appConfig.
     * This is an asynchronous operation and will return immediately, completing once the data is logged.
     *
     * @param commands The list of command strings to be logged.
     */
    override suspend fun sendCommandData(commands: List<String>) {
        appConfig.logFilePath?.let { filePath ->
            logRawDataToFile(filePath, commands).subscribe()  // Subscribe to ensure the operation is executed
        }
    }

    /**
     * Stops the service and clears any queued commands.
     * For the fake implementation, this just sets the internal `isStopped` flag.
     */
    override suspend fun stopAndClearQueue() {
        isStopped = true
        log.info("FakeKeyboardService: stopped and cleared queue.")
    }

    /**
     * Resumes the service by resetting the paused and stopped flags.
     */
    override suspend fun resume() {
        isPaused = false
        isStopped = false
        log.info("FakeKeyboardService: resumed.")
    }
}
