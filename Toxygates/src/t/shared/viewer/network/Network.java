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

package t.shared.viewer.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An interaction network.
 *
 */
@SuppressWarnings("serial")
public class Network implements Serializable {

  public static final String mrnaType = "mRNA";
  public static final String mirnaType = "miRNA";

  private List<Interaction> interactions = new ArrayList<Interaction>();
  private List<Node> nodes = new ArrayList<Node>();
  private String title;

  /*
   * Stores the JSON representation of the JavaScript version of this network, so that it doesn't
   * need to be computed more than once.
   */
  private String jsonString = "";
  
  Network() {}
  
  /**
   * Construct a network.
   * @param title
   * @param nodes
   * @param interactions
   * Only defined if wasTruncated is true.
   */
  public Network(String title, List<Node> nodes, List<Interaction> interactions) {
    this(title, nodes, interactions, "");
  }

  public Network(String title,
      List<Node> nodes, List<Interaction> interactions, String jsonString) {
    this.title = title;
    this.nodes = nodes;
    this.interactions = interactions;
    this.jsonString = jsonString;
  }
  
  public List<Interaction> interactionsFrom(Node from) {
    return interactions.stream().filter(i -> i.from.equals(from)).collect(Collectors.toList());
  }
  
  public List<Interaction> interactionsTo(Node to) {
    return interactions.stream().filter(i -> i.to.equals(to)).collect(Collectors.toList());
  }
  
  public List<Interaction> interactionsFrom(String fromId) {
    return interactionsFrom(new Node(fromId, null, null, null));
  }
  
  public List<Interaction> interactionsTo(String toId) {
    return interactionsTo(new Node(toId, null, null, null));
  }
  
  public String title() { return title; }
  
  public List<Node> nodes() { return nodes; }

  public List<Interaction> interactions() { return interactions; }

  public void changeTitle(String newTitle) {
    title = newTitle;
  }
  
  public String jsonString() {
    return jsonString;
  }
  
  public void storeJsonString(String string) {
    jsonString = string;
  }
}
