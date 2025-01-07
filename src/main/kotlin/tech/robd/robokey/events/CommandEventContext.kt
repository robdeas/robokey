package tech.robd.robokey.events

import org.springframework.context.ApplicationEventPublisher
import tech.robd.robokey.Utils
import java.time.Instant
import java.util.UUID

/**
 * Contextual container that links events to a specific "parent" event within the RoboKey system.
 *
 *  NOTE all events will self-publish
 *
 * The `CommandEventContext` class provides a unique identifier, timestamp, and metadata, associating
 * various events with a particular command or batch operation. This facilitates structured and
 * hierarchical event management, allowing events to be traced back to a common origin or "parent."
 *
 * Key features:
 * - Automatically publishes a `START_COMMAND_EVENT` when initialized.
 * - Provides utility methods to publish additional events tied to this context, supporting complex
 *   workflows where commands or batches require multiple steps.
 *
 * @property eventPublisher The `ApplicationEventPublisher` used to publish events within the application context.
 * @property uuid Unique identifier for this parent context, generated by default.
 * @property timestamp Timestamp indicating when this context was created.
 * @property eventSourceActor The originator or "actor" responsible for the event(s) associated with this context.
 * @property eventCommand The primary command associated with this context; defaults to `UNDEFINED` if undetermined.
 * @property commandContents Optional string value associated with this context, e.g., command parameters.
 */
class CommandEventContext(
    private val eventPublisher: ApplicationEventPublisher, // Injected publisher for publishing events
    override val uuid: String = UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    override val eventSourceActor: EventSourceActor? = null,
    val commandContents: String? = null,
    override val eventCommand: EventCommand =
        commandContents?.let { Utils.extractCommandFromString(it) } ?: EventCommand.UNDEFINED,
) : EventGroup() {
    init {
        // Automatically publish a START_COMMAND_EVENT upon instantiation
        if (Utils.ENABLE_AUTO_PUBLISH) publishRoboKeyEvent(EventType.START_COMMAND_EVENT, "CommandEventContext initialized")
    }

    /**
     * Helper method to create and publish a RoboKeyEvent based on this CommandEventContext.
     * @param eventType The type of event to publish.
     * @param data Optional data to include with the event.
     */
    fun publishRoboKeyEvent(
        eventType: EventType,
        data: String? = null,
    ) {
        RoboKeyEvent(
            eventSourceActor = eventSourceActor ?: EventSourceActor.UNKNOWN,
            eventType = eventType,
            eventData = data ?: "Event from CommandEventContext with ID: $uuid",
            parentEvent = this,
            eventPublisher = eventPublisher,
        )
    }

    override fun isBatch(): Boolean = false
}
