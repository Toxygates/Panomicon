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

package t.common.server

import t.sparql.Datasets
import t.common.shared.Dataset

/**
 * This trait provides conversion into t.common.sharedDataset
 */
trait SharedDatasets {
  this: Datasets =>

  def sharedList: Iterable[Dataset] = {
    val com = comments
    val ts = timestamps
    val descs = descriptions
    list.map(d => new Dataset(d, descs.getOrElse(d, ""),
      com.getOrElse(d, ""), ts.getOrElse(d, null)))
  }
}