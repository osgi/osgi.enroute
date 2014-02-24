package osgi.enroute.dto.api;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * An interface to transform a DTO into an output stream. Primarily invented to
 * be used with JSON but it can be used for other formats as well.
 */
public interface Codec {
	enum Format {
		COMPACT, CANONICAL, PRETTY;
	}

	/**
	 * A Builder interface to build an encoder.
	 */
	interface Encoder extends Closeable {
		/**
		 * Send the output to the given outputstream.
		 * 
		 * @param out
		 *            an output stream
		 * @param charset
		 *            The character set to use, if null, use the codec default
		 *            character set, which is by default UTF-8.
		 * @return the encoder
		 */
		Encoder to(OutputStream out, String charset);

		/**
		 * Send the output to the given appendable
		 * 
		 * @param out
		 *            the writer
		 * @return the encoder
		 */
		Encoder to(Appendable out);

		/**
		 * Send the output to the given file
		 * 
		 * @param out
		 *            the file to write
		 * @return the encoder
		 */
		Encoder to(File out);

		/**
		 * Send the output to the given URL
		 * 
		 * @param out
		 *            the url to write
		 * @return the encoder
		 */
		Encoder to(URL out);

		/**
		 * Send defaults or ignore them.
		 * 
		 * @param yes
		 *            if true, ignore defaults, otherwise send defaults.
		 * @return this encoder
		 */
		Encoder defaults(boolean yes);

		/**
		 * Define the way the output is formatted
		 * 
		 * @param format
		 *            the format to print in
		 * @return this encoder
		 */
		Encoder format(Format format);

		/**
		 * Put the object to the output. If multiple to methods have been
		 * called, the last one will take effect. Any streams or writers will be
		 * flushed before this method returns. Streams or appendables are only
		 * closed when they are opened by the encoder.
		 * 
		 * @param dto
		 *            the dto to output
		 */
		void put(Object dto) throws Exception;
	}

	/**
	 * A Decoder builder
	 */
	interface Decoder extends Closeable {
		/**
		 * Decode from a string
		 * 
		 * @param s
		 *            the string to decode from, must not be null
		 * @return
		 */
		Decoder from(String s);

		/**
		 * Decode from an input stream. This stream will be closed when read.
		 * 
		 * @param s
		 *            the string to decode from, must not be null
		 * @param charset
		 *            the character set, if null, the codec's default is chosen,
		 *            which has a default "UTF-8"
		 * @return
		 */
		Decoder from(InputStream in, String charset);

		/**
		 * Decode from a reader. The reader will be closed when eof is reached.
		 * 
		 * @param in
		 *            the reader
		 * @return
		 */
		Decoder from(Reader in);

		/**
		 * Calls from( in.openStream(), charset). See
		 * {@link #from(InputStream, String)}
		 * 
		 * @param in
		 *            the url
		 * @param charset
		 *            the charset, may be null
		 * @return
		 */
		Decoder from(URL in, String charset);

		/**
		 * Get the actual object
		 * 
		 * @param type
		 *            the class that the input must be converted from
		 * @return null if could not convert.
		 */
		<T> T get(Class<T> type) throws Exception;

		/**
		 * Get the actual object
		 * 
		 * @param type
		 *            the type reference that the input must be converted from
		 * @return null if could not convert.
		 */
		<T> T get(TypeReference<T> type) throws Exception;

		/**
		 * Get the actual object
		 * 
		 * @param type
		 *            the type that the input must be converted from
		 * @return null if could not convert.
		 */
		Object get(Type type) throws Exception;

		/**
		 * Get the actual object. This creates default types from the
		 * information in the input stream. It will consist of Maps, Strings,
		 * Numbers, Booleans, and Lists.
		 * 
		 * @return null if could not convert.
		 */

		Object get() throws Exception;
	}

	/**
	 * Return a new encoder instance. This encode inherits settings from this
	 * codec.
	 * 
	 * @return a new encoder.
	 */
	Encoder enc();

	/**
	 * Return a new decoder instance. This decoder inherits settings from this
	 * codec.
	 * 
	 * @return a new decoder.
	 */
	Decoder dec();

	/**
	 * Encode default values in the output stream. This makes the output stream
	 * a bit longer than necessary but it is more consistent and less error prone.
	 * By default the defaults are outputed.
	 * 
	 * @param yes
	 * @return this codec to further configure
	 */
	Codec defaults(boolean yes);

	/**
	 * Define the default way the output is formatted. The default is canonical.
	 * 
	 * @param format
	 *            the format to print in
	 * @return this codec to further configure
	 */
	Codec format(Format format);


	/**
	 * Define the default character set for encoders and decoders.
	 * 
	 * @param charset One of the available Java {@link Charset} objects
	 * @return this codec to further configure
	 */
	Codec charset(String charset);
}
