package tech.robd.robokey.events

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import tech.robd.robokey.Utils

/**
 * Represents an event within the RoboKey event system.
 *
 * NOTE all events will self-publish
 *
 * `RoboKeyEvent` encapsulates event details, associating the event with a parent `EventGroup`.
 * This parent can be either a `CommandEventContext` or an `EventBatch`, allowing the event to
 * inherit traceability from its parent through the `eventId` property.
 *
 * @param eventSourceActor The source of the event, EventSourceActor enum typically identifies the object that triggered the event.
 * @param parentEvent The `EventGroup` (either a `CommandEventContext` or `EventBatch`) associated
 * with this event, providing traceability and grouping.
 * @param eventType The type of event, indicating its purpose or nature.
 * @param eventData Additional data associated with the event.
 * @param eventPublisher The Spring `ApplicationEventPublisher` to automatically publish the event
 * upon creation.
 */
class RoboKeyEvent(
    eventSourceActor: EventSourceActor,
    val parentEvent: EventGroup,
    val eventType: EventType,
    val eventData: String,
    private val eventPublisher: ApplicationEventPublisher,
    val parentEventId: String = parentEvent.uuid,
) : ApplicationEvent(eventSourceActor),
    Trackable {
    /**
     * The unique identifier for this event, inherited from the parent event group.
     */
    override val eventId: String get() = parentEventId // Inherit the event ID from parent context

    /**
     * Provides detailed information about the event for tracking purposes.
     *
     * @return A string containing the event's ID and associated data.
     */
    override fun getEventDetails(): String = "TrackableEvent with ID: $eventId and data: $eventData"

    init {
        // Automatically publish this event upon creation
        if (Utils.ENABLE_AUTO_PUBLISH) eventPublisher.publishEvent(this)
    }
}
