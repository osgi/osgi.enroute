package osgi.enroute.gogo.shell.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.shell.Builtin;
import org.apache.felix.gogo.shell.Converters;
import org.apache.felix.gogo.shell.Posix;
import org.apache.felix.gogo.shell.Procedural;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jline.Terminal;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;

@Designate(ocd = Shell.Config.class)
@Component
public class Shell {

	private static final String PROMPT = "prompt";

	@ObjectClassDefinition
	@interface Config {
		String motd() default "";

		String prompt() default "G! ";
	}

	private final Set<ServiceRegistration<?>> registrations = new HashSet<>();

	private Thread			thread;
	private BundleContext	context;
	private FileHistory		history;

	@Reference
	private CommandProcessor cmdProc;


	private static final class CommandName {
		private final String	scope;
		private final String	func;

		private CommandName(String scope, String func) {
			this.scope = scope;
			this.func = func;
		}

		public String getFunc() {
			return func;
		}

		@Override
		public String toString() {
			return scope + ":" + func;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((func == null) ? 0 : func.hashCode());
			result = prime * result + ((scope == null) ? 0 : scope.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CommandName other = (CommandName) obj;
			if (func == null) {
				if (other.func != null)
					return false;
			} else if (!func.equals(other.func))
				return false;
			if (scope == null) {
				if (other.scope != null)
					return false;
			} else if (!scope.equals(other.scope))
				return false;
			return true;
		}
	}

	@Activate
	void activate(BundleContext context, Config config) throws Exception {
		this.context = context;

		Dictionary<String, Object> dict = new Hashtable<String, Object>();
		dict.put(CommandProcessor.COMMAND_SCOPE, "gogo");

		// register converters
		registrations.add(context.registerService(Converter.class.getName(),
				new Converters(context.getBundle(0).getBundleContext()), null));

		// register commands
		dict.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "format", "getopt", "new", "set", "tac", "type" });
		registrations.add(context.registerService(Builtin.class.getName(), new Builtin(), dict));

		dict.put(CommandProcessor.COMMAND_FUNCTION,
				new String[] { "each", "if", "not", "throw", "try", "until", "while" });
		registrations.add(context.registerService(Procedural.class.getName(), new Procedural(), dict));

		dict.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "cat", "echo", "grep" });
		registrations.add(context.registerService(Posix.class.getName(), new Posix(), dict));

		dict.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "aslist" });
		registrations.add(context.registerService(EnRouteCommands.class.getName(), new EnRouteCommands(), dict));
		
		// Setup command history
		File historyFile = context.getDataFile("history");
		history = new FileHistory(historyFile);

		// Start shell on a separate thread
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				CommandSession cmdSession = null;
				ConsoleReader console = null;

				try {
					Terminal terminal = TerminalFactory.get();
					try {
						terminal.init();
					} catch (Exception e) {
						throw new RuntimeException("Failed to initialise terminal: ", e);
					}

					console = new ConsoleReader(System.in, System.out, terminal);
					console.setHistory(history);

					cmdSession = cmdProc.createSession(console.getInput(),
							new PrintStream(new WriterOutputStream(console.getOutput())), System.err);

					cmdSession.put(PROMPT, config.prompt());
					setupSession(cmdSession);

					console.addCompleter(new Completer() {
						@Override
						public int complete(String buffer, int cursor, List<CharSequence> candidates) {
							String prefix = buffer.substring(0, cursor);
							boolean prefixHasColon = prefix.indexOf(':') >= 0;

							Collection<CommandName> commands = listCommands();
							for (CommandName command : commands) {
								String scopedName = command.toString();
								if (prefixHasColon) {
									if (scopedName.startsWith(prefix))
										candidates.add(scopedName);
								} else {
									if (scopedName.startsWith(prefix) || command.getFunc().startsWith(prefix))
										candidates.add(scopedName);
								}
							}
							return 0;
						}
					});
					printMotd(console);
					while (!Thread.interrupted()) {
						// Read a line of input
						String inputLine;
						try {
							Object prompt = cmdSession.get(PROMPT);
							if ( prompt == null)
								prompt = config.prompt();
							if ( prompt instanceof String && ((String) prompt).startsWith("("))
								prompt = cmdSession.execute((String)prompt);
							else
								prompt = prompt.toString();
							
							inputLine = console.readLine((String)prompt, null);
							if (inputLine == null) {
								shutdown();
								return;
							}
							if (inputLine.isEmpty())
								continue;
						} catch (UserInterruptException e) {
							console.println("Use Ctrl-D to exit from enRoute OSGi Shell.");
							continue;
						}

						// Try to execute the command
						try {
							Object reply = cmdSession.execute(inputLine);
							if (reply != null) {
								CharSequence replyStr = cmdSession.format(reply, Converter.INSPECT);
								String[] replyLines = replyStr.toString().split("[\n\r\f]+");
								for (String replyLine : replyLines) {
									console.println(replyLine);
								}
							}
						} catch (Exception e) {
							cmdSession.put("exception-cmd", inputLine);
							cmdSession.put("exception", e);
							String message = e.getMessage();
							console.println(message != null ? message : "<null>");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (cmdSession != null)
						cmdSession.close();
					if (console != null)
						console.shutdown();
				}
			}

			private void printMotd(ConsoleReader console) throws IOException {
				String motd = config.motd();
				if (motd != null && !motd.isEmpty()) {
					if (!motd.equals("!"))
						console.println(motd);
				} else {
					URL motdEntry = context.getBundle().getEntry("motd.txt");
					if (motdEntry != null) {
						try (InputStream in = motdEntry.openStream()) {
							BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
							while (true) {
								String line = reader.readLine();
								if (line == null)
									break;
								console.println(line);
							}
						} catch (IOException e) {
							// No so important ...
						}
					}
				}
			}
		};
		thread = new Thread(runnable, "OSGi enRoute Gogo Shell");
		thread.start();
	}

	@Deactivate
	void deactivate() {
		for (ServiceRegistration<?> reg : registrations) {
			try {
				reg.unregister();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		thread.interrupt();
		try {
			thread.join(1000);
			history.flush();
		} catch (InterruptedException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setupSession(CommandSession session) throws Exception {
		// Make the 'system' and 'context' scopes map to java.lang.System and
		// org.osgi.framework.BundleContext respsectively
		session.execute("addcommand context ${.context}");
		session.execute("addcommand system (((${.context} bundles) 0) loadclass java.lang.System)");

		// Alias 'e' to print stack trace of last exception
		session.execute("e = {$exception printStackTrace}");
	}

	private Collection<CommandName> listCommands() {
		Set<CommandName> commands = new HashSet<>();

		ServiceReference<?>[] refs;
		try {
			refs = context.getAllServiceReferences(null, "(osgi.command.scope=*)");
		} catch (InvalidSyntaxException e) {
			// should never happen
			throw new RuntimeException(e);
		}
		for (ServiceReference<?> ref : refs) {
			String scope = (String) ref.getProperty("osgi.command.scope");
			Object funcsObj = ref.getProperty("osgi.command.function");
			String[] funcs;
			if (funcsObj instanceof String[])
				funcs = (String[]) funcsObj;
			else if (funcsObj instanceof String)
				funcs = new String[] { (String) funcsObj };
			else
				funcs = new String[0];

			for (String func : funcs) {
				CommandName command = new CommandName(scope, func);
				commands.add(command);
			}
		}
		return commands;
	}

	private void shutdown() throws BundleException {
		context.getBundle(0).stop();
	}
}
