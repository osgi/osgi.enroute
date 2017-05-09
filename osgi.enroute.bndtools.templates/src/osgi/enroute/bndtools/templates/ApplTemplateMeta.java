package osgi.enroute.bndtools.templates;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "osgi.enroute.bndtools.templates.application", name = ApplTemplateMeta.NAME, description = ApplTemplateMeta.NAME)
public @interface ApplTemplateMeta {

	public static final String NAME = "enRoute Application Project Template";

	@AttributeDefinition(name = "Application Primary Name", description = "A simple name for the primary class that holds the application together.")
	String primaryName() default "Example";
}
