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

package otgviewer.server.rpc

import java.util.{ List => JList }
import otgviewer.server.rpc.Conversions.asScala
import t.common.shared.ValueType
import org.rosuda.REngine.Rserve.RserveException
import otgviewer.server.R
import t.viewer.server.Configuration
import java.util.logging.Logger
import t.common.shared.sample._
import java.io.File
import javax.servlet.ServletContext
import t.common.shared.userclustering.Algorithm

class MatrixServiceImpl extends t.viewer.server.rpc.MatrixServiceImpl
    with OTGServiceServlet {

  private val logger = Logger.getLogger("MatrixService")

  override def localInit(config: Configuration) {
    super.localInit(config)
  }

}
