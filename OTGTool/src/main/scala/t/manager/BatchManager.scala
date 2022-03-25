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

package t.manager
import t._
import t.db._
import t.db.file.{CSVRawExpressionData, PFoldValueBuilder, TSVMetadata}
import t.db.kyotocabinet._
import t.global.KCDBRegistry
import t.model.sample.{AttributeSet, CoreParameter}
import t.sparql._
import t.util.DoThenClose._
import t.util.TempFiles

import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
 * Batch management CLI
 */
object BatchManager extends ManagerTool {
  def apply(args: Seq[String])(implicit context: Context): Unit = {

    def config = context.config

    val batches = new BatchStore(config.triplestoreConfig)
    if (args.size < 1) {
      showHelp()
    } else {
      args(0) match {
        case "add" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          val append = booleanOption(args, "-append")
          val comment = stringOption(args, "-comment").getOrElse("")
          val genAttrib = booleanOption(args, "-genAttrib")
          val idConversion = Some(t.db.IDConverter.fromArgument(stringOption(args, "-idConversion"), context))

          val bm = new BatchManager(context)

          val metaList = stringListOption(args, "-multiMetadata") orElse
            stringListOption(args, "-multimetadata")
          metaList match {
            case Some(metaFiles) =>
              KCDBRegistry.setMaintenance(true)
              // For the first metadata file, we use the value of the -append argument; for all other
              // the batch will certainly exist so we always append
              var first = true
              new PlatformStore(config).populateAttributes(config.attributes)
              val tasks = metaFiles.map({ metadata =>
                //val md = factory.tsvMetadata(metadata, config.attributes)
                val dataFile = metadata.replace(".meta.tsv", ".data.csv")

                val f = new java.io.File(metadata.replace(".meta.tsv", ".call.csv"))
                val callFile = if (f.exists()) Some(f.getPath) else None
                println(s"Insert $dataFile")
                val task = bm.add(Batch(title, comment, None, None),
                  metadata, dataFile, callFile,
                  if (first) append else true, genAttrib, conversion = idConversion)

                first = false
                task
              })
              startTaskRunner(tasks.reduce(_ andThen _))
            case None =>
              val metaFile = require(stringOption(args, "-metadata"),
                "Please specify a metadata file with -metadata")
              val dataFile = require(stringOption(args, "-data"),
                  "Please specify a data file with -data")
              val callFile = stringOption(args, "-calls")

              new PlatformStore(config).populateAttributes(config.attributes)
              //val md = factory.tsvMetadata(metaFile, config.attributes)
              startTaskRunner(bm.add(Batch(title, comment, None, None),
                metaFile, dataFile, callFile, append, genAttrib, conversion = idConversion))
          }

        case "recalculate" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          new PlatformStore(config).populateAttributes(config.attributes)
          val sampleFilter = new SampleFilter(None, Some(BatchStore.packURI(title)))
          val metadata =
            new CachingTriplestoreMetadata(context.sampleStore, config.attributes,
                config.attributes.getHighLevel.asScala ++
                config.attributes.getUnitLevel.asScala ++
                List(CoreParameter.Platform, CoreParameter.Treatment,
                  CoreParameter.ControlTreatment, CoreParameter.Batch),
              sampleFilter)
          startTaskRunner(new BatchManager(context).recalculateFoldsAndSeries(metadata, false))

        case "updateMetadata" | "updatemetadata" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          val comment = stringOption(args, "-comment").getOrElse("")
          val bm = new BatchManager(context)
          val metaFile = require(stringOption(args, "-metadata"),
            "Please specify a metadata file with -metadata")
          val force = booleanOption(args, "-force")
          val recalculate = booleanOption(args, "-recalculate")
          new PlatformStore(config).populateAttributes(config.attributes)
          //val md = factory.tsvMetadata(metaFile, config.attributes)
          startTaskRunner(bm.updateMetadataFromFile(Batch(title, comment, None, None),
              metaFile, customAttributes = false, append = false, recalculate = recalculate, force = force))

        case "delete" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          val rdfOnly = booleanOption(args, "-rdfonly")
          verifyExists(batches, title)
          val bm = new BatchManager(context)
          startTaskRunner(bm.delete(title, rdfOnly))
        case "list" =>
          println("Batch list")
          for (b <- batches.getList()) {
            println(b)
          }
        case "list-access" =>
          expectArgs(args, 2)
          verifyExists(batches, args(1))
          println(s"List of instances that have access to batch ${args(1)}")
          for (i <- batches.listAccess(args(1))) {
            println(i)
          }
        case "enable" =>
          expectArgs(args, 3)
          verifyExists(batches, args(1))
          batches.enableAccess(args(1), args(2))
        case "disable" =>
          expectArgs(args, 3)
          verifyExists(batches, args(1))
          batches.disableAccess(args(1), args(2))
        case "loadTest" =>
          expectArgs(args, 3)
          val len = Integer.parseInt(args(1))
          val n = Integer.parseInt(args(2))
          for (i <- 0 until n) {
            println(s"$i of $n")

            val bm = new BatchManager(context)
            val db = config.data.absoluteDBReader(context.matrix)
            try {
              val keys = bm.matrixContext.probeMap.keys
              val xs = bm.matrixContext.sampleMap.tokens.take(len).map(Sample(_))
              db.valuesInSamples(xs, db.sortProbes(keys), true)
            } finally {
              db.release
            }
          }
        case "sampleCheck" =>
          sampleCheck(config.data.exprDb,
            args.size > 1 && args(1) == "delete")
          sampleCheck(config.data.foldDb,
            args.size > 1 && args(1) == "delete")
        case _ => showHelp()
      }
    }
  }

