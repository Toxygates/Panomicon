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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TaskRunnerTest extends TTestSuite {

  test("basic") {
    var hasRun = false
    val t = Tasklet.simple("simple", () => { hasRun = true })
    TaskRunner.runAndStop(t)
    hasRun should equal(true)
  }

  test("logging and progress") {
    val t = new Tasklet("simple") {
      def run() {
        log("logged")
        while(shouldContinue(50)) {
          Thread.sleep(100)
        }
        println("tasklet stopping")
      }
    }

    TaskRunner += t
    TaskRunner.queueSize() should equal(1)
    TaskRunner.start()
    Thread.sleep(200)
    TaskRunner.currentTask should equal(Some(t))
    TaskRunner.waitingForTask should equal(true)
    TaskRunner.shutdown()
    Thread.sleep(2000)
    TaskRunner.logMessages should contain("logged")
    TaskRunner.currentTask should equal(None)
  }

  test("Exception") {
    var hasRun = false
    val e = new Exception("trouble")
    val t2 = Tasklet.simple("none", () => { hasRun = true })
    val t = new Tasklet("simple") {
      def run() {
        throw e
      }
    }
    TaskRunner += t
    TaskRunner += t2
    TaskRunner.queueSize() should equal(2)
    TaskRunner.start()
    Thread.sleep(2000)
    TaskRunner.currentTask should equal(None)
    TaskRunner.waitingForTask should equal(false)
    TaskRunner.errorCause should equal(Some(e))
    TaskRunner.queueSize() should equal(0)
    hasRun should equal(false)

    //verify that we can use it again after the error

    TaskRunner += t2
    TaskRunner.start()
    Thread.sleep(100)
    hasRun should equal(true)
    TaskRunner.shutdown()
  }

}
