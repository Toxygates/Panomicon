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

package t.viewer.server

import t.common.shared.Dataset

import t.sparql.DatasetStore

/**
 * This trait provides conversion into t.common.sharedDataset
 */
trait SharedDatasets {
  this: DatasetStore =>

  def sharedList(instanceUri: Option[String]): Iterable[Dataset] = {
    val all = keyAttributes
    val nbs = numBatches
    val descs = descriptions

    val filtered = (
      instanceUri match {
        case Some(uri) =>
          val permitted = withBatchesInInstance(uri).toSet
          all.filter(x => permitted.contains(x._1))
        case None => all
      }
    )
    filtered.map(x => {
      new Dataset(x._1, descs.getOrElse(x._1, ""), x._3, x._2, x._4, nbs.getOrElse(x._1, 0))
    })
  }
}
