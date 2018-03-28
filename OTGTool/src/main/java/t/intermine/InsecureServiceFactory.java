package t.intermine;

import org.intermine.webservice.client.core.ServiceFactory;

/**
 * This class exists purely to suppress the deprecation warning for the insecure
 * username/password-based ServiceFactory constructor below, since we can't do so in Scala. To be
 * removed once we switch to auth tokens.
 */
public final class InsecureServiceFactory {
  @SuppressWarnings("deprecation")
  public static ServiceFactory fromUserAndPass(String rootUrl, String userName,
      String userPass) {
    return new ServiceFactory(rootUrl, userName, userPass);
  }
}
