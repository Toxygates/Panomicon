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

package t.db.file

import java.io._

import org.apache.commons.math3.stat.inference.TTest

import friedrich.util.CmdLineOptions
import t.db._
import scala.collection.mutable.HashMap
import scala.collection.{Map => CMap}

/**
 * log-2 fold values constructed from the input data
 * The sample space of the output may be smaller (control samples have no folds)
 */
abstract class FoldValueBuilder(md: Metadata, input: RawExpressionData)
  extends RawExpressionData {

  type Triple = (Sample, String, FoldPExpr)

  def samples = input.samples.filter(!md.isControl(_))

  protected lazy val groups = md.treatedControlGroups(input.samples)

  /**
   * This method may return values for more samples than the one requested. Callers should
   * inspect the results for efficiency.
   */
  def values(s: Sample): Seq[Triple] = {
    println("Compute control values")
    var r = List[Triple]()
    for ((ts, cs) <- groups;
      if ts.toSet.contains(s)) {
      println("Control barcodes: " + cs)
      println("Treated: " + ts)
      r = makeFolds(cs.toSeq, ts.toSeq, s, r)
    }
    r
  }

  /**
   * Construct fold values for a sample group.
   */
  protected def makeFolds(controlSamples: Seq[Sample],
      treatedSamples: Seq[Sample], sample: Sample,
      accumulator: List[Triple]): List[Triple]

  /**
   * Compute a control sample (as a mean).
   */
  protected def controlMeanSample(controlSamples: Seq[Sample], from: RawExpressionData): CMap[String, Double] = {
    var controlValues = HashMap[String, Double]()

    for (probe <- from.probes) {
      val usableVals = controlSamples.flatMap(from.expr(_, probe))
      if (usableVals.size > 0) {
        val mean = usableVals.sum / usableVals.size
        controlValues += (probe -> mean)
      }
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
  extends FoldValueBuilder(md, input) {
  val tt = new TTest

  //TODO: factor out some code shared with the superclass

  /**
   * @param sample one of the treated samples to build fold values for.
   */
  override protected def makeFolds(controlSamples: Seq[Sample],
    treatedSamples: Seq[Sample],
    sample: Sample,
    accumulator: List[Triple]): List[Triple] = {

    val controlMean = controlMeanSample(controlSamples, input)
    val l2 = Math.log(2)
    var pVals = Vector[Double]()

    val controlData = controlSamples.map(input.data)
    val treatedData = treatedSamples.map(input.data)
    val probes = input.probes.toSeq

    for (probe <- probes) {
      val cs = controlData.flatMap(_.get(probe)).map(_._1)
      val ts = treatedData.flatMap(_.get(probe)).map(_._1)
      val pval = if (cs.size >= 2 && ts.size >= 2) {
        tt.tTest(cs.toArray, ts.toArray)
      } else {
        Double.NaN
      }
      pVals :+= pval
    }

    var r = accumulator
    for ((p, pv) <- probes zip pVals) {
      (input.expr(sample, p), controlMean.get(p)) match {
        case (Some(v), Some(control)) =>
          val foldVal = Math.log(v / control) / l2
          val controlCalls = controlSamples.flatMap(input.call(_, p))
          val treatedCalls = treatedSamples.flatMap(input.call(_, p))
          val pacall = foldPACall(foldVal, controlCalls, treatedCalls)
          r ::= (sample, p, (foldVal, pacall, pv))
        case _ =>
          r ::= (sample, p, (Double.NaN, 'A', Double.NaN))
      }
    }
    r
  }

  import scala.collection.{Map => CMap}

  def data(s: Sample): CMap[String, FoldPExpr] = {
    val vs = values(s)
    Map() ++ vs.filter(_._1 == s).map(v => v._2 -> v._3)
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

      //TODO - avoid/clarify dependency from t.db -> otg
    val factory = new otg.OTGFactory

    val md = factory.tsvMetadata(mdfile, otg.model.sample.AttributeSet.getDefault)
    val data = new CSVRawExpressionData(List(input), List(calls),
        Some(md.samples.size), println)
    val builder = new PFoldValueBuilder(md, data)

    val output = input.replace(".csv", "_fold.csv")
    val callout = calls.replace(".csv", "_fold.csv")
    val pout = input.replace(".csv", "_fold_p.csv")

    val writer = new BufferedWriter(new FileWriter(output))
    val callwriter = new BufferedWriter(new FileWriter(callout))
    val pwriter = new BufferedWriter(new FileWriter(pout))

    val writers = List(writer, callwriter, pwriter)

    try {
      val samples = builder.samples.toSeq
      val vals = samples.flatMap(builder.values)
//      val samples = vals.map(_._1).toSeq.distinct
      for (w <- writers) { headers(w, samples) }

      val values = vals.groupBy(_._2)
      for ((p, vs) <- values; sorted = vs.toSeq.sortBy(s => samples.indexOf(s._1))) {
        for (w <- writers) { w.write("\"" + p + "\",") }

        writer.write(sorted.map(_._3._1).mkString(","))
        callwriter.write(sorted.map(x => "\"" + x._3._2 + "\"").mkString(","))
        pwriter.write(sorted.map(_._3._3).mkString(","))

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
