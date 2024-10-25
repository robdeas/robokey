package tech.robd.robokey

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import tech.robd.robokey.tasks.TaskPoolManager
import java.awt.Frame
import javax.swing.SwingUtilities

@Component
class SystemExitHandler(
    private val context: ApplicationContext,  // Spring application context
    private val coroutineScope: CoroutineScope,  //  global coroutine scope
    private val taskPoolManager: TaskPoolManager // Thread pool used for tasks
) {

    companion object : Logable {
        private val log = setupLogs
    }

    fun exitProcess(status: Int) {
        log.info("System cleanup in progress...")
        logActiveThreads()
        log.info("Completing Shutdown process.")
        shutdown(status)
    }

    fun logActiveThreads() {
        val threadSet = Thread.getAllStackTraces().keys
        for (thread in threadSet) {
            log.info("Thread: ${thread.name}, Daemon: ${thread.isDaemon}, State: ${thread.state}")
        }

    }

    fun exit() {
        shutdown(0)
    }

    private fun shutdown(status: Int) {
        try {
            // 1. Shutdown coroutine dispatchers
            shutdownCoroutines()

            // 2. Shutdown custom thread pools
            shutdownThreadPool(taskPoolManager)

            // 3. Shutdown Spring Boot application (Tomcat threads)
            shutdownSpringApplication()

            // 4. Close GUI (Swing/AWT)
            shutdownSwing()

            // 5. Stop file watchers
            stopFileWatchers()

        } catch (e: Exception) {
            log.info("Error during shutdown: ${e.message}")
        } finally {
            // Forcefully terminate the JVM if necessary
            System.exit(0)
        }
    }

    private fun shutdownCoroutines() {
        log.info("Shutting down coroutines...")
        coroutineScope.cancel()
        runBlocking {
            coroutineScope.coroutineContext[Job]?.join()
        }
    }

    private fun shutdownThreadPool(taskPoolManager: TaskPoolManager) {
        log.info("Shutting down thread pool...")
        taskPoolManager.shutdown()
    }

    private fun shutdownSpringApplication() {
        log.info("Shutting down Spring Boot application context...")
        SpringApplication.exit(context)
    }

    private fun shutdownSwing() {
        log.info("Shutting down Swing/AWT components...")
        SwingUtilities.invokeLater {
            val frames = Frame.getFrames()
            for (frame in frames) {
                frame.dispose()  // Close all open windows
            }
        }
    }

    private fun stopFileWatchers() {
        log.info("Stopping file watchers...")
        // TODO logic to stop file watching threads goes here
    }
}
