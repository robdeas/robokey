/*
 * Copyright (C) 2024 Rob Deas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.  

 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.  
 */
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

        val testLines = listOf("line1", "line2", "line3")

        ensureFileAndParentDirsDoNotExist()

        StepVerifier.create(fakeKeyboardService.logRawDataToFile(TEST_FILE_PATH, testLines))
            .verifyComplete()

        verifyFileContent( testLines)

        cleanUpTestFiles()
    }

    private fun ensureFileAndParentDirsDoNotExist() {
        val testFile = File(Companion.TEST_FILE_PATH)
        if (testFile.exists()) {
            testFile.delete()
        }
        testFile.parentFile?.deleteRecursively()
    }

    private fun verifyFileContent(expectedLines: List<String>) {
        val testFile = File(Companion.TEST_FILE_PATH)
        assert(testFile.exists())
        val fileContent = testFile.readLines()
        assert(fileContent == expectedLines)
    }

    private fun cleanUpTestFiles() {
        val testFile = File(Companion.TEST_FILE_PATH)
        testFile.delete()
        testFile.parentFile?.deleteRecursively()
    }

    companion object {
        private const val TEST_FILE_PATH = "test-log/test-log.txt"
    }
}