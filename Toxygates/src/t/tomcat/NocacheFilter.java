/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

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
