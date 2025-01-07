package tech.robd.robokey.events

/**
 * Interface for handling `RoboKeyEvent` instances within the RoboKey application.
 *
 * `RoboKeyEventListenerInterface` defines a single method, `handleRoboKeyEvent`, which
 * must be implemented by any class that listens for and processes `RoboKeyEvent` instances.
 * This interface supports flexible event handling by enabling different implementations with
 * varied responses to events, promoting a modular and extensible event-driven architecture.
 *
 * Example Implementations:
 * - `RoboKeyEventListener`: A production listener that handles and logs `RoboKeyEvent` instances.
 * - `TestRoboKeyEventListener`: A test-specific listener that captures events for verification.
 *
 * Usage:
 * ```
 * class CustomEventListener : RoboKeyEventListenerInterface {
 *     override fun handleRoboKeyEvent(event: RoboKeyEvent) {
 *         // Custom logic for processing the event
 *     }
 * }
 * ```
 *
 * @see RoboKeyEvent
 */
interface RoboKeyEventListenerInterface {
    /**
     * Handles an incoming `RoboKeyEvent`.
     *
     * Implementing classes define this method to process or respond to `RoboKeyEvent`
     * instances, allowing each listener to handle events according to specific logic.
     *
     * @param event The `RoboKeyEvent` instance to be processed.
     */
    fun handleRoboKeyEvent(event: RoboKeyEvent)
}
