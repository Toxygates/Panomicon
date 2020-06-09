package t.manager

import friedrich.util.CmdLineOptions

trait ManagerTool extends CmdLineOptions {
  def expectArgs(args: Seq[String], n: Int) {
    if (args.size < n) {
      showHelp()
      throw new Exception("Insufficient arguments")
    }
  }

  def startTaskRunner(task: Task[_]) {
    TaskRunner.runThenFinally(task)(())
  }

  def showHelp(): Unit
}
