package otg

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
  
  val supportedSpecies = List(Rat, Human, Mouse)
}

