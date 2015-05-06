package t

/**
 * A timestamp-based refreshable item that loads the latest data
 * when it is available.
 */
abstract class Refreshable[T] {

  var lastTimestamp: Long = 0  
  def currentTimestamp: Long  
  var latestVal: Option[T] = None
  
  private def shouldRefresh: Boolean = currentTimestamp > lastTimestamp
  
  def latest: T = {
    if (shouldRefresh || latestVal == None) {
      println(this + ": change detected, reloading data")
      latestVal = Some(reload())
      lastTimestamp = currentTimestamp
    }
    latestVal.get
  }
  
  def reload(): T
}