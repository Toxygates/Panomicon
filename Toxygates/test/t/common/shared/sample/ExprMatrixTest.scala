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

package t.common.shared.sample

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import friedrich.data.immutable._
import t.viewer.server.EVArray
import t.viewer.server.EVABuilder

@RunWith(classOf[JUnitRunner])
class ExprMatrixTest extends FunSuite {

  /**
   * Test data
   *
   * 3	3	5	3	3	5	0.422649731
   * 1	2	1	9	8	10	0.008391321
   * 2	1	1	19	18	20	0.001609947
   * 4	4	4	2	1	2	0.125665916
   * 5	2	3	2	4	3	0.822206065
   *
   * First 3 columns are group 1, following 3 group 2,
   *  last column expected p-value of comparison between the two groups.
   *
   */

  val testMatrix = {
    val data = List(
      List(3, 3, 5, 3, 3, 5),
      List(1, 2, 1, 9, 8, 10),
      List(2, 1, 1, 19, 18, 20),
      List(4, 4, 4, 2, 1, 2),
      List(5, 2, 3, 2, 4, 3)).map(_.map(new ExpressionValue(_)))
    new ExprMatrix(data.map(x => EVArray(x)), data.size, data(0).size,
    		Map("a" -> 0, "b" -> 1, "c" -> 2, "d" -> 3, "e" -> 4),
    		Map("a" -> 0, "b" -> 1, "c" -> 2, "d" -> 3, "e" -> 4, "f" -> 5),
    		(1 to 5).map(x => new SimpleAnnotation("p" + x)))
  }

  test("basic operations") {
    val em = testMatrix
    assert(em.column(0).size === 5)
    assert(em.row(0).size === 6)
    assert(em.column("a") === em.column(0))
    assert(em.row("a") === em.row(0))

    def tv(x: Seq[Int]) = EVArray(x.map(new ExpressionValue(_)))

    assert(em.column(0) === tv(Seq(3,1,2,4,5)))
    assert(em.column(1) === tv(Seq(3,2,1,4,2)))
    assert(em.row(0) === tv(Seq(3,3,5,3,3,5)))
    assert(em.row(1) === tv(Seq(1,2,1,9,8,10)))

    val transpose = em.copyWith(em.toColVectors)
    for (i <- 0 until 5) {
    	assert(transpose.row(i) === em.column(i))
    }

    val transpose2 = em.copyWithColumns(em.toRowVectors)
    for (i <- 0 until 5) {
    	assert(transpose2.row(i) === em.column(i))
    }

    val em2 = em.copyWithColumns(em.toColVectors)
    assert(em2.rows === 5)
    assert(em2.columns === 6)
    for (i <- 0 until 5) {
      assert(em2.row(i) === em.row(i))
      assert(em2.column(i) === em.column(i))
    }

    val em3 = em.copyWithColumns(List(tv(Seq(1,2,3,4))))
    assert(em3.rows === 4)
    assert(em3.columns === 1)

  }

  test("t-test and sorting") {
    val em = testMatrix
    assert(em.columns === 6)
    assert(em.rows === 5)

    val em2 = em.appendTTest(em, Seq("a", "b", "c"), Seq("d", "e", "f"), "TTest")
    assert(em2.columns === 7)

    val em3 = em2.sortRows((v1, v2) => v1(6).getValue < v2(6).getValue)
    println(em3.row(0))
    assert(em3(0,6).getValue < 0.002)
    assert(em3(4,6).getValue > 0.8)

    val em4 = em3.sortRows((v1, v2) => v1(6).getValue > v2(6).getValue)
    println(em4.row(0))
    assert(em4(4,6).getValue < 0.002)
    assert(em4(0,6).getValue > 0.8)

  }

  test("sorting") {
    val em = testMatrix
    val em2 = em.sortRows(
        (v1, v2) => v1(0).getValue < v2(0).getValue)
    println(em2)
    println(em2.rowMap)
    assert(em2.rowMap("b") === 0)

    assert(em2("b", "b").getValue === 2)
    assert(em2("b", "c").getValue === 1)
    assert(em2("d", "a").getValue === 4)

    assert(em2.annotations(0).probe === "p2")
    assert(em2.annotations(1).probe === "p3")
    assert(em2.annotations(2).probe === "p1")

    val em3 = em2.sortRows((v1, v2) => v1(0).getValue > v2(0).getValue)
    println(em3)
    println(em3.rowMap)

    assert(em2.toRowVectors.reverse === em3.toRowVectors)
  }

