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

package t.sparql

import t.db.Sample
import t.db.SampleParameter
import t.db.Metadata

/**
 * Metadata from a triplestore.
 * The graph to be queried can be influenced by setting
 * os.batchURI.
 */
class TriplestoreMetadata(os: Samples)(implicit sf: SampleFilter) extends Metadata {

  /**
   * Retrieve the set of control samples corresponding to a given sample.
   */
  override def controlSamples(s: Sample): Iterable[Sample] = {
    throw new Exception("Implement me")
  }

  def samples: Iterable[Sample] = os.samples

  def parameters(s: Sample): Iterable[(SampleParameter, String)] = {
    os.parameterQuery(s.identifier).collect( {
      case (sp, Some(s)) => (sp, s)
    })
  }

  def parameterValues(identifier: String): Set[String] =
    os.allValuesForSampleAttribute(identifier).toSet
}
