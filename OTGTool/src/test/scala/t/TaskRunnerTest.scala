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

@RunWith(classOf[JUnitRunner])
class TaskRunnerTest extends TTestSuite {

//  def runAndWait(tasklets: Iterable[Tasklet], maxDuration: Duration = 200 millis):
//    Unit  = {
//    val future = TaskRunner.runThenFinally(tasklets)(())
//    Await.result(future, maxDuration)
//  }
//
//  def runAndWaitSingle(tasklet: Tasklet, maxDuration: Duration = 200 millis): Unit =
//    runAndWait(List(tasklet), maxDuration)
//
//  test("basic") {
//    var hasRun = false
//    val t = Tasklet.simple("simple", () => { hasRun = true })
//    runAndWaitSingle(t)
//    hasRun should equal(true)
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.available should equal(true)
//  }
//
//  test("logging and progress") {
//    val promise = Promise[Unit]()
//    val t = new Tasklet("simple") {
//      def run() {
//        promise.success(())
//        log("logged")
//        while(shouldContinue(50)) {
//          Thread.sleep(10)
//        }
//        println("tasklet stopping")
//      }
//    }
//
//    val future = TaskRunner.runThenFinally(t)(())
//    Await.result(promise.future, 200 millis)
//    TaskRunner.currentTask should equal(Some(t))
//    TaskRunner.available should equal(false)
//    TaskRunner.shutdown()
//    Await.result(future, 200 millis)
//    TaskRunner.logMessages should contain("logged")
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.available should equal(true)
//  }
//
//  test("Exception") {
//    var hasRun = false
//    val e = new Exception("trouble")
//    val t = new Tasklet("simple") {
//      def run() {
//        throw e
//      }
//    }
//    val t2 = Tasklet.simple("none", () => { hasRun = true })
//
//    runAndWait(List(t, t2))
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.available should be(true)
//    TaskRunner.errorCause should equal(Some(e))
//    TaskRunner.queueSize() should equal(0)
//    hasRun should equal(false)
//
//    //verify that we can use it again after the error
//
//    runAndWaitSingle(t2)
//    hasRun should equal(true)
//    TaskRunner.shutdown()
//  }
//
//  test("Interrupted task") {
//    val promise = Promise[Unit]()
//    var finished = false
//    val t = new Tasklet("interruptable") {
//      def run() {
//        promise.success(())
//        while(shouldContinue(0.5)) {
//          println("working")
//          Thread.sleep(10)
//        }
//        finished = true
//      }
//    }
//
//    val future = TaskRunner.runThenFinally(t)(())
//    Await.result(promise.future, 200 millis)
//    TaskRunner.currentTask should equal(Some(t))
//    TaskRunner.available should be(false)
//    TaskRunner.queueSize() should equal(0)
//
//    TaskRunner.shutdown()
//    Await.result(future, 200 millis)
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.available should be(true)
//    TaskRunner.queueSize() should equal(0)
//    finished should be(true)
//  }

}
