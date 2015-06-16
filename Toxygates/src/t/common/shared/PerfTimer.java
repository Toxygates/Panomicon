package t.common.shared;

import java.util.logging.Logger;


/**
 * For performance measurements
 */
public class PerfTimer {

	private final long start = System.currentTimeMillis();
	private long last = start;
	private final Logger logger;	
	
	public PerfTimer(Logger logger) {
		this.logger = logger;		
	}
	
	public void mark(String title) {
		long l = last;
		last = System.currentTimeMillis();
		long elapsed = last - l;

		logger.info(title + " in " + elapsed + " ms");
	}
	
	public void finish() {
		last = System.currentTimeMillis();
		long elapsed = last - start;
		logger.info("finished in " + elapsed + " ms");
	}

}
