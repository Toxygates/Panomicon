package t.viewer.server.matrix

import t.db.{BasicExprValue, ExprValue}

case class ExpressionRow(probe: String,
                         atomicProbes: Array[String],
                         probeTitles: Array[String],
                         geneIds: Array[String],
                         geneSymbols: Array[String],
                         values: Array[ExprValue]) {

  var geneIdLabels: Array[String] = Array()
}
