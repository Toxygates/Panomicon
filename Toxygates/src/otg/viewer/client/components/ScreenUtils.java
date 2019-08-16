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

package otg.viewer.client.components;

import otg.viewer.client.SampleDetailScreen;
import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;

public class ScreenUtils {
  public static void displaySampleDetail(OTGScreen screen, Sample sample) {
    Group group = new Group(screen.manager().schema(), "custom", new Sample[] {sample});
    screen.getStorage().customColumnStorage.store(group);
    screen.manager().attemptProceed(SampleDetailScreen.key);
  }
}
