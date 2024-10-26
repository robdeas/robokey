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
package tech.robd.robokey.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for managing task-related dependencies.
 *
 * This configuration provides beans for the coroutine-based task management system, including:
 * - A `CoroutineScope` for managing task lifecycles.
 * - A `TaskPoolManager` that uses the provided coroutine scope to execute tasks asynchronously.
 *
 * The configuration is set up using Spring's `@Configuration` and `@Bean` annotations, allowing these
 * beans to be injected into other parts of the application.
 */
@Configuration
open class TaskManagementConfig {

    /**
     * Provides a `CoroutineScope` for managing the lifecycle of coroutines in the application.
     *
     * This scope uses a **`SupervisorJob`** to ensure that the failure of one task does not
     * cancel other sibling tasks, allowing for independent failure handling.
     *
     * The scope also uses **`Dispatchers.IO`**, optimised for I/O-bound tasks such as
     * file or network operations, allowing them to run on a dedicated thread pool without blocking
     * the main or CPU-bound threads.
     *
     * ### Why `SupervisorJob`?
     * - A `SupervisorJob` allows the coroutines launched in this scope to fail independently. If one coroutine
     *   fails, it will not affect the others running in the same scope.
     *
     * ### Why `Dispatchers.IO`?
     * - `Dispatchers.IO` is used for offloading I/O operations like file reading/writing, database access, or network
     *   calls, allowing them to run on a thread pool specifically designed for I/O-bound work.
     *
     * @return A `CoroutineScope` using `SupervisorJob` and `Dispatchers.IO` for optimal task execution.
     */
    @Bean
    open fun taskScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    /**
     * Provides a `TaskPoolManager` bean that uses the provided `CoroutineScope` to manage tasks.
     *
     * The `TaskPoolManager` will execute tasks within the provided coroutine scope, ensuring that
     * all tasks are run asynchronously and respect the lifecycle management provided by the scope.
     *
     * @param scope The `CoroutineScope` used to manage task lifecycles and ensure proper cancellation, error handling, and
     * resource management for all running tasks.
     * @return A `TaskPoolManager` that manages tasks within the given coroutine scope.
     */
    @Bean
    open fun taskPoolManager(scope: CoroutineScope): TaskPoolManager {
        return TaskPoolManager(scope)
    }
}
