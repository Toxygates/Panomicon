/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.shared;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class Pair<T, U> implements Serializable {

  public Pair() {}

  public Pair(T t, U u) {
    this._t = t;
    this._u = u;
  }

  protected T _t;

  public T first() {
    return _t;
  }

  protected U _u;

  public U second() {
    return _u;
  }

  @Override
  public String toString() {
    return "(" + (_t == null ? "null" : _t.toString()) + ", "
        + (_u == null ? "null" : _u.toString()) + ")";
  }

  public int hashCode() {
    return _t.hashCode() * 41 + _u.hashCode();
  }

  public boolean equals(Object other) {
    if (other instanceof Pair<?, ?>) {
      Pair<?, ?> o = (Pair<?, ?>) other;
      return (o.first().equals(_t) && o.second().equals(_u));
    }
    return false;
  }

  public static <A, B> List<A> mapFirst(Collection<Pair<A, B>> pairs) {
    List<A> r = new ArrayList<A>();
    for (Pair<A, B> p : pairs) {
      r.add(p.first());
    }
    return r;
  }

  public static <A, B> List<B> mapSecond(Collection<Pair<A, B>> pairs) {
    List<B> r = new ArrayList<B>();
    for (Pair<A, B> p : pairs) {
      r.add(p.second());
    }
    return r;
  }

  public static <A> List<Pair<A, A>> duplicate(Collection<A> items) {
    List<Pair<A, A>> r = new ArrayList<Pair<A, A>>();
    for (A a : items) {
      r.add(new Pair<A, A>(a, a));
    }
    return r;
  }

}
