package tech.robd.robokey

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.robd.robokey.commands.CommandProcessor
import tech.robd.robokey.commands.CommandProcessorService

/**
 * Spring configuration class for general application-wide beans.
 *
 * This configuration class provides beans for command processing, making the `CommandProcessor`
 * available as a Spring-managed bean throughout the application. It integrates with the `CommandProcessorService`
 * to obtain and manage the lifecycle of the command processor.
 *
 * @param commandProcessorService The service responsible for managing and providing the `CommandProcessor`.
 */
@Configuration
open class GeneralApplicationConfig(
    private val commandProcessorService: CommandProcessorService
) {

    /**
     * Provides a `CommandProcessor` bean.
     *
     * This bean is created by delegating to the `CommandProcessorService`, which manages the instantiation
     * and lifecycle of the `CommandProcessor`. The command processor is responsible for handling
     * all commands related to the keyboard system, including processing, queuing, and managing commands.
     *
     * @return The `CommandProcessor` instance, managed by Spring as a bean.
     */
    @Bean
    open fun commandProcessor(): CommandProcessor {
        return commandProcessorService.getCommandProcessor()
    }
}
