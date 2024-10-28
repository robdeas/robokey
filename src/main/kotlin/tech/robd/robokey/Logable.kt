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

/**
 * Marker interface for classes that require logging.
 *
 * Any class that implements the `Logable` interface can use the `setupLogs` property to create and initialize an SLF4J logger.
 * This interface does not define any methods; it serves as a marker to indicate that a class supports logging through the `setupLogs` utility.
 *
 * ### Example:
 * ```
 * class MyClass : Logable {
 *     private val log = setupLogs
 * }
 * ```
 * In this example, `MyClass` implements `Logable`, which allows it to use `setupLogs` to easily initialize a logger.
 */
interface Logable
