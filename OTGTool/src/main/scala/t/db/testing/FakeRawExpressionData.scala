package t.db.testing

import t.db.Sample
import t.db.RawExpressionData

/**
 * In-memory raw data for testing.
 */
class FakeRawExpressionData(val data: Map[Sample, Map[String, (Double, Char, Double)]])
	extends RawExpressionData