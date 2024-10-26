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

/**
 * Enum class representing priority keyboard commands.
 *
 * These commands have higher priority and can interrupt or override other queued commands.
 * They are primarily used for controlling the state of the system (such as stopping, pausing or resuming the keyboard operation).
 *
 * These commands are represented as strings and can be accessed in various cases (including uppercase and lowercase).
 *
 * @param textValue The text value of the command (e.g., "STOP", "PAUSE").
 */
enum class PriorityKeyboardCommand(val textValue: String) {
    /**
     * Command to stop the keyboard operation.
     */
    STOP("STOP"),

    /**
     * Command to pause the keyboard operation.
     */
    PAUSE("PAUSE"),

    /**
     * Command to resume the keyboard operation after a pause.
     */
    RESUME("RESUME"),

    /**
     * Command to reset the keyboard or system state.
     */
    RESET("RESET");

    /**
     * Returns the original `textValue` of the command.
     */
    val txt: String
        get() = textValue

    /**
     * Returns the uppercase version of the command's `textValue`.
     */
    val uc: String
        get() = textValue.uppercase()

    /**
     * Returns the lowercase version of the command's `textValue`.
     */
    val lc: String
        get() = textValue.lowercase()

    /**
     * Provides a string representation of the enum by returning the command's `textValue`.
     *
     * @return The text value of the command (e.g., "STOP", "PAUSE").
     */
    override fun toString(): String {
        return textValue
    }

    companion object {
        /**
         * Retrieves a list of all the `textValue` strings for each priority command.
         *
         * @return A list of all the text values (e.g., ["STOP", "PAUSE", "RESUME", "RESET"]).
         */
        fun getAllTextValues(): List<String> {
            return PriorityKeyboardCommand.entries.map { it.textValue }
        }
    }
}
