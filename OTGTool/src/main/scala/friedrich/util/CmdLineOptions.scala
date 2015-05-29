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

/**
 * Part of the Friedrich bioinformatics framework.
 * Copyright (C) Gabriel Keeble-Gagnere and Johan Nystrom-Persson 2010-2012.
 * Dual GPL/MIT license. Please see the files README and LICENSE for details.
 */
package friedrich.util

trait CmdLineOptions {

  /**
   * Look for an option in the form of a string.
   * E.g.  to look for -input file.txt, use
   * "-input" as the ident parameter.
   */
  protected def stringOption(args: Seq[String], ident: String): Option[String] = {
    val i = args.indexOf(ident)
    if (i != -1 && i < args.size - 1) {
      Some(args(i+1))
    } else {
      None
    }
  }
  
  /**
   * Look for a list of options at the end of the command line.
   */
  protected def stringListOption(args: Seq[String], ident: String): Option[List[String]] = {
    val i = args.indexOf(ident)
    if (i != -1 && i < args.size - 1) {
      Some(args.drop(i + 1).toList)
    } else {
      None
    }
  }
  
  /**
   * Look for an option in the form of a integer.
   * E.g.  to look for -k 31, use
   * "-k" as the ident parameter.
   */
  protected def intOption(args: Seq[String], ident: String): Option[Int] = {
    stringOption(args, ident).map(_.toInt)
  }
  
  protected def longOption(args: Seq[String], ident: String): Option[Long] = {
    stringOption(args, ident).map(_.toLong)
  }
  
  /**
   * Look for an option in the form of a double.
   * E.g.  to look for -threshold 3.2, use
   * "-threshold" as the ident parameter.
   */
  protected def doubleOption(args: Seq[String], ident: String): Option[Double] = {
    stringOption(args, ident).map(_.toDouble)
  }
  
  /**
   * Look for a boolean option.
   * If ident is "-verbose", then true will be returned if the
   * flag is present, false otherwise.
   */
  protected def booleanOption(args: Seq[String], ident: String): Boolean = {
     args.indexOf(ident) != -1        
  }
  
  /**
   * If this method wraps a call to resolveInt or resolveString (the varieties
   * that return an option), an error will result when a necessary option is missing.
   */
  protected def require[T](parameter: Option[T], message: String): T = parameter match {
    case None => {      
      Console.err.println(message)          
      throw new Exception("Incorrect or missing option")
    }
    case Some(x) => x
  }
   
}