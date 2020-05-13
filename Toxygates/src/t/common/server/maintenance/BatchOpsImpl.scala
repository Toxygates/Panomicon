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

package t.common.server.maintenance

import javax.annotation.Nullable
import t.BatchManager
import t.TaskRunner
import t.common.shared.Dataset
import t.common.shared.ManagedItem
import t.common.shared.maintenance.Batch
import t.common.shared.maintenance.BatchUploadException
import t.common.shared.maintenance.MaintenanceConstants._
import t.common.shared.maintenance.MaintenanceException
import t.db.{IDConverter, Metadata}
import t.model.sample.{Attribute, CoreParameter}
import t.sparql.Batches
import t.sparql.Datasets
import t.sparql.SampleFilter
import t.sparql.TRDF
import t.util.TempFiles
import collection.JavaConverters._
import scala.language.implicitConversions

import t.viewer.server.rpc.TServiceServlet
import t.BaseConfig
import gwtupload.server.UploadServlet

/**
 * Routines for servlets that support the management of batches.
 */
trait BatchOpsImpl extends MaintenanceOpsImpl
    with t.common.client.rpc.BatchOperations {
  this: TServiceServlet =>

  /**
   * From the triplestore, read attributes that do not yet exist
   * in the attribute set and populate them once.
   */
  protected def populateAttributes(bc: BaseConfig) {
    val platforms = new t.sparql.Platforms(bc)
    platforms.populateAttributes(bc.attributes)
  }

  protected def simpleLog2: Boolean = false

  protected def mayAppendBatch: Boolean = true

  def addBatchAsync(batch: Batch): Unit = {
    ensureNotMaintenance()
    showUploadedFiles()
    grabRunner()

    val batchManager = new BatchManager(context)

    maintenance {
      setLastTask("Add batch")

      val existingBatches = new Batches(context.config.triplestore).list
      if (existingBatches.contains(batch.getId) && !mayAppendBatch) {
        throw BatchUploadException.badID(
            s"The batch ${batch.getId} already exists and appending is not allowed. " +
            "Please choose a different name.")
      }

      val metaFile = getLatestFile(maintenanceUploads(), metaPrefix, metaPrefix, "tsv")
      val dataFile = getLatestFile(maintenanceUploads(), dataPrefix, dataPrefix, "csv")
      val callsFile = getLatestFile(maintenanceUploads(), callPrefix, callPrefix, "csv")
      val probesFile = getLatestFile(maintenanceUploads(), probesPrefix, probesPrefix, "tsv")

      if (metaFile.isEmpty) {
        throw BatchUploadException.badMetaData("The metadata file has not been uploaded yet.")
      }
      if (dataFile.isEmpty) {
        throw BatchUploadException.badNormalizedData("The normalized intensity file has not been uploaded yet.")
      }

      val conversion = probesFile.map(pf => {
        val meta = factory.tsvMetadata(metaFile.get.getAbsolutePath(),
          context.config.attributes)
        val pfs = meta.attributeValues(CoreParameter.Platform)
        if (pfs.size != 1) {
          throw BatchUploadException.badPlatformForConversion("Need exactly one platform in batch for probe conversion");
        }
        try {
          IDConverter.fromPlatform(pfs.head, context, pf.getAbsolutePath)
        } catch {
          case e: Exception =>
            e.printStackTrace()
            throw BatchUploadException.badPlatformForConversion("Unable to convert probes into platform " + pfs.head)
        }
      })

      runTasks(batchManager.add(batch, metaFile.get.getAbsolutePath,
        dataFile.get.getAbsolutePath,
        callsFile.map(_.getAbsolutePath),
        false, simpleLog2,
        conversion = conversion.getOrElse(BatchManager.identityConverter)))
    }
  }

  def updateBatchMetadataAsync(batch: Batch, recalculate: Boolean): Unit = {
    ensureNotMaintenance()
    showUploadedFiles()
    grabRunner()

    val batchManager = new BatchManager(context)

    maintenance {
      setLastTask("Update batch metadata")

      val metaFile = getLatestFile(maintenanceUploads(), metaPrefix, metaPrefix, "tsv")
      if (metaFile.isEmpty) {
        throw BatchUploadException.badMetaData("The metadata file has not been uploaded yet.")
      }

      runTasks(batchManager.updateMetadata(batch, metaFile.get.getAbsolutePath, recalculate))
    }
  }

  implicit def batch2bmBatch(b: Batch): BatchManager.Batch =
    BatchManager.Batch(b.getId, b.getComment, Some(b.getEnabledInstances.asScala.toSeq),
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
    maintenance {
      setLastTask("Delete batch")
      runTasks(bm.delete(b.getId, false))
    }
  }

  protected def updateBatch(b: Batch): Unit = {
    new BatchManager(context).updateBatch(b).run()
  }

  protected def overviewParameters: Seq[Attribute] =
    context.config.attributes.getRequired.asScala.toSeq

  def batchAttributeSummary(batch: Batch): Array[Array[String]] = {
    val samples = context.sampleStore
    val params = overviewParameters
    val batchURI = Batches.packURI(batch.getId)
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

    val id = d.getId()
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
    /*
     * This method has no security check since it is not public.
     * Public user-facing methods that can reach this are responsible for
     * security checking.
     */
    val ds = new Datasets(baseConfig.triplestore)
    ds.setComment(d.getId, TRDF.escape(d.getComment))
    ds.setDescription(d.getId, TRDF.escape(d.getDescription))
    ds.setPublicComment(d.getId, TRDF.escape(d.getPublicComment))
  }

  def datasetSampleSummary(dataset: Dataset,
                           rowAttributes: Array[Attribute],
                           columnAttributes: Array[Attribute],
                           cellAttribute: Attribute): Array[Array[String]] = {
    val batches = getBatches(Array(dataset.getId))

    val samples = context.sampleStore
    val allAttribs = rowAttributes ++ columnAttributes ++ Option(cellAttribute)

    val adata = batches.toSeq.flatMap(b => {
      val batchURI = Batches.packURI(b.getId)
      val sf = SampleFilter(None, Some(batchURI))
      samples.sampleCountQuery(allAttribs)(sf)()
    }).filter(_.keySet.size > 1) //For empty batches, the only key will be 'count'

    import t.model.sample.OTGAttribute._
    val compoundEdit = Compound.id + "Edit"

    def getKey(data: Map[String, String])(key: Attribute) =
      if (key == Compound && data.contains(compoundEdit)) data(compoundEdit) else data(key.id)

    //NB the toSeq conversion is essential.
    //Equality for arrays is not deep by default
    def rowKey(data: Map[String, String]) = rowAttributes.toSeq.map(getKey(data))
    def colKey(data: Map[String, String]) = columnAttributes.toSeq.map(getKey(data))

    /**
     * Construct a pivot table from the raw data.
     * Example: row attributes are compound, exposure period
     * Column attributes are Species, Organ
     * Resulting table of sample counts can be e.g.
     *
     * Compound | Exposure period | Rat/Liver | Rat/Kidney | Mouse/Liver | Mouse/Kidney
     * A        | 3h              |       3   |      6     |      3      |       6
     * B        | 3h              |       3   |      6     |      3      |       6
     */

    val byRow = adata.groupBy(rowKey).toSeq.sortBy(_._1.mkString(""))
    val columns = adata.map(colKey).distinct

    val headers = rowAttributes.map(_.title).toArray ++
      columns.map(_.mkString("/"))

    def cellValue(rows: Iterable[Map[String, String]]) =
      Option(cellAttribute) match {
        case Some(a) => rows.map(_(a.id)).toSeq.distinct.mkString(", ")
        case None => rows.map(_("count").toInt).sum.toString
      }

    val rows = byRow.map {case (rkey, data) =>
      rkey ++ columns.map(ckey => cellValue(data.filter(colKey(_) == ckey)))
    }

    Array(headers) ++ rows.map(_.toArray)
  }

}
