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

package otg.db

import otg.Annotation
import t.db.Sample
import t.db.ParameterSet

/**
 * Information about a set of samples.
 */
trait Metadata extends t.db.Metadata {

  override def isControl(s: Sample): Boolean = parameter(s, "dose_level") == "Control"

  /**
   * Retrieve the set of compounds that exist in this metadata set.
   */
  def compounds: Set[String] = parameterValues("compound_name")

  /**
   * Retrieve the compound associated with a particular sample.
   */
  def compound(s: Sample): String = parameter(s, "compound_name")

}

object SampleParameter extends ParameterSet {
  val all =
    Annotation.keys.map(x => t.db.SampleParameter(x._2, x._1)) ++
      List(t.db.SampleParameter("sample_id", "Sample ID"),
        t.db.SampleParameter("control_group", "Control group"))

  val highLevel = List("organism", "test_type", "sin_rep_type", "organ_id").map(byId)

  val required = highLevel ++ List("sample_id",
    "compound_name", "dose_level", "exposure_time",
    "platform_id", "control_group").map(byId)

}
