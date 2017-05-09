package osgi.enroute.bndtools.templates;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "osgi.enroute.bndtools.templates.api", name = Api.NAME, description = Api.NAME)
public @interface Api {

	public static final String NAME = "enRoute API Project Template";

	@AttributeDefinition(name = "Service Name", description = "The simple name of the primary service")
	String service() default "ExampleService";
}
