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