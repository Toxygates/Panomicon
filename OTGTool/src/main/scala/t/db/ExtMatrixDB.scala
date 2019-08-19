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

package t.db

import t.platform.Probe

/**
 * The extended microarray DB is a microarray DB that also has p-values and
 * custom PA-calls for each sample group.
 */
trait ExtMatrixDBReader extends MatrixDBReader[PExprValue] {
  def emptyValue(probe: String) = {
    val r = PExprValue(0.0, Double.NaN, 'A', probe)
    r.isPadding = true
    r
  }
}

trait ExtMatrixDBWriter extends MatrixDBWriter[PExprValue]

trait ExtMatrixDB extends MatrixDB[PExprValue, PExprValue]
  with ExtMatrixDBReader with ExtMatrixDBWriter
