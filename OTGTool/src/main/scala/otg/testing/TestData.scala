package otg.testing

object TestData {
  private def mm(ss: Seq[String]) =
    Map() ++ ss.zipWithIndex

    val enumMaps = Map(
        "compound_name" -> mm(Seq("acetaminophen", "methapyrilene")),
        "dose_level" -> mm(Seq("Low", "Middle", "High", "Really high")),
        "organism" -> mm(Seq("Giraffe", "Squirrel", "Rat", "Mouse", "Human")),
        "exposure_time" -> mm(Seq("3 hr", "6 hr", "9 hr", "24 hr")),
        "sin_rep_type" -> mm(Seq("Single", "Repeat")),
        "organ_id" -> mm(Seq("Liver", "Kidney")),
        "test_type" -> mm(Seq("Vitro", "Vivo"))
        )
}
