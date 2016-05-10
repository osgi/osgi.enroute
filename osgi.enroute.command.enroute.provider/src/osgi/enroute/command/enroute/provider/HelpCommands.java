package osgi.enroute.command.enroute.provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.lib.collections.MultiMap;
import aQute.lib.justif.Justif;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.dto.api.TypeReference;

@Descriptor("Provides help for Gogo commands")
@Component(name = "osgi.enroute.command.help", property = { Debug.COMMAND_SCOPE + "=help",
		Debug.COMMAND_FUNCTION + "=help", Debug.COMMAND_FUNCTION + "=scope" })
public class HelpCommands implements Converter {

	@Reference
	DTOs dtos;

	BundleContext						context		= FrameworkUtil.getBundle(OSGiCommands.class).getBundleContext();
	static TypeReference<Set<String>>	STRING_SET	= new TypeReference<Set<String>>() {
	};

	class HelpScope {
		String								name;
		String								description;
		Map<String, HelpCommand>			commands	= new HashMap<>();
		public List<ServiceReference<?>>	references	= new ArrayList<>();
	}

	class HelpCommand {
		String						scope;
		String						name;
		List<HelpCommandPrototype>	prototypes	= new ArrayList<>();;
	}

	class HelpCommandPrototype {
		String				description;
		List<HelpOption>	options		= new ArrayList<>();
		List<String>		arguments	= new ArrayList<>();
	}

	class HelpOption {
		String[]	names;
		boolean		flag;
		String		deflt;
		String		type;
		String		description;
	}

	@Descriptor("Show all scopes and their distinct command names")
	public Collection<HelpScope> help() throws Exception {
		return getHelp("*").values();
	}

	@Descriptor("Inspect a scope")
	public HelpScope scope(@Descriptor("scope")String scope) throws Exception {
		Map<String, HelpScope> help = getHelp(scope);
		if (help.containsKey(scope))
			return help.get(scope);
		return null;
	}

	@Descriptor("Show the help for a specific function. You can specify the name of the function and optionally prefix it with the scope.")
	public Object help(@Descriptor("[scope:]function") String function) throws Exception {
		String scope = "*";
		if (function.indexOf(':') > 0) {
			String parts[] = function.split(":");
			scope = parts[0];
			function = parts[1];
		}

		Map<String, HelpScope> scopes = getHelp(scope);
		if (scopes.containsKey(scope)) {
			HelpScope s = scopes.get(scope);
			return s.commands.get(function);
		}

		// wildcard scope
		Glob functionWildcard = new Glob(function);

		List<HelpCommand> commands = scopes.values().stream() //
				.flatMap((HelpScope s) -> s.commands.values().stream()) //
				.filter((HelpCommand f) -> functionWildcard.matcher(f.name).matches())//
				.collect(Collectors.toList());

		switch (commands.size()) {
		case 0:
			return scope(function);
		case 1:
			return commands.get(0);
		default:
			return commands.stream().map(f -> f.scope + ":" + f.name).collect(Collectors.toList());
		}
	}

	private Map<String, HelpScope> getHelp(String scope) throws Exception {
		ServiceReference<?>[] scopes = context.getServiceReferences((String) null,
				"(" + Debug.COMMAND_SCOPE + "=" + scope + ")");

		if (scopes == null)
			scopes = new ServiceReference[0];

		Map<String, HelpScope> map = new HashMap<>();
		Stream.of(scopes).map(this::toHelpScope).forEach((s) -> {
			HelpScope prev = map.get(s.name);
			if (prev == null)
				map.put(s.name, s);
			else
				merge(prev, s);
		});
		return map;
	}

	private void merge(HelpScope prev, HelpScope next) {
		prev.description = mergeDescription(prev.description, next.description);

		for (Entry<String, HelpCommand> e : next.commands.entrySet()) {
			HelpCommand hc = prev.commands.get(e.getKey());
			if (hc == null)
				prev.commands.put(e.getKey(), e.getValue());
			else
				merge(hc, e.getValue());
		}
	}

	private String mergeDescription(String a, String b) {
		if (a != null && b != null)
			return a + "\n" + b;

		if (a != null)
			return a;
		else
			return b;
	}

