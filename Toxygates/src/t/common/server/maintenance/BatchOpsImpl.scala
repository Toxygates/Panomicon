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

package t.common.server.maintenance

import t.viewer.server.rpc.TServiceServlet
import t.common.shared.maintenance.MaintenanceException
import t.BatchManager
import t.TaskRunner
import t.common.shared.maintenance.Batch
import t.common.shared.maintenance.MaintenanceConstants._
import t.util.TempFiles
import t.Tasklet
import t.sparql.SampleFilter
import t.sparql.Datasets
import t.sparql.Batches
import scala.collection.JavaConversions._
import t.common.shared.ManagedItem
import t.sparql.TRDF
import t.common.shared.Dataset
import t.db.Metadata
import javax.annotation.Nullable
import t.db.SampleParameter

/**
 * Routines for servlets that support the management of batches.
 */
trait BatchOpsImpl extends MaintenanceOpsImpl
    with t.common.client.rpc.BatchOperations {
  this: TServiceServlet =>

  protected def simpleLog2: Boolean = false

  protected def mayAppendBatch: Boolean = true

  def addBatchAsync(b: Batch): Unit = {
    showUploadedFiles()
    grabRunner()

    val bm = new BatchManager(context) //TODO configuration parsing

    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Add batch")

      val exbs = new Batches(context.config.triplestore).list
      if (exbs.contains(b.getTitle) && !mayAppendBatch) {
        throw new MaintenanceException(
            s"The batch ${b.getTitle} already exists and appending is not allowed. " +
            "Please choose a different name.")
      }

      val tempFiles = new TempFiles()
      setAttribute("tempFiles", tempFiles)

      if (getFile(metaPrefix) == None) {
        throw new MaintenanceException("The metadata file has not been uploaded yet.")
      }
      if (getFile(dataPrefix) == None) {
        throw new MaintenanceException("The normalized intensity file has not been uploaded yet.")
      }

      val metaFile = getAsTempFile(tempFiles, metaPrefix, metaPrefix, "tsv").get
      val dataFile = getAsTempFile(tempFiles, dataPrefix, dataPrefix, "csv")
      val callsFile = getAsTempFile(tempFiles, callPrefix, callPrefix, "csv")

      var md: Metadata = null
      try {
        md = factory.tsvMetadata(metaFile.getAbsolutePath(),
          context.config.sampleParameters)
      } catch {
        case e: Exception =>
          e.printStackTrace()
          throw new MaintenanceException("Error while parsing metadata. Please check the file.")
      }

      checkMetadata(md)
      md = alterMetadataPriorToInsert(md)

      TaskRunner += bm.addRecord(b.getTitle, b.getComment, context.config.triplestore)
      //Set the parameters immediately, so that the batch is in the right dataset
      // -> can be seen and deleted, in the case of e.g. user data
      TaskRunner += Tasklet.simple("Set batch parameters", () => updateBatch(b))

      TaskRunner ++= bm.add(b.getTitle, b.getComment, md,
        dataFile.get.getAbsolutePath(),
        callsFile.map(_.getAbsolutePath()),
        true, baseConfig.seriesBuilder,
        simpleLog2)
    }
  }

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

    val bm = new BatchManager(context) //TODO configuration parsing
    cleanMaintenance {
      TaskRunner.start()
      setLastTask("Delete batch")
      TaskRunner ++= bm.delete(b.getTitle, baseConfig.seriesBuilder, false)
    }
  }

  protected def updateBatch(b: Batch): Unit = {
    val bs = new Batches(baseConfig.triplestore)
    val existingAccess = bs.listAccess(b.getTitle())
    val newAccess = b.getEnabledInstances()
    for (i <- newAccess; if !existingAccess.contains(i)) {
      bs.enableAccess(b.getTitle(), i)
    }
    for (i <- existingAccess; if !newAccess.contains(i)) {
      bs.disableAccess(b.getTitle(), i)
    }

    val oldDs = bs.datasets.getOrElse(b.getTitle, null)
    val newDs = b.getDataset
    if (newDs != oldDs) {
      val ds = new Datasets(baseConfig.triplestore)
      if (oldDs != null) {
        ds.removeMember(b.getTitle, oldDs)
      }
      ds.addMember(b.getTitle, newDs)
    }
    bs.setComment(b.getTitle, TRDF.escape(b.getComment))
  }

  protected def overviewParameters: Seq[SampleParameter] =
    context.config.sampleParameters.required.toSeq

  def batchParameterSummary(batch: Batch): Array[Array[String]] = {
    val samples = context.samples
    val params = overviewParameters
    val paramIds = params.map(_.identifier)
    val batchURI = Batches.packURI(batch.getTitle)
    val sf = SampleFilter(None, Some(batchURI))
    val data = samples.sampleAttributeQuery(paramIds)(sf)()
    val titles = params.map(_.humanReadable).toArray
    val adata = data.map(row => paramIds.map(c => row(c)).toArray).toArray
    Array(titles) ++ adata
  }

  def update(i: ManagedItem): Unit = {
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
      throw new MaintenanceException(
        s"Invalid name: $id (quotation marks and spaces, etc., are not allowed)")
    }

    if (dm.list.contains(id)) {
      if (mustNotExist) {
        throw new MaintenanceException(s"The dataset $id already exists, please choose a different name")
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
