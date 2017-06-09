package t.viewer.testing

import t.viewer.server.Configuration

/**
 * Generates configurations for testing of servlet implementations.
 * Maps the TestConfig from OTGTool into a Configuration.
 */
object TestConfiguration {
  val tc = t.testing.TestConfig
  val ts = tc.tsConfig
  val data = tc.dataConfig

  lazy val config = new Configuration(ts.repository,
      data.dir, null, null,
      ts.url, ts.updateUrl,
      ts.user, ts.pass,
      null, null,
      data.matrixDbOptions)

}