  type ExpressionConverter = ColumnExpressionData => ColumnExpressionData

  private def sampleCheck(dbf: String, delete: Boolean)(implicit context: Context) {
    val bm = new BatchManager(context)
    implicit val mc = bm.matrixContext
    val kdb = MatrixDB.get(dbf, true)

    try {
      val ss = kdb.allSamples
      val xs = bm.matrixContext.sampleMap
      val unknowns = ss.toSet -- xs.tokens.map(Sample(_))

      println("Unknown set: " + unknowns)
      if (delete && !unknowns.isEmpty) {
        println("DELETING.")
        kdb.deleteSamples(unknowns, List())
      }
    } finally {
      kdb.release
    }
  }

  def verifyExists(bs: BatchStore, batch: String): Unit =
    bs.verifyExists(batch)

  def showHelp() {
    println("Please specify a command (add/updateMetadata/recalculate/delete/list/list-access/enable/disable)")
  }

  case class Batch(title: String, comment: String, instances: Option[Seq[String]], dataset: Option[String])
}

class BatchManager(context: Context) {
  import BatchManager.{Batch, _}
  import TRDF._

  def config = context.config
  def samples = context.sampleStore

  def matrixContext(): MatrixContext =
    new MatrixContext {
      def foldsDBReader = ???
      def absoluteDBReader = ???
      def timeSeriesBuilder = ???
      def doseSeriesBuilder = ???
      def timeSeriesDBReader = ???
      def doseSeriesDBReader = ???

      lazy val probeMap: ProbeMap =
        new ProbeIndex(KCIndexDB.readOnce(config.data.probeIndex))
      lazy val sampleMap: SampleMap =
        SampleIndex.fromRaw(KCIndexDB.readOnce(config.data.sampleIndex))

      override lazy val probeSets =
        new ProbeStore(config.triplestoreConfig).platformsAndProbes.
          mapValues(_.toSeq.map(p => probeMap.pack(p.identifier)))
      lazy val enumMaps: Map[String, Map[String, Int]] = {
        val db = KCIndexDB(config.data.enumIndex, false)
        doThenClose(db)(db => db.enumMaps(config.timeSeriesBuilder.enums))
      }
    }

  val requiredParameters = config.attributes.getRequired.asScala.map(_.id)
  val hlParameters = config.attributes.getHighLevel.asScala.map(_.id)