	private void merge(HelpCommand a, HelpCommand b) {
		a.prototypes.addAll(b.prototypes);
	}

	private HelpScope toHelpScope(ServiceReference<?> ref) {
		Object service = context.getService(ref);
		if (service == null)
			return null;

		try {
			HelpScope scope = new HelpScope();
			scope.references.add(ref);
			scope.name = (String) ref.getProperty(Debug.COMMAND_SCOPE);
			Descriptor descriptor = service.getClass().getAnnotation(Descriptor.class);
			if (descriptor != null)
				scope.description = descriptor.value();

			Set<String> names = dtos.convert(ref.getProperty(Debug.COMMAND_FUNCTION)).to(STRING_SET);

			MultiMap<String, Method> allMethods = getMethods(service.getClass());

			for (String name : names) {
				HelpCommand command = new HelpCommand();
				command.name = name;
				command.scope = scope.name;

				List<Method> methods = allMethods.get(name);
				if (methods == null || methods.isEmpty())
					continue;

				for (Method method : methods)
					command.prototypes.add(getPrototype(method));

				scope.commands.put(name, command);
			}
			return scope;

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			context.ungetService(ref);
		}
	}

	private MultiMap<String, Method> getMethods(Class<?> c) {
		MultiMap<String, Method> methods = new MultiMap<>();

		for (Method m : c.getMethods()) {
			methods.add(m.getName(), m);
		}
		return methods;
	}

	private HelpCommandPrototype getPrototype(Method m) {
		HelpCommandPrototype help = new HelpCommandPrototype();

		Descriptor description = m.getAnnotation(Descriptor.class);
		if (description != null)
			help.description = description.value();

		Annotation[][] annotations = m.getParameterAnnotations();
		Type[] arguments = m.getGenericParameterTypes();
		for (int i = 0; i < arguments.length; i++) {
			Annotation[] argAnns = annotations[i];
			Type type = arguments[i];
			if (type == CommandSession.class)
				continue;

			Optional<Descriptor> descr = find(argAnns, Descriptor.class);
			HelpOption helpOption = helpOption(type, descr, find(argAnns, Parameter.class));
			if (helpOption != null) {
				help.options.add(helpOption);
			} else {
				String varargs = "";
				if (isVarargs(m, arguments, i, type)) {
					varargs = "…";
					type = String.class;
				}

				String name;
				if (descr.isPresent())
					name = "<"+descr.get().value()+">"+varargs ;
				else if (type == Function.class)
					name = "{}" + varargs;
				else if (type == Object.class)
					name = "any"+varargs;
				else if (type == String.class)
					name = "_"+varargs;
				else
					name = "<"+toShortName(type)+">"+varargs ;

				
				help.arguments.add(name);
			}
		}
		return help;
	}

	private boolean isVarargs(Method m, Type[] arguments, int i, Type type) {
		return i == arguments.length - 1 && (m.isVarArgs() || (type instanceof Class && ((Class<?>) type).isArray()));
	}

	private HelpOption helpOption(Type type, Optional<Descriptor> description, Optional<Parameter> parameter) {
		if (!parameter.isPresent())
			return null;

		HelpOption option = new HelpOption();
		option.names = parameter.get().names();
		option.flag = !parameter.get().presentValue().equals(Parameter.UNSPECIFIED);
		option.deflt = parameter.get().absentValue();
		option.type = toShortName(type);
		if (description.isPresent())
			option.description = description.get().value();

		return option;
	}

	private <T extends Annotation> Optional<T> find(Annotation[] annotations, Class<T> annotationType) {
		Optional<T> findFirst = Stream.of(annotations)//
				.filter(a -> annotationType.isInstance(a))//
				.map(a -> annotationType.cast(a)).findFirst();
		return findFirst;
	}

