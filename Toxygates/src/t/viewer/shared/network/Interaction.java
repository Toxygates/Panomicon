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

package t.viewer.shared.network;

import java.io.Serializable;
import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class Interaction implements Serializable {
  //GWT constructor
  Interaction() {}
  
  String label;
  Double weight;
  Node from, to;
  
  public Interaction(Node from, Node to, @Nullable String label, @Nullable Double weight) {
    this.label = label;
    this.weight = weight;
    this.from = from;
    this.to = to;
  }
  
  public Node from() { return from; }
  public Node to() { return to; }
  public String label() { return label; }
  public Double weight() { return weight; }
}
