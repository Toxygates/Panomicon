package t

/**
 * A command line tool for a T application.
 */
trait Tool {

  def factory: Factory
  
  def main(args: Array[String]) {
    runCommand(args(0), args.drop(1))
  }
  
  def runCommand(cmd: String, args: Seq[String]): Unit = {
    
  }
  
}