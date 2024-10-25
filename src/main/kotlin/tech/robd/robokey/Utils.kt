package tech.robd.robokey

import com.ibm.icu.text.CharsetDetector
import com.ibm.icu.text.CharsetMatch
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Utility class that provides various helper functions for string manipulation, charset detection,
 * and operating system-specific behaviors. This class centralizes common utility methods that
 * can be used throughout the application.
 */
class Utils {
    companion object : Logable {
        private val log = setupLogs

        /**
         * The default character set used by non-Windows systems, which is UTF-8.
         */
        private val DEFAULT_CHARSET = StandardCharsets.UTF_8

        /**
         * The default character set for Windows, which is UTF-16LE.
         */
        private val WINDOWS_DEFAULT_CHARSET = StandardCharsets.UTF_16LE

        /**
         * Converts a given string into its hexadecimal byte representation.
         *
         * This function takes a string, converts it to a byte array, and then converts each byte to
         * its hexadecimal representation. The hexadecimal values are returned as a string, with
         * each byte separated by a space.
         *
         * ### Example:
         * ```
         * val hexString = Utils.getByteValuesAsHexString("ABC")
         * // hexString will be "41 42 43"
         * ```
         *
         * @param text The input string to convert to hexadecimal.
         * @return A string of space-separated hexadecimal byte values.
         */
        fun getByteValuesAsHexString(text: String): String {
            return text.toByteArray().joinToString(" ") { String.format("%02X", it) }
        }

        /**
         * Detects the charset of a given file.
         *
         * This function reads the bytes of a file and uses the ICU4J `CharsetDetector` to detect
         * the most likely character set for the file. If a charset is successfully detected, it is returned;
         * otherwise, the function logs a warning and returns `null`.
         *
         * ### Example:
         * ```
         * val charset = Utils.detectCharset("path/to/file.txt")
         * if (charset != null) {
         *     println("Detected charset: $charset")
         * } else {
         *     println("Charset detection failed.")
         * }
         * ```
         *
         * @param filePath The path to the file for which the charset needs to be detected.
         * @return The name of the detected charset, or `null` if detection fails.
         */
        fun detectCharset(filePath: String): String? {
            val path = Paths.get(filePath)
            return try {
                val bytes = Files.readAllBytes(path)
                val detector = CharsetDetector()
                detector.setText(bytes)
                val match: CharsetMatch? = detector.detect()

                if (match != null) {
                    log.info("Charset detected: ${match.name}")
                    match.name
                } else {
                    log.warn("No charset match found.")
                    null
                }
            } catch (e: Exception) {
                log.error("Error detecting charset for file $filePath: ${e.message}", e)
                null
            }
        }

        /**
         * Checks if the operating system is Windows.
         *
         * This function examines the system property `"os.name"` and determines if the current
         * operating system is a Windows variant by checking if the name contains `"win"`.
         *
         * @return `true` if the operating system is Windows, `false` otherwise.
         */
        fun isOsWindows(): Boolean {
            return System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
        }

        /**
         * Returns the default character set for the operating system.
         *
         * On Windows, the default character set is UTF-16LE. On non-Windows systems, the default
         * character set is UTF-8. This function logs the detected operating system and returns the
         * appropriate default charset.
         *
         * ### Why This Matters:
         * - The default character set varies across operating systems, and this function allows
         *   the application to automatically adapt to the environment.
         *
         *  NOTE: Be careful it appears Windows commands could append UTF-8 characters to a UTF16_LE file
         *
         *
         * @return The default character set for the current operating system.
         */
        fun getOsDefaultCharset(): Charset {
            return if (isOsWindows()) {
                log.info("Detected Windows OS. Using default charset UTF-16LE.")
                WINDOWS_DEFAULT_CHARSET
            } else {
                log.info("Detected non-Windows OS. Using default charset UTF-8.")
                DEFAULT_CHARSET
            }
        }
    }
}
