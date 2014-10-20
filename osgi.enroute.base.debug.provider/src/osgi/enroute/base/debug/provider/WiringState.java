package osgi.enroute.base.debug.provider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevisions;

/**
 * Analyzes the framework for any anomalies
 */
public class WiringState {
	
	final BundleContext	context;
	boolean ok;
	
	public WiringState(BundleContext context) {
		this.context = context;
	}

	public boolean isOk() {
		verify();
		return ok;
	}

	void verify() {
		for ( Bundle b : context.getBundles()) {
			BundleRevisions revisions = b.adapt(BundleRevisions.class);
			if ( revisions.getRevisions().size() != 1 ) {
				System.out.println("Needs refresh " + b);
			}
		}
	}

}
