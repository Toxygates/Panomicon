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

package t

import scala.Vector
import scala.collection.JavaConverters._
import scala.language.implicitConversions

import t.db._
import t.db.file.CSVRawExpressionData
import t.db.file.PFoldValueBuilder
import t.db.kyotocabinet._
import t.global.KCDBRegistry
import t.sparql._
import t.util.TempFiles
import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.model.sample.CoreParameter
import t.db.file.TSVMetadata
import t.db.file.CachedCSVRawExpressionData

/**
 * Batch management CLI
 */
object BatchManager extends ManagerTool {
  def apply(args: Seq[String])(implicit context: Context): Unit = {

    def config = context.config
    def factory = context.factory

    val batches = new Batches(config.triplestore)
    if (args.size < 1) {
      showHelp()
    } else {
      args(0) match {
        case "add" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          val append = booleanOption(args, "-append")
          val cached = booleanOption(args, "-cached")
          val comment = stringOption(args, "-comment").getOrElse("")

          val bm = new BatchManager(context)

          val metaList = stringListOption(args, "-multiMetadata") orElse
            stringListOption(args, "-multimetadata")
          metaList match {
            case Some(metaFiles) =>
              KCDBRegistry.setMaintenance(true)
              // For the first metadata file, we use the value of the -append argument; for all other
              // the batch will certainly exist so we always append
              var first = true
              new Platforms(config).populateAttributes(config.attributes)
              val tasks = metaFiles.map({ metadata =>
                //val md = factory.tsvMetadata(metadata, config.attributes)
                val dataFile = metadata.replace(".meta.tsv", ".data.csv")

                val f = new java.io.File(metadata.replace(".meta.tsv", ".call.csv"))
                val callFile = if (f.exists()) Some(f.getPath) else None
                println(s"Insert $dataFile")
                val task = bm.add(Batch(title, comment, None, None),
                  metadata, dataFile, callFile,
                  if (first) append else true, cached = cached)
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

              new Platforms(config).populateAttributes(config.attributes)
              //val md = factory.tsvMetadata(metaFile, config.attributes)
              startTaskRunner(bm.add(Batch(title, comment, None, None),
                metaFile, dataFile, callFile, append, cached = cached))
          }

        case "recalculate" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          new Platforms(config).populateAttributes(config.attributes)
          val sampleFilter = new SampleFilter(None, Some(Batches.packURI(title)))
          val metadata =
            factory.cachingTriplestoreMetadata(context.samples, config.attributes,
                config.attributes.getHighLevel.asScala ++ 
                config.attributes.getUnitLevel.asScala ++
                List(CoreParameter.Platform, CoreParameter.ControlGroup,
                  CoreParameter.Batch))(sampleFilter)
          startTaskRunner(new BatchManager(context).recalculateFoldsAndSeries(
            Batch(title, "", None, None), metadata))

        case "updateMetadata" | "updatemetadata" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          val comment = stringOption(args, "-comment").getOrElse("")
          val bm = new BatchManager(context)
          val metaFile = require(stringOption(args, "-metadata"),
            "Please specify a metadata file with -metadata")
          val force = booleanOption(args, "-force")
          val recalculate = booleanOption(args, "-recalculate")
          new Platforms(config).populateAttributes(config.attributes)
          //val md = factory.tsvMetadata(metaFile, config.attributes)
          startTaskRunner(bm.updateMetadata(Batch(title, comment, None, None),
              metaFile, recalculate, force = force))

        case "delete" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          val rdfOnly = booleanOption(args, "-rdfonly")
          verifyExists(batches, title)
          val bm = new BatchManager(context)
          startTaskRunner(bm.delete(title, rdfOnly))
        case "list" =>
          println("Batch list")
          for (b <- batches.list) {
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
          //TODO: do not access the db files directly (obtain readers instead)
          sampleCheck(config.data.exprDb,
            args.size > 1 && args(1) == "delete")
          sampleCheck(config.data.foldDb,
            args.size > 1 && args(1) == "delete")
        case _ => showHelp()
      }
    }
  }

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
        kdb.deleteSamples(unknowns)
      }
    } finally {
      kdb.release
    }
  }

  def verifyExists(bs: Batches, batch: String): Unit =
    bs.verifyExists(batch)

