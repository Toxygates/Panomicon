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

package otgviewer.client.components;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ProvidesResize;

import otgviewer.client.Resources;
import otgviewer.client.UIFactory;
import otgviewer.client.rpc.ProbeServiceAsync;
import otgviewer.client.rpc.SampleServiceAsync;
import t.common.shared.DataSchema;
import t.viewer.client.StorageParser;
import t.viewer.client.rpc.*;
import t.viewer.shared.AppInfo;

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
  void reconfigureAll(String fromToken);

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

  DataSchema schema();

  Resources resources();

  AppInfo appInfo();
  
  void reloadAppInfo(final AsyncCallback<AppInfo> handler);

  SampleServiceAsync sampleService();
  
  ProbeServiceAsync probeService();

  MatrixServiceAsync matrixService();

  SeriesServiceAsync seriesService();
  
  UserDataServiceAsync userDataService();
  
  NetworkServiceAsync networkService();

  UIFactory factory();
  
  StorageParser getParser();
}
