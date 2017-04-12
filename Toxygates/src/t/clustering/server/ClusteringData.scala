package t.clustering.server

import java.util.{List => JList}
import scala.collection.JavaConversions._

/**
 * Data provider for a heatmap clustering.
 */
abstract class ClusteringData {
  def rowNames: Array[String]
  
  def colNames: Array[String]
  
    /**
   * A directory where temporary data can be stored
   */
  def userDir: String
  
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
class RandomData(val userDir: String, cols: Array[String], rows: Array[String]) 
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