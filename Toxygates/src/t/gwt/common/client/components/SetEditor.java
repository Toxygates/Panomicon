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

package t.gwt.common.client.components;

import java.util.*;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

/**
 * A SetEditor tracks a selection and makes available a method for modifying it.
 * 
 */
public interface SetEditor<T> {

  void setSelection(Collection<T> items);

  void setSelection(Collection<T> items, @Nullable SetEditor<T> fromSelector);

  Set<T> getSelection();

  List<T> availableItems();

  Set<T> validateItems(List<T> items);

  List<Suggestion> getSuggestions(String request);

}
