package t

import t.sparql.Platforms
import t.platform.PlatformDefFile
import t.util.TempFiles
import t.platform.AffymetrixConverter
import t.db.kyotocabinet.KCIndexDB
import t.sparql.Probes
import t.sparql.TRDF

/**
 * Platform/probe management CLI
 */
object PlatformManager extends ManagerTool {
  def apply(args: Seq[String])(implicit context: Context): Unit = {
	val platforms = new Platforms(context.config.triplestore)
    if (args.size < 1) {
      showHelp()
    }
	args(0) match {	  
	  case "add" =>
	    val title = require(stringOption(args, "-title"), 
	        "Please specify a title with -title")
	    val inputFile = require(stringOption(args, "-input"), 
	        "Please specify a definition file with -input")
	    val defns = new PlatformDefFile(inputFile).records
	    val comment = stringOption(args, "-comment").getOrElse("")
	    platforms.redefine(title, comment, defns) //TODO	    
	  case "delete" =>
	    val title = require(stringOption(args, "-title"), 
	        "Please specify a title with -title")	    
	    platforms.delete(title)
	  case "list" =>
	    for (p <- platforms.list) {
	      println(p)
	    }	 
	  case _ => showHelp()
	}
  }
  
  def showHelp() {
      throw new Exception("Please specify a command (add/delete/list)")
  }
}

class PlatformManager(context: Context) {
  import TRDF._
  def config = context.config
  
  def addPlatform(title: String, comment: String,
      inputFile: String, affymetrixFormat: Boolean): Iterable[Tasklet] = {
    val pf = new Platforms(config.triplestore)    
    var r = Vector[Tasklet]()
    r :+= consistencyCheck(title)
    
    if (affymetrixFormat) {
      //assume Affymetrix format
      r :+= addFromAffymetrix(title, comment, inputFile)     
      r :+= addProbeIDs(title)
    } else {
      //assume T format
      r :+= addStandard(title, comment, inputFile)
      r :+= addProbeIDs(title)
    }
    
    r
  }
  
  def consistencyCheck(title: String) = new Tasklet("Consistency check") {
    def run() {      
      checkValidIdentifier(title, "platform title")             
    }
  }
  
  def addFromAffymetrix(title: String, comment: String, file: String) =
    new Tasklet("Insert platform from Affymetrix data") {
    def run() {
      val tf = new TempFiles()
      try {
        val platforms = new Platforms(config.triplestore)
        val temp = tf.makeNew("TPLATFORM", "tsv")
        AffymetrixConverter.convert(file, temp.getAbsolutePath())
        val defns = new PlatformDefFile(temp.getAbsolutePath()).records
        for (d <- defns) {
          checkValidIdentifier(d.id, "probe ID")           
        }
        val total = defns.size.toDouble
        
        val g = 1000
        
        val (start, rest) = defns.splitAt(g)        
        platforms.redefine(title, comment, start)
        var pcomp = 0d
        val groups = rest.grouped(g)
        while (groups.hasNext && shouldContinue(pcomp)) {
          val tg = groups.next
          val ttl = Probes.recordsToTTL(tf, title, tg)
          pcomp += g.toDouble * 100.0 / total
          platforms.ts.addTTL(ttl, Platforms.context(title))          
        }                
      } finally {
        tf.dropAll()
      }
    }
  }
  
  def addStandard(title: String, comment: String, file: String) = 
  	new Tasklet("Add platform (RDF)") {
    def run() {
      val defns = new PlatformDefFile(file).records
      val platforms = new Platforms(config.triplestore)
      platforms.redefine(title, TRDF.escape(comment), defns)
    }
  }
  
  def addProbeIDs(title: String) = 
    new Tasklet("Add probe IDs") {
      def run() {
        var newProbes, existingProbes: Int = 0
        val probes = new Probes(config.triplestore).forPlatform(title)
        val dbfile = config.data.probeIndex
        val db = KCIndexDB(dbfile, true)
        log(s"Opened $dbfile for writing")
        try {
          for (p <- probes) {
            db.get(p) match {
              case Some(id) => existingProbes += 1
              case None =>
                db.put(p)
                newProbes += 1
            }
          }
          logResult(s"$newProbes new probes added, $existingProbes probes already existed")
        } finally {
          db.release()
          log(s"Closed $dbfile")
        }
      }
    }
  
  def deletePlatform(title: String): Iterable[Tasklet] = {
    Vector[Tasklet](
        //Do not delete the probe IDs - keep them so they can be reused if we 
        //redefine the platform
        //deleteProbeIDs(title),
        deletePlatformRDF(title)
        )    
  }
  
  def deletePlatformRDF(title: String): Tasklet = new Tasklet("Delete platform") {
    def run() {
      val platforms = new Platforms(config.triplestore)
      platforms.delete(title)
    }
  }
  
  def deleteProbeIDs(title: String) =
    new Tasklet("Delete probe IDs") {
      def run() {
        val dbfile = config.data.probeIndex
        val db = KCIndexDB(dbfile, true)
        val probes = new Probes(config.triplestore).forPlatform(title)
        try {
          log(s"Opened $dbfile for writing")
          db.remove(probes)
        } finally {
          db.release()
        }
      }
  }
}