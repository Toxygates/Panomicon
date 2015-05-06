package t.db.file

import friedrich.util.formats.TSVFile
import t.db.ParameterSet
import t.db.Sample
import t.db.Metadata

class TSVMetadata(file: String, parameters: ParameterSet) extends Metadata {
    protected val raw: Map[String, Array[String]] = {
    val columns = TSVFile.readMap("", file)
    columns.flatMap(x => {
      val lc = x._1.toLowerCase()
      //Normalise the upper/lowercase-ness and remove unknown columns
      parameters.byIdLowercase.get(lc).map(sp => sp.identifier -> x._2)      
    })
  }
  val requiredColumns = parameters.required.map(_.identifier.toLowerCase)
    
  protected def metadata = raw
    
  val neColumns = requiredColumns.filter(! raw.keySet.contains(_)) 
  if (!neColumns.isEmpty) {
    println(s"The following columns are missing in $file: $neColumns")
    throw new Exception("Missing columns in metadata")
  }
  
  var uniqueIds = Set[String]()
  //sanity check
  for (id <- metadata("sample_id")) {
    if (uniqueIds.contains(id)) {
      throw new Exception(s"Metadata error in $file. The sample '${id}' is defined twice.")
    }
    uniqueIds += id
  }
  
  def samples: Iterable[Sample] = {
    val ids = metadata("sample_id")
    ids.map(Sample(_))
  }
  
  protected lazy val idxLookup = Map() ++ metadata("sample_id").zipWithIndex
  
  protected def getIdx(s: Sample): Int = {
    idxLookup.get(s.identifier) match {
      case Some(i) => i
      case _ =>
        throw new Exception(s"Sample (${s.sampleId}) not found in metadata")
    }        
  } 
 
   def parameters(s: Sample): Iterable[(t.db.SampleParameter, String)] = {
    val idx = getIdx(s)
    metadata.map(column => (parameters.byId(column._1), column._2(idx)))     
  }
  
  def parameterValues(identifier: String): Set[String] = 
    metadata(identifier).toSet
  
  override def parameter(s: Sample, identifier: String): String = {
    val idx = getIdx(s)
    metadata(identifier)(idx)
  }
}