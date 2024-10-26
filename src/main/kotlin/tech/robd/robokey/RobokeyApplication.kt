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
package tech.robd.robokey

import tech.robd.robokey.commands.ConsoleCommandHandler
import tech.robd.robokey.commands.FileMonitorCommandHandler
import tech.robd.robokey.tasks.TaskName
import tech.robd.robokey.tasks.TaskPoolManager
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.awt.GraphicsEnvironment
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory

/**
 * The main entry point of the Robokey application.
 *
 * This Spring Boot application manages tasks, command handlers, and optionally displays a GUI.
 * It uses a `TaskPoolManager` to manage background tasks such as console command handling and
 * file monitoring. Depending on the environment, the application may display a GUI if it is not
 * running in headless mode.
 *
 * @param appConfig The application configuration settings.
 * @param swingMainWindow The main GUI window for the application.
 * @param consoleCommandHandler The handler for processing console commands.
 * @param fileMonitorCommandHandler The handler for monitoring file changes.
 * @param taskPoolManager The manager responsible for task execution and lifecycle.
 */
@SpringBootApplication
open class RobokeyApplication(
    val appConfig: AppConfig,
    private val swingMainWindow: SwingMainWindow,
    private val consoleCommandHandler: ConsoleCommandHandler,
    private val fileMonitorCommandHandler: FileMonitorCommandHandler,
    private val taskPoolManager: TaskPoolManager
) {
    companion object : Logable {
        private val log = setupLogs
    }

    /**
     * Bean that runs tasks on application startup.
     *
     * This function sets up and runs specific tasks, such as the console command handler
     * and file monitoring tasks, based on configuration settings. It also initializes and
     * displays the GUI if the application is not running in headless mode.
     *
     * ### Task Initialization:
     * - If the application is configured to handle console commands, it submits the console
     *   command handler to the task pool.
     * - If file monitoring is enabled, the file monitor task is submitted.
     *
     * ### GUI Initialization:
     * - Checks if the environment is headless and, if not, displays the GUI.
     *
     * @return An `ApplicationRunner` that is executed on application startup.
     */
    @Bean
    open fun runOnStartup() = ApplicationRunner {

        log.info("Application starting up...")

        // Attempt to display the GUI if not running in headless mode
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("Headless environment detected. Attempting to override to false.")
            System.setProperty("java.awt.headless", "false")
        }

        if (!GraphicsEnvironment.isHeadless()) {
            log.info("Displaying GUI...")
            swingMainWindow.createAndShowGUI()
        } else {
            log.warn("Headless environment detected. GUI will not be displayed.")
        }

        // Start the console command handler if enabled
        if (appConfig.consoleCommands) {
            log.info("Starting console commands handler...")
            taskPoolManager.submitTask(TaskName.CONSOLE_COMMANDS) { consoleCommandHandler.start() }
        }

        // Start the file monitoring service if enabled
        if (appConfig.fileWatcher.enabled) {
            log.info("Starting file monitor...")
            taskPoolManager.submitTask(TaskName.FILE_MONITOR) { fileMonitorCommandHandler.start() }
        }
    }

    /**
     * Cleans up resources and shuts down tasks when the application is terminated.
     *
     * This method is annotated with `@PreDestroy` to ensure that the task pool is properly
     * shut down before the application exits, preventing any lingering tasks or resource leaks.
     */
    @PreDestroy
    fun shutdown() {
        log.info("Shutting down application and cleaning up resources...")
        taskPoolManager.shutdown()
    }
}

/**
 * Performs an immediate shutdown of the application without cleaning up resources.
 *
 * This is used when the application needs to terminate quickly, such as when displaying
 * help information or exiting due to invalid startup arguments.
 *
 * @param status The exit status code.
 */
fun quickShutdown(status: Int) {
    kotlin.system.exitProcess(status)
}

/**
 * The main function that launches the Robokey application.
 *
 * This function checks for help arguments (`--help`, `-h`, `/h`) and displays usage information if needed.
 * If no help flag is present, it launches the application with the provided arguments.
 *
 * ### Key Behaviors:
 * - Displays help information if help flags are detected.
 * - Ensures that the application is not running in headless mode.
 *
 * @param args The command-line arguments passed to the application.
 */
fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger(RobokeyApplication::class.java)

    // Check for help arguments and display help information
    if (args.contains("--help") || args.contains("-h") || args.contains("/h")) {
        log.info("Displaying help information...")
        ConsoleCommandHandler.help()

        // Exit gracefully since the task pool hasn't started
        quickShutdown(0)
    }

    log.info("Starting RobokeyApplication with arguments: ${args.joinToString()}")
    System.setProperty("java.awt.headless", "false")
    runApplication<RobokeyApplication>(*args)
}
