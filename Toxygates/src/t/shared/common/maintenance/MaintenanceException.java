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

package t.shared.common.maintenance;

@SuppressWarnings("serial")
public class MaintenanceException extends Exception {

  public MaintenanceException() {}

  public MaintenanceException(String message) {
    super(message);
    printStackTrace();
  }

  public MaintenanceException(Throwable cause) {
    super(cause);
    printStackTrace();
  }

  public MaintenanceException(String message, Throwable cause) {
    super(message, cause);
    printStackTrace();
  }

}
