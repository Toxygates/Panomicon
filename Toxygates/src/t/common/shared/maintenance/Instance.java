/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.common.shared.maintenance;

import java.util.Date;

import t.common.client.maintenance.AccessPolicy;
import t.common.shared.ManagedItem;

@SuppressWarnings("serial")
public class Instance extends ManagedItem {

  private AccessPolicy policy;
  private String policyParameter;

  public Instance() {}

  public Instance(String id, String comment, Date date) {
    super(id, comment, date);
  }

  public void setAccessPolicy(AccessPolicy p, String parameter) {
    this.policy = p;
    this.policyParameter = parameter;
  }

  public AccessPolicy getAccessPolicy() {
    return policy;
  }

  public String getPolicyParameter() {
    return policyParameter;
  }
}
