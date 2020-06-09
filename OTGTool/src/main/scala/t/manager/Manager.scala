package t.manager

import scala.collection.JavaConverters._
import friedrich.util.CmdLineOptions
import t.global.KCDBRegistry
import t.platform.SSOrthTTL
import t.{BaseConfig, Context, DataConfig, Factory, TriplestoreConfig}
import t.platform.Species._

/**
 * Management tool for T framework applications.
 */
class Manager extends CmdLineOptions {
  import scala.collection.{Map => CMap}

  def requireEnv(env: scala.collection.Map[String, String], key: String, errMsg: String) =
    env.getOrElse(key, throw new Exception(s"Missing environment variable $key: $errMsg"))

  def getTSConfig(env: CMap[String, String]): TriplestoreConfig =
    TriplestoreConfig(
      requireEnv(env, "T_TS_URL", "Please specify triplestore URL"),
      env.getOrElse("T_TS_UPDATE_URL", null),
      env.getOrElse("T_TS_USER", null),
      env.getOrElse("T_TS_PASS", null),
      env.getOrElse("T_TS_REPO", null))

  def getDataConfig(env: CMap[String, String]): DataConfig =
    factory.dataConfig(
      requireEnv(env, "T_DATA_DIR", "Please specify data directory"),
      requireEnv(env, "T_DATA_MATDBCONFIG", "Please specify matrix db flags"))

  def getBaseConfig(): BaseConfig = {
    val env = System.getenv().asScala
    val ts = getTSConfig(env)
    val d = getDataConfig(env)
    makeBaseConfig(ts, d)
  }

  def makeBaseConfig(ts: TriplestoreConfig, d: DataConfig): BaseConfig =
    BaseConfig(ts, d)

  lazy val factory: Factory = new Factory()

  def initContext(bc: BaseConfig): Context = Context(bc)

  def main(args: Array[String]) {
    implicit val c = initContext(getBaseConfig)

    if (args.length < 1 || args(0) == "help") {
      showHelp()
      sys.exit(1)
    }

    val mainThread = Thread.currentThread();
    // Runs on normal shutdown or when user interrupt received
    Runtime.getRuntime.addShutdownHook(new Thread() { override def run = {
      if (TaskRunner.busy) TaskRunner.shutdown()
      KCDBRegistry.closeWriters(true)
      mainThread.join() // because program will shut down when this thread does
    }})

    try {
      handleArgs(args)
      waitForTasks()
    } catch {
      case e: Exception => e.printStackTrace
    } finally {
      KCDBRegistry.closeWriters()
    }
  }

  protected def showHelp() {
    println("Please supply one of the following commands:")
    println(" batch, instance, platform, matrix")
  }

  protected def handleArgs(args: Array[String])(implicit context: Context) {
    args(0) match {
      case "orthologs" =>
        val output = require(stringOption(args, "-output"),
          "Please specify an output file with -output")
        val intermineURL = require(stringOption(args, "-intermineURL"),
          "Please specify an intermine URL with -intermineURL, e.g. https://mizuguchilab.org/targetmine/service")
        val intermineAppName = require(stringOption(args, "-intermineAppName"),
          "Please specify an intermine app name with -intermineAppName, e.g. targetmine")

        val spPairs = Seq((Rat, Human), (Human, Mouse), (Mouse, Rat))

        val conn = new t.intermine.Connector(intermineAppName, intermineURL)
        new SSOrthTTL(context.probeStore, output).generateFromIntermine(conn, spPairs)
      case "batch"    => BatchManager(args.drop(1))
      case "instance" => InstanceManager(args.drop(1))
      case "platform" => PlatformManager(args.drop(1))
      case "matrix"   => MatrixManager(args.drop(1), this)
      case _ => showHelp()
    }
  }

  /**
   * Wait for the task runner and monitor its progress on the console
   */
  def waitForTasks() {
    var lastLog = ""
    while (!TaskRunner.available) {
      TaskRunner.currentAtomicTask match {
        case Some(t) =>
          val logMsg = s"${t.name} - ${t.percentComplete}%"
          if (logMsg != lastLog) {
            println(logMsg)
            lastLog = logMsg
          }
        case _       =>
      }
      Thread.sleep(50)
    }
  }
}
