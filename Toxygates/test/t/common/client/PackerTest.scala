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

package t.common.client

import org.scalatest._
import otg.model.sample.AttributeSet
import t.model.SampleClass
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import otg.model.sample.OTGAttribute
import t.viewer.client.storage.Packer

/**
 * Tests for unpacking model classes from string input.
 */
@RunWith(classOf[JUnitRunner])
class PackerTest extends FunSuite with Matchers {
  val attributes = AttributeSet.getDefault

//  test("unpackSampleClass gracefully handles an odd number of input tokens") {
//    val input = "test_type,,,SAT,,,sin_rep_type"
//    var sampleClass = Packer.unpackSampleClass(attributes, input)
//
//    sampleClass shouldBe a [SampleClass]
//    // We should end up with two keys: test_type, which was in the input, and
//    // type, which is added by unpackSampleClass. sin_rep_type, which didn't have
//    // a value, should not be in there.
//    sampleClass.getKeys.size shouldBe 2
//    sampleClass.getKeys.contains(OTGAttribute.Repeat) shouldBe false
//    sampleClass.get(OTGAttribute.TestType) shouldBe "SAT"
//  }
//
//  test("unpackSampleClass ignores invalid attributes") {
//    val input = "asdf,,,42,,,test_type,,,SAT"
//    var sampleClass = Packer.unpackSampleClass(attributes, input)
//
//    sampleClass shouldBe a [SampleClass]
//    sampleClass.getKeys.size shouldBe 2 // as above, type is added by unpackSampleClass
//    sampleClass.get(OTGAttribute.TestType) shouldBe "SAT"
//  }
}
