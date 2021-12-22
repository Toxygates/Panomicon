/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.intermine;

import org.intermine.client.core.ServiceFactory;

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
