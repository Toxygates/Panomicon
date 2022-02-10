/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.server.viewer

import t.BaseConfig
import t.shared.common.DataSchema
import t.shared.common.sample.Sample
import t.platform.BioParameter
import t.sparql.SampleStore

import scala.language.implicitConversions

class AnnotationStore(val schema: DataSchema, val baseConfig: BaseConfig) {

  //Note: currently this cannot be updated without restarting the application
  lazy val bioParameters = {
    val pfs = new t.sparql.PlatformStore(baseConfig)
    pfs.bioParameters
  }

  //Task: use ControlGroup to calculate bounds here too
  def prepareCSVDownload(sampleStore: SampleStore, samples: Seq[Sample],
                         csvDir: String, csvUrlBase: String): String = {
    val timepoints = samples.flatMap(s =>
      Option(s.get(schema.timeParameter()))).distinct

    val params = bioParameters.sampleParameters
    val raw = samples.map(x => {
      sampleStore.parameterQuery(x.id, params)
    })

    val colNames = params.map(_.title)

    val data = Vector.tabulate(raw.size, colNames.size)((s, a) =>
      raw(s)(a)._2.getOrElse(""))

    val bps = params.map(p => bioParameters.get(p))
    def extracts(b: Option[BioParameter], f: BioParameter => Option[String]): String =
      b.map(f).flatten.getOrElse("")

    def extractd(b: Option[BioParameter], f: BioParameter => Option[Double]): String =
      b.map(f).flatten.map(_.toString).getOrElse("")

    //Note: currently this data isn't populated and the min/max thresholds are always empty
    val rr = timepoints.map(t => {
      val bpt = bioParameters.forTimePoint(t)
      val bps = params.map(p => bpt.get(p))
      (Seq(s"Healthy min. $t", s"Healthy max. $t"),
          Seq(bps.map(extractd(_, _.lowerBound)), bps.map(extractd(_, _.upperBound))))
    })
    val helpRowTitles = Seq("Section") ++ rr.flatMap(_._1)
    val helpRowData = Seq(bps.map(extracts(_, _.section))) ++ rr.flatMap(_._2)

    csvUrlBase + "/" +
      CSVHelper.writeCSV("toxygates", csvDir,
      helpRowTitles ++ samples.map(_.id), colNames, helpRowData ++ data)
  }
}
