package t.util

import friedrich.data.Statistics

/**
 * Statistics functions that are safe to use when there may be missing data.
 */
object SafeMath {
  
  private def filtered(vs: Iterable[Double]) = 
    vs.filter(! java.lang.Double.isNaN(_))
  
  private def safely[T](vs: Iterable[Double], 
      f: Iterable[Double] => Double): Double = {
    val fvs = filtered(vs)
    if (fvs.isEmpty) Double.NaN else f(fvs)
  }
  
  def safeProduct(vs: Iterable[Double]) = 
    safely(vs, _.product) 
    
  def safeMax(vs: Iterable[Double]) =  
    safely(vs, _.max)
  
  def safeMin(vs: Iterable[Double]) = 
    safely(vs, _.min)
    
  def safeMean(vs: Iterable[Double]) = 
    safely(vs, fs => fs.sum/fs.size)
    
  def safeSum(vs: Iterable[Double]) = 
    safely(vs, _.sum)    
  
  def safeSigma(vs: Iterable[Double]) = 
    safely(vs, Statistics.sigma(_))   
}