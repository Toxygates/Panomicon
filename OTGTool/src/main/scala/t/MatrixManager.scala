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

package t
import java.lang.System
import scala.collection.JavaConversions._
import t.db.MatrixInsert
import t.db.kyotocabinet.KCMatrixDB
import t.db.RawExpressionData
import t.db.SimplePFoldValueInsert
import t.db.AbsoluteValueInsert
import t.global.KCDBRegistry
import t.db.Sample

/**
 * Mid-level copy tool for copying matrix data between different formats.
 * Leaves other databases (such as sample/probe index, series data) untouched.
 */
object MatrixManager extends ManagerTool {

  def apply(args: Seq[String], m: Manager[_, _])(implicit context: Context): Unit = {

    def config = context.config
    def factory = context.factory

    KCDBRegistry.setMaintenance(true)

    args(0) match {
      case "copy" =>
        val todir = require(stringOption(args, "-toDir"),
          "Please specify a destination directory with -toDir")
        val tsconfig = config.triplestore
        val dataParams = (Map() ++ mapAsScalaMap(System.getenv())) + ("T_DATA_DIR" -> todir)
        val toDConfig = m.getDataConfig(dataParams)
        val toBConfig = m.makeBaseConfig(tsconfig, toDConfig)

        implicit val mat = context.matrix

        val allProbes = mat.probeMap.keys.toSeq.sorted

        //Do not do the log-2 wrap
        val from = config.data.foldsDBReaderNowrap(mat)

        val allSamples = mat.sampleMap.tokens.map(Sample(_))

        for (s <- allSamples) {
          val vs = from.valuesInSample(s, allProbes)
          val raw = new RawExpressionData {
            val data = Map(s -> (Map() ++
              vs.map(v => v.probe -> (v.value, v.call, v.p))))
          }
          val t = new SimplePFoldValueInsert(() => toDConfig.extWriter(toDConfig.foldDb), raw).
            insert("insert folds")
          TaskRunner.runAndStop(t)
          println(s"$s (folds)")
        }
        val from2 = mat.absoluteDBReader

        for (s <- allSamples) {
          val vs = from2.valuesInSample(s, allProbes)
          val raw = new RawExpressionData {
            val data = Map(s -> (Map() ++
              vs.map(v => v.probe -> (v.value, v.call, 0.0))))
          }
          //Not a bug - we insert PExprValue here as well, so we
          //do not use the AbsoluteValueInsert. The BasicExprValue format is
          //deprecated.
          val t = new SimplePFoldValueInsert(() => toDConfig.extWriter(toDConfig.exprDb), raw).
            insert("insert absolute values")
          TaskRunner.runAndStop(t)
          println(s"$s (abs)")
        }

      case _ => showHelp()
    }

    KCDBRegistry.setMaintenance(false)
    KCDBRegistry.closeAll()
  }

  def showHelp(): Unit = {
    throw new Exception("Please specify a command (copy/...)")
  }
}
