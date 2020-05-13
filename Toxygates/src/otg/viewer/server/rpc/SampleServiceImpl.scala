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

package otg.viewer.server.rpc

import otg.viewer.shared.Pathology
import t.common.shared.sample.Sample
import t.sparql.SampleStore
import t.viewer.shared.TimeoutException

class SampleServiceImpl extends t.viewer.server.rpc.SampleServiceImpl with OTGServiceServlet
  with otg.viewer.client.rpc.SampleService {

  private def sampleStore: SampleStore = context.sampleStore

  @throws[TimeoutException]
  override def pathologies(column: Array[Sample]): Array[Pathology] =
    column.flatMap(x => sampleStore.pathologies(x.id)).map(
        otg.viewer.server.rpc.Conversions.asJava(_))

}
