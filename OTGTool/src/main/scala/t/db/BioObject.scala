package t.db

/**
 * A BioObject is some biological entity that can be uniquely identified
 * by a string. It can also have a name, which by default is the same
 * as the identifier.
 */
trait GenBioObject {
  def identifier: String
  def name: String = identifier
  override def hashCode = identifier.hashCode
}

/**
 * The default implementation of a BioObject.
 */
case class DefaultBio(identifier: String, override val name: String = "") extends GenBioObject

trait BioObject[T <: BioObject[T]] extends GenBioObject {
  this: T =>
  def getAttributes(implicit store: Store[T]) = store.withAttributes(List(this)).head
}

/**
 * A Store is a way of looking up additional information about some type of BioObject.
 */
trait Store[T <: BioObject[T]] {
  
  /**
   * For the given BioObjects of type T, assuming that only the identifier is available,
   * look up all available information and return new copies with all information filled in. 
   */
  def withAttributes(objs: Iterable[T]): Iterable[T] = objs
  
  /**
   * Look up all available information for a single bioObject (where only the identifier
   * needs to be available) 
   */
  def withAttributes(obj: T): T = withAttributes(List(obj)).head
}

