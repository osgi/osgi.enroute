package {{basePackageName}}.command;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.osgi.service.log.LogService;
import osgi.enroute.debug.api.Debug;

@Component(service=ExampleCommand.class, property = { Debug.COMMAND_SCOPE + "=example",
		Debug.COMMAND_FUNCTION + "=example" })
public class ExampleCommand {
	@Reference
	private LogService log;

	public void example(String message) {
		log.log(LogService.LOG_INFO, message);
	}
}