  /**
   * Add a batch, or append samples to an existing batch
   * @param batch The batch, with visible instances and dataset flags
   * @param metadataFile File to read metadata from
   * @param dataFile Expression data file
   * @param callFile Calls (P/A for e.g. affymetrix arrays) file
   * @param append Whether to append to a pre-existing batch.
   * @param generateAttributes Whether to generate per-batch attributes in the RDF
   *                           data for this batch. If false, system-wide attributes will be used
   * @param conversion The transformation to apply to expression data as it is read from the file, if any
   *
   * @return
   */
  def add(batch: Batch, metadataFile: String,
    dataFile: String, callFile: Option[String],
    append: Boolean, generateAttributes: Boolean,
          conversion: Option[ExpressionConverter]): Task[Unit] = {

    for {
      metadata <- readTSVMetadata(batch, metadataFile, generateAttributes)
      data <- readCSVExpressionData(metadata, dataFile, callFile, conversion)
      _ <- newMetadataCheck(batch.title, metadata, config, append, Some(data)) andThen
        (if (append) updateMetadata(batch, metadata, generateAttributes, append = true, recalculate = false) else
          addMetadata(batch, metadata, generateAttributes)) andThen
        addEnums(metadata, generateAttributes) andThen
        // Note that we rely on probe maps, sample maps etc in matrixContext
        // not being read until they are needed
        // (after addSampleIDs has run, which happens in addMetadata)
        (for {
          mc <- Task.simple("Create matrix context") {
            matrixContext()
          }
          _ <- addExprData(metadata, dataFile, callFile, conversion)(mc) andThen
                recalculateFoldsAndSeries(metadata, generateAttributes)
        } yield ())
    } yield ()
  }

  def updateMetadataFromFile(batch: Batch, metaFile: String, customAttributes: Boolean,
                             append: Boolean, recalculate: Boolean, force: Boolean = false): Task[Unit] = {
    for {
      metadata <- readTSVMetadata(batch, metaFile, customAttributes)
      _ <- updateMetadata(batch, metadata, customAttributes, append, recalculate, force)
    } yield ()
  }

  /**
   * Update metadata for samples in a batch. This (partially) deletes batch metadata and reinserts
   * the specified new metadata.
   *
   * @param metadata Specifies the samples to update metadata for. If append is enabled, this may be a mix of
   *                 existing and new samples.
   * @param append Whether to allow adding new samples
   * @param force Allow the update to proceed even if some sanity checks do not pass
   */
  def updateMetadata(batch: Batch, metadata: Metadata, customAttributes: Boolean,
                     append: Boolean, recalculate: Boolean, force: Boolean = false): Task[Unit] = {

    for {
      _ <- updateMetadataCheck(batch.title, metadata, config, append, force) andThen
        deleteBatchRDF(batch.title, Some(metadata)) andThen
        addMetadata(batch, metadata, customAttributes) andThen
        (if (recalculate) recalculateFoldsAndSeries(metadata, customAttributes) else Task.success)
    } yield ()
  }

  def recalculateFoldsAndSeries(metadata: Metadata, customAttributes: Boolean): Task[Unit] = {
    implicit val mc = matrixContext()

    val platforms = metadata.attributeValues(CoreParameter.Platform)
    val probeMap = new ProbeStore(config.triplestoreConfig).platformsAndProbes
    val probes = platforms.flatMap(probeMap(_))
    val codedProbes = probes.map(p => mc.probeMap.pack(p.identifier))

    val dbReader = () => config.data.absoluteDBReader
    val units = metadata.treatedControlGroups(metadata.samples)

    val recalculateChunks = (for (unitChunk <- units.grouped(50);
      sampleChunk = unitChunk.flatMap(u => u._1 ++ u._2).distinct;
      filteredMetadata = new FilteredMetadata(metadata, sampleChunk)
    ) yield {
      for {
        _ <- insertFoldsDataFromExpressionData(dbReader, codedProbes,
            filteredMetadata) andThen
          addTimeSeriesData(filteredMetadata) andThen
          addDoseSeriesData(filteredMetadata)
      } yield ()
    }).reduce(_ andThen _)

    addEnums(metadata, customAttributes) andThen recalculateChunks

  }

  def insertFoldsDataFromExpressionData(reader: () => MatrixDBReader[PExprValue],
    probes: Iterable[Int], metadata: Metadata)(implicit mc: MatrixContext) =
    new AtomicTask[Unit]("Insert fold value data from expression data") {
      def run() {
        val expressionData = new DBColumnExpressionData(reader(), metadata.samples, probes) {
          override def logEvent(msg: String) { log(msg) }
        }
        doThenClose(expressionData)(data => addFoldsData(metadata, data).execute())
      }
    }

  def readTSVMetadata(batch: Batch, filename: String, customAttributes: Boolean) =
    new AtomicTask[Metadata]("Read TSV metadata") {
    override def run(): Metadata = {
      val attrSet = if (customAttributes) {
        //If the batch already had custom attributes, the existing custom set will be used as a basis.
        //Else we create a new set
        val batchURI = BatchStore.packURI(batch.title)
        samples.attributeSetForBatch(batchURI, AttributeSet.newMinimalSet())
      } else Some(config.attributes)

      TSVMetadata(filename, attrSet, log(_))
    }
  }

