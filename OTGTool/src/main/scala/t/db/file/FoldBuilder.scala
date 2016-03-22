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

package t.db.file

import friedrich.util.CmdLineOptions
import java.io._
import org.apache.commons.math3.stat.inference.TTest
import t.db.RawExpressionData
import t.db.Metadata
import t.db.Sample
import t.db.PExprValue
import t.db.ExprValue
import otg.Factory
import scala.Vector
import t.db.FoldPExpr

/**
 * log-2 fold values constructed from the input data
 * The sample space of the output may be smaller (control samples have no folds)
 */
abstract class FoldValueBuilder[E <: ExprValue](md: Metadata, input: RawExpressionData)
  extends RawExpressionData {

  lazy val values: Iterable[(Sample, String, E)] = {
    println("Compute control values")
    val treatedSamples = input.data.keys.filter(x => !md.isControl(x))
    val groups = treatedSamples.groupBy(md.controlSamples(_))
    var r = Vector[(Sample, String, E)]()

    for ((cxs, xs) <- groups) {
      println("Control barcodes: " + cxs)
      println("Non-control: " + (xs.toSet -- cxs))
      r ++= makeFolds(cxs, xs.toSet -- cxs)
    }
    r
  }

  /**
   * Construct fold values for a sample group.
   */
  protected def makeFolds(controlSamples: Iterable[Sample],
      treatedSamples: Iterable[Sample]): Iterable[(Sample, String, E)]

  /**
   * Compute a control sample (as a mean).
   * @param expr: expression values for each barcode. (probe -> value)
   * @param calls: calls for each barcode (probe -> call)
   * @param cbs: control barcodes.
   */
  protected def controlMeanSample(cbs: Iterable[Sample]): Map[String, Double] = {
    var controlValues = Map[String, Double]()
    for (probe <- input.probes) {
      val eval = cbs.map(input.expr(_, probe)).sum / cbs.size
      val acalls = cbs.map(input.call(_, probe)).toList.distinct
      //      val call = acalls.reduce(safeCall)
      controlValues += (probe -> eval)
    }
    controlValues
  }

  protected def foldPACall(log2fold: Double, controlCalls: Iterable[Char],
    treatedCalls: Iterable[Char]): Char = {

    //Treat M as A
    val controlPresent = controlCalls.count(_ == 'P') > controlCalls.size / 2
    val treatedPresent = treatedCalls.count(_ == 'P') > treatedCalls.size / 2
    if (log2fold > 0 && treatedPresent && !controlPresent) {
      'P'
    } else if (log2fold < 0 && !treatedPresent && controlPresent) {
      'P'
    } else if (treatedPresent && controlPresent) {
      'P'
    } else {
      'A'
    }
  }
}

/**
 * log-2 fold values with P-values.
 * This could be stored in a separate table, but for simplicity, we are grouping it with
 * expression data for now.
 */
class PFoldValueBuilder(md: Metadata, input: RawExpressionData)
  extends FoldValueBuilder[PExprValue](md, input) {
  val tt = new TTest

  //TODO: factor out some code shared with the superclass

  override protected def makeFolds(controlSamples: Iterable[Sample],
    treatedSamples: Iterable[Sample]): Iterable[(Sample, String, PExprValue)] = {

    val controlMean = controlMeanSample(controlSamples)
    var r = Vector[(Sample, String, PExprValue)]()
    val l2 = Math.log(2)
    var pVals = Map[String, Double]()

    for (probe <- input.probes) {
      val cs = controlSamples.map(input.expr(_, probe))
      val ts = treatedSamples.map(input.expr(_, probe))
      val pval = if (cs.size >= 2 && ts.size >= 2) {
        tt.tTest(cs.toArray, ts.toArray)
      } else {
        Double.NaN
      }
      pVals += (probe -> pval)
    }

    for (x <- treatedSamples) {
      println(x)
      r ++= input.probes.map(p => {
        val control = controlMean(p)
        val foldVal = Math.log(input.expr(x, p) / control) / l2
        val pacall = foldPACall(foldVal, controlSamples.map(input.call(_, p)),
          treatedSamples.map(input.call(_, p)))
        (x, p, PExprValue(foldVal, pVals(p), pacall))
      })
    }
    r
  }

  import scala.collection.{Map => CMap}

  lazy val data: CMap[Sample, CMap[String, FoldPExpr]] = {
    val bySample = values.groupBy(_._1)
    val byProbe = bySample.mapValues(x => x.groupBy(_._2))
    byProbe.mapValues(v => v.mapValues(_.map(p => (p._3.value, p._3.call, p._3.p)).head))
  }
}

object FoldBuilder extends CmdLineOptions {

  def main(args: Array[String]) {
    val input = require(stringOption(args, "-input"),
      "Please specify input file with -input")
    val calls = require(stringOption(args, "-calls"),
      "Please specify calls file with -calls")

    val mdfile = require(stringOption(args, "-metadata"),
      "Please specify metadata file with -metadata")

    val factory = new otg.Factory //TODO

    val md = factory.tsvMetadata(input)
    val data = new CSVRawExpressionData(List(input), Some(List(calls)))
    val builder = new PFoldValueBuilder(md, data)

    val output = input.replace(".csv", "_fold.csv")
    val callout = calls.replace(".csv", "_fold.csv")
    val pout = input.replace(".csv", "_fold_p.csv")

    val writer = new BufferedWriter(new FileWriter(output))
    val callwriter = new BufferedWriter(new FileWriter(callout))
    val pwriter = new BufferedWriter(new FileWriter(pout))

    val writers = List(writer, callwriter, pwriter)

    try {
      val vals = builder.values
      val samples = vals.map(_._1).toSeq.distinct
      for (w <- writers) { headers(w, samples) }

      val values = vals.groupBy(_._2)
      for ((p, vs) <- values; sorted = vs.toSeq.sortBy(s => samples.indexOf(s._1))) {
        for (w <- writers) { w.write("\"" + p + "\",") }

        writer.write(sorted.map(_._3.value).mkString(","))
        callwriter.write(sorted.map(x => "\"" + x._3.call + "\"").mkString(","))
        pwriter.write(sorted.map(_._3.p).mkString(","))

        for (w <- writers) { w.newLine() }
      }
    } finally {
      for (w <- writers) { w.close() }
    }
  }

  def headers(writer: BufferedWriter, samples: Iterable[Sample]) {
    writer.write("\"\",")
    writer.write(samples.map("\"" + _.identifier + "\"").mkString(","))
    writer.newLine()
  }

}
