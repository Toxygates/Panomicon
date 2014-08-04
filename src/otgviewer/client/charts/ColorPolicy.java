package otgviewer.client.charts;
import java.util.Map;

import javax.annotation.Nullable;

import otgviewer.client.charts.ChartDataSource.ChartSample;
import otgviewer.shared.OTGSample;

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
			if (sample.minor.equals(timeDose) || sample.medium.equals(timeDose)) {
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
