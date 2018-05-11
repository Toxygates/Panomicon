/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package t.model.sample

import scala.collection.JavaConversions._

object Helpers { 

  implicit class AttributeSetHelper(attr: AttributeSet) {
    
    lazy val byIdLowercase = Map() ++ attr.byId.map(a => a._1.toLowerCase() -> a._2)
    
    def getById(id: String): Option[Attribute] = 
      Option(attr.byId(id))
      
    def getByTitle(title: String): Option[Attribute] =
      Option(attr.byTitle(title))
  }
}
