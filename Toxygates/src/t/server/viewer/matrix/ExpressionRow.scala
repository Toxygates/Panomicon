package t.server.viewer.matrix

import t.db.{BasicExprValue, ExprValue}

case class ExpressionRow(probe: String,
                         atomicProbes: Array[String],
                         probeTitles: Array[String],
                         geneIds: Array[String],
                         geneSymbols: Array[String],
                         values: Array[BasicExprValue]) {

  var geneIdLabels: Array[String] = Array()
}
