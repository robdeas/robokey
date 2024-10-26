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
