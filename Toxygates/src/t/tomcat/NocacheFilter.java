package t.tomcat;

import java.io.IOException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Nocache filter for Tomcat
 * Packaged in a separate jar. 
 * *nocache.js files are tiny, so not caching them is not a problem.
 * 
 * Source: https://seewah.blogspot.jp/2009/02/gwt-tips-2-nocachejs-getting-cached-in.html
 * Also see 
 * https://stackoverflow.com/questions/3407649/stop-browser-scripts-caching-in-gwt-app
 */
public class NocacheFilter implements Filter {

 public void destroy() {
 }

 public void init(FilterConfig config) throws ServletException {
 }

 public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
   ServletException {

  HttpServletRequest httpRequest = (HttpServletRequest) request;
  String requestURI = httpRequest.getRequestURI();

  if (requestURI.contains(".nocache.")) {
   Date now = new Date();
   HttpServletResponse httpResponse = (HttpServletResponse) response;
   httpResponse.setDateHeader("Date", now.getTime());
   // one day old
   httpResponse.setDateHeader("Expires", now.getTime() - 86400000L);
   httpResponse.setHeader("Pragma", "no-cache");
   httpResponse.setHeader("Cache-control", "no-cache, no-store, must-revalidate");
  }

  filterChain.doFilter(request, response);
 }
}