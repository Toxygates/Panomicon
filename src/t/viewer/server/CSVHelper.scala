package t.viewer.server

import otg.ExprValue

object CSVHelper {

  /**
   * This trait can be implemented by classes that wish to write data as
   * CSV files.
   */
  trait CSVFile {
    def write(out: String) {
      println("Rows " + rows + " cols " + columns)

      val outb = new java.io.BufferedWriter(new java.io.FileWriter(out))
      var x = 0
      var y = 0
      while (x < columns && y < rows) {

        outb.write(format(apply(x, y)))
        if (x < columns - 1) {
          outb.write(",")
        }
        x += 1
        if (x == columns) {
          x = 0
          y += 1
          outb.write("\n")
        }
      }
      outb.close()
    }

    def format(obj: Any): String = {
      obj match {
        case Some(x) => {
          x match {
            case d: Double => format(d)
            case _ => ""
          }          
        }
        case None => ""

        case d: Double => "%.3f".format(d)
        case i: Int    => i.toString
        case _         => "\"" + obj.toString + "\""
      }
    }

    def columns: Int
    def rows: Int

    /**
     * Obtain the item at the specified coordinate.
     */
    def apply(x: Int, y: Int): Any

  }

  /**
   * Write expression values to a CSV files.
   * The given probes and geneIds only will be written.
   * The generated url will be returned.
   */
  def writeCSV(dir: String, urlbase: String, 
      probes: Seq[String], titles: Seq[String], 
      geneIds: Seq[String], expr: Seq[Seq[ExprValue]]): String = {

    if (expr.size == 0) {
      throw new Exception("No data supplied")
    }
    
    val file = "otg" + System.currentTimeMillis + ".csv"
    val fullName = dir + "/" + file
    
    new CSVFile {
    	def columns = titles.size + 2
    	def rows = expr.size + 1
      def apply(x: Int, y: Int) = if (y == 0) {
        if (x > 1) {
          titles(x - 2)
        } else if (x == 1) {
          "Gene"
        } else {
          "Probe"
        }
      } else {  //y < 0
        if (x == 0) { 
          probes(y - 1)
        } else if (x == 1) {
          geneIds(y - 1)
        } else {
          expr(y - 1)(x - 2).value
        }
      }
    }.write(fullName)

    urlbase + "/" + file
  }
}