  def showHelp() {
    println("Please specify a command (add/updateMetadata/recalculate/delete/list/list-access/enable/disable)")
  }

  case class Batch(title: String, comment: String, instances: Option[Seq[String]], dataset: Option[String])
}

class BatchManager(context: Context) {
  import TRDF._
  import BatchManager.Batch

  def config = context.config
  def samples = context.samples

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
        new SampleIndex(KCIndexDB.readOnce(config.data.sampleIndex))

      override lazy val probeSets =
        new Probes(config.triplestore).platformsAndProbes.
          mapValues(_.toSeq.map(p => probeMap.pack(p.identifier)))

      lazy val enumMaps: Map[String, Map[String, Int]] = {
        val db = KCIndexDB(config.data.enumIndex, false)
        try {
          db.enumMaps(config.timeSeriesBuilder.enums)
        } finally {
          db.release()
        }
      }
    }

  val requiredParameters = config.attributes.getRequired.asScala.map(_.id)
  val hlParameters = config.attributes.getHighLevel.asScala.map(_.id)

  def add[S <: Series[S]](batch: Batch, metadataFile: String,
    dataFile: String, callFile: Option[String],
    append: Boolean, simpleLog2: Boolean = false,
    cached: Boolean = false): Task[Unit] = {

    for {
      metadata <- readTSVMetadata(metadataFile)
      _ <- newMetadataCheck(batch.title, metadata, config, append) andThen
        addMetadata(batch, metadata, append) andThen
        addEnums(metadata) andThen
        // Note that we rely on probe maps, sample maps etc in matrixContext
        // not being read until they are needed
        // (after addSampleIDs has run, which happens in addMetadata)
        (for {
          mc <- Task.simple("Create matrix context") {
            matrixContext()
          }
          _ <- addExprData(metadata, dataFile, callFile, cached)(mc) andThen
                recalculateFoldsAndSeries(batch, metadata, simpleLog2)
        } yield ())
    } yield ()
  }

  def updateMetadata[S <: Series[S]](batch: Batch, metaFile: String,
      recalculate: Boolean = false, simpleLog2: Boolean = false,
      force: Boolean = false): Task[Unit] = {
    for {
      metadata <- readTSVMetadata(metaFile)
      _ <- updateMetadataCheck(batch.title, metadata, config, force) andThen
        deleteRDF(batch.title) andThen
        addMetadata(batch, metadata, false, true) andThen
        (if (recalculate) recalculateFoldsAndSeries(batch, metadata, simpleLog2) else Task.success)
    } yield ()
  }

  def recalculateFoldsAndSeries[S <: Series[S]](batch: Batch, metadata: Metadata,
      simpleLog2: Boolean = false): Task[Unit] = {
    implicit val mc = matrixContext()

    val platforms = metadata.attributeValues(CoreParameter.Platform)
    val probeMap = new Probes(config.triplestore).platformsAndProbes
    val probes = platforms.flatMap(probeMap(_)).toSeq
    val codedProbes = probes.map(p => mc.probeMap.pack(p.identifier))

    val treatedSamples = metadata.samples.filter(!metadata.isControl(_))

    val dbReader = () => config.data.absoluteDBReader
    val units = metadata.treatedControlGroups(metadata.samples)

    val recalculateChunks = (for (unitChunk <- units.grouped(50);
      sampleChunk = unitChunk.toSeq.flatMap(u => u._1 ++ u._2).distinct;
      filteredMetadata = context.factory.filteredMetadata(metadata, sampleChunk)
    ) yield {
      for {
        _ <- insertFoldsDataFromExpressionData(dbReader, codedProbes,
            filteredMetadata, simpleLog2) andThen
          addTimeSeriesData(filteredMetadata) andThen
          addDoseSeriesData(filteredMetadata)
      } yield ()
    }).reduce(_ andThen _)

    addEnums(metadata) andThen recalculateChunks

  }

  def insertFoldsDataFromExpressionData(reader: () => MatrixDBReader[ExprValue],
    probes: Iterable[Int], metadata: Metadata, simpleLog2: Boolean)(implicit mc: MatrixContext) =
    new AtomicTask[Unit]("Insert fold value data from expression data") {
      def run() {
        val expressionData = new DBColumnExpressionData(reader(), metadata.samples, probes) {
          override def logEvent(msg: String) { log(msg) }
        }
        try {
          addFoldsData(metadata, expressionData, simpleLog2).execute()
        } finally {
          expressionData.release()
        }
      }
    }

  def readTSVMetadata(filename: String) = new AtomicTask[Metadata]("Read TSV metadata") {
    override def run(): Metadata = {
      context.factory.tsvMetadata(filename, config.attributes, log(_))
    }
  }

  def addMetadata[S <: Series[S]](batch: Batch, metadata: Metadata,
      append: Boolean, update: Boolean = false): Task[Unit] = {
    val ts = config.triplestore.get

    val addRecordIfNecessary =
      if (!append) {
        addRecord(batch.title, batch.comment, config.triplestore) andThen
          updateBatch(batch)
      } else {
        Task.success
      }

    addRecordIfNecessary andThen
      (if (!update) addSampleIDs(metadata) else Task.success) andThen
      addRDF(batch.title, metadata, ts)
  }

  def updateBatch(batch: Batch) = new AtomicTask[Unit]("Update batch record") {
    override def run(): Unit = {
      val bs = new Batches(config.triplestore)
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
        val oldDataset = bs.datasets.getOrElse(batch.title, null)
        if (dataset != oldDataset) {
          val ds = new Datasets(config.triplestore)
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

  def delete[S <: Series[S]](title: String, rdfOnly: Boolean = false): Task[Unit] = {
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
      deleteRDF(title) //Also removes the "batch record"
  }

  def newMetadataCheck(title: String, metadata: Metadata, baseConfig: BaseConfig, append: Boolean) =
      new AtomicTask[Unit]("Check validity of new metadata") {
    override def run(): Unit = {
        checkValidIdentifier(title, "batch ID")

        val batches = new Batches(baseConfig.triplestore)
        val batchExists = batches.list.contains(title)
        if (append && !batchExists) {
          throw new Exception(s"Cannot append to nonexsistent batch $title")
        } else if (!append && batchExists) {
          throw new Exception(s"Cannot create new batch $title: batch already exists")
        }

        val batchSampleIds = batches.samples(title).toSet
        platformsCheck(metadata)
        val metadataIds = metadata.samples.map(_.identifier)
        metadataIds.foreach(checkValidIdentifier(_, "sample ID"))

        val (foundInBatch, notInBatch) = metadataIds.partition(batchSampleIds contains _)
        if (foundInBatch.size > 0) {
          log(s"Will replace samples ${foundInBatch mkString ", "}")
        }

        val existingSamples = samples.list.toSet
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
      force: Boolean) =
      new AtomicTask[Unit]("Check validity of metadata update") {
    override def run(): Unit = {
      checkValidIdentifier(title, "batch ID")

      val batches = new Batches(baseConfig.triplestore)
      val batchExists = batches.list.contains(title)
      if (!batchExists && !force) {
        throw new Exception(s"Cannot update metadata for nonexistent batch $title")
      }

      val batchSampleIds = batches.samples(title).toSet
      platformsCheck(metadata)
      val metadataIds = metadata.samples.map(_.identifier)
      metadataIds.foreach(checkValidIdentifier(_, "sample ID"))

      val (foundInBatch, notInBatch) = metadataIds.partition(batchSampleIds contains _)
      if (notInBatch.size > 0 && !force) {
        val msg = "New metadata file contained the following samples that " +
          s"could not be found in the existing batch: ${notInBatch mkString " "}"
        throw new Exception(msg)
      }

      val inBatchSet = foundInBatch.toSet
      val notInMetadata = batchSampleIds.filter(x => !(inBatchSet contains x))
      if (notInMetadata.size > 0 && !force) {
        val msg = "New metadata file is missing the following batch samples: " +
          (notInMetadata mkString " ")
        throw new Exception(msg)
      }
    }
  }

  private def platformsCheck(metadata: Metadata) {
    val platforms = new Platforms(config).list.toSet
    for (s <- metadata.samples; p = metadata.platform(s)) {
      if (!platforms.contains(p)) {
        throw new Exception(s"The sample ${s.identifier} contained an undefined platform_id ($p)")
      }
    }
  }

  private def suggestSampleId(existing: Set[String], candidate: String): String = {
    var n = 1
    var cand = candidate
    while(existing.contains(cand)) {
      cand = s"${candidate}_$n"
      n += 1
    }
    cand
  }

  def addRecord(title: String, comment: String, ts: TriplestoreConfig) =
    new AtomicTask[Unit]("Add batch record") {
      override def run(): Unit = {
        val bs = new Batches(ts)
        bs.addWithTimestamp(title, TRDF.escape(comment))
      }
    }

  def addSampleIDs(metadata: Metadata) = new AtomicTask[Unit]("Insert sample IDs") {
    override def run(): Unit = {
      var newSamples, existingSamples: Int = 0
      val dbfile = config.data.sampleIndex
      val db = KCIndexDB(dbfile, true)
      log(s"Opened $dbfile for writing")
      for (s <- metadata.samples; id = s.identifier) {
        db.get(id) match {
          case Some(id) => existingSamples += 1
          case None =>
            db.put(id)
            newSamples += 1
        }
      }
      logResult(s"$newSamples new samples added, $existingSamples samples already existed")
      log(s"Closed $dbfile")
    }
  }

  def deleteSampleIDs(title: String) = new AtomicTask[Unit]("Delete Sample IDs") {
    override def run(): Unit = {
      val dbfile = config.data.sampleIndex
      val db = KCIndexDB(dbfile, true)
      log(s"Opened $dbfile for writing")
      val bs = new Batches(config.triplestore)
      db.remove(bs.samples(title))
    }
  }

  def addRDF(title: String, metadata: Metadata, ts: Triplestore) =
    new AtomicTask[Unit]("Insert sample RDF data") {
      override def run(): Unit = {
        val tempFiles = new TempFiles()
        //time series and dose series use same enums
        val summaries = config.timeSeriesBuilder.enums.map(e => AttribValueSummary(context.samples, e))

        try {
          val total = metadata.samples.size
          val grs = metadata.samples.grouped(250)
          var percentComplete = 0d
          while (grs.hasNext && shouldContinue(percentComplete)) {
            val g = grs.next
            for (s <- summaries) {
              s.check(metadata, g)
            }
            val ttl = Batches.metadataToTTL(metadata, tempFiles, g)
            val context = Batches.context(title)
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

  def deleteRDF(title: String) = new AtomicTask[Unit]("Delete RDF data") {
    override def run(): Unit = {
      val bs = new Batches(config.triplestore)
      bs.delete(title)
    }
  }

  def readCSVExpressionData(md: Metadata, niFile: String,
      callFile: Option[String], cached: Boolean): Task[ColumnExpressionData] =
    new AtomicTask[ColumnExpressionData]("Read raw expression data") {
      override def run() = {
        if (cached) {
          new CachedCSVRawExpressionData(niFile, callFile,
            Some(md.samples.size), m => log(s"Warning: $m"))
        } else {
          new CSVRawExpressionData(niFile, callFile,
            Some(md.samples.size), m => log(s"Warning: $m"))
        }
    }
  }

  def addExprData(md: Metadata, niFile: String, callFile: Option[String], cached: Boolean)
      (implicit mc: MatrixContext) = {
    val db = () => config.data.extWriter(config.data.exprDb)
    for {
      data <- readCSVExpressionData(md, niFile, callFile, cached)
      _ <- new SimpleValueInsert(db, data).insert("Insert expression value data")
    } yield ()
  }

  def addFoldsData(md: Metadata, data: ColumnExpressionData, simpleLog2: Boolean)
      (implicit mc: MatrixContext) = {
    val db = () => config.data.extWriter(config.data.foldDb)
    for {
      fvs <- Task.simple("Generate expression data") {
        if (simpleLog2) {
          new Log2Data(data)
        } else {
          new PFoldValueBuilder(md, data)
        }
      }
      _ <- new SimpleValueInsert(db, fvs).insert("Insert fold value data")
    } yield ()
  }

  private def deleteFromDB(db: MatrixDBWriter[_], samples: Iterable[Sample]) {
    try {
      db.deleteSamples(samples)
    } catch {
      case lf: LookupFailedException =>
        println(s"Lookup failed for sample, ignoring (possible reason: interrupted data insertion)")
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
        val bs = new Batches(config.triplestore)
        val ss = bs.samples(title).map(Sample(_))
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
            deleteFromDB(db, chunk)
            percentComplete += 100 * 25.0 / ss.size
          }
        } finally {
          db.release()
        }
      }
    }

  def addEnums(md: Metadata) =
    new AtomicTask[Unit]("Add enum values") {
      /*
       * Note: enums currently cannot be deleted. We may eventually need a system
       * to rebuild enum databases.
       */
      override def run(): Unit = {
        val db = KCIndexDB(config.data.enumIndex, true)
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
      }
    }

  def addTimeSeriesData[S <: Series[S], E <: ExprValue](md: Metadata)(implicit mc: MatrixContext) =
    addSeriesData(md, config.data.timeSeriesDb, config.timeSeriesBuilder, "time")(mc)

  def addDoseSeriesData[S <: Series[S], E <: ExprValue](md: Metadata)(implicit mc: MatrixContext) =
    addSeriesData(md, config.data.doseSeriesDb, config.doseSeriesBuilder, "dose")(mc)

  def addSeriesData[S <: Series[S], E <: ExprValue](md: Metadata, dbName: String,
    builder: SeriesBuilder[S], kind: String)(implicit mc: MatrixContext) =
      new AtomicTask[Unit](s"Insert $kind series data") {
    override def run(): Unit = {
      //idea: use RawExpressionData directly as source +
      //give KCMatrixDB and e.g. CSVRawExpressionData a common trait/adapter

      val source: MatrixDBReader[PExprValue] = config.data.foldsDBReader
      var target: KCSeriesDB[S] = null
      var inserted = 0
      val controlGroups = md.treatedControlGroups(md.samples)
      val treated = controlGroups.toSeq.flatMap(_._1)

      val bySeries = builder.groupSamples(treated, md).map(_._2)
      val total = bySeries.size

      try {
        target = KCSeriesDB[S](dbName, true, builder, false)
          var pcomp = 0d
          for ( //TODO might want to chunk these
            samples <- bySeries;
            if shouldContinue(pcomp)
          ) {
            val filtered = new FilteredMetadata(md, samples)
            val xs = builder.makeNew(source, filtered)

            for (x <- xs; if shouldContinue(pcomp)) {
              target.addPoints(x)
              }
            pcomp = 100.0 * inserted / total
            inserted += 1
          }
      } finally {
        logResult(s"Series for $inserted control groups inserted")
        if (target != null) {
          target.release
        }
        source.release
      }
    }
  }

  def deleteTimeSeriesData[S <: Series[S]](batch: String)(implicit mc: MatrixContext) =
    deleteSeriesData(batch, config.data.timeSeriesDb, config.timeSeriesBuilder, "time")(mc)

  def deleteDoseSeriesData[S <: Series[S]](batch: String)(implicit mc: MatrixContext) =
    deleteSeriesData(batch, config.data.doseSeriesDb, config.doseSeriesBuilder, "dose")(mc)

  def deleteSeriesData[S <: Series[S]](batch: String, dbName: String,
      builder: SeriesBuilder[S], kind: String)(implicit mc: MatrixContext) =
        new AtomicTask[Unit](s"Delete $kind series data") {
    override def run(): Unit = {

      val batchURI = Batches.defaultPrefix + "/" + batch

      val sf = SampleFilter(batchURI = Some(batchURI))
      val tsmd = new TriplestoreMetadata(samples, config.attributes)(sf)

      //Note, strictly speaking we don't need the source data here.
      //This dependency could be removed by having the builder make points
      //with all zeroes.

      //Note: this can probably be made considerably faster by structuring it like addSeriesData
      //above, but then TriplestoreMetadata.controlSamples would have to be implemented first.
      val source: MatrixDBReader[PExprValue] = config.data.foldsDBReader
      var target: KCSeriesDB[S] = null
      try {
        target = KCSeriesDB[S](dbName, true, builder, false)
        val filtSamples = tsmd.samples
        val total = filtSamples.size
        var pcomp = 0d
        var it = filtSamples.grouped(100)
        while (it.hasNext && shouldContinue(pcomp)) {
          val sg = it.next
          val xs = builder.makeNew(source, tsmd, sg)
          for (x <- xs) {
            try {
              target.removePoints(x)
            } catch {
              case lfe: LookupFailedException =>
                println(lfe)
                println("Exception caught, continuing deletion of series")
            }
          }
          pcomp += 100.0 / total
        }
      } finally {
        if (target != null) {
          target.release
        }
        source.release
      }
    }
  }

}
