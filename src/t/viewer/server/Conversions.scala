package t.viewer.server

import t.viewer.shared.SampleClass
import scala.collection.JavaConversions._

object Conversions {
	implicit def scAsScala(sc: SampleClass): t.sparql.SampleClass = 
	  new t.sparql.SampleClass(asScalaMap(sc.getMap))	
	
	implicit def asSpecies(sc: SampleClass): otg.Species.Species = 
	  otg.Species.withName(sc.get("organism"))
}