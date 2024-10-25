package tech.robd.robokey.keyboards

/**
 * KeyboardInterface defines the essential functions required for any keyboard service implementation.
 * This interface is implemented by services that send commands to either a local or remote keyboard.
 */
interface KeyboardInterface {

    /**
     * Sends a list of commands to the keyboard.
     * This function is typically asynchronous and will execute the commands as they are provided.
     *
     * @param commands The list of command strings to be sent to the keyboard.
     */
    suspend fun sendCommandData(commands: List<String>)

    /**
     * Stops the keyboard service and clears any pending commands in the queue.
     * This function should halt ongoing operations and ensure that any queued commands are discarded.
     */
    suspend fun stopAndClearQueue()

    /**
     * Resumes the keyboard service after being paused or stopped.
     * This function should allow the service to continue processing commands.
     */
    suspend fun resume()
}
