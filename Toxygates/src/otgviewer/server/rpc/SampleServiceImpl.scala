/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package otgviewer.server.rpc

import t.common.shared.sample.SampleColumn
import t.viewer.shared.TimeoutException
import otgviewer.shared.Pathology
import otg.sparql.OTGSamples

class SampleServiceImpl extends t.viewer.server.rpc.SampleServiceImpl with OTGServiceServlet
  with otgviewer.client.rpc.SampleService {

  private def sampleStore: OTGSamples = context.samples

  @throws[TimeoutException]
  override def pathologies(column: SampleColumn): Array[Pathology] =
    column.getSamples.flatMap(x => sampleStore.pathologies(x.id)).map(
        otgviewer.server.rpc.Conversions.asJava(_))

}
