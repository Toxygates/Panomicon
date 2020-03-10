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

package t

import t.db.kyotocabinet.KCIndexDB
import t.global.KCDBRegistry
import t.platform.PlatformDefFile
import t.platform.affy.Converter
import t.sparql.Platforms
import t.sparql.Probes
import t.sparql.TRDF
import t.util.TempFiles
import t.util.DoThenClose._

/**
 * Platform/probe management CLI
 */
object PlatformManager extends ManagerTool {
  def format(command: String): PlatformFormat = command match {
    case "add" => GeneralPlatform
    case "addEnsembl" => EnsemblPlatform
    case "addAffy" => AffymetrixPlatform
  }

  def apply(args: Seq[String])(implicit context: Context): Unit = {

    if (args.size < 1) {
      showHelp()
    } else {
      val manager = new PlatformManager(context)
      val platforms = new Platforms(context.config)
      try {
        args(0) match {
          case "add" | "addEnsembl" =>
            val pfFormat = format(args(0))

            val title = require(stringOption(args, "-title"),
              "Please specify a title with -title")
            val inputFile = require(stringOption(args, "-input"),
              "Please specify a definition file with -input")
            val defns = new PlatformDefFile(inputFile).records
            val comment = stringOption(args, "-comment").getOrElse("")
            startTaskRunner(manager.add(title, comment, inputFile, pfFormat))
          case "delete" =>
            val title = require(stringOption(args, "-title"),
              "Please specify a title with -title")
            startTaskRunner(manager.delete(title))
          case "list" =>
            for (p <- platforms.list) {
              println(p)
            }
          case _ => showHelp()
        }
      } finally {
        KCDBRegistry.closeWriters()
      }
    }
  }

  def showHelp() {
    println("Please specify a command (add/delete/list)")
  }
}

sealed trait PlatformFormat
case object AffymetrixPlatform extends PlatformFormat
case object EnsemblPlatform extends PlatformFormat
case object GeneralPlatform extends PlatformFormat
case object BioPlatform extends PlatformFormat

class PlatformManager(context: Context) {
  import TRDF._
  def config = context.config

  /*
   * Note: AffymetrixFormat and BioFormat might be represented differently,
   * e.g. with case objects or enums.
   * They are mutually exclusive.
   */
  def add(title: String, comment: String,
    inputFile: String, format: PlatformFormat): Task[Unit] = {
    val pf = new Platforms(config)

    consistencyCheck(title) andThen
      (
        format match {
          case AffymetrixPlatform =>
            addFromAffymetrix(title, comment, inputFile) andThen
              addProbeIDs(title)
          case GeneralPlatform =>
            addStandard(title, comment, inputFile, format == BioPlatform) andThen
              addProbeIDs(title)
          case EnsemblPlatform =>
            addFromEnsembl(title, comment, inputFile) andThen
              addProbeIDs(title)
          case _ => throw new Exception("Unsupported platform format")
        }
        )
  }

  def consistencyCheck(title: String): AtomicTask[Unit] = new AtomicTask[Unit]("Consistency check") {
    override def run(): Unit = {
      checkValidIdentifier(title, "platform title")
    }
  }

  def addFromAffymetrix(title: String, comment: String, file: String): AtomicTask[Unit] =
    new AtomicTask[Unit]("Insert platform from Affymetrix data") {
      override def run(): Unit = {
        val tf = new TempFiles()
        try {
          val platforms = new Platforms(config)
          val temp = tf.makeNew("TPLATFORM", "tsv")
          Converter.convert(file, temp.getAbsolutePath())
          val defns = new PlatformDefFile(temp.getAbsolutePath()).records
          for (d <- defns) {
            checkValidIdentifier(d.id, "probe ID")
          }
          val total = defns.size.toDouble

          val g = 1000

          val (start, rest) = defns.splitAt(g)
          platforms.redefine(title, comment, false, start)
          var pcomp = 0d
          val groups = rest.grouped(g)
          while (groups.hasNext && shouldContinue(pcomp)) {
            val tg = groups.next
            val ttl = Probes.recordsToTTL(tf, title, tg)
            pcomp += g.toDouble * 100.0 / total
            platforms.triplestore.addTTL(ttl, Platforms.context(title))
          }
        } finally {
          tf.dropAll()
        }
      }
    }

  /**
   * Add a platform from Ensembl data.
   * Expected input is an RDF file (e.g. TTL) preprocessed with prepare_ensembl.sh.
   * @param title
   * @param comment
   * @param file
   * @return
   */

  def addFromEnsembl(title: String, comment: String, file: String): AtomicTask[Unit] = {
    ???
  }

  /**
   * Add a platform from the "standard" T platform format (tsv).
   * @param biological Is this platform a "biological" parameter platform, with e.g. blood data,
   * and not an 'omics platform?
   */
  def addStandard(title: String, comment: String, file: String,
      biological: Boolean): AtomicTask[Unit] =
    new AtomicTask[Unit]("Add platform (RDF)") {
      override def run(): Unit = {
        val defns = new PlatformDefFile(file).records
        val platforms = new Platforms(config)
        platforms.redefine(title, TRDF.escape(comment), biological, defns)
      }
    }

  def addProbeIDs(title: String): AtomicTask[Unit] =
    new AtomicTask[Unit]("Add probe IDs") {
      override def run(): Unit = {
        var newProbes, existingProbes: Int = 0
        val probes = new Probes(config.triplestore).forPlatform(title)
        val dbfile = config.data.probeIndex
        val db = KCIndexDB(dbfile, true)
        doThenClose(db)(db => {
          log(s"Opened $dbfile for writing")
          for (p <- probes) {
            db.get(p) match {
              case Some(id) => existingProbes += 1
              case None =>
                db.put(p)
                newProbes += 1
            }
          }
          logResult(s"$newProbes new probes added, $existingProbes probes already existed")
        })
      }
    }

  def delete(title: String): Task[Unit] = {
      //Do not delete the probe IDs - keep them so they can be reused if we
      //redefine the platform
      //deleteProbeIDs(title) andThen
      deleteRDF(title)
  }

  def deleteRDF(title: String): AtomicTask[Unit] = new AtomicTask[Unit]("Delete platform") {
    override def run(): Unit = {
      val platforms = new Platforms(config)
      platforms.delete(title)
    }
  }

  def deleteProbeIDs(title: String): AtomicTask[Unit] =
    new AtomicTask[Unit]("Delete probe IDs") {
      override def run(): Unit = {
        val dbfile = config.data.probeIndex
        val db = KCIndexDB(dbfile, true)
        doThenClose(db)(db => {
          val probes = new Probes(config.triplestore).forPlatform(title)
          log(s"Opened $dbfile for writing")
          db.remove(probes)
        })
      }
    }
}
