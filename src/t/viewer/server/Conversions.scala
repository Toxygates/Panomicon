package t.viewer.server

import scala.collection.JavaConversions._

import t.common.shared.SampleClass

object Conversions {
	implicit def scAsScala(sc: SampleClass): t.sparql.SampleClass = 
	  new t.sparql.SampleClass(asScalaMap(sc.getMap))	
	
	implicit def asSpecies(sc: SampleClass): otg.Species.Species = 
	  otg.Species.withName(sc.get("organism"))
	  
	implicit def scAsJava(sc: t.sparql.SampleClass): SampleClass =
	  new SampleClass(new java.util.HashMap(asJavaMap(sc.constraints)))
}