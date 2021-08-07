package panomicon

import org.junit.runner.RunWith
import org.scalatest.OptionValues._
import org.scalatest.TryValues._
import org.scalatest.junit.JUnitRunner
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtUpickle}
import t.TTestSuite
import upickle.default._

import java.time.Instant
import scala.util.Failure

@RunWith(classOf[JUnitRunner])
class SecurityTest extends TTestSuite {

  test("JWT should authenticate properly encrypted token") {
    val claim = JwtClaim(
      issuer = Some("Panomicon"),
      expiration = Some(Instant.now.plusSeconds(60 * 15).getEpochSecond),
      subject = Some("diogenes")
    )
    val key = "secretKey"
    val algo = JwtAlgorithm.HS256
    val token = JwtUpickle.encode(claim, key, algo)
    val decoded = JwtUpickle.decode(token, key, Seq(JwtAlgorithm.HS256))
    println(JwtUpickle.decode(token, key, Seq(JwtAlgorithm.HS256)))
    decoded.success.value.issuer shouldBe Some("Panomicon")
    decoded.success.value.subject shouldBe Some("diogenes")
    assert(decoded.success.value.expiration.value > Instant.now.plusSeconds(60 * 10).getEpochSecond)
  }

  test("JWT shouldn't authenticate unencrypted token") {
    val claimJson = read[ujson.Value](s"""{"expires":${Instant.now.plusSeconds(60 * 15).getEpochSecond}}""")
    val header = read[ujson.Value]( """{"typ":"JWT","alg":"none"}""")
    val token = JwtUpickle.encode(header, claimJson)
    val decode = JwtUpickle.decodeJson(token, "key", Seq(JwtAlgorithm.HS256))
    decode shouldBe a [Failure[ujson.Value]]
  }
}
