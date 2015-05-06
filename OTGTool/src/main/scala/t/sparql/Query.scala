package t.sparql

/**
 * A query that can be extended gradually and returns results of a particular
 * type.
 * 
 * A model query is e.g.
 * 
 * PREFIX x:<http://x.x.x/x>
 * PREFIX ...
 * 
 * select * { ?x ?y ?z; ?a ?b ... ['pattern, extensible part']
 * } ... ['suffix']
 */
 
case class Query[+T](prefix: String, pattern: String, 
    suffix: String = "\n}", eval: (String) => T = null)  {
	def queryText: String = s"$prefix\n$pattern\n$suffix\n"	
	
	def constrain(constraint: String): Query[T] = copy(pattern = pattern + "\n " + constraint)
  def constrain(filter: t.sparql.Filter): Query[T] = 
    copy(pattern = pattern + filter.queryPattern + "\n " + filter.queryFilter)
  
	/**
	 * Run the query
	 */
	def apply(): T = eval(queryText)
}