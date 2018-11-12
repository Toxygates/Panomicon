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

import scala.io.Source
import java.io._

/*
 * Script to translate MIMAT style miRNA identifiers, such as MIMAT0004495
 * into the miRBase form hsa-miR-22-5p.
 * 
 * Arguments: 1. miRBase platform file, e.g. mirbase-v21_platform.tsv
 * 2. Data file to convert
 * 
 * The output file will be named by amending the input file name with a new suffix.
 */
 
def readRecord(line: String) = {
  //Example line:
  // hsa-miR-22-5p   title=hsa-miR-22-5p,accession=MIMAT0004495,species=Homo sapiens
  val spl = line.split("\t")
  val id = spl(0)  
  val rest = spl(1).split("accession=")
  val mimat = rest(1).split(",")(0)
  (mimat, id)
}

val platform = Source.fromFile(args(0)).getLines
val lookup = Map() ++ platform.map(readRecord)
val input = Source.fromFile(args(1)).getLines
val out = new PrintWriter(args(1).replace(".csv", ".mir_translate.csv"))

try {
  out.println(input.next)
  for (line <- input) {
    val spl = line.split(",", -1)
    val mimat = spl(0).replace("\"", "")
    lookup.get(mimat) match {
      case Some(id) => out.println("\"" + id + "\"," + spl.drop(1).mkString(","))
      case None => scala.Console.err.println(s"Couldn't translate miRNA ${spl(0)}")
    }
  }  
} finally {
  out.close()
}
