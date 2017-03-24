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
