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

import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import tech.robd.robokey.AppConfig
import tech.robd.robokey.Logable
import tech.robd.robokey.commands.KeyboardCommand
import tech.robd.robokey.setupLogs
import java.awt.Robot
import java.awt.event.KeyEvent
import java.util.Locale

/**
 * Service responsible for simulating keyboard input locally using the Java `Robot` class.
 *
 * The `LocalRobotKeyboardService` implements the `KeyboardInterface` and interacts with the local system's
 * keyboard by sending key press and key release events. It supports a wide range of commands, such as sending text,
 * pressing individual keys, and processing key combinations.
 *
 * It also manages system states such as pause, stop, and resume, and supports key mapping for different locales (e.g., US, UK).
 *
 * @param appConfig The application configuration that provides settings related to keyboard output and delays.
 */
@Service
class LocalRobotKeyboardService(val appConfig: AppConfig) : KeyboardInterface {

    // Lazy initialisation of the `Robot`
    private var robot: Robot? = null
        get() {
            if (field == null) {
                field = Robot()  // Create new Robot if it hasn't been initialised
            }
            return field
        }

    private var isPaused = false
    private var isStopped = false

    companion object : Logable {

        private val log = setupLogs

        /**
         * Represents a key mapping, including whether the Shift or AltGr key is required.
         *
         * @param key The keycode for the key.
         * @param needsShift Indicates if the Shift key is required.
         * @param needsAltGr Indicates if the AltGr key is required (common in non-US locales).
         */
        data class KeyMapping(
            val key: Int,
            val needsShift: Boolean = false,
            val needsAltGr: Boolean = false
        )

        /**
         * A map of common key commands to their respective `KeyEvent` keycodes.
         * These mappings allow for the use of symbolic names in command processing.
         */
        val keyMap = mapOf(
            "@C" to KeyEvent.VK_CONTROL,
            "@S" to KeyEvent.VK_SHIFT,
            "@A" to KeyEvent.VK_ALT,
            "\\R" to KeyEvent.VK_ENTER,
            "\\B" to KeyEvent.VK_BACK_SPACE,
            // Additional key mappings here...
            "ENTER" to KeyEvent.VK_ENTER,
            "UP" to KeyEvent.VK_UP,
            "DOWN" to KeyEvent.VK_DOWN,
            "LEFT" to KeyEvent.VK_LEFT,
            "RIGHT" to KeyEvent.VK_RIGHT,
            "CTRL" to KeyEvent.VK_CONTROL,
            "SHIFT" to KeyEvent.VK_SHIFT,
            "ALT" to KeyEvent.VK_ALT,
            "GUI" to KeyEvent.VK_WINDOWS,
            "ALT_GR" to KeyEvent.VK_ALT_GRAPH,
            "DEL" to KeyEvent.VK_DELETE,
            "ENTER" to KeyEvent.VK_ENTER,
            "BS" to KeyEvent.VK_BACK_SPACE,
            "BKSP" to KeyEvent.VK_BACK_SPACE,
            "LEFT_CTRL" to KeyEvent.VK_CONTROL,
            "LEFT_SHIFT" to KeyEvent.VK_SHIFT,
            "LEFT_ALT" to KeyEvent.VK_ALT,
            "LEFT_GUI" to KeyEvent.VK_WINDOWS,
            "RIGHT_CTRL" to KeyEvent.VK_CONTROL,
            "RIGHT_SHIFT" to KeyEvent.VK_SHIFT,
            "RIGHT_ALT" to KeyEvent.VK_ALT_GRAPH,
            "RIGHT_GUI" to KeyEvent.VK_WINDOWS,
            "UP_ARROW" to KeyEvent.VK_UP,
            "DOWN_ARROW" to KeyEvent.VK_DOWN,
            "LEFT_ARROW" to KeyEvent.VK_LEFT,
            "RIGHT_ARROW" to KeyEvent.VK_RIGHT,
            "BACKSPACE" to KeyEvent.VK_BACK_SPACE,
            "TAB" to KeyEvent.VK_TAB,
            "RETURN" to KeyEvent.VK_ENTER,
            "MENU" to KeyEvent.VK_CONTEXT_MENU,
            "ESC" to KeyEvent.VK_ESCAPE,
            "INSERT" to KeyEvent.VK_INSERT,
            "DELETE" to KeyEvent.VK_DELETE,
            "PAGE_UP" to KeyEvent.VK_PAGE_UP,
            "PAGE_DOWN" to KeyEvent.VK_PAGE_DOWN,
            "HOME" to KeyEvent.VK_HOME,
            "END" to KeyEvent.VK_END,
            "CAPS_LOCK" to KeyEvent.VK_CAPS_LOCK,
            "PRINT_SCREEN" to KeyEvent.VK_PRINTSCREEN,
            "SCROLL_LOCK" to KeyEvent.VK_SCROLL_LOCK,
            "PAUSE" to KeyEvent.VK_PAUSE,
            "NUM_LOCK" to KeyEvent.VK_NUM_LOCK,
            "KP_SLASH" to KeyEvent.VK_DIVIDE,
            "KP_ASTERISK" to KeyEvent.VK_MULTIPLY,
            "KP_MINUS" to KeyEvent.VK_SUBTRACT,
            "KP_PLUS" to KeyEvent.VK_ADD,
            "KP_ENTER" to KeyEvent.VK_ENTER,
            "KP_1" to KeyEvent.VK_NUMPAD1,
            "KP_2" to KeyEvent.VK_NUMPAD2,
            "KP_3" to KeyEvent.VK_NUMPAD3,
            "KP_4" to KeyEvent.VK_NUMPAD4,
            "KP_5" to KeyEvent.VK_NUMPAD5,
            "KP_6" to KeyEvent.VK_NUMPAD6,
            "KP_7" to KeyEvent.VK_NUMPAD7,
            "KP_8" to KeyEvent.VK_NUMPAD8,
            "KP_9" to KeyEvent.VK_NUMPAD9,
            "KP_0" to KeyEvent.VK_NUMPAD0,
            "KP_DOT" to KeyEvent.VK_DECIMAL,
            "F1" to KeyEvent.VK_F1,
            "F2" to KeyEvent.VK_F2,
            "F3" to KeyEvent.VK_F3,
            "F4" to KeyEvent.VK_F4,
            "F5" to KeyEvent.VK_F5,
            "F6" to KeyEvent.VK_F6,
            "F7" to KeyEvent.VK_F7,
            "F8" to KeyEvent.VK_F8,
            "F9" to KeyEvent.VK_F9,
            "F10" to KeyEvent.VK_F10,
            "F11" to KeyEvent.VK_F11,
            "F12" to KeyEvent.VK_F12,
            "F13" to KeyEvent.VK_F13,
            "F14" to KeyEvent.VK_F14,
            "F15" to KeyEvent.VK_F15,
            "F16" to KeyEvent.VK_F16,
            "F17" to KeyEvent.VK_F17,
            "F18" to KeyEvent.VK_F18,
            "F19" to KeyEvent.VK_F19,
            "F20" to KeyEvent.VK_F20,
            "F21" to KeyEvent.VK_F21,
            "F22" to KeyEvent.VK_F22,
            "F23" to KeyEvent.VK_F23,
            "F24" to KeyEvent.VK_F24,
        )

        /**
         * A map of locale-specific key mappings for characters that require special handling (e.g., Shift or AltGr).
         * This map allows for localized character handling, particularly for special characters and symbols.
         */
        private val localeKeyMap: Map<Locale, Map<Char, KeyMapping>> = mapOf(
            Locale.US to mapOf(
                ':' to KeyMapping(KeyEvent.VK_SEMICOLON, needsShift = true),
                ';' to KeyMapping(KeyEvent.VK_SEMICOLON),
                '!' to KeyMapping(KeyEvent.VK_1, needsShift = true),
                '@' to KeyMapping(KeyEvent.VK_2, needsShift = true),
                '#' to KeyMapping(KeyEvent.VK_3, needsShift = true),
                '$' to KeyMapping(KeyEvent.VK_4, needsShift = true),
                '%' to KeyMapping(KeyEvent.VK_5, needsShift = true),
                '^' to KeyMapping(KeyEvent.VK_6, needsShift = true),
                '&' to KeyMapping(KeyEvent.VK_7, needsShift = true),
                '*' to KeyMapping(KeyEvent.VK_8, needsShift = true),
                '(' to KeyMapping(KeyEvent.VK_9, needsShift = true),
                ')' to KeyMapping(KeyEvent.VK_0, needsShift = true),
                '_' to KeyMapping(KeyEvent.VK_MINUS, needsShift = true),
                '+' to KeyMapping(KeyEvent.VK_EQUALS, needsShift = true),
                '{' to KeyMapping(KeyEvent.VK_BRACELEFT, needsShift = true),
                '}' to KeyMapping(KeyEvent.VK_BRACERIGHT, needsShift = true),
                '|' to KeyMapping(KeyEvent.VK_BACK_SLASH, needsShift = true),
                '<' to KeyMapping(KeyEvent.VK_COMMA, needsShift = true),
                '>' to KeyMapping(KeyEvent.VK_PERIOD, needsShift = true),
                '?' to KeyMapping(KeyEvent.VK_SLASH, needsShift = true),
                '"' to KeyMapping(KeyEvent.VK_QUOTE, needsShift = true), // Adjusted for US keyboard
                '~' to KeyMapping(KeyEvent.VK_BACK_QUOTE, needsShift = true),
                '`' to KeyMapping(KeyEvent.VK_BACK_QUOTE),
                '-' to KeyMapping(KeyEvent.VK_MINUS),
                '=' to KeyMapping(KeyEvent.VK_EQUALS),
                '[' to KeyMapping(KeyEvent.VK_OPEN_BRACKET),
                ']' to KeyMapping(KeyEvent.VK_CLOSE_BRACKET)
                // Additional characters and locales would be similar
            ),
            Locale.UK to mapOf(
                // Adjust these mappings for the UK keyboard if they differ
                ':' to KeyMapping(KeyEvent.VK_SEMICOLON, needsShift = true),
                ';' to KeyMapping(KeyEvent.VK_SEMICOLON),
                '!' to KeyMapping(KeyEvent.VK_1, needsShift = true),
                '@' to KeyMapping(KeyEvent.VK_QUOTE, needsShift = true), // Adjusted for UK keyboard
                '£' to KeyMapping(KeyEvent.VK_3, needsShift = true), // NOT on US keyboard
                '#' to KeyMapping(KeyEvent.VK_NUMBER_SIGN), // No shift needed for UK
                '$' to KeyMapping(KeyEvent.VK_4, needsShift = true),
                '%' to KeyMapping(KeyEvent.VK_5, needsShift = true),
                '^' to KeyMapping(KeyEvent.VK_6, needsShift = true),
                '&' to KeyMapping(KeyEvent.VK_7, needsShift = true),
                '*' to KeyMapping(KeyEvent.VK_8, needsShift = true),
                '(' to KeyMapping(KeyEvent.VK_9, needsShift = true),
                ')' to KeyMapping(KeyEvent.VK_0, needsShift = true),
                '_' to KeyMapping(KeyEvent.VK_MINUS, needsShift = true),
                '+' to KeyMapping(KeyEvent.VK_EQUALS, needsShift = true),
                '{' to KeyMapping(KeyEvent.VK_BRACELEFT, needsShift = true),
                '}' to KeyMapping(KeyEvent.VK_BRACERIGHT, needsShift = true),
                '|' to KeyMapping(KeyEvent.VK_BACK_SLASH, needsShift = true),
                '<' to KeyMapping(KeyEvent.VK_COMMA, needsShift = true),
                '>' to KeyMapping(KeyEvent.VK_PERIOD, needsShift = true),
                '?' to KeyMapping(KeyEvent.VK_SLASH, needsShift = true),
                '"' to KeyMapping(KeyEvent.VK_2, needsShift = true), // Adjusted for UK keyboard
                '~' to KeyMapping(KeyEvent.VK_BACK_QUOTE, needsShift = true),
                '`' to KeyMapping(KeyEvent.VK_BACK_QUOTE),
                '-' to KeyMapping(KeyEvent.VK_MINUS),
                '=' to KeyMapping(KeyEvent.VK_EQUALS),
                '[' to KeyMapping(KeyEvent.VK_OPEN_BRACKET),
                ']' to KeyMapping(KeyEvent.VK_CLOSE_BRACKET)
            ),
        )
    }

