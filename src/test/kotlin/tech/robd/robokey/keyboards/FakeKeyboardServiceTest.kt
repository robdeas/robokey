package tech.robd.robokey.keyboards

import io.mockk.*
import org.junit.jupiter.api.*
import reactor.test.StepVerifier
import tech.robd.robokey.AppConfig
import java.io.File

class FakeKeyboardServiceTest {
    private lateinit var fakeKeyboardService: FakeKeyboardService

    @BeforeEach
    fun setUp() {
        val appConfig = mockk<AppConfig>()
        fakeKeyboardService = spyk(FakeKeyboardService(appConfig = appConfig), recordPrivateCalls = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test logRawDataToFile creates file and writes lines`() {
        val testFilePath = "test-log/test-log.txt"
        val testLines = listOf("line1", "line2", "line3")

        ensureFileAndParentDirsDoNotExist(testFilePath)

        StepVerifier.create(fakeKeyboardService.logRawDataToFile(testFilePath, testLines))
            .verifyComplete()

        verifyFileContent(testFilePath, testLines)

        cleanUpTestFiles(testFilePath)
    }

    private fun ensureFileAndParentDirsDoNotExist(filePath: String) {
        val testFile = File(filePath)
        if (testFile.exists()) {
            testFile.delete()
        }
        testFile.parentFile?.deleteRecursively()
    }

    private fun verifyFileContent(filePath: String, expectedLines: List<String>) {
        val testFile = File(filePath)
        assert(testFile.exists())
        val fileContent = testFile.readLines()
        assert(fileContent == expectedLines)
    }

    private fun cleanUpTestFiles(filePath: String) {
        val testFile = File(filePath)
        testFile.delete()
        testFile.parentFile?.deleteRecursively()
    }
}