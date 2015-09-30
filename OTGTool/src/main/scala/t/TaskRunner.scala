/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

/**
 * TODO consider replacing with Futures
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
 * A way of running administration tasks in a way that lets
 * them be monitored or stopped.
 * Tasks are queued up and run sequentially.
 */
object TaskRunner {
  import scala.concurrent.ExecutionContext.Implicits.global

  @volatile private var _currentTask: Option[Tasklet] = None
  private var tasks: Vector[Tasklet] = Vector()
  @volatile private var _shouldStop = false
  private var _logMessages: Vector[String] = Vector()
  @volatile private var _resultMessages: Vector[String] = Vector()
  @volatile private var _errorCause: Option[Throwable] = None
  @volatile private var _waitingForTask: Boolean = false

  def queueSize(): Int = synchronized {
    tasks.size
  }

  /**
   * The task that is currently running or most recently completed.
   */
  def currentTask: Option[Tasklet] = _currentTask

  /**
   * Whether a task is currently busy. Even if this is false, the queue
   * is not necessarily empty.
   */
  def waitingForTask: Boolean = _waitingForTask
  def shouldStop = _shouldStop

  /**
   * Obtain log messages in time order
   * (and remove them from the log)
   */
  def logMessages: Iterable[String] = synchronized {
    val r = _logMessages
    _logMessages = Vector()
    r
  }

  def resultMessages: Iterable[String] = _resultMessages

  def +=(task: Tasklet) = synchronized {
    tasks :+= task
    if (_currentTask == None) {
      _currentTask = Some(task)
    }
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

  def start(): Unit = {
    _resultMessages = Vector()
    _shouldStop = false
    _errorCause = None
    Future {
      println("TaskRunner starting")
      while (!shouldStop) {
        var next: Tasklet = null
        synchronized {
          if (!tasks.isEmpty) {
            println(tasks.size + " tasks in queue")
            next = tasks.head
            _currentTask = Some(next)
            tasks = tasks.tail
          }
        }
        if (next != null) {
          log("Start task \"" + next.name + "\"")
          try {
            _waitingForTask = true
            next.run() // could take a long time to complete
            _waitingForTask = false
            log("Finish task \"" + next.name + "\"")
          } catch {
            case t: Throwable =>
              log("Error while running task " + next.name + ": " + t.getMessage())
              t.printStackTrace() //TODO pass exception to log
              log("Deleting remaining tasks")
              _errorCause = Some(t)
              _waitingForTask = false
              shutdown()
          }
        }
        Thread.sleep(1000)
      }
      println("TaskRunner stopping")
      for (r <- resultMessages) {
        println(r)
      }
    }
  }

  def shutdown(): Unit = synchronized {
    _shouldStop = true
    tasks = Vector()
    //Note that an outstanding task could still be running.
    //Clients should check waitingForTask to verify the state.
  }

  def runAndStop(tasklet: Tasklet) {
    runAndStop(List(tasklet))
  }

  def runAndStop(tasklets: Iterable[Tasklet]) {
    TaskRunner ++= tasklets
    try {
      start()
      while (currentTask != None) {
        Thread.sleep(1000)
      }
    } finally {
      shutdown()
    }
  }

}
