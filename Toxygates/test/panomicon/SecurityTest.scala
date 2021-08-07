package panomicon

import org.junit.runner.RunWith
import org.scalatest.OptionValues._
import org.scalatest.TryValues._
import org.scalatest.junit.JUnitRunner
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtUpickle}
import t.TTestSuite
import upickle.default._

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
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

  test("Should be able to get same hashed password after converting salt to and from Base64") {
    val password = "bad_password"

    val random = new SecureRandom()
    val originalSalt = new Array[Byte](16)
    random.nextBytes(originalSalt)

    val spec1 = new PBEKeySpec(password.toCharArray(), originalSalt, 65536, 128)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

    val hash1 = factory.generateSecret(spec1).getEncoded();

    val base64Salt = Base64.getEncoder.encodeToString(originalSalt);
    println("Salt:")
    println(base64Salt)

    val decodedSalt = Base64.getDecoder.decode(base64Salt)
    val spec2 = new PBEKeySpec(password.toCharArray(), decodedSalt, 65536, 128)
    val hash2 = factory.generateSecret(spec2).getEncoded()

    println("Hashed password:")
    println(Base64.getEncoder.encodeToString(hash2))

    hash1 should equal(hash2)
  }
}
