package tech.robd.robokey.tasks

import kotlinx.coroutines.*

/**
 * A task that can be started, canceled, or restarted, and managed through coroutines.
 *
 * This class uses Kotlin coroutines to manage the execution of tasks, which are
 * represented by suspend functions. The task is executed within a provided
 * [CoroutineScope], and its lifecycle (start, cancel, restart) is controlled via a [Job].
 *
 * ### Key Features:
 * - **Coroutine-based**: The task runs asynchronously using coroutines.
 * - **Timeout**: The task is automatically canceled if it runs for more than 5 seconds (configurable).
 * - **Cancellation**: The task can be canceled at any time, stopping its execution.
 * - **Restart**: Tasks can be restarted, canceling any running task and starting a new one.
 *
 * @param name A unique name for the task, typically used for logging or tracking purposes.
 * @param task The suspending function representing the actual task logic that will be run asynchronously.
 */
class NamedCancellableTask(val name: TaskName, val task: suspend () -> Unit) {
    /**
     * The [Job] associated with the coroutine that executes the task.
     *
     * A [Job] in coroutines is used to manage the lifecycle of a coroutine, allowing you
     * to cancel the task, check its status, or wait for its completion.
     */
    var job: Job? = null

    /**
     * Starts the task by launching a new coroutine within the given [CoroutineScope].
     *
     * The task is wrapped in a `withTimeoutOrNull(5000L)` block, meaning that it will automatically
     * be canceled if it runs for more than 5 seconds. The timeout can be changed if needed.
     *
     * ### How it works:
     * - The task is launched as a coroutine using the provided [scope].
     * - If the task exceeds the timeout, it will be canceled, and a timeout message will be logged.
     * - Exceptions thrown within the task are caught and logged.
     *
     * @param scope The [CoroutineScope] in which the task will be launched. This defines the lifecycle
     * of the task and ensures proper cancellation if the scope itself is canceled.
     */
    fun start(scope: CoroutineScope) {
        // Launches a new coroutine within the provided scope
        job = scope.launch {
            // Timeout set to 5 seconds; can be customized for different tasks
            withTimeoutOrNull(5000L) {
                try {
                    // Execute the suspending task
                    task()
                } catch (e: Exception) {
                    // Log any exceptions thrown during task execution
                    println("Task $name failed with exception: $e")
                }
            } ?: println("Task $name timed out.")  // Log when task times out
        }
    }

    /**
     * Cancels the running task, if there is one.
     *
     * This function cancels the current [Job] (i.e., the running coroutine). If the task is running, it will
     * stop execution immediately. If there is no running task, the function does nothing.
     */
    fun cancel() {
        job?.cancel()  // Cancel the job if it's currently running
    }

    /**
     * Restarts the task by first canceling the currently running task, if any, and then starting a new one.
     *
     * This method is useful for retrying tasks or restarting tasks that may have timed out or failed.
     * It ensures that any ongoing work is stopped before launching a fresh instance of the task.
     *
     * @param scope The [CoroutineScope] in which the new task will be launched.
     */
    fun restart(scope: CoroutineScope) {
        cancel()  // Cancel the current task if it exists
        start(scope)  // Start a new task
    }

    /**
     * Checks if the task has completed.
     *
     * @return `true` if the task has completed, `false` otherwise. This checks the status
     * of the [Job] associated with the coroutine.
     */
    fun isCompleted(): Boolean = job?.isCompleted == true
}
