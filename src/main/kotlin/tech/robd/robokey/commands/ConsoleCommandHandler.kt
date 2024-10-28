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
import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import tech.robd.robokey.AppConfig
import tech.robd.robokey.Logable
import tech.robd.robokey.SystemExitHandler
import tech.robd.robokey.setupLogs
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Scanner

/**
 * ConsoleCommandHandler is responsible for handling console input commands and processing them using
 * the CommandProcessorService. It also supports a graceful shutdown and a command-based help system.
 *
 * @param appConfig The application configuration that includes options like consoleCommands.
 * @param commandProcessorService The service used to process keyboard commands.
 */
@Component
class ConsoleCommandHandler(
    private val appConfig: AppConfig,
    private val commandProcessorService: CommandProcessorService,
    private val systemExitHandler: SystemExitHandler,
) {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    companion object : Logable {
        private val log = setupLogs

        /**
         * Provides help content by reading from a markdown file (help.md) located in the classpath.
         *
         * @return A string containing the help text rendered from markdown.
         */
        fun help(): String {
            val resource = ClassPathResource("help.md")
            val markdownContent =
                resource.inputStream.use { inputStream ->
                    InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                        FileCopyUtils.copyToString(reader)
                    }
                }
            val parser = Parser.builder().build()
            val document = parser.parse(markdownContent)
            val renderer = TextContentRenderer.builder().build()
            return renderer.render(document)
        }
    }

    /**
     * Starts the console command handler, listening for user input.
     * If consoleCommands is enabled in the configuration, it listens for commands until the application shuts down.
     */
    fun start() {
        if (appConfig.consoleCommands) {
            scope.launch {
                val scanner = Scanner(System.`in`)
                print("Enter command: ")

                try {
                    while (scanner.hasNextLine()) {
                        val command = scanner.nextLine().trim()
                        delay(2000)
                        handleCommand(command)
                        print("Enter command: ")
                    }
                } catch (e: CancellationException) {
                    log.info("Console command handler cancelled.")
                } catch (e: Exception) {
                    log.info("Error during command handling: ${e.message}")
                }
            }

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    log.info("Shutting down gracefully...")
                    job.cancel() // Cancel the coroutine scope
                    runBlocking {
                        // Introduce a timeout for the join operation
                        val shutdownComplete =
                            withTimeoutOrNull(2000L) {
                                job.join()
                            }

                        if (shutdownComplete != null) {
                            log.info("Shutdown completed within 2 seconds.")
                        } else {
                            log.info("Shutdown timed out after 2 seconds. Proceeding with forced exit.")
                        }
                    }
                    log.info("Shutdown complete.")
                },
            )
        }
    }

    /**
     * Processes a single command entered by the user. Depending on the command, it either invokes system exit,
     * prints help, or forwards the command to the CommandProcessorService.
     *
     * @param command The command entered by the user.
     */
    suspend fun handleCommand(command: String) {
        try {
            log.info("Handling command: $command")
            when (command.uppercase().trim()) {
                "EXIT" -> systemExitHandler.exitProcess(0) // System exit
                "HELP" -> log.info(help()) // Show help content
                "STOP" -> commandProcessorService.stopTyping() // Stop typing
                "RESET" -> commandProcessorService.resetKeyboardProcessor() // Reset Arduino
                "RESUME" -> commandProcessorService.resumeTyping() // Resume typing
                else -> {
                    // Forward regular commands to the CommandProcessorService
                    commandProcessorService.getCommandProcessor().enqueueCommand(command)
                }
            }
        } catch (e: Exception) {
            log.info("Error processing command: ${e.message}")
        }
    }
}
