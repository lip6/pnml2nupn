/**
 *  Copyright 2014-2019 Université Paris Nanterre and Sorbonne Université,
 *			 			CNRS, LIP6
 *
 *  All rights reserved.   This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Project leader / Initial Contributor:
 *    Lom Messan Hillah - <lom-messan.hillah@lip6.fr>
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.ValidationException;

import org.slf4j.Logger;

import com.ximpleware.extended.ParseExceptionHuge;
import com.ximpleware.extended.VTDGenHuge;
import com.ximpleware.extended.XMLMemMappedBuffer;

import fr.lip6.move.pnml2nupn.MainPNML2NUPN;
import fr.lip6.move.pnml2nupn.exceptions.InternalException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidFileException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidFileTypeException;
import fr.lip6.move.pnml2nupn.exceptions.PNMLImportExportException;
import fr.lip6.move.pnml2nupn.export.impl.NUPNConstants;
import fr.lip6.move.pnml2nupn.export.impl.NUPNWriter;
import fr.lip6.move.pnml2nupn.export.impl.OutChannelBean;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

/**
 * Provides a set of utility methods, useful mainly for channel-related
 * operations.
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

	public static OutChannelBean openOutChannel(File outFile) throws FileNotFoundException {
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
	 * Closes output channels.
	 * 
	 * @param ocbBpn
	 * @param ocbTs
	 * @param ocbPs
	 * @throws IOException
	 */
	public static final void closeChannels(OutChannelBean... channels) throws IOException {
		for (OutChannelBean ch : channels) {
			PNML2NUPNUtils.closeChannel(ch);
		}
	}
	
	/**
	 * Closes an output channel.
	 * 
	 * @param cb
	 * @throws IOException
	 */
	public static final void closeChannel(OutChannelBean cb) throws IOException {
		closeOutChannel(cb);
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
	public static synchronized void writeToChannel(OutChannelBean ocb, String output) throws IOException {

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
	public static synchronized void writeToChannel(OutChannelBean ocb, String output, ByteBuffer bytebuf,
			final List<byte[]> contents) throws IOException {

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
			int iterations = (int) Math.ceil((double) src.length() / (double) len);

			for (int i = 0; i < iterations; i++) {
				res.add(src.substring(i * len, Math.min(src.length(), (i + 1) * len)).getBytes(
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
			int iterations = (int) Math.ceil((double) src.length() / (double) len);

			for (int i = 0; i < iterations; i++) {
				contents.add(src.substring(i * len, Math.min(src.length(), (i + 1) * len)).getBytes(
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
	public static final void checkIsPnmlFile(File pFile) throws InvalidFileException, InvalidFileTypeException,
			ValidationException, InternalException {
		// boolean result = true;
		try {

			if (!pFile.exists()) {
				String message = "File " + pFile.getName() + " does not exist.";
				throw new InvalidFileException(message, new Throwable(message));
			}
			// check if regular file or directory
			if (!pFile.isFile()) {
				String message = pFile.getName() + " is not a regular file.";
				throw new InvalidFileTypeException(message, new Throwable(message));
			}
			if (!pFile.canRead()) {
				String message = "Cannot read file " + pFile.getName();
				throw new InvalidFileException(message, new Throwable(message));
			}
			final MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
			ftm.addMimeTypes("text/xml xml pnml XML PNML");
			final String contentType = ftm.getContentType(pFile);
			if (!contentType.contains("text/xml")) {
				String message = pFile.getName() + " is not an XML file: " + contentType;
				throw new InvalidFileTypeException(message, new Throwable(message));
			}
		} catch (NullPointerException npe) {
			PNML2NUPNUtils.printStackTrace(npe);
			throw new InternalException("Null pointer exception", new Throwable(
					"Something went wrong. Please, re-submit."));
		} catch (SecurityException se) {
			throw new InternalException(se.getMessage(), new Throwable(
					"Access right problem while accessing the file system. Please, re-submit."));
		}
		// return result;
	}

	/**
	 * Extracts a file from a source path to a destination path.
	 * 
	 * @param fromPath
	 * @param toPath
	 * @throws IOException
	 */
	public static final void extractFile(String fromPath, String toPath) throws IOException {
		try (InputStream input = PNML2NUPNUtils.class.getClassLoader().getResourceAsStream(fromPath);
				BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(new File(toPath)))) {

			if (input == null) {
				System.err.println("Could not found file to load.");
				return;
			}
			
			byte[] buffer = new byte[CONTENTSSIZE];
			int bytesRead = input.read(buffer);
			while (bytesRead != -1) {
				output.write(buffer, 0, bytesRead);
				bytesRead = input.read(buffer);
			}
		} catch (IOException e) {
			throw e;
		}
	}
	
	/**
	 * Emergency stop actions.
	 * 
	 * @param files the set of files to delete
	 */
	public static final void deleteOutputFiles(File... files) {
		for (File f: files) {
			PNML2NUPNUtils.deleteOutputFile(f);
		}
	}

	/**
	 * Deletes an output file.
	 * 
	 * @param oFile
	 */
	public static final void deleteOutputFile(File oFile) {
		if (oFile != null && oFile.exists()) {
			oFile.delete();
		}
	}

	/**
	 * Extracts the basename of a file path.
	 * 
	 * @param path
	 * @return
	 */
	public static final String extractBaseName(String path) {
		int dotPos = path.lastIndexOf('.');
		return path.substring(0, dotPos);
	}
	
	/**
	 * Initialises and returns and blocking queue.
	 * @return
	 */
	public static final BlockingQueue<String> initQueue() {
		BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
		return queue;
	}
	
	/**
	 * Creates and starts a new thread.
	 * @param ocb
	 * @param queue
	 * @return the started thread
	 */
	public static final Thread startWriter(OutChannelBean ocb, BlockingQueue<String> queue) {
		Thread t = new Thread(new NUPNWriter(ocb, queue));
		t.start();
		return t;
	}
	
	/**
	 * Cancels writers in case of emergency stop.
	 * 
	 * @param bpnQueue
	 * @param tsQueue
	 * @param psQueue
	 * @throws InterruptedException
	 */
	@SafeVarargs
	public static final void cancelWriters(BlockingQueue<String>...queues) throws InterruptedException {
		for (BlockingQueue<String> q: queues) {
			PNML2NUPNUtils.cancelWriter(q);
		}
	}
	
	/**
	 * Cancel a writer by sending a cancellation message to it.
	 * 
	 * @param queue
	 * @throws InterruptedException
	 */
	public static final void cancelWriter(BlockingQueue<String> queue) throws InterruptedException {
		if (queue != null) {
			queue.put(NUPNConstants.CANCEL);
		}
	}
	/**
	 * Normal stop of writers.
	 * 
	 * @param queues the set of queues to the writers
	 * @throws InterruptedException
	 */
	@SafeVarargs
	public static final void stopWriters(BlockingQueue<String>...queues) throws InterruptedException {
		for (BlockingQueue<String> q : queues) {
			PNML2NUPNUtils.stopWriter(q);
		}
	}
	
	/**
	 * Normal stop of a writer.
	 * 
	 * @param queue
	 * @throws InterruptedException
	 */
	public static final void stopWriter(BlockingQueue<String> queue) throws InterruptedException {
		if (queue != null)
			queue.put(NUPNConstants.STOP);
	}
	
	public static final void insertCreatorPragma(BlockingQueue<String> nupnQueue) throws InterruptedException {
		insertPragma(MainPNML2NUPN.getPragmaCreator() + NUPNConstants.NL, nupnQueue);
	}
	
	public static final void insertPragma(String pragma, BlockingQueue<String> nupnQueue) throws InterruptedException {
		nupnQueue.put(pragma);
	}
	
	public static final VTDGenHuge openXMLStream(File inFile) throws PNMLImportExportException {
		XMLMemMappedBuffer xb = new XMLMemMappedBuffer();
		VTDGenHuge vg = new VTDGenHuge();
		try {
			xb.readFile(inFile.getCanonicalPath());
			vg.setDoc(xb);
			vg.parse(true);
		} catch (ParseExceptionHuge | IOException e) {
			throw new PNMLImportExportException(e);
		}
		return vg;
	}
	
	public static final void setMin(long newNb, LongArrayList minHolder) {
		if (minHolder.isEmpty()) {
			minHolder.add(newNb);
		} else {
			long currentDiff = minHolder.getLong(0);
			currentDiff = Math.min(currentDiff, newNb);
			minHolder.set(0, currentDiff);
		}	
	}

	public static final void setMax(long newNb, LongArrayList maxHolder) {
		if (maxHolder.isEmpty()) {
			maxHolder.add(newNb);
		} else {
			long currentDiff = maxHolder.getLong(0);
			currentDiff = Math.max(currentDiff, newNb);
			maxHolder.set(0, currentDiff);
		}
	}
	
	/**
	 * It is assumed that the list in argument is sorted.
	 * @param ll
	 * @return
	 */
	public static final long arithmeticProgression(LongList ll) {
		int n = ll.size();
		return ((long)n * (ll.getLong(0) + ll.getLong(ll.size() - 1))) / 2L;
	}
	
	public static final long sum(LongList ll) {
		return ll.stream().mapToLong(x -> x).sum();
	}
	
	/**
	 * It is assumed that the list in argument is sorted.
	 * The constant in the arithmetic progression between NUPN Ids is 1. 
	 * @param idList
	 * @param faultyIds the resulting list where the consecutive elements not being in arithmetic progression  
	 * @return true if there is arithmetic progression, false otherwise
	 */
	public static boolean isArithmeticProgressionOnNUPNIds(LongList idList, LongSortedSet faultyIds) {
		Boolean isArPr = Boolean.TRUE;
		if (faultyIds == null) {
			faultyIds = new LongAVLTreeSet();
		}
		for (int i = 0; i < idList.size() - 1; i++) {
			if (idList.getLong(i + 1) - idList.getLong(i) != 1L) {
				faultyIds.add(idList.getLong(i));
				faultyIds.add(idList.getLong(i + 1));
		    	 isArPr =Boolean.valueOf(Boolean.logicalAnd(isArPr.booleanValue(), false));
		     }
		}
		return isArPr;
	}
	
	/**
	 * Prints debug message (using debug level), only if debug env variable is set.
	 * @param msg the message to print
	 * @param log  the logger to use
	 * @param args optional arguments for variable substitution with {}
	 */
	public static final void debug(String msg,  Logger log, Object... args){
		if (MainPNML2NUPN.isDebug()){
			log.debug(msg, args);
		}
	}
	
	/**
	 * Prints the stack trace of the exception passed as parameter.
	 * 
	 * @param e
	 */
	public static synchronized void printStackTrace(Exception e) {
		if (MainPNML2NUPN.isDebug()) {
			e.printStackTrace();
		}
	}

	

}
