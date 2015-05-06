package t.sparql

trait TRDF {
	val tRoot = "http://level-five.jp/t"
}

trait RDFClass extends TRDF {
  def itemClass: String
  def defaultPrefix: String
    
  def packURI(name: String) = s"$defaultPrefix/$name"
  def unpackURI(uri: String) = uri.split(defaultPrefix + "/")(1)
}

object TRDF {
  //Reference: http://www.w3.org/TR/rdf-sparql-query/#grammarEscapes
  
  //For ID strings (may be part of URLs)
  val invalidChars = Set(' ', '\t', '\n', '\r', '\b', '\f', '"', '\'')
  
  val replacements = Map(
      '\t' -> "\\\t",
	  '\n' -> "\\\n",
	  '\r' -> "\\\r",
	  '\b' -> "\\\b",
	  '\"' -> "\\\"",
	  '\'' -> "\\'",
	  '\\' -> "\\\\"
	  )	  
  
  def isValidIdentifier(x: String) = !invalidChars.exists(c => x.contains(c))
  def checkValidIdentifier(x: String, typ: String) {
    if (!isValidIdentifier(x)) {
      throw new Exception(s"Invalid $typ: $x (spaces, quotation marks etc. are not allowed)")
    }
  }
  
  def escape(s: String): String = {
    val sb = new StringBuilder
    for (i <- 0 until s.length(); c = s.charAt(i)) {
      if (replacements.keySet.contains(c)) {
        sb.append(replacements(c))
      } else {
        sb.append(c)
      }
    }
    sb.toString
  }
}