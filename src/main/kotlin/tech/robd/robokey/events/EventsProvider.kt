package tech.robd.robokey.events

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Factory component for creating `CommandEventContext` instances within the RoboKey event system.
 *
 * The `EventsProvider` acts as a centralised factory for generating and configuring
 * `CommandEventContext` objects. These contexts provide a unique identifier (UUID) and metadata
 * for tracking and managing events throughout their lifecycle. By using this provider, other
 * components can create event contexts that adhere to a consistent structure, enabling reliable
 * traceability and event-driven workflows across the application.
 *
 * Key Features:
 * - **Standardised Context Creation**: Ensures all `CommandEventContext` instances are created
 *   with the necessary metadata, including a unique UUID and optional source actor or command.
 * - **Traceability**: Embeds a UUID in every context, allowing events to be grouped and tracked
 *   as part of a parent command or workflow.
 * - **Spring Integration**: Registered as a Spring bean using the `@Component` annotation, making
 *   it easily accessible through dependency injection.
 *
 * Use Cases:
 * - Grouping related events under a single parent context for traceability.
 * - Associating events with high-level commands or workflows.
 * - Facilitating consistent lifecycle management of event-driven processes.
 *
 * Spring Annotations:
 * - `@Component`: Registers this provider as a Spring-managed bean, enabling dependency injection
 *   wherever `CommandEventContext` creation is required.
 *
 * @property eventPublisher The `ApplicationEventPublisher` used for publishing events within the created contexts.
 */
@Component
class EventsProvider(
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * Factory method to create a new instance of `CommandEventContext`.
     *
     * This method creates a fully initialised `CommandEventContext`, pre-configured with the
     * application's `eventPublisher` and optional parameters for specifying the event's
     * source actor and primary command. Each context is assigned a unique
     * identifier (UUID) for traceability and grouping of related events.
     *
     * @param eventSourceActor Optional source of the event, identifying the originator (e.g. WEB, SYSTEM).
     * @param eventCommand Optional primary command associated with the event, representing the main
     *   action or purpose (e.g. PROCESS_COMMAND, RESET).
     * @return A fully initialised `CommandEventContext` instance, ready to manage and publish events.
     */
    fun createRootParentEventContext(
        eventSourceActor: EventSourceActor? = null,
        eventCommand: EventCommand = EventCommand.UNDEFINED,
        eventValue: String? = null,
    ): CommandEventContext =
        CommandEventContext(
            eventPublisher = eventPublisher,
            eventSourceActor = eventSourceActor,
            eventCommand = eventCommand,
            commandContents = eventValue,
        )

    /**
     * Factory method to create a new instance of `EventBatch`.
     *
     * This method creates an `EventBatch`, pre-configured with the application's `eventPublisher`
     * and optional parameters for specifying the batch's source actor and purpose.
     *
     * @param eventSourceActor Optional source of the batch event, identifying the originator (e.g. SYSTEM, USER).
     * @param batchContents Optional full contents or metadata of the batch, describing its purpose or actions.
     * @return A fully initialised `EventBatch` instance, ready to manage and publish events.
     */
    fun createEventBatch(
        eventSourceActor: EventSourceActor? = null,
        batchContents: String? = null,
    ): EventBatch =
        EventBatch(
            eventPublisher = eventPublisher,
            eventSourceActor = eventSourceActor,
            batchContents = batchContents,
        )

    /**
     * Creates a new `CommandEventContext` and associates it with an existing `EventBatch`.
     *
     * This method creates a new `CommandEventContext`, assigns it to the given `EventBatch`,
     * and ensures traceability by associating the batch's UUID with the context.
     *
     * @param batch The batch to which the new context will belong.
     * @param eventSourceActor Optional source of the event.
     * @param eventCommand Optional primary command associated with the context.
     * @return A fully initialised `CommandEventContext` instance that is part of the given batch.
     */
    fun createParentEventContextForBatch(
        batch: EventBatch,
        eventSourceActor: EventSourceActor? = null,
        eventCommand: EventCommand = EventCommand.UNDEFINED,
        eventValue: String? = null,
    ): CommandEventContext {
        val context =
            CommandEventContext(
                eventPublisher = eventPublisher,
                eventSourceActor = eventSourceActor,
                eventCommand = eventCommand,
                commandContents = eventValue,
            )
        batch.addContext(context) // Add the new context to the batch
        return context
    }

    /**
     * Factory method to create and publish a new `RoboKeyEvent`.
     *
     * This method creates a `RoboKeyEvent` instance based on the given parameters and
     * automatically publishes it using the `ApplicationEventPublisher`.
     *
     * @param source The source object triggering the event.
     * @param parentEvent The parent event group associated with this event (e.g., `CommandEventContext` or `EventBatch`).
     * @param eventType The type of event being created, indicating its purpose or nature.
     * @param eventData Additional data or metadata to associate with the event.
     * @return The created `RoboKeyEvent` instance.
     */
    fun createRoboKeyEvent(
        eventSourceActor: EventSourceActor,
        parentEvent: EventGroup,
        eventType: EventType,
        eventData: String? = null,
        parentEventId: String? = null,
    ): RoboKeyEvent {
        val event =
            RoboKeyEvent(
                eventSourceActor = eventSourceActor,
                parentEvent = parentEvent,
                eventType = eventType,
                eventData = eventData ?: "No additional data",
                eventPublisher = eventPublisher,
                parentEventId = parentEventId ?: parentEvent.uuid,
            )

        return event
    }
}
