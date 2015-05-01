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