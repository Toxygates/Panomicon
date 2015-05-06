package otg

import scala.collection.immutable.ListMap

//TODO only used by tests now, can be removed
@deprecated
object RepeatType extends Enumeration(0) {
  type RepeatType = Value
  val Single, Repeat = Value
}

//ditto
@deprecated
object Organ extends Enumeration(0) {
  type Organ = Value
  val Vitro, Liver, Kidney, Lung, Spleen, Muscle, LymphNode = Value
  
  def lookup(s: String): Organ = {
    s match {
      case "in vitro" => Vitro
      case _ => withName(s)
    }
  }
}

@deprecated
object Species extends Enumeration(0) {
  type Species = Value
  val Human, Rat, Mouse = Value

  implicit class ExtSpecies(s: Species) {    
    def longName: String = s match {
      case Human => "Homo sapiens"
      case Rat => "Rattus norvegicus"
      case Mouse => "Mus musculus"
    }
    def taxon: Int = s match {
      case Human => 9606
      case Rat => 10116
      case Mouse => 10090
    }
    def shortCode: String = s match {
      case Human => "hsa"
      case Rat => "rno"
      case Mouse => "mmu"
    }    
  }
}

