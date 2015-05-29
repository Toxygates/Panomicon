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