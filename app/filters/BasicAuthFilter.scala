package filters

import play.api.Logger
import sun.misc.BASE64Decoder
import play.api.mvc._
import scala.concurrent.Future

object BasicAuthFilter extends Filter {
  private lazy val unauthResult = Results.Unauthorized.withHeaders(("WWW-Authenticate", "Basic realm=\"myRealm\""))
  private lazy val passwordRequired = true
  private lazy val username = "someUsername"
  private lazy val password = "somePassword"
  private lazy val protectedUriSubstring = "admin"
  private lazy val basicSt = "basic "

  private def getUserIPAddress(request: RequestHeader): String = {
    request.headers.get("x-forwarded-for").getOrElse(request.remoteAddress.toString)
  }

  private def logFailedAttempt(requestHeader: RequestHeader) = {
    Logger.warn(s"IP address ${getUserIPAddress(requestHeader)} failed to log in, requested uri: ${requestHeader.uri}")
  }

  private def decodeBasicAuth(auth: String): Option[(String, String)] = {
    if (auth.length() < basicSt.length()) {
      return None
    }

    val basicReqSt = auth.substring(0, basicSt.length())

    if (basicReqSt.toLowerCase != basicSt) {
      return None
    }

    val basicAuthSt = auth.replaceFirst(basicReqSt, "")
    val decoder = new BASE64Decoder()
    val decodedAuthSt = new String(decoder.decodeBuffer(basicAuthSt), "UTF-8")
    val usernamePassword = decodedAuthSt.split(":")

    if (usernamePassword.length >= 2) {
      //account for ":" in passwords
      return Some(usernamePassword(0), usernamePassword.splitAt(1)._2.mkString)
    }
    None
  }

  private def isOutsideSecurityRealm(requestHeader: RequestHeader): Boolean = {
    !(requestHeader.uri contains protectedUriSubstring)
  }

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if (!passwordRequired || isOutsideSecurityRealm(requestHeader)) {
      return nextFilter(requestHeader)
    }

    requestHeader.headers.get("authorization").map { basicAuth =>
      decodeBasicAuth(basicAuth) match {
        case Some((user, pass)) => {
          if (username == user && password == pass) {
            return nextFilter(requestHeader)
          }
        }
        case _ => ;
      }

      logFailedAttempt(requestHeader)
      return Future.successful(unauthResult)
    }.getOrElse({
      logFailedAttempt(requestHeader)
      Future.successful(unauthResult)
    })
  }
}