  def addMetadata(batch: Batch, metadata: Metadata, generateAttributes: Boolean): Task[Unit] = {
    val ts = config.triplestoreConfig.getTriplestore()

    //CreateBatchGraph will have no effect if the batch already exists, other than adding a new timestamp
    createBatchGraph(batch.title, batch.comment, config.triplestoreConfig) andThen
      updateBatchProperties(batch) andThen
      addSampleIDs(metadata) andThen
      addSampleRDF(batch.title, metadata, ts, generateAttributes)
  }

  /** Update batch properties like visibility, comments */
  def updateBatchProperties(batch: Batch) = new AtomicTask[Unit]("Update batch record") {
    override def run(): Unit = {
      val bs = new BatchStore(config.triplestoreConfig)
      // Update instances and dataset if specified in batch
      batch.instances.foreach(instances => {
        val existingInstances = bs.listAccess(batch.title)
        for (i <- instances; if !existingInstances.contains(i)) {
          log(s"Enabling access to instance $i")
          bs.enableAccess(batch.title, i)
        }
        for (i <- existingInstances; if !instances.contains(i)) {
          log(s"Disabling access to instance $i")
          bs.disableAccess(batch.title, i)
        }
      })
      batch.dataset.foreach(dataset => {
        val oldDataset = bs.getDatasets().getOrElse(batch.title, null)
        if (dataset != oldDataset) {
          val ds = new DatasetStore(config.triplestoreConfig)
          if (oldDataset != null) {
            log(s"Removing association with dataset $oldDataset")
            ds.removeMember(batch.title, oldDataset)
          }
          ds.addMember(batch.title, dataset)
          log(s"Associating batch with dataset $dataset")
        }
        bs.setComment(batch.title, TRDF.escape(batch.comment))
      })
    }
  }

  def delete(title: String, rdfOnly: Boolean = false): Task[Unit] = {
    implicit val mc = matrixContext()

    //Enums can not yet be deleted.
    (if (!rdfOnly) {
      deleteTimeSeriesData(title) andThen
        deleteDoseSeriesData(title) andThen
        deleteFoldData(title) andThen
        deleteExprData(title) andThen
        deleteSampleIDs(title)
    } else {
      println("RDF ONLY mode - not deleting series, fold, expr, sample ID data")
      Task.success
    }) andThen
      deleteBatchRDF(title, None) //Also removes the "batch record"
  }

  /** Validate metadata for insertion.
   * @param append Whether the operation is an append (new sample insertion is allowed)
   * @param expressionData Expression data to co-validate with the metadata, if any
   */
  def newMetadataCheck(title: String, metadata: Metadata, baseConfig: BaseConfig, append: Boolean,
                       expressionData: Option[ColumnExpressionData]) =
      new AtomicTask[Unit]("Check validity of new metadata") {
    override def run(): Unit = {
        checkValidIdentifier(title, "batch ID")

        val batches = new BatchStore(baseConfig.triplestoreConfig)
        val batchExists = batches.getList().contains(title)
        if (append && !batchExists) {
          throw new Exception(s"Cannot append to nonexistent batch $title")
        } else if (!append && batchExists) {
          throw new Exception(s"Cannot create new batch $title: batch already exists")
        }

        val batchSampleIds = batches.getSamples(title).toSet
        platformsCheck(metadata)
        val metadataIds = metadata.samples.map(_.identifier)
        metadataIds.foreach(checkValidIdentifier(_, "sample ID"))

        val (foundInBatch, notInBatch) = metadataIds.partition(batchSampleIds contains _)
        if (foundInBatch.size > 0) {
          log(s"Will replace samples ${foundInBatch mkString ", "}")
        }

        for {exprData <- expressionData} {
          val samples = exprData.samples.map(_.sampleId)
          val notFoundInExprData = metadataIds.toSet -- samples
          if (notFoundInExprData.nonEmpty) {
            throw new Exception(s"The samples ${notFoundInExprData mkString ", "} are defined in the metadata, but not defined in the expression data.")
          }

          val notFoundInMetadata = samples.toSet -- metadataIds
          if (notFoundInMetadata.nonEmpty) {
            throw new Exception(s"The samples ${notFoundInMetadata mkString ","} are defined in the expression data, but not in the metadata.")
          }
        }

        val existingSamples = samples.getList().toSet
        val (idCollisions, newSamples) = notInBatch.partition(existingSamples contains _)
        if (idCollisions.size > 0) {
          throw new Exception(s"The samples ${idCollisions mkString ", "} have already been " +
              "defined in other batches.")
        } else {
          log(s"Will create samples ${newSamples mkString ", "}")
        }
      }
  }

