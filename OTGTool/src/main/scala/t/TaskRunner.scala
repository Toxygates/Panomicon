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
 */
abstract class Tasklet(val name: String) {
  self =>

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

  def toTask = new AtomicTask[Unit](name) {
    def run() = {
      try {
        self.run()
        Success(())
      } catch {
        case e: Exception => Failure(e)
      }
    }
  }
}

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

/**
 * All the actual work in any non-trivial Task will happen in an AtomicTask,
 * which are tracked by the TaskRunner.
 */
abstract class AtomicTask[+T](val name: String) extends Task[T] {
  def run(): T

  def execute(): Try[T] = {
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

  // Converts a list of tasklets into a task
  implicit class ConvertTasklets(tasklets: Iterable[Tasklet]) {
    def toTask = {
      assert(tasklets.size > 0)
      tasklets.tail.foldLeft[Task[Unit]](tasklets.head.toTask) { (task: Task[_], tasklet: Tasklet) =>
        task.flatMap(_ => tasklet.toTask)
      }
    }
  }

  // Implicitly converts a list of tasklets into a task
  implicit def IterableToTask(tasklets: Iterable[Tasklet]): Task[Unit] = {
    assert(tasklets.size > 0)
    tasklets.tail.foldLeft[Task[Unit]](tasklets.head.toTask) { (task: Task[_], tasklet: Tasklet) =>
      task.flatMap(_ => tasklet.toTask)
    }
  }

  // Converts a tasklet into an atomic tasklet
  implicit def TaskletToAtomicTask(tasklet: Tasklet) = tasklet.toTask

  @volatile var currentAtomicTask: Option[AtomicTask[_]] = None

  @volatile private var _currentTask: Option[Tasklet] = None
  @volatile private var tasks: Vector[Tasklet] = Vector()
  @volatile private var _shouldStop = false
  @volatile private var _available: Boolean = true

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
   * The new version of the runThenFinally method, which runs Tasks. The old
   * version will eventually be deleted; for now we make sure that only one
   * Task *or* Tasklet can be running at any given time, by sharing the
   * _available flag.
   */
  def runThenFinally2(task: Task[_])(cleanup: => Unit):Future[Unit] = synchronized {
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

  def runThenFinally(tasklets: Iterable[Tasklet])(cleanup: => Unit):
    Future[Unit] = synchronized {
    if (!available) {
      throw new Exception("TaskRunner is busy.")
    }

    _resultMessages = Vector()
    _shouldStop = false
    _errorCause = None
    _available = false
    tasks = tasklets.toVector

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
        if (_currentTask == None) {
          _shouldStop = true
        } else {
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
        }
      }
      println("TaskRunner stopping")
      for (r <- _resultMessages) {
        println(r)
      }
      cleanup
      _available = true
    }
  }

  def runThenFinally(tasklet: Tasklet)(cleanup: => Unit): Future[Unit] =
    runThenFinally(List(tasklet))(cleanup)

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

}
