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
package tech.robd.robokey.tasks

/**
 * Enum class representing various tasks that can be executed by the system.
 *
 * Each `TaskName` is associated with:
 * - A **description**: A brief summary of what the task does.
 * - An **ID**: A unique integer ID that can be used to reference the task programmatically.
 *
 * This enum is useful for identifying and managing tasks within the system, providing a common
 * set of task names that can be referred to by other components, such as the `TaskPoolManager`.
 *
 * ### Utility Functions:
 * - `fromId()`: Find a task by its ID.
 * - `fromDescription()`: Find a task by its description (case-insensitive).
 *
 * @param description A brief description of what the task does.
 * @param id A unique identifier for the task.
 */
enum class TaskName(val description: String, val id: Int) {
    /**
     * Task that monitors changes to files.
     */
    FILE_MONITOR("Monitors file changes", 1),

    /**
     * Task responsible for handling console input and commands.
     */
    CONSOLE_COMMANDS("Handles console commands", 2),

    /**
     * Task that handles key output, typically for sending simulated keyboard input.
     */
    KEY_OUTPUT("Handles key output", 3),

    TASK_1("Task 1", 5),

    TASK_2("Task 2", 6),

    TASK_3("Task 3", 7),

    TASK_4("Task 4", 8),

    TASK_5("Task 5", 9),

    TASK_6("Task 6", 10),

    TASK_7("Task 7", 11),

    TASK_8("Task 8", 12),

    TASK_9("Task 9", 13),

    TASK_10("Task 10", 14);

    /**
     * Custom string representation of the task, displaying the task name, ID, and description.
     *
     * @return A string representation of the task in the format: "TaskName (ID: id, Description: description)".
     */
    override fun toString(): String {
        return "$name (ID: $id, Description: $description)"
    }

    companion object {
        /**
         * Finds a `TaskName` by its ID.
         *
         * This utility function looks through the entries of `TaskName` and returns the matching task
         * based on its unique integer ID. If no match is found, `null` is returned.
         *
         * @param id The unique ID of the task to find.
         * @return The matching `TaskName`, or `null` if no match is found.
         */
        fun fromId(id: Int): TaskName? {
            return TaskName.entries.firstOrNull { it.id == id }
        }

        /**
         * Finds a `TaskName` by its description.
         *
         * This utility function searches for a task by its description, performing a case-insensitive
         * comparison to find the matching task. If no match is found, `null` is returned.
         *
         * @param description The description of the task to search for.
         * @return The matching `TaskName`, or `null` if no match is found.
         */
        fun fromDescription(description: String): TaskName? {
            return TaskName.entries.firstOrNull { it.description.equals(description, ignoreCase = true) }
        }
    }
}
