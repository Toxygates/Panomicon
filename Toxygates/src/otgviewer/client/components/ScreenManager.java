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

package otgviewer.client.components;

import otgviewer.client.Resources;
import otgviewer.client.UIFactory;
import t.common.shared.DataSchema;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.rpc.ProbeServiceAsync;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.client.rpc.SeriesServiceAsync;
import otgviewer.client.rpc.SparqlServiceAsync;
import t.viewer.client.rpc.UserDataServiceAsync;
import t.viewer.shared.AppInfo;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ProvidesResize;

/**
 * A screen manager provides screen management services.
 * 
 * @author johan
 *
 */
public interface ScreenManager extends ProvidesResize {

  /**
   * Indicate that the given screen is or is not configured.
   * 
   * @param s
   * @param configured
   */
  void setConfigured(Screen s, boolean configured);

  /**
   * Invalidate all screens' "configured" state and subsequently attempt their reconfiguration.
   * Precondition: all screens must have been displayed at least once using Screen.show()
   */
  void deconfigureAll(Screen from);

  /**
   * Test whether the given screen is configured.
   * 
   * @param key
   * @return
   */
  boolean isConfigured(String key);

  /**
   * Try to proceed to a new screen, displaying it instead of the current one.
   * 
   * @param to
   */
  void attemptProceed(String to);

  /**
   * A string uniquely identifying the user interface we wish to display. TODO: replace with enum
   * 
   * @return
   */
  @Deprecated
  String getUIType();

  DataSchema schema();

  Resources resources();

  AppInfo appInfo();
  
  void reloadAppInfo(final AsyncCallback<AppInfo> handler);

  SparqlServiceAsync sparqlService();
  
  SampleServiceAsync sampleService();
  
  ProbeServiceAsync probeService();

  MatrixServiceAsync matrixService();

  SeriesServiceAsync seriesService();
  
  UserDataServiceAsync userDataService();

  UIFactory factory();
  
  StorageParser getParser();
}
