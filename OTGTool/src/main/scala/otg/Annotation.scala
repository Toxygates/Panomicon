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

import scala.collection.immutable.ListMap

@deprecated("Being replaced with SampleParameter and ParameterSet", "July 15")
object Annotation {
  /**
   * Map human-readable descriptions to the keys we expect to find in the RDF data.
   * The RDF predicates are of the form <http://127.0.0.1:3333/(key)>.
   *
   * TODO this should be unified with the SampleParameter API.
   */

  val nonNumericalKeys = ListMap("Platform ID" -> "platform_id",
    "Experiment ID" -> "exp_id",
    "Group ID" -> "group_id",
    "Individual ID" -> "individual_id",
    "Organ" -> "organ_id",
    "Material ID" -> "material_id",
    "Compound" -> "compound_name",
    "Compound abbreviation" -> "compound_abbr",
    "Compound no" -> "compound_no",
    "CAS number" -> "cas_number",
    "KEGG drug" -> "kegg_drug",
    "KEGG compound" -> "kegg_compound",
    "Organism" -> "organism",
    "Test type" -> "test_type",
    "Repeat?" -> "sin_rep_type",
    "Sex" -> "sex_type",
    "Strain" -> "strain_type",
    "Administration route" -> "adm_route_type",
    "Animal age (weeks)" -> "animal_age_week",
    "Exposure time" -> "exposure_time",
    "Dose" -> "dose",
    "Dose unit" -> "dose_unit",
    "Dose level" -> "dose_level",
    "Medium type" -> "medium_type",
    "Product information" -> "product_information",
    "CRO type" -> "cro_type")

  /**
   * These are the annotations that it makes sense to consider in a
   * statistical, numerical way.
   */
  val numericalKeys = ListMap("Terminal body weight (g)" -> "terminal_bw",
    "Liver weight (g)" -> "liver_wt",
    "Kidney weight total (g)" -> "kidney_total_wt",
    "Kidney weight left (g)" -> "kidney_wt_left",
    "Kidney weight right (g)" -> "kidney_wt_right",
    "RBC (x 10^4/uL)" -> "RBC",
    "Hb (g/DL)" -> "Hb",
    "Ht (%)" -> "Ht",
    "MCV (fL)" -> "MCV",
    "MCH (pg)" -> "MCH",
    "MCHC (%)" -> "MCHC",
    "Ret (%)" -> "Ret",
    "Plat (x 10^4/uL)" -> "Plat",
    "WBC (x 10^2/uL)" -> "WBC",
    "Neutrophil (%)" -> "Neu",
    "Eosinophil (%)" -> "Eos",
    "Basophil (%)" -> "Bas",
    "Monocyte (%)" -> "Mono",
    "Lymphocyte (%)" -> "Lym",
    "PT (s)" -> "PT",
    "APTT (s)" -> "APTT",
    "Fbg (mg/dL)" -> "Fbg",
    "ALP (IU/L)" -> "ALP",
    "TC (mg/dL)" -> "TC",
    "TBIL (mg/dL)" -> "TBIL",
    "DBIL (mg/dL)" -> "DBIL",
    "GLC (mg/dL)" -> "GLC",
    "BUN (mg/dL)" -> "BUN",
    "CRE (mg/dL)" -> "CRE",
    "Na (meq/L)" -> "Na",
    "K (meq/L)" -> "K",
    "Cl (meq/L)" -> "Cl",
    "Ca (mg/dL)" -> "Ca",
    "IP (mg/dL)" -> "IP",
    "TP (g/dL)" -> "TP",
    "RALB (g/dL)" -> "RALB",
    "A / G ratio" -> "AGratio",
    "AST (GOT) (IU/L)" -> "AST",
    "ALT (GPT) (IU/L)" -> "ALT",
    "LDH (IU/L)" -> "LDH",
    "gamma-GTP (IU/L)" -> "GTP",
    "DNA (%)" -> "DNA")

  val keys = nonNumericalKeys ++ numericalKeys

  def isNumerical(key: String) = numericalKeys.keySet.contains(key)
}

@deprecated("being replaced with SampleParameter", "July 15")
case class Annotation(data: Seq[(String, String)], barcode: String = null) {
  def postReadAdjustment: Annotation = {
    Annotation(data.map(x => x._1 match {
      //expect e.g. http://127.0.0.1:3333/000080-77-3
      case "CAS number" => {
        val s = x._2.split("3333/")
        val v = if (s.size > 1) {
          s(1)
        } else {
          "N/A"
        }
        (x._1, v)
      }
      //expect e.g. http://bio2rdf.org/dr:D00268
      case "KEGG drug" | "KEGG compound" => {
        val s = x._2.split(":")
        val v = if (s.size > 2) {
          s(2)
        } else {
          "N/A"
        }
        (x._1, v)
      }
      case _ => x
    }), barcode)
  }
}