	String toCommands(ServiceReference<?> sr) {
		try {
			StringBuilder sb = new StringBuilder((String) sr.getProperty(Debug.COMMAND_SCOPE));
			String del = " : ";
			ArrayList<?> l = dtos.convert(sr.getProperty(Debug.COMMAND_FUNCTION)).to(ArrayList.class);
			for (Object o : l) {
				sb.append(del).append(o);
				del = ", ";
			}
			return sb.toString();
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	private String toShortName(Type type) {
		if (type instanceof Class) {
			Class<?> c = (Class<?>) type;
			if (c.isArray())
				return toShortName(c.getComponentType()) + "[]";
			toShortName(c.getName());
		}

		return toShortName(type.toString());
	}

	private String toShortName(String oc) {
		int n = oc.lastIndexOf('.');
		return oc.substring(n + 1);
	}

	
	@Override
	public Object convert(Class<?> arg0, Object arg1) throws Exception {
		return null;
	}

	@Override
	public CharSequence format(Object help, int type, Converter arg2) throws Exception {
		Justif justif = new Justif(80, 10, 40, 42, 44, 48, 50, 52);
		Formatter f = justif.formatter();
		try {
			if (help instanceof HelpScope) {
				switch (type) {
				case Converter.INSPECT:
					inspect(f, (HelpScope) help);
					break;

				case Converter.LINE:
					line(f, (HelpScope) help);
					break;

				case Converter.PART:
					part(f, (HelpScope) help);
					break;
				}
				return justif.wrap();
			} else if (help instanceof HelpCommand) {
				switch (type) {
				case Converter.INSPECT:
					inspect(f, (HelpCommand) help);
					break;
				case Converter.LINE:
					line(f, (HelpCommand) help);
					break;

				case Converter.PART:
					part(f, (HelpCommand) help);
					break;
				}
				return justif.wrap();
			} else
				return null;
		} finally {
			f.close();
		}
	}

	private void part(Formatter f, HelpScope help) {
		f.format("%s", help.name);
	}

	private void line(Formatter f, HelpScope help) {
		String s = Strings.join(", ",help.commands.keySet());
		f.format("%s\t1: \t2%s", help.name, s);
	}

	private void inspect(Formatter f, HelpScope help) {
		f.format("SCOPE\n");
		f.format("    %s\n\n", help.name);
		if (help.description != null) {
			f.format("DESCRIPTION\n");
			f.format("    %s\n\n", help.description);
		}
		f.format("COMMANDS\n");
		for (HelpCommand cmd : help.commands.values()) {
			line(f, cmd);
		}

	}

	private void part(Formatter f, HelpCommand help) {
		f.format("%s", help.name);
	}

	private void line(Formatter f, HelpCommand help) {
		StringBuilder sb = new StringBuilder();
		help.prototypes.stream()//
				.flatMap((HelpCommandPrototype p) -> p.options.stream()) //
				.flatMap((HelpOption o) -> Stream.of(o.names)) //
				.filter(s -> s.matches("-[^-]")) //
				.forEach(s -> sb.append(s.charAt(1)));

		f.format( "%s\t2-\t3", help.name);
		int n = 1;
		for ( HelpCommandPrototype hcp : help.prototypes) {
			f.format("%s. %s\f",n,hcp.description);
			n++;
		}
		f.format("\n");
	}


	private void inspect(Formatter f, HelpCommand help) {
		f.format("NAME\n");
		f.format("    [%s:]%s\n\n", help.scope, help.name);

		f.format("PROTOTYPES\n");
		for (HelpCommandPrototype p : help.prototypes) {
			f.format("   %s ", help.name);
			line( f, p);			
		}
	}

	private void line(Formatter f, HelpCommandPrototype p) {
		String del = "";
		if ( !p.options.isEmpty()) {
			f.format(" [options] ");
		}
		del = args(f, p, del);		
		if ( p.description != null)
			f.format("\t2-\t3%s", p.description);
		
		f.format("\n");

		for( HelpOption option : p.options) {
			if ( option.flag) {
				f.format("            %s[%s]", del,Strings.join(",", option.names));
			} else {
				f.format("            %s[%s (%s)]", del,Strings.join(",", option.names), option.deflt);
			}
			del = " ";
			f.format("\t2-\t3%s\n", option.description == null ? "" : option.description);
		}
	}

	private String args(Formatter f, HelpCommandPrototype p, String del) {
		for( String arg : p.arguments) {
			f.format("%s%s", del, arg);
			del =" ";
		}
		return del;
	}
}
