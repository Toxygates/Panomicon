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

package otg

import com.gdevelop.gwt.syncrpc.SyncProxy
import t.viewer.client.rpc.SparqlService
import t.common.shared.SampleClass
import scala.collection.JavaConversions._
import otgviewer.shared.OTGSchema

object SampleSearch {
  import ProxyTools._

  def showHelp() {
    println("Usage: sampleSearch (url) param1=val1 param2=val2 ...")
    val s = new OTGSchema()
    val allParams = s.macroParameters() ++
      List(s.majorParameter(), s.mediumParameter(), s.minorParameter())
    println("Valid parameters include: " + allParams.mkString(" "))
  }

  def main(args: Array[String]) {
    if (args.length < 2) {
      showHelp()
      System.exit(1)
    }

    val url = args(0)
    SyncProxy.setBaseURL(url)

    val sparqlServiceAsync = getProxy(classOf[SparqlService], "sparql")

    val sc = new SampleClass()
    for (kv <- args.drop(1)) {
      val s = kv.split("=")
      val (k, v) = (s(0), s(1))
      sc.put(k, v)
    }
    println(sc)

    val unwantedKeys = List("id", "x")

    val r = sparqlServiceAsync.samples(sc)
    if (r.length == 0) {
      println("No samples found.")
    } else {
      for (s <- r) {
        val m = mapAsScalaMap(s.sampleClass().getMap)
        val filteredm = m.filter( x => ! unwantedKeys.contains(x._1))
        println(s.id() + "\t" + filteredm.map(x => x._1 + "=" + x._2).mkString("\t"))
      }
      println(r.map(_.id).mkString(" "))
    }
  }

}
