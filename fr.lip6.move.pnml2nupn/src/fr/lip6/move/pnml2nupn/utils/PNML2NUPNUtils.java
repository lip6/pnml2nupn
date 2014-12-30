/**
 *  Copyright 2014-2015 Université Paris Ouest and Sorbonne Universités, Univ. Paris 06 - CNRS UMR 7606 (LIP6)
 *
 *  All rights reserved.   This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Project leader / Initial Contributor:
 *    Lom Messan Hillah - <lom-messan.hillah@lip6.fr>
 *
 *  Contributors:
 *    ${ocontributors} - <$oemails}>
 *
 *  Mailing list:
 *    lom-messan.hillah@lip6.fr
 */
package fr.lip6.move.pnml2nupn.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.ValidationException;

import fr.lip6.move.pnml2nupn.MainPNML2NUPN;
import fr.lip6.move.pnml2nupn.exceptions.InternalException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidFileException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidFileTypeException;
import fr.lip6.move.pnml2nupn.export.impl.OutChannelBean;

/**
 * Provides a set of utility methods, useful mainly for channel-related operations.
 * 
 * These operations are synchronized.
 * @author lom
 *
 */
public final class PNML2NUPNUtils {

	private static final int BUFFERSIZEKB = 8;
	private static final int CONTENTSSIZEKB = 6;
	public static final int BUFFERSIZE = BUFFERSIZEKB * 1024;
	public static final int CONTENTSSIZE = CONTENTSSIZEKB * 1024;
	public static final String FILE_ENCODING = "ISO-8859-1";

	private PNML2NUPNUtils() {
		super();
	}

	public static OutChannelBean openOutChannel(File outFile)
			throws FileNotFoundException {
		final FileOutputStream fos = new FileOutputStream(outFile);
		final FileChannel fc = fos.getChannel();
		OutChannelBean ocb = new OutChannelBean(fc, fos);
		return ocb;

	}

	public static void closeOutChannel(OutChannelBean ocb) throws IOException {
		if (ocb != null) {
			ocb.getFc().close();
			ocb.getFos().close();
		}

	}

	/**
	 * Writes output to file using Java NIO API. Buffer size is 8K and choped
	 * contents size is 6K.
	 * 
	 * @param file
	 *            destination file
	 * @param output
	 *            the string to write there
	 * @throws IOException
	 * @see {@link #chopString(String, int)}
	 */
	public static synchronized void writeToChannel(OutChannelBean ocb, String output)
			throws IOException {

		ByteBuffer bytebuf;
		bytebuf = ByteBuffer.allocateDirect(BUFFERSIZE);
		List<byte[]> contents = chopString(output, CONTENTSSIZE);
		for (byte[] cont : contents) {
			bytebuf.put(cont);
			bytebuf.flip();
			ocb.getFc().write(bytebuf);
			bytebuf.clear();
		}

	}
	
	/**
	 * @see #writeToChannel(OutChannelBean, String)
	 */
	public static synchronized void writeToChannel(OutChannelBean ocb, String output, ByteBuffer bytebuf, final List<byte[]> contents)
			throws IOException {
		
		final List<byte[]> contents2 = chopString(output, CONTENTSSIZE, contents);
		for (byte[] cont : contents2) {
			bytebuf.put(cont);
			bytebuf.flip();
			ocb.getFc().write(bytebuf);
			bytebuf.clear();
		}
	}

	/**
	 * Chops a string into chunks of len long.
	 * 
	 * @param src
	 *            the string to chop
	 * @param len
	 *            the length of each chunk
	 * @return the list of chunks
	 */
	public static synchronized List<byte[]> chopString(String src, int len) {
		List<byte[]> res = new ArrayList<byte[]>();
		if (src.length() > len) {
			int iterations = (int) Math.ceil((double) src.length()
					/ (double) len);

			for (int i = 0; i < iterations; i++) {
				res.add(src.substring(i * len,
						Math.min(src.length(), (i + 1) * len)).getBytes(
						Charset.forName(FILE_ENCODING)));
			}
		} else {
			res.add(src.getBytes(Charset.forName(FILE_ENCODING)));
		}
		return res;
	}
	
	/**
	 * @see #chopString(String, int)
	 * @param src
	 * @param len
	 * @param contents
	 * @return
	 */
	public static synchronized List<byte[]> chopString(String src, int len, final List<byte[]> contents) {
		if (src.length() > len) {
			int iterations = (int) Math.ceil((double) src.length()
					/ (double) len);

			for (int i = 0; i < iterations; i++) {
				contents.add(src.substring(i * len,
						Math.min(src.length(), (i + 1) * len)).getBytes(
						Charset.forName(FILE_ENCODING)));
			}
		} else {
			contents.add(src.getBytes(Charset.forName(FILE_ENCODING)));
		}
		return contents;
	}
	/**
	 * Checks the basic external expected characteristics of a PNML document.
	 * 
	 * @throws InvalidFileException
	 *             document has formating errors.
	 * @throws InvalidFileTypeException
	 *             document is not of the correct type.
	 * @throws ValidationException
	 *             document is not valid
	 * @throws InternalException
	 *             some internal problem
	 * @param pFile
	 *            the file corresponding to the PNML document.
	 * @return the validation message.
	 */
	public static final void checkIsPnmlFile(File pFile)
			throws InvalidFileException, InvalidFileTypeException,
			ValidationException, InternalException {
		//boolean result = true;
		try {
			
			if (!pFile.exists()) {
				String message = "File " + pFile.getName() + " does not exist.";
				throw new InvalidFileException(message, new Throwable(message));
			}
			// check if regular file or directory
			if (!pFile.isFile()) {
				String message = pFile.getName() + " is not a regular file.";
				throw new InvalidFileTypeException(message, new Throwable(
						message));
			}
			if (!pFile.canRead()) {
				String message = "Cannot read file " + pFile.getName();
				throw new InvalidFileException(message, new Throwable(message));
			}
			final MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
			ftm.addMimeTypes("text/xml xml pnml XML PNML");
			final String contentType = ftm.getContentType(pFile);
			if (!contentType.contains("text/xml")) {
				String message = pFile.getName() + " is not an XML file: "
						+ contentType;
				throw new InvalidFileTypeException(message, new Throwable(
						message));
			}
		} catch (NullPointerException npe) {
			MainPNML2NUPN.printStackTrace(npe);
			throw new InternalException("Null pointer exception",
					new Throwable("Something went wrong. Please, re-submit."));
		} catch (SecurityException se) {
			throw new InternalException(
					se.getMessage(),
					new Throwable(
							"Access right problem while accessing the file system. Please, re-submit."));
		}
		//return result;
	}
	
	/**
	 * Extracts a file from a source path to a destination path.
	 * @param fromPath
	 * @param toPath
	 * @throws IOException
	 */
	public static final void extractFile(String fromPath, String toPath) throws IOException {
		
		InputStream input = PNML2NUPNUtils.class.getClassLoader().getResourceAsStream(fromPath);
		if (input == null) {
			System.err.println("Could not found file to load.");
			return;
		}
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(new File(toPath)));
		
		byte [] buffer = new byte[CONTENTSSIZE];
		int bytesRead = input.read(buffer);
		while (bytesRead != -1) {
		    output.write(buffer, 0, bytesRead);
		    bytesRead = input.read(buffer);
		}
		output.close();
		input.close();
	}
	/**
	 * Extracts the basename of a file path.
	 * @param path
	 * @return
	 */
	public static final String extractBaseName(String path) {
		int dotPos = path.lastIndexOf('.');
		return path.substring(0, dotPos);
	}
	
}
