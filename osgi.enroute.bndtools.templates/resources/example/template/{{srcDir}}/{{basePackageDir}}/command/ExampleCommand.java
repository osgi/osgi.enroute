package {{basePackageName}}.command;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import {{basePackageName}}.api.ExampleApi;
import osgi.enroute.debug.api.Debug;

/**
 * This is the implementation. It registers the _stem_ interface and calls it
 * through a Gogo command.
 *
 */
@Component(service=ExampleCommand.class, property = { Debug.COMMAND_SCOPE + "=example",
		Debug.COMMAND_FUNCTION + "=example" })
public class ExampleCommand {
	@Reference
	private ExampleApi target;

	public void example(String message) {
		target.say(message);
	}
}