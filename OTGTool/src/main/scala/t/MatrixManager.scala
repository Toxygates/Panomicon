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

import scala.collection.JavaConversions._

import t.db._
import t.sparql.Batches
import t.sparql.SampleClassFilter

/**
 * Mid-level copy tool for copying matrix data between different formats.
 * Leaves other databases (such as sample/probe index, series data) untouched.
 */
object MatrixManager extends ManagerTool {

  def apply(args: Seq[String], m: Manager[_, _])(implicit context: Context): Unit = {

    def config = context.config
    def factory = context.factory

    def samplesInBatch(batch: String) = {
      val sf = t.sparql.SampleFilter(None, Some(Batches.packURI(batch)))
      context.samples.samples(SampleClassFilter())(sf)
    }
    
    def matcopy[E >: Null <: ExprValue](from: MatrixDBReader[E],
      batch: Option[String],
      getDB: () => MatrixDBWriter[PExprValue],      
      formVal: E => FoldPExpr,
      label: String)(implicit mat: MatrixContext) {
      val allProbes = mat.probeMap.keys.toSeq.sorted
      def allSamples = from.sortSamples(mat.sampleMap.tokens.map(Sample(_)).toSeq)
      
      val useSamples = batch.map(samplesInBatch).getOrElse(allSamples)
      
      for (ss <- useSamples.grouped(50)) {
        val vs = from.valuesInSamples(ss, allProbes)
        val svs = Map() ++ (ss zip vs)
        val raw = new RawExpressionData {
          val samples = ss
          def data(s: Sample) = Map() ++
            svs(s).filter(!from.isEmptyValue(_)).map(v => v.probe -> formVal(v))
        }
        val t = new SimplePFoldValueInsert(getDB, raw).
          insert(s"$label")
        TaskRunner.runAndStop(t)
        println(s"$ss ($label)")
      }
    }

    args(0) match {
      case "copy" =>
        val todir = require(stringOption(args, "-toDir"),
          "Please specify a destination directory with -toDir")
        val tsconfig = config.triplestore
        val dataParams = (Map() ++ mapAsScalaMap(System.getenv())) + ("T_DATA_DIR" -> todir)
        val toDConfig = m.getDataConfig(dataParams)
        val toBConfig = m.makeBaseConfig(tsconfig, toDConfig)
        //If the batch is specified, only that batch will be copied.
        //Otherwise, all batches are copied.
        val batch = stringOption(args, "-batch")

        implicit val mat = context.matrix

        //No log-2 wrap
        matcopy[PExprValue](config.data.foldsDBReaderNowrap(mat),
            batch,
          () => toDConfig.extWriter(toDConfig.foldDb),
          v => (v.value, v.call, v.p),
          "Insert folds")

        matcopy[ExprValue](mat.absoluteDBReader,
            batch,
          () => toDConfig.extWriter(toDConfig.exprDb),
          v => (v.value, v.call, 0.0),
          "Insert absolute values")

      case _ => showHelp()
    }
  }

  def showHelp(): Unit = {
    throw new Exception("Please specify a command (copy/...)")
  }
}
