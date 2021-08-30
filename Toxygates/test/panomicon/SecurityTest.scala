package panomicon

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.TTestSuite

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@RunWith(classOf[JUnitRunner])
class SecurityTest extends TTestSuite {

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
