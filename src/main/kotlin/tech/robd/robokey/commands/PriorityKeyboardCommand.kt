package tech.robd.robokey.commands

/**
 * Enum class representing priority keyboard commands.
 *
 * These commands have higher priority and can interrupt or override other queued commands.
 * They are primarily used for controlling the state of the system (such as stopping, pausing or resuming the keyboard operation).
 *
 * These commands are represented as strings and can be accessed in various cases (including uppercase and lowercase).
 *
 * @param textValue The text value of the command (e.g., "STOP", "PAUSE").
 */
enum class PriorityKeyboardCommand(val textValue: String) {
    /**
     * Command to stop the keyboard operation.
     */
    STOP("STOP"),

    /**
     * Command to pause the keyboard operation.
     */
    PAUSE("PAUSE"),

    /**
     * Command to resume the keyboard operation after a pause.
     */
    RESUME("RESUME"),

    /**
     * Command to reset the keyboard or system state.
     */
    RESET("RESET");

    /**
     * Returns the original `textValue` of the command.
     */
    val txt: String
        get() = textValue

    /**
     * Returns the uppercase version of the command's `textValue`.
     */
    val uc: String
        get() = textValue.uppercase()

    /**
     * Returns the lowercase version of the command's `textValue`.
     */
    val lc: String
        get() = textValue.lowercase()

    /**
     * Provides a string representation of the enum by returning the command's `textValue`.
     *
     * @return The text value of the command (e.g., "STOP", "PAUSE").
     */
    override fun toString(): String {
        return textValue
    }

    companion object {
        /**
         * Retrieves a list of all the `textValue` strings for each priority command.
         *
         * @return A list of all the text values (e.g., ["STOP", "PAUSE", "RESUME", "RESET"]).
         */
        fun getAllTextValues(): List<String> {
            return PriorityKeyboardCommand.entries.map { it.textValue }
        }
    }
}
