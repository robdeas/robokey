package tech.robd.robokey.events

import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue

@Profile("test")
@Component
class TestRoboKeyEventListener : RoboKeyEventListenerInterface {
    private val receivedEvents = ConcurrentLinkedQueue<RoboKeyEvent>()

    @EventListener
    override fun handleRoboKeyEvent(event: RoboKeyEvent) {
        receivedEvents.add(event)
    }

    fun reset() {
        receivedEvents.clear()
    }

    fun hasEvent(
        eventType: EventType,
        data: Any? = null,
    ): Boolean =
        receivedEvents.any {
            it.eventType == eventType && (data == null || it.eventData == data)
        }

    fun getAllEvents(): List<RoboKeyEvent> = receivedEvents.toList()
}
