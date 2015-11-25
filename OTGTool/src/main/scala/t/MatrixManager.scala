package t
import java.lang.System
import scala.collection.JavaConversions._

object MatrixManager extends ManagerTool {

  def apply(args: Seq[String], m: Manager[_, _])(implicit context: Context): Unit = {

    def config = context.config
    def factory = context.factory

    args(0) match {
      case "copy" =>
        val todir = require(stringOption(args, "-toDir"),
            "Please specify a destination directory with -toDir")
        val tsconfig = config.triplestore
        val dataParams = (Map() ++ mapAsScalaMap(System.getenv())) + ("T_DATA_DIR" -> todir)
        val toDConfig = m.getDataConfig(dataParams)
        val toBConfig = m.makeBaseConfig(tsconfig, toDConfig)

        
        
      case _ => showHelp()
    }
  }

  def showHelp(): Unit = {
     throw new Exception("Please specify a command (copy/...)")
  }
}
