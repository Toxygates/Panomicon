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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Promise
import scala.util.Random
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class TaskRunnerTest extends TTestSuite {

  def runAndWait[T](task: Task[T], maxDuration: Duration = 200 millis): Unit = {
    val future = TaskRunner.runThenFinally(task)(())
    Await.ready(future, maxDuration)
  }

  test("Basic") {
    var hasRun = false
    val t = Task.simple("simple") {
      hasRun = true
    }
    hasRun should equal(false)
    runAndWait(t)
    println("Done waiting")
    hasRun should equal(true)
    TaskRunner.currentAtomicTask should equal(None)
    TaskRunner.available should equal(true)
  }

  test("Logging and progress") {
    val promise = Promise[Unit]()
    val t = new AtomicTask[Unit]("simple") {
      override def run() = {
        promise.success(())
        log("logged")
        while(shouldContinue(50)) {
          Thread.sleep(10)
        }
        println("tasklet stopping")
      }
    }

    val future = TaskRunner.runThenFinally(t)(())
    Await.result(promise.future, 200 millis)
    TaskRunner.currentAtomicTask should equal(Some(t))
    TaskRunner.available should equal(false)
    TaskRunner.shutdown()
    Await.result(future, 200 millis)
    TaskRunner.logMessages should contain("logged")
    TaskRunner.currentAtomicTask should equal(None)
    TaskRunner.available should equal(true)
  }

  test("flatMap and task result") {
    val randomInt = Random.nextInt()
    val task1 = Task.simple[Int]("Output integer") {
      randomInt
    }
    def task2(int: Int) = Task.simple[Boolean]("Check integer value") {
      int == randomInt
    }

    val future1 = TaskRunner.runThenFinally(
      for {
        int <- task1
        result <- task2(int)
      } yield result)(())

    Await.result(future1, 200 millis) should equal(true)

    val future2 = TaskRunner.runThenFinally(
      for {
        int <- task1
        result <- task2(int + 1)
      } yield result)(())

    Await.result(future2, 200 millis) should equal(false)
  }

  test("Exception") {
    var t2HasRun = false
    val e = new Exception("trouble")
    val t = Task.simple[Unit]("throw exception") {
      throw e
    }
    val t2 = Task.simple("set hasRun flag") { t2HasRun = true }

    runAndWait(t andThen t2)
    TaskRunner.currentAtomicTask should equal(None)
    TaskRunner.available should be(true)
    TaskRunner.errorCause should equal(Some(e))
    t2HasRun should equal(false)

    //verify that we can use the TaskRunner again after the error
    runAndWait(t2)
    t2HasRun should equal(true)
    TaskRunner.shutdown()
  }

  test("Interrupted task") {
    val promise = Promise[Unit]()
    var finished = false
    val t = new AtomicTask[Unit]("interruptable") {
      override def run() {
        promise.success(())
        while(shouldContinue(0.5)) {
          println("working")
          Thread.sleep(10)
        }
        finished = true
      }
    }

    val future = TaskRunner.runThenFinally(t)(())
    Await.result(promise.future, 200 millis)
    TaskRunner.currentAtomicTask should equal(Some(t))
    TaskRunner.available should be(false)

    TaskRunner.shutdown()
    Await.result(future, 200 millis)
    TaskRunner.currentAtomicTask should equal(None)
    TaskRunner.available should be(true)
    finished should be(true)
  }
}
