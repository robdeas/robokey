package tech.robd.robokey.events

/**
 * Represents a group of events in the RoboKey system.
 *
 * This sealed class provides a common base for managing both individual event contexts
 * (`CommandEventContext`) and batches of contexts (`EventBatch`), ensuring type safety
 * and consistent traceability across the event system.
 */
sealed class EventGroup {
    abstract val eventSourceActor: EventSourceActor?

    /**
     * The primary command associated with this event group, it wont actually be relevant to a batch
     */
    abstract val eventCommand: EventCommand

    /**
     * Unique identifier for the event group.
     */
    abstract val uuid: String

    /**
     * Checks if this instance represents a batch.
     * can also do is EventBatch which will then cast so probably more useful usually
     */
    abstract fun isBatch(): Boolean
}
