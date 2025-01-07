import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import purejavacomm.CommPortIdentifier
import purejavacomm.SerialPort
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Integration test class for sending commands to an Arduino device and verifying responses.
 * NOTE: The PC client program cannot be running at the same time as integration tests.
 * Otherwise, there will be a port conflict
 *
 * This class establishes a serial port connection to an Arduino, sends commands,
 * and verifies the responses.
 *
 * It provides an almost full test of the Arduino and kotlin code, except it
 * does not type anything
 */
class ArduinoCommandIntegrationTest {
    private lateinit var serialPort: SerialPort
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream

    @BeforeEach
    fun setup() {
        // Initialize serial port connection
        val portId = CommPortIdentifier.getPortIdentifier("COM3") // Replace with actual port if necessary
        serialPort = portId.open(this::class.java.name, 2000) as SerialPort
        serialPort.setSerialPortParams(
            9600, // Baud rate
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_1,
            SerialPort.PARITY_NONE,
        )

        // Assign outputStream and inputStream from serialPort directly
        outputStream = serialPort.outputStream
        inputStream = serialPort.inputStream
    }

    @AfterEach
    fun teardown() {
        // Close streams and serial port after each test
        outputStream.close()
        inputStream.close()
        serialPort.close()
    }

    /**
     * Sends a pause command to the Arduino device and verifies the response.
     * The method:
     * 1. Generates a unique UUID for the pause command.
     * 2. Constructs the pause command string and sends it through the outputStream.
     * 3. Reads and verifies the JSON response to confirm the pause status.
     * 4. Sends a resume command with a new UUID to undo the pause.
     * 5. Reads and verifies the JSON response to confirm the resume status.
     *
     * The pause command response is verified to ensure that:
     * - The `paused` status in the response is `"yes"`.
     * - The UUID in the response matches the UUID of the pause command sent.
     *
     * The resume command response is verified to ensure that:
     * - The `paused` status in the response is `"no"`.
     */
    @Test
    fun `send pause command and verify response`() {
        val testUUIDPause = UUID.randomUUID().toString() // Generate a unique UUID for this command
        val command = "!$testUUIDPause,CMD:PAUSE\n" // Construct the command with UUID

        // Send the command
        outputStream.write(command.toByteArray())
        outputStream.flush()

        // Read all JSON responses until we get a full one
        val pauseResponses = readFullJsonResponse()
        pauseResponses.forEach { response ->
            println("Complete JSON response for pause: $response")
            if (response.contains(testUUIDPause)) {
                val parsedResponse = parseJsonResponse(response)
                assertEquals("yes", parsedResponse["paused"], "Pause status should be 'yes'")
                assertEquals(testUUIDPause, parsedResponse["commandUUID"], "UUID should match the pause command UUID")
            }
        }
        // Now send the resume command to undo the pause
        val testUUIDResume = UUID.randomUUID().toString() // Generate a unique UUID for the resume command
        val commandResume = "!$testUUIDResume,CMD:RESUME\n" // Construct the resume command with UUID

        // Send the resume command
        outputStream.write(commandResume.toByteArray())
        outputStream.flush()

        // Read and verify the resume response
        val resumeResponses = readFullJsonResponse()
        resumeResponses.forEach { response ->
            println("Complete JSON response for resume: $response")
            if (response.contains(testUUIDResume)) {
                val parsedResponse = parseJsonResponse(response)
                assertEquals("no", parsedResponse["paused"]) // Check if paused status is reset
            }
        }
    }

    /**
     * Reads data from the inputStream, assembling it into complete JSON responses.
     * The method continues reading until it either receives at least one full JSON response
     * or until the timeout of 5 seconds is reached.
     *
     * @return A list of complete JSON response strings.
     */
    private fun readFullJsonResponse(): List<String> {
        val buffer = ByteArray(4096)
        val responseBuilder = StringBuilder()
        val timeoutMillis = 5000 // 5 seconds timeout
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (inputStream.available() > 0) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break // End of stream
                val chunk = String(buffer, 0, bytesRead)

                // Append and print each chunk
                println("Chunk received: $chunk")
                responseBuilder.append(chunk)

                // Check if we have at least one full JSON response (ends with `}\n`)
                if (responseBuilder.contains("}\n")) {
                    println("Detected end of JSON response:\n$responseBuilder")
                    break
                }
            } else {
                Thread.sleep(50) // Small delay to wait for more data
            }
        }

        // Split accumulated response by `}\n` to get individual JSON responses
        return responseBuilder
            .toString()
            .split("}\n")
            .map { it.trim() + "}" } // Append the closing brace back to each response
            .filter { it.startsWith("{") } // Ensure it's a JSON object
    }

    private fun parseJsonResponse(response: String): Map<String, String> {
        // Basic parsing example (assumes Arduino JSON responses)
        val result = mutableMapOf<String, String>()
        val regex = "\"(\\w+)\":\\s*\"(.*?)\"".toRegex()
        regex.findAll(response).forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }
        return result
    }
}