  test("row select") {
    val em = testMatrix
    val em1 = em.selectRows(List(1,3,4))
    assert(em1.rows === 3)
    assert(em1.columns === 6)
    for (i <- 0 until 6) {
    	assert(em1.column(i).size === 3)
    }
    for (i <- 0 until 3) {
      assert(em1.row(i).size === 6)
    }

    println(em1.rowMap)
    assert(em1.rowMap === Map("b" -> 0, "d" -> 1, "e" -> 2))
    assert(em1.columnMap === em.columnMap)
    assert(em1.annotations(0).probe === "p2")
    assert(em1.annotations(1).probe === "p4")
    assert(em1.annotations(2).probe === "p5")

    //select and permute
    val em3 = em.selectRows(List(3,1,4))
    assert(em3.rows === 3)
    println(em3.rowMap)
    assert(em3.rowMap === Map("d" -> 0, "b" -> 1, "e" -> 2))
    assert(em3.columnMap === em.columnMap)
    assert(em3.annotations(0).probe === "p4")
    assert(em3.annotations(1).probe === "p2")
    assert(em3.annotations(2).probe === "p5")

    val em4 = em.selectNamedRows(List("e", "d"))
    assert(em4.rows === 2)
    assert(em4.columns === 6)
    println(em4.rowMap)
    assert(em4.rowMap === Map("e" -> 0, "d" -> 1))
    assert(em4.annotations(0).probe === "p5")
    assert(em4.annotations(1).probe === "p4")

  }

  test("column select") {
    val em = testMatrix
    val em2 = em.selectColumns(List(1,3))
    println(em2)
    assert(em2.columns === 2)
    assert(em2.rows === 5)
    for (i <- 0 until 5) {
    	assert(em2.row(i).size === 2)
    }
    for (i <- 0 until 2) {
    	assert(em2.column(i).size === 5)
    }

    assert(em2.rowMap === em.rowMap)
    assert(em2.columnMap === Map("b" -> 0, "d" -> 1))
    assert(em2.annotations(0).probe === "p1")
    assert(em2.annotations(1).probe === "p2")
    assert(em2.annotations(2).probe === "p3")

    val em5 = em.selectNamedColumns(List("d", "e"))
    assert(em5.columns === 2)
    assert(em5.rows === 5)
    assert(em5.columnMap === Map("d" -> 0, "e" -> 1))
  }

  test("filtering") {
    val em = testMatrix
    val f = em.filterRows(_.head.getValue > 2)
    assert(f.columnMap === em.columnMap)
    assert(f.rowMap.keySet subsetOf em.rowMap.keySet)
    assert(f.annotations(0) === em.annotations(0))
    assert(f.annotations(1) === em.annotations(3))
    assert(f.annotations(2) === em.annotations(4))
  }

  test("adjoin") {
    val em = testMatrix
    val small = ExprMatrix.withRows(List(List(1),
        List(2),
        List(3),
        List(4),
        List(5)).map(r => EVArray(r.map(new ExpressionValue(_)))))
    val r = em.adjoinRight(small)
    assert(r.columns === 7)
    assert(r.rows === 5)
  }

  test("appendColumn") {
    implicit def builder = EVABuilder

    val em = testMatrix
    val evals = EVArray((1 to 5).map(new ExpressionValue(_)))

    val r = em.appendColumn(evals)

    assert(r.columns === 7)
    assert(r.rows === 5)
  }

  test("joint modify") {
    val em = testMatrix
    val rows = List(List(1),
        List(2),
        List(3),
        List(4),
        List(5)).map(r => EVArray(r.map(new ExpressionValue(_))))

    val small = ExprMatrix.withRows(rows)

    val (s1, s2) = em.modifyJointly(small,
        _.sortRows((v1, v2) => v1(0).getValue < v2(0).getValue))
    println(em.rowMap)
    println(s1.rowMap)
    assert(s2.rowMap === s1.rowMap)
    assert(s2.annotations === s1.annotations)
    println(em.annotations.toVector)
    println(s2.annotations.toVector)
    println(s1.annotations.toVector)
    assert(s2.annotations(0).probe === "p2")
    assert(s2.annotations(1).probe === "p3")
    assert(s2.annotations(2).probe === "p1")
  }

  test("empty matrix") {
    val m = testMatrix
    val empty = m.copyWithColumns(Seq())
    assert(empty.rows === 0)
    assert(empty.columns === 0)

    // TODO it's not clear how the column allocation should behave
    //when we remove rows and columns. Currently it is kept.
//    assert(empty.rowKeys.isEmpty)
//    assert(empty.columnKeys.isEmpty)
  }
}
