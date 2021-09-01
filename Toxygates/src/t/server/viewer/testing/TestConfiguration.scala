package t.server.viewer.testing

import t.db.kyotocabinet.chunk.KCChunkMatrixDB
import t.server.viewer.Configuration

/**
 * Generates configurations for testing of servlet implementations.
 * Maps the TestConfig from OTGTool into a Configuration.
 */
object TestConfiguration {
  val tc = t.testing.TestConfig
  val ts = tc.tsConfig
  val data = tc.dataConfig

  lazy val config = new Configuration(ts.repository,
    KCChunkMatrixDB.CHUNK_PREFIX + data.dir, null, null,
    ts.url, ts.updateUrl,
    ts.user, ts.pass,
    null, null,
    data.matrixDbOptions)

}
