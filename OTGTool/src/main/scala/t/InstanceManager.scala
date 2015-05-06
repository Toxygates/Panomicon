package t
import t.sparql._

import t.sparql.Instances

/**
 * Instance management CLI
 */
object InstanceManager extends ManagerTool {
  def apply(args: Seq[String])(implicit context: Context): Unit = {
    
    val instances = new Instances(context.config.triplestore)

    args(0) match {
      case "add" =>
        expectArgs(args, 2)
        //TODO move verification into the instances API
        if (!instances.list.contains(args(1))) {
          instances.add(args(1))
        } else {
          val msg = s"Instance ${args(1)} already exists"
          throw new Exception(msg)
        }

      case "list" =>
        println("Instance list")
        for (i <- instances.list) println(i)
        println("(end of list)")

      case "delete" =>
        expectArgs(args, 2)
        if (instances.list.contains(args(1))) {
          instances.delete(args(1))
        } else {
          val msg = s"Instance ${args(1)} does not exist"
          throw new Exception(msg)
        }

      //TODO: access management
      case "help" => showHelp()
    }    
  }

  def showHelp() {

  }

}