  def updateMetadataCheck(title: String, metadata: Metadata, baseConfig: BaseConfig,
                          append: Boolean, force: Boolean) =
      new AtomicTask[Unit]("Check validity of metadata update") {
    override def run(): Unit = {
      checkValidIdentifier(title, "batch ID")

      val batches = new BatchStore(baseConfig.triplestoreConfig)
      val batchExists = batches.getList().contains(title)
      if (!batchExists && !force) {
        throw new Exception(s"Cannot update metadata for nonexistent batch $title")
      }

      val batchSampleIds = batches.getSamples(title).toSet
      platformsCheck(metadata)
      val metadataIds = metadata.samples.map(_.identifier)
      metadataIds.foreach(checkValidIdentifier(_, "sample ID"))

      val (foundInBatch, notInBatch) = metadataIds.partition(batchSampleIds contains _)
      if (notInBatch.nonEmpty && !append && !force) {
        val msg = "New metadata file contained the following samples that " +
          s"could not be found in the existing batch: ${notInBatch mkString " "}"
        throw new Exception(msg)
      }

      val inBatchSet = foundInBatch.toSet
      val notInMetadata = batchSampleIds.filter(x => !(inBatchSet contains x))
      if (notInMetadata.nonEmpty) {
        log(s"${notInMetadata.size} samples in the batch were not included in the new metadata. They will remain unchanged.")
      }
    }
  }

  private def platformsCheck(metadata: Metadata) {
    val platforms = new PlatformStore(config).getList().toSet
    for (s <- metadata.samples; p = metadata.platform(s)) {
      if (!platforms.contains(p)) {
        throw new Exception(s"The sample ${s.identifier} contained an undefined platform_id ($p)")
      }
    }
  }

  def createBatchGraph(title: String, comment: String, ts: TriplestoreConfig) =
    new AtomicTask[Unit]("Add batch record") {
      override def run(): Unit = {
        val bs = new BatchStore(ts)
        bs.addWithTimestamp(title, TRDF.escape(comment))
      }
    }

  def addSampleIDs(metadata: Metadata) = new AtomicTask[Unit]("Insert sample IDs") {
    override def run(): Unit = {
      var newSamples, existingSamples: Int = 0
      val dbfile = config.data.sampleIndex
      val db = KCIndexDB(dbfile, true)
      doThenClose(db)(db => {
        log(s"Writing to $dbfile")
        for (s <- metadata.samples; id = s.identifier) {
          db.get(id) match {
            case Some(id) => existingSamples += 1
            case None =>
              db.put(id)
              newSamples += 1
          }
        }
        logResult(s"$newSamples new samples added, $existingSamples samples already existed")
      }
      )
    }
  }

  def deleteSampleIDs(title: String) = new AtomicTask[Unit]("Delete Sample IDs") {
    override def run(): Unit = {
      val dbfile = config.data.sampleIndex
      val db = KCIndexDB(dbfile, true)
      doThenClose(db)(db => {
        log(s"Opened $dbfile for writing")
        val bs = new BatchStore(config.triplestoreConfig)
        db.remove(bs.getSamples(title))
      })
    }
  }

  def addSampleRDF(title: String, metadata: Metadata, ts: Triplestore,
                   addAttributes: Boolean) =
    new AtomicTask[Unit]("Insert sample RDF data") {
      override def run(): Unit = {
        val tempFiles = new TempFiles()
        //time series and dose series use same enums
        val summaries = config.timeSeriesBuilder.enums.map(e => AttribValueSummary(context.sampleStore, e))

        try {
          val total = metadata.samples.size
          val grs = metadata.samples.grouped(250)
          var percentComplete = 0d
          val context = BatchStore.context(title)
          if (addAttributes) {
            val ttl = BatchStore.attributesToTTL(metadata.attributeSet, tempFiles)
            ts.addTTL(ttl, context)
          }
          while (grs.hasNext && shouldContinue(percentComplete)) {
            val g = grs.next
            for (s <- summaries) {
              s.check(metadata, g)
            }
            val ttl = BatchStore.metadataSamplesToTTL(metadata, tempFiles, g)
            ts.addTTL(ttl, context)
            percentComplete += 250.0 * 100.0 / total
          }

          for (s <- summaries) {
            logResult(s.summary(true))
          }

        } finally {
          tempFiles.dropAll
        }
      }
    }

