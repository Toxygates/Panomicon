package t

import t.sparql.Samples
import t.sparql.Probes
import t.db.MatrixContext

/**
 * Top level configuration object for a T framework
 * application
 */
class Context(val config: BaseConfig, 
    val factory: Factory, 
    val probes: Probes, 
    val samples: Samples,
    val matrix: MatrixContext) { 
}