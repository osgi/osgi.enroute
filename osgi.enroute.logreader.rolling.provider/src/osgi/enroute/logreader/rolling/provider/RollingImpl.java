package osgi.enroute.logreader.rolling.provider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;

/**
 * 
 */
@Component(name = "osgi.enroute.logreader.rolling")
public class RollingImpl extends Thread implements LogListener {
	FileWriter	fw;
	File		current;
	File		root;

	@interface Config {
		String where() default "messages";

		int logSize() default 100;

		int numberOfLogs() default 10;

		int level() default LogService.LOG_WARNING;

		String format() default "%T %8s %s\n";
	}

	Config							config;
	final BlockingQueue<LogEntry>	queue	= new LinkedBlockingQueue<>(100);

	@Activate
	void activate(BundleContext context, Config config) {
		this.config = config;
		File f = new File(config.where());
		if (f.isAbsolute())
			root = f;
		else
			root = context.getDataFile(config.where());

		root.mkdirs();
		if (!root.isDirectory())
			throw new IllegalStateException("Cannot create directory " + root);

	}

	@Deactivate
	void deactivate() throws InterruptedException {
		interrupt();
		join();
	}

	
	@Override
	public void logged(LogEntry entry) {
		if (entry.getLevel() < config.level())
			return;

		queue.offer(entry);
	}

	public void run() {

		try {

			RandomAccessFile file = getNextFile();
			long limit = config.logSize() * 1024;

			while (!isInterrupted()) {

				LogEntry entry = queue.take();
				String log = String.format(config.format(), entry.getTime(), level(entry.getLevel()),
						entry.getMessage(), entry.getException().getMessage(), entry.getBundle().getBundleId());

				file.write(log.getBytes(StandardCharsets.UTF_8));

				if (file.length() > limit) {
					file.close();
					file = getNextFile();
				}
			}

		} catch (InterruptedException e) {
			return;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				return;
			}
		}
	}

	private RandomAccessFile getNextFile() throws IOException {
		String name = String.format("%T.log", System.currentTimeMillis());
		File f = new File(root, name);
		RandomAccessFile raf = new RandomAccessFile(f, "w");
		raf.seek(raf.length());
		
		purge();
		
		return raf;
	}

	private void purge() {
		Stream.of(root.listFiles()) //
				.sorted( (a,b) -> Long.compare(a.lastModified(), b.lastModified())) //
				.skip(config.numberOfLogs()) //
				.forEach( ff -> ff.delete());
	}

	private String level(int level) {
		switch (level) {
		case LogService.LOG_DEBUG:
			return "debug";
		case LogService.LOG_INFO:
			return "info";
		case LogService.LOG_WARNING:
			return "warning";
		case LogService.LOG_ERROR:
			return "error";
		default:
			return "?" + level;
		}
	}

}
