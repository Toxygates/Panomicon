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

package t.common.shared.maintenance;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Progress implements Serializable {

  public Progress() {}

  public Progress(String task, int percentage, boolean allFinished) {
    this.task = task;
    this.percentage = percentage;
    this.allFinished = allFinished;
  }

  private String task;
  private int percentage;
  private String[] messages = new String[0];
  private boolean allFinished;

  public void setMessages(String[] messages) {
    this.messages = messages;
  }

  public String getTask() {
    return task;
  }

  public int getPercentage() {
    return percentage;
  }

  public String[] getMessages() {
    return messages;
  }

  /**
   * Is every task finished?
   * 
   * @return
   */
  public boolean isAllFinished() {
    return allFinished;
  }
}
