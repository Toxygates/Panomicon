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

import scala.collection.JavaConversions._

import friedrich.util.CmdLineOptions

/**
 * Management tool for T framework applications.
 */
abstract class Manager[C <: Context, B <: BaseConfig] {

  def requireEnv(env: scala.collection.Map[String, String], key: String, errMsg: String) =
    env.getOrElse(key, throw new Exception(s"Missing environment variable $key: $errMsg"))

  import scala.collection.{ Map => CMap }

  def getTSConfig(env: CMap[String, String]): TriplestoreConfig =
    TriplestoreConfig(
      requireEnv(env, "T_TS_URL", "Please specify triplestore URL"),
      env.getOrElse("T_TS_UPDATE_URL", null),
      requireEnv(env, "T_TS_USER", "Please specify triplestore username"),
      requireEnv(env, "T_TS_PASS", "Please specify triplestore password"),
      requireEnv(env, "T_TS_REPO", "Please specify triplestore repository"))

  def getDataConfig(env: CMap[String, String]): DataConfig =
    new DataConfig(
      requireEnv(env, "T_DATA_DIR", "Please specify data directory"),
      requireEnv(env, "T_DATA_MATDBCONFIG", "Please specify matrix db flags"))

  def getBaseConfig(): B = {
    val env = mapAsScalaMap(System.getenv())
    val ts = getTSConfig(env)
    val d = getDataConfig(env)
    makeBaseConfig(ts, d)
  }

  def makeBaseConfig(ts: TriplestoreConfig, d: DataConfig): B

  def factory: Factory
  def initContext(bc: B): C

  def main(args: Array[String]) {
    implicit val c = initContext(getBaseConfig)

    if (args.length < 1) {
      showHelp()
      sys.exit(1)
    }

    try {
      handleArgs(args)
    } catch {
      case e: Exception => e.printStackTrace
    }
    sys.exit(0) // Get rid of lingering threads
  }

  protected def showHelp() {
    println("Please supply one of the following commands")
    println(" batch, instance, platform, help")
  }

  protected def handleArgs(args: Array[String])(implicit context: C) {
    args(0) match {
      case "batch"    => BatchManager(args.drop(1))
      case "instance" => InstanceManager(args.drop(1))
      case "platform" => PlatformManager(args.drop(1))
      case "help"     => showHelp()
    }
  }
}

trait ManagerTool extends CmdLineOptions {
  def withCloseable[T](cl: Closeable)(f: => T): T = {
    try {
      f
    } finally {
      cl.close()
    }
  }

  def expectArgs(args: Seq[String], n: Int) {
    if (args.size < n) {
      showHelp()
      throw new Exception("Insufficient arguments")
    }
  }

  /**
   * Start the TaskRunner, run a series of tasklets while printing
   * log messages, then stop it again.
   */
  def withTaskRunner(tasklets: Iterable[Tasklet]) {
    TaskRunner ++= tasklets
    TaskRunner.start()
    try {
      while (TaskRunner.currentTask != None) {
        for (m <- TaskRunner.logMessages) {
          println(m)
        }
        TaskRunner.currentTask match {
          case Some(t) => println(s"${t.name} - ${t.percentComplete}%")
          case _       =>
        }
        Thread.sleep(2000)
      }
      TaskRunner.errorCause match {
        case None    => //all good
        case Some(e) => throw e
      }
    } finally {
      TaskRunner.shutdown()
    }
  }

  def showHelp(): Unit
}
