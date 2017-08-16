package t.platform.mirbase

import scala.io.Source
import otg.Species


case class MirnaRecord(id: String, accession: String, species: Species.Species) {
  def asTPlatformRecord: String = {
    s"$id\ttitle=$id,accession=$accession,species=${species.longName}"
  }
}

/**
 * Reads raw miRBase dat files and produces T platform format files.
 */
object Converter {
 
  val speciesOfInterest = Species.values.map(_.shortCode)
  
  def main(args: Array[String]) {
      val lines = Source.fromFile(args(0)).getLines()
      var (record, next) = lines.span(! _.startsWith("//"))
      
      def remainingRecords: Stream[Iterable[String]] = {
        if (!lines.hasNext) {
          Stream.empty
        } else {
          val r = lines.takeWhile(! _.startsWith("//")).toSeq
          lines.drop(1)
          Stream.cons(r, remainingRecords)
        }
      }
      
      val mrecs = for (rec <- remainingRecords;
        //e.g. 
        //ID   mmu-let-7g        standard; RNA; MMU; 88 BP.
        id <- rec.find(_.startsWith("ID"));
        //e.g.
        //AC   MI0000137;
        accession <- rec.find(_.startsWith("AC"));        
        ids = id.split("\\s+");
        acs = accession.split("\\s+");
        sp <- Species.values.find(s => ids(1).startsWith(s.shortCode)); 
        mrec = Some(MirnaRecord(ids(1), acs(1).replace(";", ""), sp))              
      ) yield mrec
      
      for (mr <- mrecs.flatten) {
        println(mr.asTPlatformRecord)
      }
  }
}