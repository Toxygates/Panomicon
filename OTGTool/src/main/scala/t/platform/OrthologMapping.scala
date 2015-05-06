package t.platform

case class OrthologMapping(name: String, mappings: Iterable[Iterable[String]]) {

  lazy val forProbe: Map[String, Seq[String]] = 
    Map() ++ mappings.flatMap(m => {
      val s = m.toSeq
      s.map(x => (x-> s))
    })
  
}