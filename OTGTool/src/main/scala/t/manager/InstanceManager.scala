package t.manager

import t.Context
import t.sparql.InstanceStore

/**
 * Instance management CLI
 */
object InstanceManager extends ManagerTool {
  def apply(args: Seq[String])(implicit context: Context): Unit = {

    if (args.size < 1) {
      showHelp()
    } else {
      val instances = new InstanceStore(context.config.triplestoreConfig)
      args(0) match {
        case "add" =>
          expectArgs(args, 2)
          //Note: might want to move verification into the instances API
          if (!instances.getList().contains(args(1))) {
            instances.add(args(1))
          } else {
            val msg = s"Instance ${args(1)} already exists"
            throw new Exception(msg)
          }

        case "list" =>
          println("Instance list")
          for (i <- instances.getList()) println(i)
          println("(end of list)")

        case "delete" =>
          expectArgs(args, 2)
          if (instances.getList().contains(args(1))) {
            instances.delete(args(1))
          } else {
            val msg = s"Instance ${args(1)} does not exist"
            throw new Exception(msg)
          }

          //Note: we should eventually add management of other properties, such as
          //access and visibility, here
        case "help" => showHelp()
      }
    }
  }

  def showHelp() {
    println("Please specify a command (add/list/delete)")
  }

}
