package panomicon

import com.inversoft.rest.ClientResponse
import io.fusionauth.client.FusionAuthClient
import io.fusionauth.domain.oauth2.{AccessToken, OAuthError}
import io.fusionauth.jwt.JWTDecoder
import io.fusionauth.jwt.domain.JWT
import io.fusionauth.jwt.rsa.RSAVerifier

import java.security.{MessageDigest, SecureRandom}
import java.util.Base64
import javax.servlet.http.Cookie

class Authentication {
  val fusionAuthBaseUrl = System.getenv("FUSIONAUTH_BASEURL")
  val fusionAuthClientId = System.getenv("FUSIONAUTH_CLIENTID")
  val fusionAuthClientSecret = System.getenv("FUSIONAUTH_CLIENTSECRET")
  val redirectAfterAuthUrl = System.getenv("REDIRECT_AFTER_AUTH_URL")
  val redirectAfterRegistrationUrl = System.getenv("REDIRECT_AFTER_REGISTRATION_URL")
  val jwtIssuer = System.getenv("JWT_ISSUER")

  val logoutUrl = s"http://localhost:9011/oauth2/logout?client_id=$fusionAuthClientId"

  val fusionAuthClient = new FusionAuthClient("noapikeyneeded", fusionAuthBaseUrl);
  val random = new SecureRandom()

  def loginRedirectUri(challenge: String): String =
    s"$fusionAuthBaseUrl/oauth2/authorize?client_id=$fusionAuthClientId" +
      s"&response_type=code&redirect_uri=$redirectAfterAuthUrl" +
      s"&scope=openid offline_access&code_challenge=$challenge&code_challenge_method=S256"

  def registrationRedirectUri(challenge: String): String =
    s"$fusionAuthBaseUrl/oauth2/register?client_id=$fusionAuthClientId" +
      s"&response_type=code&redirect_uri=$redirectAfterAuthUrl" +
      s"&scope=openid offline_access&code_challenge=$challenge&code_challenge_method=S256"

  def generatePKCEVerifier(): String =  {
    val codeVerifier = new Array[Byte](32);
    random.nextBytes(codeVerifier)
    Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier)
  }

  def generatePKCEChallenge(codeVerifier: String): String = {
    val bytes = codeVerifier.getBytes("US-ASCII");
    val messageDigest = MessageDigest.getInstance("SHA-256");
    messageDigest.update(bytes, 0, bytes.length);
    val digest = messageDigest.digest();
    Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  def generatePKCEPair(): (String, String) = {
    val verifier = generatePKCEVerifier()
    val challenge = generatePKCEChallenge(verifier)
    (verifier, challenge)
  }

  def exchangeOAuthCodeForAccessToken(authorizationCode: String,
                                      verifier: String): ClientResponse[AccessToken, OAuthError] = {
    fusionAuthClient.exchangeOAuthCodeForAccessTokenUsingPKCE(authorizationCode,
      fusionAuthClientId, fusionAuthClientSecret, redirectAfterAuthUrl, verifier)
  }

  def getJwtToken(cookies: Array[Cookie]): Either[(JWT, String), String] = {
    try {
      val tokenCookie = cookies.find(c => c.getName == "__Host-jwt").get

      val jwt = new JWTDecoder().decode(tokenCookie.getValue,
        RSAVerifier.newVerifier(System.getenv("RSA_PUBLIC_KEY"))
      )

      if (jwt.audience != fusionAuthClientId) {
        Right("Wrong token audience")
      } else if (jwt.issuer != jwtIssuer) {
        Right("Wrong token issuer")
      } else if (jwt.isExpired()) {
        val refreshToken = cookies.find(c => c.getName == "__Host-refreshToken").get.getValue
        val refreshResponse =
          fusionAuthClient.exchangeRefreshTokenForAccessToken(refreshToken,
            fusionAuthClientId, fusionAuthClientSecret, "", "")
        if (refreshResponse.wasSuccessful()) {
          val newToken = refreshResponse.successResponse.token
          Left(new JWTDecoder().decode(newToken,
            RSAVerifier.newVerifier(System.getenv("RSA_PUBLIC_KEY"))
          ), newToken)
        } else {
          Right(s"Error getting refresh token: ${refreshResponse.exception.toString()}")
        }
      } else {
        Left(jwt, tokenCookie.getValue)
      }
    } catch {
      case e: Throwable => {
        Right(s"Error getting token: ${e.toString}")
      }
    }
  }

  def cookieHeader(token: String) =
    s"__Host-jwt=${token}; Secure; Path=/; HttpOnly; SameSite=Strict"
}
