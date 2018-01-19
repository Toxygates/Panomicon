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

package t.common.server.maintenance

import scala.collection.JavaConversions._

import javax.annotation.Nullable
import t.BatchManager
import t.TaskRunner
import t.Tasklet
import t.common.shared.Dataset
import t.common.shared.ManagedItem
import t.common.shared.maintenance.Batch
import t.common.shared.maintenance.BatchUploadException
import t.common.shared.maintenance.MaintenanceConstants._
import t.common.shared.maintenance.MaintenanceException
import t.db.Metadata
import t.model.sample.Attribute
import t.sparql.Batches
import t.sparql.Datasets
import t.sparql.SampleFilter
import t.sparql.TRDF
import t.util.TempFiles
import collection.JavaConverters.asScalaSetConverter
import scala.language.implicitConversions

import t.viewer.server.rpc.TServiceServlet

/**
 * Routines for servlets that support the management of batches.
 */
trait BatchOpsImpl extends MaintenanceOpsImpl
    with t.common.client.rpc.BatchOperations {
  this: TServiceServlet =>

  protected def simpleLog2: Boolean = false

  protected def mayAppendBatch: Boolean = true

  def addBatchAsync(batch: Batch): Unit = {
    ensureNotMaintenance()
    showUploadedFiles()
    grabRunner()

    val batchManager = new BatchManager(context)

    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Add batch")

      val existingBatches = new Batches(context.config.triplestore).list
      if (existingBatches.contains(batch.getTitle) && !mayAppendBatch) {
        throw BatchUploadException.badID(
            s"The batch ${batch.getTitle} already exists and appending is not allowed. " +
            "Please choose a different name.")
      }

      val tempFiles = new TempFiles()
      setAttribute("tempFiles", tempFiles)

      if (getFile(metaPrefix) == None) {
        throw BatchUploadException.badMetaData("The metadata file has not been uploaded yet.")
      }
      if (getFile(dataPrefix) == None) {
        throw BatchUploadException.badNormalizedData("The normalized intensity file has not been uploaded yet.")
      }

      val metaFile = getAsTempFile(tempFiles, metaPrefix, metaPrefix, "tsv").get
      val dataFile = getAsTempFile(tempFiles, dataPrefix, dataPrefix, "csv")
      val callsFile = getAsTempFile(tempFiles, callPrefix, callPrefix, "csv")

      var md: Metadata = null
      try {
        md = factory.tsvMetadata(metaFile.getAbsolutePath(),
          context.config.attributes)
      } catch {
        case e: Exception =>
          e.printStackTrace()
          throw BatchUploadException.badMetaData("Error while parsing metadata. Please check the file. " + e.getMessage)
      }

      checkMetadata(md)
      md = alterMetadataPriorToInsert(md)

      TaskRunner ++= batchManager.add(batch, md,
        dataFile.get.getAbsolutePath(),
        callsFile.map(_.getAbsolutePath()),
        false, baseConfig.seriesBuilder,
        simpleLog2)
    }
  }

  def updateBatchMetadataAsync(batch: Batch): Unit = {
    ensureNotMaintenance()
    showUploadedFiles()
    grabRunner()

    val batchManager = new BatchManager(context)

    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Update batch metadata")

      val tempFiles = new TempFiles()
      setAttribute("tempFiles", tempFiles)
      val metaFile = getAsTempFile(tempFiles, metaPrefix, metaPrefix, "tsv").getOrElse {
        throw BatchUploadException.badMetaData("The metadata file has not been uploaded yet.")
      }
      val metadata = createMetadata(metaFile)

      TaskRunner ++= batchManager.updateMetadata(batch, metadata, baseConfig.seriesBuilder)
    }
  }

  protected def createMetadata(metaFile: java.io.File): Metadata = {
    try {
      val md = factory.tsvMetadata(metaFile.getAbsolutePath(),
        context.config.attributes)
      checkMetadata(md)
      alterMetadataPriorToInsert(md)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw BatchUploadException.badMetaData("Error while parsing metadata. Please check the file. "
            + e.getMessage)
    }
  }

  implicit def batch2bmBatch(b: Batch): BatchManager.Batch =
    BatchManager.Batch(b.getTitle, b.getComment, Some(b.getEnabledInstances.toSeq),
        Some(b.getDataset))

  protected def alterMetadataPriorToInsert(md: Metadata): Metadata = md

  /**
   * Check the validity of the sample parameters and throw an exception if there's a problem.
   */
  @throws(classOf[MaintenanceException])
  protected def checkMetadata(md: Metadata): Unit = {}

  import java.util.HashSet

  def getBatches(@Nullable dss: Array[String]): Array[Batch] = {
    val useDatasets = Option(dss).toSet.flatten
    val bs = new Batches(baseConfig.triplestore)
    val ns = bs.numSamples
    val comments = bs.comments
    val dates = bs.timestamps
    val datasets = bs.datasets
    val r = bs.list.map(b => {
      val samples = ns.getOrElse(b, 0)
      new Batch(b, samples, comments.getOrElse(b, ""),
        dates.getOrElse(b, null),
        new HashSet(setAsJavaSet(bs.listAccess(b).toSet)),
        datasets.getOrElse(b, ""))
    }).toArray
    r.filter(b => useDatasets.isEmpty || useDatasets.contains(b.getDataset))
  }

  def deleteBatchAsync(b: Batch): Unit = {

    val bm = new BatchManager(context)
    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Delete batch")
      TaskRunner ++= bm.delete(b.getTitle, baseConfig.seriesBuilder, false)
    }
  }

  protected def updateBatch(b: Batch): Unit = {
    new BatchManager(context).updateBatch(b).run()
  }

  protected def overviewParameters: Seq[Attribute] =
    context.config.attributes.getRequired.toSeq

  def batchParameterSummary(batch: Batch): Array[Array[String]] = {
    val samples = context.samples
    val params = overviewParameters
    val batchURI = Batches.packURI(batch.getTitle)
    val sf = SampleFilter(None, Some(batchURI))
    val data = samples.sampleAttributeValueQuery(params)(sf)()
    val titles = params.map(_.title).toArray
    val adata = data.map(row => params.map(c => row(c.id)).toArray).toArray
    Array(titles) ++ adata
  }

  def update(i: ManagedItem): Unit = {
    ensureNotMaintenance()
    i match {
      case b: Batch => updateBatch(b)
      case _        => throw new Exception(s"Unexpected item type $i")
    }
  }

  /**
   * Add a new dataset.
   * @param mustNotExist if true, we throw an exception if the dataset already exists.
   */
  protected def addDataset(d: Dataset, mustNotExist: Boolean): Unit = {
    val dm = new Datasets(baseConfig.triplestore)

    val id = d.getTitle()
    if (!TRDF.isValidIdentifier(id)) {
      throw BatchUploadException.badID(
        s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
    }

    if (dm.list.contains(id)) {
      if (mustNotExist) {
        throw BatchUploadException.badID(s"The dataset $id already exists, please choose a different name")
      }
    } else {
      maintenance {
        dm.addWithTimestamp(id, TRDF.escape(d.getComment))
        updateDataset(d)
      }
    }
  }

  protected def updateDataset(d: Dataset): Unit = {
    //TODO security check

    val ds = new Datasets(baseConfig.triplestore)
    ds.setComment(d.getTitle, TRDF.escape(d.getComment))
    ds.setDescription(d.getTitle, TRDF.escape(d.getDescription))
    ds.setPublicComment(d.getTitle, TRDF.escape(d.getPublicComment))
  }
}
