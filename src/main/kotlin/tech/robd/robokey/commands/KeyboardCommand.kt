package tech.robd.robokey.commands

/**
 * These are the commands implemented in the Kotlin parts of the code, if using an Arduino there are many more commands,
 * which are sent unprocessed to the Arduino.
 *
 * note at the end of the text in some commands is a colon that means there are additional parameters required
 *
 * The LOREM_LINES_B ... is a way of allowing different separators in the commands.
 * There are not likely to be many, as more sophisticated commands are handled in the arduino code
 */
enum class KeyboardCommand(val textValue: String) {
    PING("ping"),
    LOREM("lorem"),
    LOREM_LINES("lorem_lines"),
    LOREM_LINES_B("lorem-lines"),
    LOREM_LINES_C("lorem lines"),
    TEXT("text:"),
    LINE("line:"),
    KEY("key:"),
    COMBO("combo:"),
    ;

    val txt: String
        get() = textValue

    val uc: String
        get() = textValue.uppercase()

    val lc: String
        get() = textValue.lowercase()

    override fun toString(): String {
        return textValue
    }

    companion object {
        // Function to get a list of all textValues
        fun getAllTextValues(): List<String> {
            return PriorityKeyboardCommand.entries.map { it.textValue }
        }
    }
}