/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

import scala.Codec
import scala.io.Source
import java.io._

/* This script takes a GEO Series Matrix file as input, and outputs an expression 
   data CSV and a metadata TSV file. In the metadata file, the sample ID column 
   will be generated based on the input, but the other required columns will be 
   blank, to be filled in by hand by the user based on other information from the 
   input data. To facilitate this process, all per-sample fields from the input 
   data are also copied into the generated TSV file.
*/

val inputFile = args(0)
println("Processing " + inputFile)

val lines = Source.fromFile(inputFile)(Codec.UTF8).getLines.toList
val (nonMatrixLines, otherLines) = lines.span(!_.startsWith("!series_matrix_table_begin"))

// Generate data CSV
val matrixLines = otherLines.drop(1).takeWhile(!_.startsWith("!series_matrix_table_end"))
val fixedFirstRow = ("\"\"" + matrixLines(0).dropWhile(_ != '\t')) // clear the first cell
val fixedLines = fixedFirstRow +: matrixLines.drop(1)
val csvLines = fixedLines.map(_.split("\t").mkString(","))

val matrixFileName = inputFile.split('.')(0) + ".data.csv"
writeStringToFile(csvLines.mkString("\n"), matrixFileName)

// Generate metadata TSV
val sampleIdColumn = "sample_id" +: fixedFirstRow.split("\t").drop(1).toList

val nonMatrixCells = nonMatrixLines.map(_.split("\t").toList)
val metadataCells = nonMatrixCells.filter(_.size == sampleIdColumn.size)

val requiredMetadataColumns = List("control_group", "platform_id", "type", 
  "organism", "test_type", "sin_rep_type", "organ_id", "compound_name", 
  "dose_level", "exposure_time").map(_ +: List.fill(sampleIdColumn.size-1)(""))
  
val allMetadataCells = ((sampleIdColumn +: requiredMetadataColumns) ++ metadataCells).transpose
val unquotedCells = allMetadataCells.map(_.map(_.stripPrefix("\"").stripSuffix("\"")))
val metadataLines = unquotedCells.map(_.mkString("\t"))

val metadataFileName = inputFile.split('.')(0) + ".meta.tsv"
writeStringToFile(metadataLines.mkString("\n"), metadataFileName)

def writeStringToFile(string: String, fileName: String) {
  val file = new File(fileName)
  println("Writing to " + file.getAbsolutePath())
  val writer = new PrintWriter(file)
  writer.write(string)
  writer.close()
}
