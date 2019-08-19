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

/**
 * A bidirectional mapping between tokens and their database codes.
 */
trait LookupMap[Code, Token] {
  /**
   * Keys that are actually used
   */
  lazy val keys: Set[Code] = revMap.keySet
  lazy val tokens: Set[Token] = data.keySet

  def unpack(item: Code): Token = revMap(item)
  def tryUnpack(item: Code): Option[Token] = revMap.get(item)

  def isToken(t: Token): Boolean = tokens.contains(t)

  def data: Map[Token, Code]

  protected val revMap = Map[Code, Token]() ++ data.map(_.swap)

  def pack(item: Token): Code = data.getOrElse(item,
    throw new LookupFailedException(s"Lookup failed for $item"))
}

/*
 * More efficient lookup for integers
 */
trait CachedIntLookupMap[Token] extends LookupMap[Int, Token] {
  protected val revLookup = Array.tabulate(revMap.keys.max + 1)(x => revMap.get(x))

  override def tryUnpack(x: Int) = revLookup(x)
  override def unpack(x: Int) =  revLookup(x).get
}

class LookupFailedException(reason: String) extends Exception(reason)
