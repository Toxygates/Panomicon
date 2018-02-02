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

@RunWith(classOf[JUnitRunner])
class TaskRunnerTest extends TTestSuite {

//  test("runAndStop") {
//    var hasRun = false
//    val t = Tasklet.simple("simple", () => { hasRun = true })
//    TaskRunner.runAndStop(t)
//    hasRun should equal(true)
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.waitingForTask should equal(false)
//  }
//
//  test("basic with no shutdown") {
//    var hasRun = false
//    val t = Tasklet.simple("simple", () => { hasRun = true })
//    TaskRunner.start()
//    TaskRunner += t
//    while (!hasRun) {
//      Thread.sleep(1000)
//    }
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.waitingForTask should equal(false)
//  }
//
//  //TODO it would be better if the tests don't depend on timings like this
//  test("basic with shutdown") {
//    var hasRun = false
//    val t = Tasklet.simple("simple", () => { hasRun = true })
//    TaskRunner += t
//    TaskRunner.start()
//    while (!hasRun) {
//      Thread.sleep(100)
//    }
//    TaskRunner.shutdown
//    Thread.sleep(3000)
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.waitingForTask should equal(false)
//  }
//
//  test("logging and progress") {
//    val t = new Tasklet("simple") {
//      def run() {
//        log("logged")
//        while(shouldContinue(50)) {
//          Thread.sleep(100)
//        }
//        println("tasklet stopping")
//      }
//    }
//
//    TaskRunner += t
//    TaskRunner.queueSize() should equal(1)
//    TaskRunner.start()
//    Thread.sleep(200)
//    TaskRunner.currentTask should equal(Some(t))
//    TaskRunner.waitingForTask should equal(true)
//    TaskRunner.shutdown()
//    Thread.sleep(2000)
//    TaskRunner.logMessages should contain("logged")
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.waitingForTask should equal(false)
//  }
//
//  test("Exception") {
//    var hasRun = false
//    val e = new Exception("trouble")
//    val t2 = Tasklet.simple("none", () => { hasRun = true })
//    val t = new Tasklet("simple") {
//      def run() {
//        throw e
//      }
//    }
//    TaskRunner += t
//    TaskRunner += t2
//    TaskRunner.queueSize() should equal(2)
//    TaskRunner.start()
//    Thread.sleep(2000)
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.waitingForTask should be(false)
//    TaskRunner.errorCause should equal(Some(e))
//    TaskRunner.queueSize() should equal(0)
//    hasRun should equal(false)
//
//    //verify that we can use it again after the error
//
//    TaskRunner += t2
//    TaskRunner.start()
//    Thread.sleep(100)
//    hasRun should equal(true)
//    TaskRunner.shutdown()
//  }
//
//  test("Interrupted task") {
//    var finished = false
//    val t = new Tasklet("interruptable") {
//      def run() {
//        while(shouldContinue(0.5)) {
//          println("working")
//          Thread.sleep(100)
//        }
//        finished = true
//      }
//    }
//
//    TaskRunner += t
//    TaskRunner.start()
//    Thread.sleep(500)
//    TaskRunner.currentTask should equal(Some(t))
//    TaskRunner.waitingForTask should be(true)
//    TaskRunner.queueSize() should equal(0)
//
//    TaskRunner.shutdown()
//    Thread.sleep(500)
//    TaskRunner.currentTask should equal(None)
//    TaskRunner.waitingForTask should be(false)
//    TaskRunner.queueSize() should equal(0)
//    finished should be(true)
//  }

}
