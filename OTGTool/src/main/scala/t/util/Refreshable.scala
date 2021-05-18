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

package t.util

/**
 * A timestamp-based refreshable asset that provides the latest data
 * when appropriate.
 */
abstract class Refreshable[T](title: String) {

  /**
   * Time of last refresh (ms)
   */
  protected var lastTimestamp: Long = 0
  protected var lastCheckTimestamp: Long = 0

  /**
   * Current timestamp of asset (ms)
   */
  def currentTimestamp: Long
  protected var latestVal: Option[T] = None

  /**
   * Maximum interval to check the timestamp (ms)
   * (Since even checking the timestamp may require I/O)
   */
  protected val timestampMaxCheckInterval = 1000

  protected def shouldRefresh: Boolean = currentTimestamp > lastTimestamp

  def latest: T = {
    val checkInterval = System.currentTimeMillis() - lastCheckTimestamp
    if ((checkInterval > timestampMaxCheckInterval && shouldRefresh) || latestVal == None) {
      synchronized {
        println(s"Refreshable $title: reloading data")
        latestVal = Some(reload())
        lastTimestamp = currentTimestamp
      }
    }
    lastCheckTimestamp = System.currentTimeMillis()
    latestVal.get
  }

  def reload(): T
}

abstract class PeriodicRefresh[T](title: String, intervalSeconds: Long) extends Refreshable[T](title) {

  override def currentTimestamp = System.currentTimeMillis

  override protected def shouldRefresh: Boolean =
    (currentTimestamp - lastTimestamp) / 1000 >= intervalSeconds
}
