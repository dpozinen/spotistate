package com.spotistate.service

import com.spotistate.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@Service
class ScheduledTasks(
    private val userRepository: UserRepository,
    private val spotifyService: SpotifyService
) {
    private val logger = LoggerFactory.getLogger(ScheduledTasks::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Run daily at 2:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    fun reimportAllUserLibraries() {
        logger.info("Starting scheduled library reimport for all users at {}", LocalDateTime.now())
        val users = userRepository.findAll()
        
        logger.info("Found {} users to process", users.size)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        
        runBlocking {
            val startTime = System.currentTimeMillis()
            
            // Create a deferred task for each user
            val deferredResults = users.map { user ->
                coroutineScope.async {
                    try {
                        logger.info("Reimporting library for user: {} ({})", user.displayName, user.id)
                        val playlists = spotifyService.importUserLibrary(user.id)
                        logger.info("Successfully reimported {} playlists for user {}", playlists.size, user.id)
                        successCount.incrementAndGet()
                        true
                    } catch (e: Exception) {
                        logger.error("Failed to reimport library for user: {} ({}). Error: {}", 
                            user.displayName, user.id, e.message, e)
                        failureCount.incrementAndGet()
                        false
                    }
                }
            }
            
            // Wait for all coroutines to complete
            deferredResults.awaitAll()
            
            val totalTime = System.currentTimeMillis() - startTime
            logger.info("Completed scheduled library reimport in {}ms. Success: {}, Failures: {}", 
                totalTime, successCount.get(), failureCount.get())
        }
    }
}
