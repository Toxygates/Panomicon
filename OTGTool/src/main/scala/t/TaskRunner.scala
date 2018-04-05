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
import scala.language.implicitConversions
import scala.util.Try
import scala.util.Success
import scala.util.Failure

/**
 * New monadic Task API that allows for comprehensions
 * Tasks are synchronously executing tasks with deferred execution, typically meant
 * to be run somewhere other than the main thread.
 */
trait Task[+T] {
  self =>

  def execute(): Try[T]

  def map[S](f: T => S): Task[S] = new Task[S] {
    def execute(): Try[S] = {
      self.execute() match {
        case Success(r) => Success(f(r))
        case Failure(t) => Failure(t)
      }
    }
  }

  def flatMap[S](f: T => Task[S]): Task[S] = new Task[S] {
    def execute(): Try[S] = {
      self.execute() match {
        case Success(r) => {
          val newTask = f(r)
          if (!TaskRunner.shouldStop) {
            newTask.execute()
          } else {
            Failure(new Exception("TaskRunner aborted"))
          }
        }
        case Failure(t) => Failure(t)
      }
    }
  }

  def andThen[S](t: Task[S]): Task[S] = {
    flatMap(_ => t)
  }
}

object Task {
  def success = new Task[Unit] {
    def execute = Success(())
  }
}

object AtomicTask {
  def simple[T](name: String)(doWork: T) = new AtomicTask[T](name) {
    override def run(): T = doWork
  }
}

/**
 * All the actual work in any non-trivial Task will happen in AtomicTasks,
 * which are tracked by the TaskRunner.
 */
abstract class AtomicTask[+T](val name: String) extends Task[T] {
  def run(): T

  // The task should regularly update this variable.
  @volatile
  protected var _percentComplete: Double = 0.0

  def percentComplete: Int = {
    val r = _percentComplete.toInt
    if (r > 100) 100 else r
  }

  /**
   * Tasks should periodically call this method to update progress and check
   * whether they should abort.
   */
  def shouldContinue(pc: Double): Boolean = {
    _percentComplete = pc
    !TaskRunner.shouldStop
  }

  def execute(): Try[T] = {
    if (!TaskRunner.shouldStop) {
      TaskRunner.currentAtomicTask = Some(this)
      log("Start task \"" + name + "\"")
      try {
        val result = run()
        log("Finish task \"" + name + "\"")
        Success(result)
      } catch {
        case e: Exception =>
          log("Failed task \"" + name + "\"")
          Failure(e)
      }
    } else {
      Failure(new Exception("Task not started due to TaskRunner shutdown"))
    }
  }

  def log(message: String) = TaskRunner.log(message)
  def logResult(message: String) = TaskRunner.logResult(s"$name: $message")
}

/**
 * A way of running tasks, on a single thread, in a way that lets
 * them be monitored or stopped.
 * Tasks are queued up and run sequentially.
 */
object TaskRunner {
  import scala.concurrent.ExecutionContext.Implicits.global

  @volatile var currentAtomicTask: Option[AtomicTask[_]] = None

  @volatile private var _shouldStop = false
  @volatile private var _available: Boolean = true

  @volatile private var _logMessages: Vector[String] = Vector()
  @volatile private var _resultMessages: Vector[String] = Vector()
  @volatile private var _errorCause: Option[Throwable] = None

  def shouldStop = _shouldStop

  /**
   * When true, the TaskRunner is available to receive tasks
   */
  def available = _available

  def busy = !available

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

  def log(msg: String) = synchronized {
    _logMessages :+= msg
    println(msg)
  }

  def logResult(msg: String) = synchronized {
    _resultMessages :+= msg
  }

  def errorCause: Option[Throwable] = _errorCause

  /**
   * Run a task, and then perform some cleanup regardless of whether the task
   * finished successfully. Does nothing if TaskRunner is unavailable.
   */
  def runThenFinally(task: Task[_])(cleanup: => Unit):Future[Unit] = synchronized {
    if (!available) {
      throw new Exception("TaskRunner is busy.")
    }

    _resultMessages = Vector()
    _shouldStop = false
    _errorCause = None
    _available = false

    Future{
      println("TaskRunner starting")
      // Do we need a way to print number of tasks in queue?
      task.execute() match {
        case Success(r) =>
        case Failure(t) => {
          log(s"Error while running task ${currentAtomicTask.get.name}: ${t.getMessage()}")
          t.printStackTrace()
          log("Remaining tasks will not be executed")
          _errorCause = Some(t)
        }
      }
      println("TaskRunner stopping")
      for (r <- _resultMessages) {
        println(r)
      }
      cleanup
      currentAtomicTask = None
      _available = true
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
  }

}