    /**
     * Sends a list of commands to be processed by the keyboard.
     *
     * This method processes each command in the provided list, sending the appropriate key press or text input
     * to the system via the `Robot`. It handles system states such as pause and stop, and only processes commands
     * if the system is active.
     *
     * @param lines A list of commands to process.
     */
    override suspend fun sendCommandData(lines: List<String>) {
        lines.forEach { line ->
            val lowerCaseLine = line.lowercase()

            // Don't process commands if paused
            if (isPaused) {
                log.info("System is paused. Command queued but not sent: $line")
                return  // Skip command execution while paused
            }

            try {
                CoroutineScope(Dispatchers.IO).launch {
                    when {
                        lowerCaseLine.startsWith(KeyboardCommand.LOREM.lc) -> {
                            val newLines =
                                lowerCaseLine.startsWith(KeyboardCommand.LOREM_LINES.txt) ||
                                        lowerCaseLine.startsWith(KeyboardCommand.LOREM_LINES_B.txt) ||
                                        lowerCaseLine.startsWith(KeyboardCommand.LOREM_LINES_C.txt)
                            loremIpsum.forEach { loremIpsumLine ->
                                typeTextLine(robot!!, loremIpsumLine, newLines)
                            }
                            log.info("Finished typing lorem ipsum.")
                        }

                        lowerCaseLine.startsWith(KeyboardCommand.LINE.lc) || line.startsWith(KeyboardCommand.TEXT.lc) -> {
                            val newLines = lowerCaseLine.startsWith(KeyboardCommand.LINE.lc)
                            val text = line.substringAfter(":")
                            typeTextLine(robot!!, text, newLines)
                        }

                        lowerCaseLine == KeyboardCommand.PING.lc -> {
                            log.info("Ping received. System is responsive.")
                        }

                        lowerCaseLine.startsWith(KeyboardCommand.KEY.lc) -> {
                            val key = line.substringAfter(KeyboardCommand.KEY.lc).trim()
                            val keyCode = keyMap[key] ?: throw IllegalArgumentException("Key not found: $key")
                            if (appConfig.keyboard.output) {
                                processKeyEvent(robot!!, keyCode, key)
                            } else {
                                print("$keyCode,")
                            }
                        }

                        lowerCaseLine.startsWith(KeyboardCommand.COMBO.lc) -> {
                            val combo = line.substringAfter(KeyboardCommand.COMBO.lc).trim()
                            val keys = combo.split("-").map { key ->
                                keyMap[key] ?: throw IllegalArgumentException("Key not found: $key")
                            }
                            // Press each key in the combo
                            keys.forEach {
                                if (appConfig.keyboard.output) {
                                    processKeyEvent(robot!!, it, "combo-$it")
                                } else {
                                    print("combo-$it, ")
                                }
                            }
                            // Release each key in the combo
                            keys.reversed().forEach {
                                if (appConfig.keyboard.output) {
                                    processKeyEvent(robot!!, it, "release-$it")
                                } else {
                                    print("release-$it, ")
                                }
                            }
                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Error processing command: ${e.message}")
            }
        }
    }

    /**
     * Resets the `Robot` instance by stopping ongoing operations and creating a new instance.
     */
    suspend fun resetRobot() {
        log.info("Resetting Robot instance...")

        // Stop any current operations first for safety
        stopAndClearQueue()

        // Create a new Robot instance
        robot = Robot()
        log.info("Robot instance has been reset.")
    }

    /**
     * Pauses the system, preventing any further commands from being processed.
     */
    suspend fun pause() {
        log.info("Pausing operation.")
        isPaused = true
    }

    /**
     * Resumes the system, allowing commands to be processed again.
     */
    override suspend fun resume() {
        log.info("Resuming operation.")
        isPaused = false
        isStopped = false
    }

    /**
     * Stops the system and clears the command queue, resetting any ongoing operations.
     */
    override suspend fun stopAndClearQueue() {
        log.info("Stopping and clearing the queue.")
        isStopped = true
        isPaused = false // Reset paused state on stop
    }

    /**
     * Processes a key event by pressing and releasing the key with the specified key code.
     *
     * @param robot The `Robot` instance responsible for simulating key events.
     * @param keyCode The key code of the key to press and release.
     * @param debugKey A string used for logging/debugging purposes.
     */
    private suspend fun processKeyEvent(robot: Robot, keyCode: Int, debugKey: String) {
        if (appConfig.keyboard.output) {
            robot.keyPress(keyCode)
            delay(50)
            robot.keyRelease(keyCode)
        } else {
            print("$debugKey, ")
        }
    }

    /**
     * Types a line of text by pressing the appropriate keys for each character.
     *
     * Handles special characters such as newlines, tabs, and backspaces, and checks for localized key mappings.
     *
     * @param robot The `Robot` instance responsible for typing the text.
     * @param text The text to type.
     * @param addReturnChar If true, adds a return character at the end of the line.
     */
    private suspend fun typeTextLine(robot: Robot, text: String, addReturnChar: Boolean) {
        text.forEach { char ->
            if (isStopped) return

            when (char) {
                '\n' -> processKeyEvent(robot, KeyEvent.VK_ENTER, "KeyEvent.VK_ENTER")
                '\t' -> processKeyEvent(robot, KeyEvent.VK_TAB, "KeyEvent.VK_TAB")
                '\b' -> processKeyEvent(robot, KeyEvent.VK_BACK_SPACE, "KeyEvent.VK_BACK_SPACE")
                else -> {
                    val keyCode = KeyEvent.getExtendedKeyCodeForChar(char.code)
                    if (Character.isUpperCase(char)) {
                        if (appConfig.keyboard.output) {
                            val specialKey = KeyMapping(key = keyCode, needsShift = true)
                            typeSpecialChar(robot, specialKey, char)
                        } else {
                            print("SHIFTED-$keyCode,")
                        }
                    } else if (isLocalizedChar(char)) {
                        if (appConfig.keyboard.output) {
                            val specialKey: KeyMapping? = getLocalizedCharMapping(char)
                            if (specialKey != null) {
                                typeSpecialChar(robot, specialKey, char)
                            } else {
                                log.info("Warning: Key code not found for character '$char' (code: ${char.code})")
                            }
                        } else {
                            print("SPECIAL-$keyCode,")
                        }
                    } else if (KeyEvent.CHAR_UNDEFINED.code == keyCode) {
                        log.info("Warning: Key code not found for character '$char' (code: ${char.code})")
                    } else {
                        processKeyEvent(robot, keyCode, "$keyCode")
                    }
                }
            }
        }
        if (addReturnChar) {
            processKeyEvent(robot, KeyEvent.VK_ENTER, "KeyEvent.VK_ENTER")
        }
        isStopped = false
    }

    /**
     * Determines if the given character requires a special key mapping for the current locale.
     *
     * @param char The character to check.
     * @param locale The locale to check for character mappings (defaults to the system's locale).
     * @return True if the character requires a special mapping; false otherwise.
     */
    fun isLocalizedChar(char: Char, locale: Locale = Locale.getDefault()): Boolean {
        return getLocalizedCharMapping(char, locale) != null
    }

    /**
     * Retrieves the key mapping for a localized character, if available.
     *
     * @param char The character to check.
     * @param locale The locale to check for character mappings (defaults to the system's locale).
     * @return A `KeyMapping` if the character has a special mapping; null otherwise.
     */
    fun getLocalizedCharMapping(char: Char, locale: Locale = Locale.getDefault()): KeyMapping? {
        return localeKeyMap[locale]?.get(char)
    }

    /**
     * Types a special character by pressing and releasing the necessary modifier keys (e.g., Shift or AltGr).
     *
     * @param robot The `Robot` instance responsible for typing the character.
     * @param key The key mapping for the special character.
     * @param char The character being typed (for logging/debugging purposes).
     */
    private suspend fun typeSpecialChar(robot: Robot, key: KeyMapping, char: Char) {
        if (appConfig.keyboard.output) {
            if (key.needsShift) robot.keyPress(KeyEvent.VK_SHIFT)
            if (key.needsAltGr) robot.keyPress(KeyEvent.VK_ALT_GRAPH)
            robot.keyPress(key.key)
            delay(50)
            robot.keyRelease(key.key)
            if (key.needsAltGr) robot.keyRelease(KeyEvent.VK_ALT_GRAPH)
            if (key.needsShift) robot.keyRelease(KeyEvent.VK_SHIFT)
        } else {
            print("SPECIAL-$char,")
        }
    }

    private val loremIpsum = listOf(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ",
        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. ",
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. ",
        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    )
}