  /** Delete the RDF data associated with a batch.
   * @param title batch ID
   * @param metadata If not given, the entire batch is deleted.
   *                 If given, only samples in the metadata, as well as all custom attributes in the batch will
   *                 be deleted.
   */
  def deleteBatchRDF(title: String, metadata: Option[Metadata]) =
    new AtomicTask[Unit]("Delete RDF data") {
    override def run(): Unit = {
      val bs = new BatchStore(config.triplestoreConfig)
      metadata match {
        case Some(m) =>
          bs.deleteSamples(title, m.samples)
          //Delete custom attributes in the batch.
          //If we are updating metadata, then these will be reinserted after the attribute set has been updated.
          bs.deleteCustomAttributes(title)
          //Also delete the old timestamp, in anticipation of a new one
          bs.deleteTimestamp(title)
          //Delete all data associated with the batch
        case _ => bs.delete(title)
      }
    }
  }

  def readCSVExpressionData(md: Metadata, niFile: String,
      callFile: Option[String],
      conversion: Option[ExpressionConverter]): Task[ColumnExpressionData] =
    new AtomicTask[ColumnExpressionData]("Read raw expression data") {
      override def run() = {
        val raw = new CSVRawExpressionData(niFile, callFile, Some(md.samples.size), m => log(s"Warning: $m"))
        conversion.map(c => c(raw)).getOrElse(raw)
      }
  }

  def addExprData(md: Metadata, niFile: String, callFile: Option[String],
      conversion: Option[ExpressionConverter])
      (implicit mc: MatrixContext) = {
    val db = () => config.data.extWriter(config.data.exprDb)
    for {
      data <- readCSVExpressionData(md, niFile, callFile, conversion)
      _ <- new SimpleValueInsert(db, data).insert("Insert expression value data")
    } yield ()
  }

  def addFoldsData(md: Metadata, data: ColumnExpressionData)
      (implicit mc: MatrixContext) = {
    val db = () => config.data.extWriter(config.data.foldDb)
    for {
      fvs <- Task.simple("Generate expression data") {
        new PFoldValueBuilder(md, data)
      }
      _ <- new SimpleValueInsert(db, fvs).insert("Insert fold value data")
    } yield ()
  }

  private def deleteFromDB(db: MatrixDBWriter[_], samples: Iterable[Sample], platforms: Iterable[String]) {
    try {
      db.deleteSamples(samples, platforms)
    } catch {
      case lf: LookupFailedException =>
        println("Lookup failed for sample, ignoring (possible reason: interrupted data insertion)")
        println("Please investigate manually!")
      case t: Throwable => throw t
    }
  }

  def deleteFoldData(title: String)(implicit mc: MatrixContext) =
    deleteExtFormatData(title, config.data.foldDb, "Delete fold data")

  def deleteExprData(title: String)(implicit mc: MatrixContext) =
    deleteExtFormatData(title, config.data.exprDb, "Delete normalized intensity data")

  private def deleteExtFormatData(title: String, database: String, taskName: String)
    (implicit mc: MatrixContext) =
    new AtomicTask[Unit](taskName) {
      override def run(): Unit = {
        val bs = new BatchStore(config.triplestoreConfig)
        val ss = bs.getSamples(title).map(Sample(_))
        val platforms = bs.getPlatforms(title)
        if (ss.isEmpty) {
          log("Nothing to do, batch has no samples")
          return
        }
        var percentComplete = 0d
        val chunks = ss.grouped(25)
        val db = config.data.extWriter(database)
        try {
          for (
            chunk <- chunks;
            if shouldContinue(percentComplete)
          ) {
            deleteFromDB(db, chunk, platforms)
            percentComplete += 100 * 25.0 / ss.size
          }
        } finally {
          db.release()
        }
      }
    }

