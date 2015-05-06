package t.sparql

trait QueryUtils {
 /**
   * Filtering the variable v to be in a given list of values.
   * Note: use with caution! FILTER( IN (...)) often leads to slow queries.
   * Consider using the unions below instead.
   */
  def multiFilter(v: String, values: Iterable[String]): String = {
    if (values.isEmpty) {
      ""
    } else {
      " FILTER(" + v + " IN(\n" + values.grouped(10).map("\t" + _.mkString(",")).
      	mkString(",\n") + "\n)) \n"
    }
  }

  def multiFilter(v: String, values: Option[Iterable[String]]): String = 
    multiFilter(v, values.getOrElse(Iterable.empty))

  def multiUnion(subjObjs: Iterable[(String, String)], pred: String): String = 
    subjObjs.grouped(3).map(xs => xs.map(x => "{ " + x._1 + " " + pred + " " + x._2 + " . } ").
        mkString(" UNION ")).mkString(" \n UNION \n ")
  
  def multiUnionSubj(subjects: Iterable[String], pred: String, obj: String): String =
    multiUnion(subjects.map(s => (s, obj)), pred) 
  
  def multiUnionObj(subject: String, pred: String, objects: Iterable[String]): String = 
    multiUnion(objects.map(o => (subject, o)), pred)    

  def caseInsensitiveMultiFilter(v: String, values: Iterable[String]): String = {
    val m = values.map(" regex(" + v + ", " + _ + ", \"i\") ")
    if (m.size == 0) {
      ""
    } else {
      "FILTER ( " + m.mkString(" || ") + " )"
    }
  }
}

object QueryUtils extends QueryUtils