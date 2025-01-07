package tech.robd.robokey.events

import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Test listener for capturing `RoboKeyEvent` instances in the RoboKey application.
 *
 * `TestRoboKeyEventListener` implements `RoboKeyEventListenerInterface` and is intended for
 * use in testing environments. Operating under the "test" profile, it intercepts all published
 * `RoboKeyEvent` instances, storing them in a thread-safe queue. This setup allows test code
 * to verify whether specific events were received, inspect event data, and clear the event
 * history as needed for isolated tests.
 *
 * There are e2 main reasons this is only enabled in test
 * 1. The Queue would fill up after extended operation.
 * 2. Security, this application is not a keylogger
 *
 * Key Annotations:
 * - `@Profile("test")`: Restricts this listener to the "test" profile, preventing it from
 *   being active in production environments.
 * - `@Component`: Registers this listener as a Spring bean, allowing it to participate in
 *   Spring’s event-handling mechanisms.
 *
 * Usage:
 * - `handleRoboKeyEvent(event)`: Adds the event to an internal queue upon receipt.
 * - `hasEvent(eventType, data)`: Checks if an event of the specified type (and optional data) was received.
 * - `reset()`: Clears the received event queue, supporting clean test setups.
 * - `getAllEvents()`: Retrieves a list of all captured events for detailed inspection.
 *
 * @property receivedEvents A thread-safe queue that stores captured events.
 */
@Profile("test")
@Component
class TestRoboKeyEventListener : RoboKeyEventListenerInterface {
    private val receivedEvents = ConcurrentLinkedQueue<RoboKeyEvent>()

    /**
     * Handles a `RoboKeyEvent` by adding it to the queue of received events.
     *
     * This method captures each `RoboKeyEvent` received during testing, storing it
     * in a thread-safe queue for later verification. This allows test assertions
     * to validate event occurrences, types, and associated data.
     *
     * @param event The `RoboKeyEvent` instance to capture.
     */
    @EventListener
    override fun handleRoboKeyEvent(event: RoboKeyEvent) {
        receivedEvents.add(event)
    }

    /**
     * Clears all captured events from the queue.
     *
     * This method supports resetting the listener state between tests, ensuring
     * that each test starts with a clean slate of events.
     */
    fun reset() {
        receivedEvents.clear()
    }

    /**
     * Checks if a specific event type (and optional data) has been received.
     *
     * Searches the queue of captured events to determine if an event of the given
     * type, and optionally matching the provided data, exists. Useful for assertions
     * in test cases.
     *
     * @param eventType The type of event to search for.
     * @param data Optional data to match within the event.
     * @return `true` if a matching event is found; `false` otherwise.
     */
    fun hasEvent(
        eventType: EventType,
        data: Any? = null,
    ): Boolean =
        receivedEvents.any {
            it.eventType == eventType && (data == null || it.eventData == data)
        }

    /**
     * Retrieves a list of all captured events for detailed inspection.
     *
     * Provides access to all received events as a list, allowing test code to
     * perform detailed assertions and checks on the sequence and content of events.
     *
     * @return A list of all `RoboKeyEvent` instances captured by this listener.
     */
    fun getAllEvents(): List<RoboKeyEvent> = receivedEvents.toList()
}
