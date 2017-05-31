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

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import t.viewer.server.Configuration
import otgviewer.shared.RankRule
import otgviewer.shared.RuleType

class SeriesServiceTest extends FunSuite with BeforeAndAfter {

  var s: SeriesServiceImpl = _
  before {
    val conf = new Configuration("otg", "/shiba/toxygates")
    s = new SeriesServiceImpl()
    s.localInit(conf)
  }

  after {
	s.destroy
  }

  test("Ranking") {
    val sc = SparqlServiceTest.testSampleClass
    val r = new RankRule(RuleType.MaximalFold, "1370365_at") //GSS gene

    //TODO needs a valid dataset for the first argument
    val res = s.rankedCompounds(Array(), sc, Array(r)).toSeq
    println(res take 10)
  }
}
