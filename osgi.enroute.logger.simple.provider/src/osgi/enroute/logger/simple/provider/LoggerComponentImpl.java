package osgi.enroute.logger.simple.provider;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;


@Component(service=Logger.class, servicefactory=true)
public class LoggerComponentImpl extends AbstractLogger {

	@Activate
	void activate(ComponentContext context) {
		setBundle(context.getUsingBundle());
	}

	
	@Deactivate
	void deactivate() {
		close();
	}

}
