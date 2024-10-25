package tech.robd.robokey

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Inline property that sets up a logger for any class implementing the `Logable` interface.
 *
 * This property provides a convenient way to initialize an SLF4J logger for any class by accessing the `setupLogs` property.
 * It automatically resolves the class or its enclosing class and sets up the logger accordingly.
 *
 * ### How It Works:
 * - The logger is created using the `LoggerFactory.getLogger()` method from SLF4J.
 * - If the class has an enclosing class (e.g., a nested class), the enclosing class is used for the logger.
 * - If there is no enclosing class, the current class is used to configure the logger.
 *
 * ### Example Usage:
 *
 * ```
 * class ExampleClass : Logable {
 *     private val log = setupLogs
 *
 *     fun exampleFunction() {
 *         log.info("This is an example log message.")
 *     }
 * }
 * ```
 *
 * This example demonstrates how to use the `setupLogs` property to create a logger within a class that implements `Logable`.
 *
 * @param T The class type that implements `Logable`.
 * @return The logger instance for the class, or its enclosing class if present.
 */
inline val <reified T : Logable> T.setupLogs: Logger
    get() {
        val clazz = T::class.java
        val enclosingClass = clazz.enclosingClass
        return LoggerFactory.getLogger(enclosingClass ?: clazz)
    }

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
