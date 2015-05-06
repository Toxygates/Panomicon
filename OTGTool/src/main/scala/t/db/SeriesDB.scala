package t.db

/**
 * A database of expression series indexed by sample class.
 * The database must be closed after use.
 */
trait SeriesDB[S <: Series[S]] {
  /**
   * Insert the given series. If the series already exists, points will be
   * replaced or the series will be extended.
   * The series must be fully specified.
   */
  def addPoints(s: S): Unit
  
  /**
   * Remove the given points. If the series becomes empty, it will be deleted.
   * The series must be fully specified.
   */
  def removePoints(s: S): Unit
  
  /**
   * Obtain the series that match the constraints specified in the key.
   * The key can be partially specified. A SeriesBuilder will be used to find
   * the matching keys.
   */
  def read(key: S): Iterable[S]

  /**
   * Release the database 
   */
  def release(): Unit
  
}