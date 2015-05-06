package t.sparql

import scala.collection.{Map => CMap}

object SampleClass {  
  def constraint(key: String, x: Option[String]) = x.map(key -> _)
}

case class SampleClass(constraints: CMap[String, String] = Map()) {
  
  def filterAll: Filter = {
    if (constraints.isEmpty) {
      Filter("", "")
    } else {
      val ptn = "?x " + constraints.keySet.map(k => s"t:$k ?$k").mkString(";") + "."
      val cnst = "FILTER(" + constraints.keySet.map(k => {
      "?" + k + " = \"" + constraints(k) + "\""
      }).mkString(" && ") + "). "
      Filter(ptn, cnst)
    }    
  }
  
  def filter(key: String): Filter = {
    if (!constraints.contains(key)) {
      Filter("", "")
    } else {
      Filter(s"?x t:$key ?$key.", "FILTER(?" + key + " = \"" + constraints(key) + "\").")
    }
  }
   
	def apply(key: String) = constraints(key)
	
	def get(key: String) = constraints.get(key)
}