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

package t

import scala.Vector
import scala.collection.JavaConversions._

import t.db._
import t.db.file.CSVRawExpressionData
import t.db.file.PFoldValueBuilder
import t.db.kyotocabinet._
import t.global.KCDBRegistry
import t.sparql._
import t.util.TempFiles
import t.db.kyotocabinet.chunk.KCChunkMatrixDB

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
              startTaskRunner(metaFiles.flatMap { metadata =>
                val md = factory.tsvMetadata(metadata, config.attributes)
                val dataFile = metadata.replace(".meta.tsv", ".data.csv")

                val f = new java.io.File(metadata.replace(".meta.tsv", ".call.csv"))
                val callFile = if (f.exists()) Some(f.getPath) else None
                println(s"Insert $dataFile")
                val tasklets = bm.add(Batch(title, comment, None, None),
                  md, dataFile, callFile,
                  if (first) append else true, config.seriesBuilder)
                first = false
                tasklets
              })
            case None =>
              val metaFile = require(stringOption(args, "-metadata"),
                "Please specify a metadata file with -metadata")
              val dataFile = require(stringOption(args, "-data"),
                  "Please specify a data file with -data")
              val callFile = stringOption(args, "-calls")

              new Platforms(config).populateAttributes(config.attributes)
              val md = factory.tsvMetadata(metaFile, config.attributes)
              startTaskRunner(bm.add(Batch(title, comment, None, None),
                md, dataFile, callFile,
                append, config.seriesBuilder))
          }

        case "updateMetadata" | "updatemetadata" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          val comment = stringOption(args, "-comment").getOrElse("")
          val bm = new BatchManager(context)
          val metaFile = require(stringOption(args, "-metadata"),
            "Please specify a metadata file with -metadata")
          new Platforms(config).populateAttributes(config.attributes)
          val md = factory.tsvMetadata(metaFile, config.attributes)
          startTaskRunner(bm.updateMetadata(Batch(title, comment, None, None),
              md, config.seriesBuilder))

        case "delete" =>
          val title = require(stringOption(args, "-title"),
            "Please specify a title with -title")
          val rdfOnly = booleanOption(args, "-rdfonly")
          verifyExists(batches, title)
          val bm = new BatchManager(context)
          startTaskRunner(bm.delete(title, config.seriesBuilder, rdfOnly))
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
    println("Please specify a command (add/updateMetadata/delete/list/list-access/enable/disable)")
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
      def seriesBuilder = ???
      def seriesDBReader = ???
      def expectedProbes(x: Sample) = ???
      
      lazy val probeMap: ProbeMap =
        new ProbeIndex(KCIndexDB.readOnce(config.data.probeIndex))
      lazy val sampleMap: SampleMap =
        new SampleIndex(KCIndexDB.readOnce(config.data.sampleIndex))

      lazy val enumMaps: Map[String, Map[String, Int]] = {
        val db = KCIndexDB(config.data.enumIndex, false)
        try {
          db.enumMaps(config.seriesBuilder.enums)
        } finally {
          db.release()
        }
      }
    }

  val requiredParameters = config.attributes.getRequired.map(_.id)
  val hlParameters = config.attributes.getHighLevel.map(_.id)

  def add[S <: Series[S]](batch: Batch, metadata: Metadata,
    dataFile: String, callFile: Option[String],
    append: Boolean, sbuilder: SeriesBuilder[S],
    simpleLog2: Boolean = false): Iterable[Tasklet] = {
    var r: Vector[Tasklet] = Vector()

    r :+= newMetadataCheck(batch.title, metadata, config, append)
    r ++= addMetadata(batch, metadata, append, sbuilder)

    // Note that we rely on probe maps, sample maps etc in matrixContext
    // not being read until they are needed
    // (after addSampleIDs has run, which happens in addMetadata)
    // TODO: more robust updating of maps
    implicit val mc = matrixContext()
    r :+= addEnums(metadata, sbuilder)

    //TODO logging directly to TaskRunner is controversial.
    //Would be better to log from inside the tasklets.
    r :+= addExprData(metadata, dataFile, callFile,
        m => TaskRunner.log(s"Warning: $m"))
    r :+= addFoldsData(metadata, dataFile, callFile, simpleLog2,
        m => TaskRunner.log(s"Warning: $m"))
    r :+= addSeriesData(metadata, sbuilder)

    r
  }

  def updateMetadata[S <: Series[S]](batch: Batch,
      metadata: Metadata, sbuilder: SeriesBuilder[S]): Iterable[Tasklet] = {
    var r: Vector[Tasklet] = Vector()
    r :+= updateMetadataCheck(batch.title, metadata, config)
    r :+= deleteRDF(batch.title)
    r ++= addMetadata(batch, metadata, false, sbuilder)
    r
  }

  def addMetadata[S <: Series[S]](batch: Batch, metadata: Metadata,
      append: Boolean, sbuilder: SeriesBuilder[S]): Iterable[Tasklet] = {
    var r: Vector[Tasklet] = Vector()

    val ts = config.triplestore.get
    if (!append) {
      r :+= addRecord(batch.title, batch.comment, config.triplestore)
      r :+= updateBatch(batch)
    }
    r :+= addSampleIDs(metadata)
    r :+= addRDF(batch.title, metadata, sbuilder, ts)

    r
  }

  def updateBatch(batch: Batch) =
    new Tasklet("Update batch record") {
      def run() {
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

  def delete[S <: Series[S]](title: String,
    sbuilder: SeriesBuilder[S], rdfOnly: Boolean = false): Iterable[Tasklet] = {
    var r: Vector[Tasklet] = Vector()
    implicit val mc = matrixContext()

    //Enums can not yet be deleted.
    if (!rdfOnly) {
      r :+= deleteSeriesData(title, sbuilder)
      r :+= deleteFoldData(title)
      r :+= deleteExprData(title)
      r :+= deleteSampleIDs(title)
    } else {
      println("RDF ONLY mode - not deleting series, fold, expr, sample ID data")
    }
    r :+= deleteRDF(title) //Also removes the "batch record"

    r
  }

  def newMetadataCheck(title: String, metadata: Metadata, baseConfig: BaseConfig, append: Boolean) =
      new Tasklet("Check validity of new metadata") {
    def run() {
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

  def updateMetadataCheck(title: String, metadata: Metadata, baseConfig: BaseConfig) =
      new Tasklet("Check validity of metadata update") {
    def run() {
      checkValidIdentifier(title, "batch ID")

      val batches = new Batches(baseConfig.triplestore)
      val batchExists = batches.list.contains(title)
      if (!batchExists) {
        throw new Exception(s"Cannot update metadata for nonexistent batch $title")
      }

      val batchSampleIds = batches.samples(title).toSet
      platformsCheck(metadata)
      val metadataIds = metadata.samples.map(_.identifier)
      metadataIds.foreach(checkValidIdentifier(_, "sample ID"))

      val (foundInBatch, notInBatch) = metadataIds.partition(batchSampleIds contains _)
      if (notInBatch.size > 0) {
        val msg = "New metadata file contained the following samples that " +
          s"could not be found in the existing batch: ${notInBatch mkString " "}"
        throw new Exception(msg)
      }

      val notInMetadata = batchSampleIds.filter(x => !(foundInBatch contains x))
      if (notInMetadata.size > 0) {
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
    new Tasklet("Add batch record") {
      def run() {
        val bs = new Batches(ts)
        bs.addWithTimestamp(title, TRDF.escape(comment))
      }
    }

  def addSampleIDs(metadata: Metadata) = new Tasklet("Insert sample IDs") {
    def run() {
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

  def deleteSampleIDs(title: String) = new Tasklet("Delete Sample IDs") {
    def run() {
      val dbfile = config.data.sampleIndex
      val db = KCIndexDB(dbfile, true)
      log(s"Opened $dbfile for writing")
      val bs = new Batches(config.triplestore)
      db.remove(bs.samples(title))
    }
  }

  def addRDF(title: String, metadata: Metadata, sb: SeriesBuilder[_], ts: Triplestore): Tasklet = {

    new Tasklet("Insert sample RDF data") {
      def run() {
        val tempFiles = new TempFiles()
        val summaries = sb.enums.map(e => AttribValueSummary(context.samples, e))

        try {
          //TODO check existence of samples

          val total = metadata.samples.size
          val grs = metadata.samples.grouped(1000)
          var percentComplete = 0d
          while (grs.hasNext && shouldContinue(percentComplete)) {
            val g = grs.next
            for (s <- summaries) {
              s.check(metadata, g)
            }
            val ttl = Batches.metadataToTTL(metadata, tempFiles, g)
            val context = Batches.context(title)
            ts.addTTL(ttl, context)
            percentComplete += 1000.0 * 100.0 / total
          }

          for (s <- summaries) {
            logResult(s.summary(true))
          }

        } finally {
          tempFiles.dropAll
        }
      }
    }
  }

  def deleteRDF(title: String) = new Tasklet("Delete RDF data") {
    def run() {
      val bs = new Batches(config.triplestore)
      bs.delete(title)
    }
  }

  def addExprData(md: Metadata, niFile: String, callFile: Option[String],
    warningHandler: (String) => Unit)(implicit mc: MatrixContext) = {
    val data = new CSVRawExpressionData(List(niFile), callFile.toList,
        Some(md.samples.size), warningHandler)    
      val db = () => config.data.extWriter(config.data.exprDb)
      new SimpleValueInsert(db, data).insert("Insert expr value data")
  }

  def addFoldsData(md: Metadata, foldFile: String, callFile: Option[String],
      simpleLog2: Boolean, warningHandler: (String) => Unit)
  (implicit mc: MatrixContext) = {
    val data = new CSVRawExpressionData(List(foldFile), callFile.toList,
        Some(md.samples.size), warningHandler)
    val fvs = if (simpleLog2) {
      new Log2Data(data)
    } else {
      new PFoldValueBuilder(md, data)
    }
    val db = () => config.data.extWriter(config.data.foldDb)
    new SimpleValueInsert(db, fvs).insert("Insert fold value data")
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
    new Tasklet(taskName) {
      def run() {
        val bs = new Batches(config.triplestore)
        val ss = bs.samples(title).map(Sample(_))
        if (ss.isEmpty) {
          log("Nothing to do, batch has no samples")
          return
        }
        val db = config.data.extWriter(database)
        try {
          deleteFromDB(db, ss)
        } finally {
          db.release()
        }
      }
    }

  def addEnums(md: Metadata, sb: SeriesBuilder[_])(implicit mc: MatrixContext) =
    new Tasklet("Add enum values") {
      /*
       * Note: enums currently cannot be deleted. We may eventually need a system
       * to rebuild enum databases.
       */
      def run() {
        val db = KCIndexDB(config.data.enumIndex, true)
        for (
          s <- md.samples; paramMap = md.parameterMap(s);
          e <- sb.enums
        ) {
          db.findOrCreate(e, paramMap(e))
        }

        //Insert standard values to ensure they are always present
        for ((k, v) <- sb.standardEnumValues) {
          db.findOrCreate(k, v)
        }
      }
    }

  def addSeriesData[S <: Series[S], E <: ExprValue](md: Metadata,
    builder: SeriesBuilder[S])(implicit mc: MatrixContext) = new Tasklet("Insert series data") {
    def run() {
      //idea: use RawExpressionData directly as source +
      //give KCMatrixDB and e.g. CSVRawExpressionData a common trait/adapter

      val source: MatrixDBReader[PExprValue] = config.data.foldsDBReader
      var target: KCSeriesDB[S] = null
      var inserted = 0
      try {
        target = KCSeriesDB[S](config.data.seriesDb, true, builder, false)
        val xs = builder.makeNew(source, md)
        val total = xs.size
        var pcomp = 0d
        val it = xs.iterator
        while (it.hasNext && shouldContinue(pcomp)) {
          val x = it.next
          target.addPoints(x)
          pcomp += 100.0 / total
          inserted += 1
        }
      } finally {
        logResult(s"$inserted series inserted")
        if (target != null) {
          target.release
        }
        source.release
      }
    }
  }

  def deleteSeriesData[S <: Series[S]](batch: String, builder: SeriesBuilder[S])(implicit mc: MatrixContext) = new Tasklet("Delete series data") {
    def run() {

      val batchURI = Batches.defaultPrefix + "/" + batch

      val sf = SampleFilter(batchURI = Some(batchURI))
      val tsmd = new TriplestoreMetadata(samples, config.attributes)(sf)

      //Note, strictly speaking we don't need the source data here.
      //This dependency could be removed by having the builder make points
      //with all zeroes.
      val source: MatrixDBReader[PExprValue] = config.data.foldsDBReader
      var target: KCSeriesDB[S] = null
      try {
        target = KCSeriesDB[S](config.data.seriesDb, true, builder, false)
        val filtSamples = tsmd.samples
        val total = filtSamples.size
        var pcomp = 0d
        var it = filtSamples.grouped(100)
        while (it.hasNext && shouldContinue(pcomp)) {
          val sg = it.next
          val xs = builder.makeNew(source, tsmd, sg)
          for (x <- xs) {
            target.removePoints(x)
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
