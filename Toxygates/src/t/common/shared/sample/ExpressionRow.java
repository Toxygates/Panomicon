/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.shared.sample;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Expression data for a particular set of columns for a single probe. May also contain associated
 * information such as gene IDs and gene symbols.
 * 
 * TODO this class is due for an overhaul.
 */
@SuppressWarnings("serial")
public class ExpressionRow implements Comparable<ExpressionRow>, Serializable {
  private String probe = "";
  private String[] atomicProbeTitles = new String[0];
  private String[] atomicProbes = new String[0];
  private String[] geneIds = new String[0];
  private String[] geneIdLabels = new String[0];
  private String[] geneSyms = new String[0];
  private ExpressionValue[] val = new ExpressionValue[0];

  public ExpressionRow() {}

  /**
   * Single probe constructor
   * 
   * @param _probe
   * @param _title
   * @param _geneId
   * @param _geneSym
   * @param _val
   */
  public ExpressionRow(String _probe, String _title, String[] _geneId, String[] _geneSym,
      ExpressionValue[] _val) {
    this(_probe, new String[] {_probe}, new String[] {_title}, _geneId, _geneSym, _val);
  }

  /**
   * Merged probe constructor
   * 
   * @param _probe
   * @param _atomicProbes
   * @param _title
   * @param _geneId
   * @param _geneSym
   * @param _val
   */
  public ExpressionRow(String _probe, String[] _atomicProbes, String[] _titles, String[] _geneId,
      String[] _geneSym, ExpressionValue[] _val) {
    probe = _probe;
    atomicProbes = _atomicProbes;
    val = _val;
    atomicProbeTitles = _titles;
    geneIds = _geneId;
    geneIdLabels = _geneId;
    geneSyms = _geneSym;
  }

  public boolean equals(Object o) {
    if (o instanceof ExpressionRow) {
      return (probe == ((ExpressionRow) o).probe && Arrays.equals(val, ((ExpressionRow) o).val));
    }
    return false;
  }

  public String getProbe() {
    return probe;
  }

  public String[] getAtomicProbes() {
    return atomicProbes;
  }

  public ExpressionValue getValue(int i) {
    if (i < val.length) {
      return val[i];
    } else {
      return emptyValue();
    }
  }

  private ExpressionValue emptyValue() {
    return new ExpressionValue(0, 'A');
  }

  public ExpressionValue[] getValues() {
    return val;
  }

  /**
   * Obtain the number of data columns contained.
   * 
   * @return
   */
  public int getColumns() {
    return val.length;
  }

  public String[] getAtomicProbeTitles() {
    return atomicProbeTitles;
  }

  /**
   * Entrez gene IDs. URLs are constructed on the basis of this.
   * 
   * @return
   */
  public String[] getGeneIds() {
    return geneIds;
  }

  /**
   * Labels for the gene IDs above. Should normally be the same as the IDs themselves, but in the
   * case of orthologous matrices we need to do things such as Human:5351 (1 probe) for gene 5351.
   * This is the only use case currently, so this method may be retired in the future.
   * 
   * @return
   */
  public String[] getGeneIdLabels() {
    return geneIdLabels;
  }

  public void setGeneIdLabels(String[] geneIdLabels) {
    this.geneIdLabels = geneIdLabels;
  }

  public String[] getGeneSyms() {
    return geneSyms;
  }

  public int compareTo(ExpressionRow o) {
    if (o == null) {
      return -1;
    } else {
      return probe.compareTo(o.probe);
    }
  }
}
