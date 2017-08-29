/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */


package t.clustering.server

import scala.collection.JavaConversions._

/**
 * Data provider for a heatmap clustering.
 */
abstract class ClusteringData {
  def rowNames: Array[String]
  
  def colNames: Array[String]
  
  /**
   * A directory where the required R script is available
   */
  def codeDir: String
  
  /**
   * Obtain column-major data for the specified rows and columns
   */
  def data: Array[Array[Double]] 
  
  /**
   * Gene symbols for the specified rows
   */
  def geneSymbols: Array[String]
  
}

/**
 * Example data provider with random data
 */
class RandomData(val codeDir: String, cols: Array[String], rows: Array[String]) 
  extends ClusteringData {
  
  def rowNames = Array() ++ rows 
   
  def colNames = Array() ++ cols
  
  /**
   * Obtain column-major data for the specified rows and columns
   */
  def data: Array[Array[Double]] =
    Array.fill(cols.size, rows.size)(Math.random)
  
  /**
   * Gene symbols for the specified rows
   */
  def geneSymbols = Array() ++ rows
}