package otg.db.file

import friedrich.util.formats.TSVFile
import otg.db.Metadata
import scala.Array.canBuildFrom
import otg.db.SampleParameter
import t.db.Sample

/**
 * Represents a metadata file of the type used in Open TG-Gates.
 * A file with tab-separated values.
 */
class TSVMetadata(file: String) extends 
	t.db.file.TSVMetadata(file, SampleParameter) with otg.db.Metadata {
  
   /**
   * Find the files that are control samples in the collection that a given barcode
   * belongs to.
   * 
   * TODO think about ways to generalise this without depending on the 
   * key/value of dose_level = Control. Possibly depend on DataSchema
   */
  override def controlSamples(s: Sample): Iterable[Sample] = {
    val idx = getIdx(s)      
   	val expTime = metadata("exposure_time")(idx)
    val cgroup = metadata("control_group")(idx)
    
   	println(expTime + " " + cgroup)
   	
   	val idxs = (0 until metadata("sample_id").size).filter(x => {
   	  metadata("exposure_time")(x) == expTime &&
      metadata("control_group")(x) == cgroup &&
   	  metadata("dose_level")(x) == "Control"   	     	  
   	})
   	idxs.map(metadata("sample_id")(_)).map(Sample(_))
  }
 
}