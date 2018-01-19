/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t

import scala.concurrent._

object Tasklet {
  def simple(name: String, f:() => Unit) = new Tasklet(name) {
    def run() {
      f()
    }
  }
}

/**
 * Tasklets are named, small, interruptible tasks
 * designed to run in sequence on a single thread dedicated to Tasklet running.
 * Note: it may be possible to achieve a simpler design by using Futures instead
 *  (with the andThen mechanism)
 */
abstract class Tasklet(val name: String) {
  import scala.concurrent.ExecutionContext.Implicits.global

  def run(): Unit

  // The tasklet should regularly update this variable.
  @volatile
  protected var _percentComplete: Double = 0.0

  def percentComplete: Int = {
    val r = _percentComplete.toInt
    if (r > 100) 100 else r
  }

  /**
   * Tasks can use this inside a work loop to cancel gracefully
   */
  def shouldContinue(pc: Double): Boolean = {
    _percentComplete = pc
    !TaskRunner.shouldStop
  }

  def log(message: String) = TaskRunner.log(message)

  def logResult(message: String) = TaskRunner.logResult(name + ": " + message)
}

/**
 * For testing
 */
class TestTask(name: String) extends Tasklet(name) {
  def run(): Unit = {
    Thread.sleep(2000)
    println("Running...")
    Thread.sleep(2000)
  }
}

/**
 * For testing
 */
class FailingTask(name: String) extends Tasklet(name) {
  def run() {
    throw new Exception("failure!")
  }
}

/**
 * A way of running tasks, on a single thread, in a way that lets
 * them be monitored or stopped.
 * Tasks are queued up and run sequentially.
 */
object TaskRunner {
  import scala.concurrent.ExecutionContext.Implicits.global

  @volatile private var _currentTask: Option[Tasklet] = None
  @volatile private var tasks: Vector[Tasklet] = Vector()
  @volatile private var _shouldStop = false
  @volatile private var _logMessages: Vector[String] = Vector()
  @volatile private var _resultMessages: Vector[String] = Vector()
  @volatile private var _errorCause: Option[Throwable] = None

  def queueSize(): Int = {
    tasks.size
  }

  /**
   * The task that is currently running or most recently completed.
   */
  def currentTask: Option[Tasklet] =  _currentTask

  /**
   * Whether a task is currently busy. Even if this is false, the queue
   * is not necessarily empty.
   */
  def waitingForTask: Boolean = { _currentTask != None }

  def shouldStop = _shouldStop

  /**
   * Obtain log messages in time order and remove them from the log
   */
  def logMessages: Iterable[String] = synchronized {
    val r = _logMessages
    _logMessages = Vector()
    r
  }

  /**
   * Obtain result messages in time order and remove them from the log
   */
  def resultMessages: Iterable[String] = synchronized {
    val r = _resultMessages
    _resultMessages = Vector()
    r
  }

  def +=(task: Tasklet) = synchronized {
    tasks :+= task
  }

  def ++=(tasks: Iterable[Tasklet]) = synchronized {
    for (t <- tasks) {
      this += t
    }
  }

  def log(msg: String) = synchronized {
    _logMessages :+= msg
    println(msg)
  }

  def logResult(msg: String) = synchronized {
    _resultMessages :+= msg
  }

  def errorCause: Option[Throwable] = _errorCause

  def start(): Unit = synchronized {
    _resultMessages = Vector()
    _shouldStop = false
    _errorCause = None
    Future {
      println("TaskRunner starting")
      while (!shouldStop) {
        TaskRunner.synchronized {
          if (!tasks.isEmpty) {
            println(tasks.size + " tasks in queue")
            _currentTask = tasks.headOption
            tasks = tasks.tail
          }
        }
        if (_currentTask != None) {
          val nextt = _currentTask.get
          log("Start task \"" + nextt.name + "\"")
          try {
            nextt.run() // could take a long time to complete
            log("Finish task \"" + nextt.name + "\"")
            _currentTask = None
          } catch {
            case t: Throwable =>
              _currentTask = None
              log("Error while running task " + nextt.name + ": " + t.getMessage())
              t.printStackTrace() //TODO pass exception to log
              log("Deleting remaining tasks")
              _errorCause = Some(t)
              shutdown()
          }
        } else {
          Thread.sleep(50)
        }
      }
      //Received the stop signal
      println("TaskRunner stopping")
      for (r <- _resultMessages) {
        println(r)
      }
    }
  }

  /**
   * Stop the runner and drop any remaining non-started tasks.
   * Note that an outstanding task could still be running.
   * Clients should check waitingForTask to verify the state.
   */
  def shutdown(): Unit = synchronized {
    println("TaskRunner shutdown requested")
    _shouldStop = true
    tasks = Vector()
  }

  /**
   * Shutdown, as well as forcibly drop the current task
   */
  def reset(): Unit = synchronized {
    shutdown()
    _currentTask = None
  }

  def runAndStop(tasklet: Tasklet) {
    runAndStop(List(tasklet))
  }

  def runAndStop(tasklets: Iterable[Tasklet]) {
    TaskRunner ++= tasklets
    try {
      start()
      while (currentTask != None || queueSize() > 0) {
        Thread.sleep(1000)
      }
    } finally {
      shutdown()
    }
  }

}