  def addEnums(md: Metadata, customAttributes: Boolean) =
    new AtomicTask[Unit]("Add enum values") {
      /*
       * Note: enums currently cannot be deleted. We may eventually need a system
       * to rebuild enum databases.
       */
      override def run(): Unit = {
        if (customAttributes) return

        val db = KCIndexDB(config.data.enumIndex, true)
        doThenClose(db)(db => {
          for (
            s <- md.samples; paramMap = md.parameterMap(s);
            e <- config.timeSeriesBuilder.enums // time series and dose series have the same enums
          ) {
            db.findOrCreate(e, paramMap(e))
          }

          //Insert standard values to ensure they are always present
          for ((k, v) <- config.timeSeriesBuilder.standardEnumValues ++
            config.doseSeriesBuilder.standardEnumValues) {
            db.findOrCreate(k, v)
          }
        })
      }
    }

  def addTimeSeriesData(md: Metadata)(implicit mc: MatrixContext) =
    addSeriesData(md, config.data.timeSeriesDb, config.timeSeriesBuilder, "time")(mc)

  def addDoseSeriesData(md: Metadata)(implicit mc: MatrixContext) =
    addSeriesData(md, config.data.doseSeriesDb, config.doseSeriesBuilder, "dose")(mc)

  def addSeriesData[S <: Series[S]](md: Metadata, dbName: String,
    builder: SeriesBuilder[S], kind: String)(implicit mc: MatrixContext) =
      new AtomicTask[Unit](s"Insert $kind series data") {
    override def run(): Unit = {
      //idea: use RawExpressionData directly as source +
      //give KCMatrixDB and e.g. CSVRawExpressionData a common trait/adapter

      val source: MatrixDBReader[PExprValue] = config.data.foldsDBReader
      var target: KCSeriesDB[S] = null
      var inserted = 0
      val treated = md.samples.filter(x => !md.isControl(x))

      val data = builder.makeNew(source, md, treated)
      println(s"${data.size} series to insert")
      val total = data.size

      /*
      This method (and deleteSeriesData) groups the data by probe before performing db reads/writes
      to obtain better performance.
      If a large number of samples is operated on at once, we might run out of memory. If that happens,
      chunking the samples/control groups on the outer level should resolve the issue.
      The next step would be the unify the this method with deleteSeriesData, since the structure is so similar.
       */
      try {
        target = KCSeriesDB[S](dbName, true, builder, false)
          var pcomp = 0d
          for {
            ss <- data.groupBy(_.probe).values;
            s <- ss;
            if shouldContinue(pcomp)
          } {
            target.addPoints(s)
            inserted += 1
            pcomp = 100.0 * inserted / total
          }
      } finally {
        logResult(s"Series for $inserted series inserted")
        if (target != null) {
          target.release
        }
        source.release
      }
    }
  }

  def deleteTimeSeriesData(batch: String)(implicit mc: MatrixContext) =
    deleteSeriesData(batch, config.data.timeSeriesDb, config.timeSeriesBuilder, "time")(mc)

  def deleteDoseSeriesData(batch: String)(implicit mc: MatrixContext) =
    deleteSeriesData(batch, config.data.doseSeriesDb, config.doseSeriesBuilder, "dose")(mc)

  def deleteSeriesData[S <: Series[S]](batch: String, dbName: String,
      builder: SeriesBuilder[S], kind: String)(implicit mc: MatrixContext) =
        new AtomicTask[Unit](s"Delete $kind series data") {
    override def run(): Unit = {

      val batchURI = BatchStore.defaultPrefix + "/" + batch

      val sf = SampleFilter(batchURI = Some(batchURI))

      val md = new CachingTriplestoreMetadata(samples, config.attributes,
        config.attributes.getRequired.asScala ++ config.attributes.getHighLevel.asScala
          ++ config.attributes.getUnitLevel.asScala, sf)

      val controlGroups = md.treatedControlGroups(md.samples)
      val treated = controlGroups.flatMap(_._1)

      /*
        Constructs series with empty (zero) data points
        for those independent parameter values that are defined in the metadata
       */
      val data = builder.makeNewEmpty(md, treated)
      val total = data.size

      var target: KCSeriesDB[S] = null
      try {
        target = KCSeriesDB[S](dbName, true, builder, false)
        var pcomp = 0d
        for {
          ss <- data.groupBy(_.probe).values;
          s <- ss;
          if shouldContinue(pcomp)
        } {
          try {
            target.removePoints(s)
          } catch {
            case lfe: LookupFailedException =>
              println(lfe)
              println("Exception caught, continuing deletion of series")
          }
        }
        pcomp += 100.0 / total
      } finally {
        if (target != null) {
          target.release
        }
      }
    }
  }

}
