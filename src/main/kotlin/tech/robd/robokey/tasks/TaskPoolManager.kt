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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap

/**
 * A manager for handling tasks using coroutines, allowing tasks to be submitted, canceled, and restarted.
 *
 * This class manages tasks asynchronously using Kotlin coroutines, providing functionality to:
 * - Submit tasks, which are executed within a provided `CoroutineScope`.
 * - Cancel specific tasks by name.
 * - Restart tasks, which cancels an existing task and starts a new one with the same name.
 * - Track and manage the lifecycle of tasks through a `ConcurrentHashMap`.
 *
 * ### Key Features:
 * - **Coroutine-based**: All tasks are executed using coroutines, providing efficient, non-blocking concurrency.
 * - **Task Lifecycle Management**: Tasks can be submitted, canceled, or restarted, and their status is tracked.
 * - **Concurrent Map for Task Management**: Tasks are stored in a `ConcurrentHashMap`, allowing for thread-safe access to tasks.
 *
 * @param scope The `CoroutineScope` within which all tasks are launched. This scope ensures structured concurrency and proper
 * lifecycle management for all tasks managed by the `TaskPoolManager`.
 */
class TaskPoolManager(
    private val scope: CoroutineScope,
) {
    /**
     * A thread-safe map that tracks tasks by their names. The task name (`TaskName`) serves as the key, and
     * each task is represented by a `NamedCancellableTask`.
     */
    private val tasks = ConcurrentHashMap<TaskName, NamedCancellableTask>()

    /**
     * Submits a new task to the task pool and starts it asynchronously.
     *
     * The task is wrapped inside a `NamedCancellableTask` and started in the provided [CoroutineScope].
     * If a task with the same name already exists, it is replaced with the new one.
     *
     * ### How It Works:
     * - The `taskLogic` is a suspending function, allowing non-blocking operations to be performed.
     * - The task is added to the `tasks` map, replacing any existing task with the same name.
     *
     * @param name The unique name of the task, which is used to track and manage it.
     * @param taskLogic The suspending function that represents the task's logic, executed asynchronously.
     */
    fun submitTask(
        name: TaskName,
        taskLogic: suspend () -> Unit,
    ) {
        val task = NamedCancellableTask(name, taskLogic)
        task.start(scope) // Start the task in the provided coroutine scope
        tasks[name] = task // Store the task in the map
    }

    /**
     * Cancels a task with the specified name.
     *
     * If the task exists in the `tasks` map, its `cancel()` method is called, which cancels the
     * coroutine associated with the task.
     *
     * @param name The name of the task to cancel.
     */
    fun cancelTask(name: TaskName) {
        tasks[name]?.cancel() // Cancel the task if it exists
    }

    /**
     * Restarts a task with the specified name by canceling the existing task (if any) and submitting a new one.
     *
     * This method first attempts to restart the existing task if it is found in the `tasks` map.
     * If the task is not found, a new task is submitted with the provided task logic.
     *
     * @param name The name of the task to restart.
     * @param taskLogic The suspending function representing the task's logic, which will be restarted or submitted if the task doesn't exist.
     */
    fun restartTask(
        name: TaskName,
        taskLogic: suspend () -> Unit,
    ) {
        tasks[name]?.restart(scope) ?: submitTask(
            name,
            taskLogic,
        ) // Restart the task if it exists; otherwise, submit a new one
    }

    /**
     * Retrieves the names of all currently tracked tasks.
     *
     * This method returns the set of all task names currently present in the `tasks` map.
     *
     * @return A `Set` of `TaskName` objects representing the names of all tracked tasks.
     */
    fun getTaskNames(): Set<TaskName> {
        return tasks.keys // Return the set of task names
    }

    /**
     * Shuts down the task manager by canceling all running tasks and canceling the `CoroutineScope`.
     *
     * This method iterates over all the tasks in the `tasks` map and cancels each one. After canceling
     * all tasks, the provided `CoroutineScope` is also canceled, ensuring that no more tasks can be started.
     */
    fun shutdown() {
        tasks.values.forEach { it.cancel() } // Cancel all tasks
        scope.cancel() // Cancel the coroutine scope to stop further task execution
    }
}
