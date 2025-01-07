package tech.robd.robokey.events

/**
 * Enum representing recognized commands in the RoboKey event system.
 *
 * Each value in this enum defines a specific command that the system can handle,
 * allowing consistent and type-safe command references throughout the codebase.
 *
 * Usage:
 * - Allows the event system to interpret commands as standardized, pre-defined values.
 * - Supports the event-driven architecture by categorizing actions into manageable command types.
 *
 * Example:
 * ```
 * when (command) {
 *     EventCommand.START -> // handle start command
 *     EventCommand.STOP -> // handle stop command
 *     ...
 * }
 * ```
 */
enum class EventCommand {
    IS_ALIVE,
    LOREM,
    LOREM_LINES,
    STOP_TYPING,
    TEXT,
    UNDEFINED, // It is likely that some commands will be determined later
    UNDEFINED2, // TODO merge these into undefined just here to check event sources
    UNDEFINED3, // TODO merge these into undefined just here to check event sources
    KEY_PRESS,
    RESUME_TYPING,
    RESET_KEYBOARD,
    PAUSE_TYPING,
    HELP,
    TYPE_HELP,
    PING,
    STATUS,
    LINE,
    KEY,
    COMBO,
    EDIT,
    HOLD,
    CMD_OUTPUT_OFF,
    CMD_OUTPUT_ON,
    CMD_SET_DELAY,
    CMD_SET_PRESS_LENGTH,
    CMD_RESET,
    CMD_ECHO_ON,
    CMD_ECHO_OFF,
    CMD_DEBUG_ON,
    CMD_DEBUG_OFF,
    CMD_JITTER_ON,
    CMD_JITTER_OFF,
    CMD_KEY_JITTER_ON,
    CMD_KEY_JITTER_OFF,
    CMD_DELAY_JITTER_ON,
    CMD_DELAY_JITTER_OFF,
    CMD_SET_KEY_JITTER_VALUE,
    CMD_SET_DELAY_JITTER_VALUE,
    CMD_CONNECT,
    CMD_DISCONNECT,
    CMD_RECONNECT,
    CMD_SIMULATE_ON,
    CMD_SIMULATE_OFF,
    CMD_STOP,
    CMD_PAUSE,
    CMD_RESUME,
    RELEASE_KEY,
    RELEASE_ALL_KEYS,
    PASSWORD,
    PRIVATE_TEXT,
    INVALID,
    BATCH,
    SETUP,
}
