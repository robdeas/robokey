// Content of AppConfig.kt
package tech.robd.robokey

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Application configuration class that holds various configurable properties for the Robokey system.
 *
 * The `AppConfig` class is annotated with `@ConfigurationProperties` and uses the prefix `app`, meaning
 * that these properties can be set in the application's configuration file (e.g., `application.yml` or `application.properties`).
 * It configures various aspects of the application, such as the command system, GUI settings, keyboard settings, and file monitoring.
 *
 * ### Example Configuration (application.yml):
 * ```yaml
 * app:
 *   console-commands: true
 *   gui: true
 *   dark-mode: false
 *   mode: LOCAL
 *   com-port: COM3
 *   log-file-path: /var/log/robokey.log
 *   debug: true
 *   file-watcher:
 *     enabled: true
 *     win-file-path: C:/data/commands.txt
 *     posix-file-path: /tmp/robokey
 *     create-file: true
 *     charset: AUTO
 * ```
 *
 * @param consoleCommands Enables or disables the console command handler.
 * @param gui Enables or disables the graphical user interface (GUI).
 * @param darkMode Enables dark mode for the GUI (if applicable).
 * @param mode The mode of operation (e.g., "LOCAL" or "ARDUINO").
 * @param comPort The COM port for communication with the Arduino (if applicable).
 * @param logFilePath The path to the log file.
 * @param debug Enables or disables debug logging.
 * @param fileWatcher A whole section of configuration settings for the file monitoring system.
 * @param keyboard A whole section of configuration settings for keyboard input and output.
 */
@Component
@ConfigurationProperties(prefix = "app")
data class AppConfig(
    var consoleCommands: Boolean = true,
    var gui: Boolean = true,
    var darkMode: Boolean = false,
    var mode: String = "LOCAL",
    var comPort: String? = null,
    var logFilePath: String? = null,
    var debug: Boolean = false,
    var fileWatcher: FileWatcher = FileWatcher(),
    val keyboard: KeyboardConfig = KeyboardConfig(),
)
/**
 * Configuration class for keyboard settings.
 *
 * This class holds various configuration options for controlling keyboard behavior, such as output settings,
 * delays, and jitter options.
 *
 * @param output Enables or disables keyboard output.
 * @param initialDelay The initial delay (in milliseconds) before starting to type.
 * @param allJitter Enables jitter for all key presses.
 * @param uiExtraDelay Enables extra delay when interacting with the GUI.
 * @param useBusyState Enables the busy state when processing commands.
 * @param press Configuration for key press settings (e.g., jitter and time).
 * @param interval Configuration for key interval settings (e.g., jitter and time between key presses).
 */
data class KeyboardConfig(
    var output: Boolean = true,
    var initialDelay: Long = 2000, // TODO shrink it in final code
    val allJitter: Boolean = true,
    val uiExtraDelay: Boolean = true,
    val useBusyState: Boolean = true,
    val press: KeyboardKeyPressConfig = KeyboardKeyPressConfig(),
    val interval: KeyboardKeyIntervalConfig = KeyboardKeyIntervalConfig()
)

/**
 * Configuration class for key press settings.
 *
 * This class holds settings related to the timing and jitter of key presses when typing.
 *
 * @param jitter Enables or disables jitter for key presses.
 * @param jitterMax The maximum jitter value (in milliseconds) for key presses.
 * @param time The duration (in milliseconds) of a key press.
 */
data class KeyboardKeyPressConfig(
    var jitter: Boolean = false,
    var jitterMax: Int = 100,
    var time: Int = 100,
)

/**
 * Configuration class for key interval settings.
 *
 * This class holds settings related to the interval and jitter between key presses when typing.
 *
 * @param jitter Enables or disables jitter between key presses.
 * @param jitterMax The maximum jitter value (in milliseconds) between key presses.
 * @param time The time (in milliseconds) between key presses.
 */
data class KeyboardKeyIntervalConfig(
    var jitter: Boolean = false,
    var jitterMax: Int = 500,
    var time: Int = 580,
)

/**
 * Configuration class for file monitoring settings.
 *
 * This class holds settings related to the file watcher, which monitors a specific file for commands or other input.
 *
 * @param enabled Enables or disables file monitoring.
 * @param winFilePath The file path for file monitoring on Windows systems.
 * @param posixFilePath The file path for file monitoring on POSIX-compliant systems (e.g., Linux, macOS).
 * @param createFile Enables or disables automatic file creation if the file does not exist.
 * @param charset The character set to use for reading the file (default is AUTO-detection).
 * @param fallBackCharset The fallback character set to use if charset detection fails.
 */
data class FileWatcher(
    var enabled: Boolean = false, // can completely turn off file watching / processing
    var winFilePath: String = "C:\\data\\robo_keyboard_commands.txt",
    var posixFilePath: String = "/tmp/robokeybord",
    var createFile: Boolean = true,
    var charset: String = "AUTO",
    var fallBackCharset: String = "AUTO",

)