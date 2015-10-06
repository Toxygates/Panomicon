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

package t

import t.db.ExprValue
import otg.OTGContext
import otg.OTGInsert
import t.db.ProbeMap
import t.db.SampleMap
import t.db.LookupFailedException
import t.db.Metadata
import t.db.file.CSVRawExpressionData
import otg.sparql.OTGSamples
import t.db.MatrixContext
import t.db.MatrixDB
import t.db.MatrixDBReader
import t.db.MatrixDBWriter
import t.db.ProbeIndex
import t.db.Sample
import t.db.SampleIndex
import t.db.Series
import t.db.SeriesBuilder
import t.db.kyotocabinet.KCExtMatrixDB
import t.db.kyotocabinet.KCIndexDB
import t.db.kyotocabinet.KCSeriesDB
import t.sparql.Batches
import t.sparql.Platforms
import t.sparql.SimpleTriplestore
import t.sparql.TRDF
import t.sparql.Triplestore
import t.sparql.TriplestoreMetadata
import t.util.TempFiles
import t.db.kyotocabinet.KCMatrixDB
import t.sparql.SampleFilter

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
    }

    args(0) match {
      case "add" =>
        val title = require(stringOption(args, "-title"),
          "Please specify a title with -title")
        val metaFile = require(stringOption(args, "-metadata"),
          "Please specify a metadata file with -metadata")
        val niFile = stringOption(args, "-ni")
        val foldFile = require(stringOption(args, "-fold"),
          "Please specify a folds file with -fold")
        val callFile = stringOption(args, "-calls")
        val foldCallFile = stringOption(args, "-foldCalls")
        val foldPFile = stringOption(args, "-foldP")
        val append = booleanOption(args, "-append")
        val comment = stringOption(args, "-comment").getOrElse("")

        if (batches.list.contains(title) && !append) {
          val msg = s"Batch $title already exists"
          throw new Exception(msg)
        }

        val bm = new BatchManager(context)
        val md = factory.tsvMetadata(metaFile)
        withTaskRunner(bm.addBatch(title, comment,
          md, niFile, callFile,
          foldFile, foldCallFile, foldPFile,
          append, config.seriesBuilder))
      case "delete" =>
        val title = require(stringOption(args, "-title"),
          "Please specify a title with -title")
        // TODO move verification into the batches API
        verifyExists(batches, title)
        val bm = new BatchManager(context)
        withTaskRunner(bm.deleteBatch(title, config.seriesBuilder))
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
      case "checkOrder" =>
        expectArgs(args, 4)
        val len = Integer.parseInt(args(3))
        val bm = new BatchManager(context)
        val db = KCMatrixDB.apply(config.data.exprDb, false)(bm.matrixContext)
        try {
          db.dumpKeys(args(1), args(2), len)
        } finally {
          db.release
        }
      case "loadTest" =>
        expectArgs(args, 3)
        val len = Integer.parseInt(args(1))
        val n = Integer.parseInt(args(2))
        for (i <- 0 until n) {
          println(s"$i of $n")

          val bm = new BatchManager(context)
          val db = KCMatrixDB.apply(config.data.exprDb, false)(bm.matrixContext)
          try {
            val keys = bm.matrixContext.probeMap.keys
            val xs = bm.matrixContext.sampleMap.tokens.take(len).map(Sample(_))
            db.valuesInSamples(xs, keys)
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

  private def sampleCheck(dbf: String, delete: Boolean)(implicit context: Context) {
    val bm = new BatchManager(context)
    implicit val mc = bm.matrixContext
    val db = KCMatrixDB.apply(dbf, true)
    try {
      val ss = db.allSamples(true, Set())
      val xs = bm.matrixContext.sampleMap
      val unknowns = ss.map(_._1).toSet -- xs.keys

      println("Unknown set: " + unknowns)
      if (delete && !unknowns.isEmpty) {
        db.allSamples(true, unknowns)
      }
    } finally {
      db.release
    }
  }

  def verifyExists(bs: Batches, batch: String): Unit = {
    if (!bs.list.contains(batch)) {
      val msg = s"Batch $batch does not exist"
      throw new Exception(msg)
    }
  }

  def showHelp() {
    throw new Exception("Please specify a command (add/delete/list/list-access/enable/disable)")
  }
}

class BatchManager(context: Context) {
  import TRDF._

  def config = context.config
  def samples = context.samples

  def matrixContext(): MatrixContext = {
    new MatrixContext {
      def foldsDBReader = null
      def absoluteDBReader = null
      def seriesBuilder = null

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
  }

  val requiredParameters = config.sampleParameters.required.map(_.identifier)
  val hlParameters = config.sampleParameters.highLevel.map(_.identifier)

  def addBatch[S <: Series[S]](title: String, comment: String, metadata: Metadata,
    niFile: Option[String], callFile: Option[String], foldFile: String,
    foldCallFile: Option[String], foldPValueFile: Option[String],
    append: Boolean, sbuilder: SeriesBuilder[S]): Iterable[Tasklet] = {
    var r: Vector[Tasklet] = Vector()
    val ts = config.triplestore.get

    r :+= consistencyCheck(title, metadata, config)

    if (!append) {
      r :+= addBatchRecord(title, comment, config.triplestore)
    }
    r :+= addSampleIDs(metadata)
    r :+= addRDF(title, metadata, sbuilder, ts)

    // Note that we rely on these maps not being read until they are needed
    // (after addSampleIDs has run!)
    // TODO: more robust updating of maps
    implicit val mc = matrixContext()
    r :+= addEnums(metadata, sbuilder)

    if (niFile != None) {
      r :+= addExprData(niFile.get, callFile)
    }
    r :+= addFoldsData(foldFile, foldCallFile, foldPValueFile)
    r :+= addSeriesData(metadata, sbuilder)
    r
  }

  def deleteBatch[S <: Series[S]](title: String,
    sbuilder: SeriesBuilder[S]): Iterable[Tasklet] = {
    var r: Vector[Tasklet] = Vector()
    implicit val mc = matrixContext()

    //Enums can not yet be deleted.

    r :+= deleteSeriesData(title, sbuilder)
    r :+= deleteFoldData(title)
    r :+= deleteExprData(title)
    r :+= deleteSampleIDs(title)
    r :+= deleteRDF(title) //Also removes the "batch record"
    r
  }

  def consistencyCheck(title: String, md: Metadata, c: BaseConfig) = new Tasklet("Consistency check") {
    def run() {
      checkValidIdentifier(title, "batch ID")

      val batches = new Batches(c.triplestore)
      if (batches.list.contains(title)) {
        log(s"The batch $title is already defined, assuming update/append")
      }

      val bsamples = batches.samples(title).toSet

      val ps = new Platforms(config.triplestore)
      val platforms = ps.list.toSet
      val existingSamples = samples.list.toSet
      for (s <- md.samples; p = md.platform(s)) {
        if (!platforms.contains(p)) {
          throw new Exception(s"The sample ${s.identifier} contained an undefined platform_id ($p)")
        }
        if (bsamples.contains(s.identifier)) {
          log(s"Replacing sample ${s.identifier}")
        } else if (existingSamples.contains(s.identifier)) {
          throw new Exception(s"The sample ${s.identifier} has already been defined in a different batch")
        }
        checkValidIdentifier(s.identifier, "sample ID")
      }
    }
  }

  def addBatchRecord(title: String, comment: String, ts: TriplestoreConfig) =
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
      db.release()
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
      db.release()
    }
  }

  def addRDF(title: String, metadata: Metadata, sb: SeriesBuilder[_], ts: Triplestore): Tasklet = {

    new Tasklet("Insert sample RDF data") {
      def run() {
        val tempFiles = new TempFiles()
        val samples = new OTGSamples(config)
        val summaries = sb.enums.map(e => AttribValueSummary(samples, e))

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
            //TODO cancellation check
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

  def addExprData(niFile: String, callFile: Option[String])(implicit mc: MatrixContext) = {
    val ic = OTGInsert.insertionContext(false, config.data.exprDb)
    val data = new CSVRawExpressionData(List(niFile), callFile.map(List(_)), None)
    ic.insert(data)
  }

  def addFoldsData(foldFile: String, callFile: Option[String],
    pValueFile: Option[String])(implicit mc: MatrixContext) = {
    val ic = OTGInsert.insertionContext(true, config.data.foldDb)
    val data = new CSVRawExpressionData(List(foldFile), callFile.map(List(_)),
      pValueFile.map(List(_)))
    ic.insertFolds(data)
  }

  private def deleteFromDB(db: MatrixDBWriter[_], samples: Iterable[Sample]) {
    for (s <- samples) {
      try {
        db.deleteSample(s)
      } catch {
        case lf: LookupFailedException =>
          println(s"Lookup failed for sample $s, ignoring (possible reason: interrupted data insertion)")
        case t: Throwable => throw t
      }
    }
  }

  def deleteFoldData(title: String)(implicit mc: MatrixContext) =
    new Tasklet("Delete fold data") {
      def run() {
        val bs = new Batches(config.triplestore)
        val ss = bs.samples(title).map(Sample(_))
        val db = OTGInsert.matrixDB(true, config.data.foldDb)
        try {
          deleteFromDB(db, ss)
        } finally {
          db.release()
        }
      }
    }

  def deleteExprData(title: String)(implicit mc: MatrixContext) =
    new Tasklet("Delete normalized intensity data") {
      def run() {
        val bs = new Batches(config.triplestore)
        val ss = bs.samples(title).map(Sample(_))
        val db = OTGInsert.matrixDB(false, config.data.exprDb)
        try {
          deleteFromDB(db, ss)
        } finally {
          db.release()
        }
      }
    }

  def addEnums(md: Metadata, sb: SeriesBuilder[_])(implicit mc: MatrixContext) =
    new Tasklet("Add enum values") {
      //TODO these cannot be deleted currently
      def run() {
        val db = KCIndexDB(config.data.enumIndex, true)
        try {
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

        } finally {
          db.release()
        }
      }
    }

  def addSeriesData[S <: Series[S], E <: ExprValue](md: Metadata,
    builder: SeriesBuilder[S])(implicit mc: MatrixContext) = new Tasklet("Insert series data") {
    def run() {
      val source = KCMatrixDB(config.data.foldDb, false)
      val target = KCSeriesDB[S](config.data.seriesDb, true, builder)
      var inserted = 0
      try {
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
        source.release
        target.release
      }
    }
  }

  def deleteSeriesData[S <: Series[S]](batch: String, builder: SeriesBuilder[S])(implicit mc: MatrixContext) = new Tasklet("Delete series data") {
    def run() {

      val bs = new Batches(config.triplestore)
      val ss = bs.samples(batch).map(Sample(_))
      val batchURI = Batches.defaultPrefix + "/" + batch

      val sf = SampleFilter(batchURI = Some(batchURI))
      val tsmd = new TriplestoreMetadata(samples)(sf)

      //Note, strictly speaking we don't need the source data here.
      //This dependency could be removed by having the builder make points
      //with all zeroes.
      val source = KCMatrixDB(config.data.foldDb, false)
      val target = KCSeriesDB[S](config.data.seriesDb, true, builder)
      val filtSamples = tsmd.samples
      val total = filtSamples.size
      var pcomp = 0d
      try {
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
        source.release
        target.release
      }
    }
  }

}
