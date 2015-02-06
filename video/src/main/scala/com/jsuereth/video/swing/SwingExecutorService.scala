package com.jsuereth.video.swing

import akka.dispatch.{ DispatcherPrerequisites, ExecutorServiceFactory, ExecutorServiceConfigurator }
import com.typesafe.config.Config
import java.util.concurrent.{ ExecutorService, AbstractExecutorService, ThreadFactory, TimeUnit }
import java.util.Collections
import javax.swing.SwingUtilities

// First we wrap invokeLater as an ExecutorService
object SwingExecutorService extends AbstractExecutorService {
  def execute(command: Runnable) = SwingUtilities.invokeLater(command)
  def shutdown(): Unit = ()
  def shutdownNow() = Collections.emptyList[Runnable]
  def isShutdown = false
  def isTerminated = false
  def awaitTermination(l: Long, timeUnit: TimeUnit) = true
}

// Then we create an ExecutorServiceConfigurator so that Akka can use our SwingExecutorService for the dispatchers
class SwingEventThreadExecutorServiceConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends ExecutorServiceConfigurator(config, prerequisites) {
  private val f = new ExecutorServiceFactory {
    def createExecutorService: ExecutorService = SwingExecutorService
  }
  def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = f
}