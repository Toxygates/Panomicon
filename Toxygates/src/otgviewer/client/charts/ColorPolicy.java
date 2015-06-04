/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package otgviewer.client.charts;
import java.util.Map;

import javax.annotation.Nullable;

import otgviewer.shared.OTGSample;
import t.common.shared.DataSchema;

/**
 * A ColorPolicy is a way of coloring samples in a chart.
 */
class ColorPolicy {

	final String defaultColor = "grey";
	
	@Nullable
	String colorFor(ChartSample sample) {
		return defaultColor;
	}
	
	static class TimeDoseColorPolicy extends ColorPolicy {
		String timeDose;
		String color;
		
		TimeDoseColorPolicy(String timeDose, String color) {
			this.timeDose = timeDose;
			this.color = color;
		}
		
		@Override
		String colorFor(ChartSample sample) {
			DataSchema schema = sample.schema();
			if (schema.getMinor(sample).equals(timeDose) || 
					schema.getMedium(sample).equals(timeDose)) {
				return color;
			}
			return super.colorFor(sample);
		}
	}
	
	static class MapColorPolicy extends ColorPolicy {
		Map<OTGSample, String> colors;
		
		MapColorPolicy(Map<OTGSample, String> colors) {
			this.colors = colors;
		}
		
		@Override
		String colorFor(ChartSample sample) {
			if (colors.containsKey(sample.barcode)) {
				return colors.get(sample.barcode);
			}
			return super.colorFor(sample);
		}
	}
}
