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

package t.common.testing

import t.common.shared.sample.Group

import t.db.RawExpressionData
import t.viewer.server.Conversions._
import t.viewer.server.matrix.ExprMatrix

/**
 * @author johan
 */
object TestData {
  import t.db.testing.{TestData => TTestData}
  val dataSchema = new TestSchema()

  def exprMatrix: ExprMatrix = exprMatrix(TTestData.makeTestData(true))

  def exprMatrix(rd: RawExpressionData): ExprMatrix = {
    val probes = rd.probes.toSeq
    val samples = rd.samples.toSeq
    val rows = for (p <- probes) yield samples.map(s => rd.asExtValues(s)(p))
    ExprMatrix.withRows(rows, probes, samples.map(_.sampleId))
  }

  val groups = TTestData.samples.take(10).grouped(2).zipWithIndex.map(ss => {
    val sss = ss._1.map(s => asJavaSample(s))
    new Group(dataSchema, "Gr" + ss._2, sss.toArray)
  }).toSeq

}
