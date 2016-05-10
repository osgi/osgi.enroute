package osgi.enroute.command.enroute.provider;

import java.io.File;
import java.io.IOException;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.osgi.service.component.annotations.Component;

import aQute.lib.io.IO;
import osgi.enroute.debug.api.Debug;

@Component(service = Object.class, name = "osgi.enroute.command.enroute.file", property = { Debug.COMMAND_SCOPE + "=enroute",
		Debug.COMMAND_FUNCTION + "=cd", Debug.COMMAND_FUNCTION + "=ls", Debug.COMMAND_FUNCTION + "=vw" })
public class FileCommands implements Converter {

	@Descriptor("Change work directory to the process work directory")
	public String cd( CommandSession session) {
		setCwd(session,IO.work);
		return IO.work.getAbsolutePath();
	}
	@Descriptor("Change directory from the current work directory")
	public String cd( CommandSession session, @Descriptor("path") String path) {
		File cwd = getCwd(session);
		cwd = IO.getFile( cwd, path);
		if ( !cwd.isDirectory())
			throw new IllegalArgumentException("Not a directory " + cwd);
		
		session.put("_pwd", cwd);
		return cwd.getAbsolutePath();
	}


	@Descriptor("List the files in the current directory")
	public String[] ls(CommandSession session) {
		return getCwd(session).list();
	}

	@Descriptor("List the contents of a file or directory. Use forward slashes for path")
	public Object ls(CommandSession session, @Descriptor("path") String path) throws IOException {
		File pwd = getCwd(session);
		File f = IO.getFile(pwd, path);
		if ( f.isDirectory())
			return f.list();
		else if ( f.isFile())
			return IO.collect(f);
		else
			return null;
	}

	@Descriptor("View the contents of a file or directory (alias for ls)")
	public Object vw(CommandSession session, String path) throws IOException {
		return ls(session,path);
	}

	@Override
	public Object convert(Class<?> arg0, Object arg1) throws Exception {
		return null;
	}

	@Override
	public CharSequence format(Object object, int type, Converter all) throws Exception {
		if ( object instanceof File) {
			return object.toString();
		}
		return null;
	}
	File getCwd(CommandSession session) {
		File pwd = (File) session.get("_pwd");
		if ( pwd == null)
			return IO.work;
		else
			return pwd;
	}
	File setCwd(CommandSession session, File file) {
		if ( !file.isDirectory())
			throw new IllegalArgumentException("You can only set the working dir to a directory : " + file);
		
		session.put("_pwd", file);
		return file;
	}

}
