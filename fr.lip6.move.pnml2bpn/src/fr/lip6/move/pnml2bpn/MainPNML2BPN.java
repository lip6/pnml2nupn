/**
 *  Copyright 2014 Universite Paris Ouest and Sorbonne Universites, Univ. Paris 06 - CNRS UMR 7606 (LIP6)
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
package fr.lip6.move.pnml2bpn;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import fr.lip6.move.pnml2bpn.exceptions.PNMLImportExportException;
import fr.lip6.move.pnml2bpn.export.PNML2BPNFactory;
import fr.lip6.move.pnml2bpn.export.PNMLExporter;

/**
 * Main class for command-line invocation.
 * 
 * @author lom
 * 
 */
public final class MainPNML2BPN {

	public static final String NL = "\n";
	public static final String COLWS = ": ";
	public static final String WSDASH = " -";
	public static final String VERSION = "1.2.0";
	private static final String BPN_EXT = ".bpn";
	private static final String PNML_EXT = ".pnml";
	private static final String PNML2BPN_DEBUG = "PNML2BPN_DEBUG";
	private static final String CAMI_TMP_KEEP = "cami.tmp.keep";
	/**
	 * Force BPN Generation works by default for the case where bounds checking is disabled.
	 */
	private static final String FORCE_BPN_GENERATION = "force.bpn.generation";
	/**
	 * Bounds checking property.
	 */
	private static final String BOUNDS_CHECKING = "bounds.checking";
	/**
	 * Remove transitions of unsafe arcs (incoming or outgoing)
	 */
	private static final String REMOVE_TRANS_UNSAFE_ARCS = "remove.unsafe.trans";
	/**
	 * Force generation of unsafe nets, where it is clearly checked that 
	 * at least one initial has more than 1 token or the inscription of an arc
	 * is valued to more than 1. 
	 */
	private static final String GENERATE_UNSAFE = "generate.unsafe";
	
	private static StringBuilder signatureMesg;
	
	private static boolean isDebug;

	private static List<String> pathDest;
	private static List<String> pathSrc;
	private static PNMLFilenameFilter pff;
	private static DirFileFilter dff;
	private static boolean isOption;
	private static boolean isCamiTmpDelete;
	private static boolean isForceBPNGen;
	private static boolean isBoundsChecking;
	private static boolean isRemoveTransUnsafeArcs;
	private static boolean isGenerateUnsafe;

	private MainPNML2BPN() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.nanoTime();
		org.slf4j.Logger myLog = LoggerFactory.getLogger(MainPNML2BPN.class
				.getCanonicalName());
		StringBuilder msg = new StringBuilder();
		if (args.length < 1) {
			myLog.error("At least the path to one PNML P/T file is expected. You may provide a file, a directory, or a mix of several of these.");
			return;
		}
		// Debug mode?
		checkDebugMode(myLog, msg);
		// Keep Cami property
		checkCamiKeepingMode(myLog, msg);
		// Force BPN generation property
		checkForceBPNGenMode(myLog, msg);
		// Bounds checking property
		checkBoundsCheckingMode(myLog, msg);
		// Generate structural.bpn (case of forced generation in case of unsafe initial
		// places or arcs
		checkGenerateUnsafeMode(myLog, msg);
		// Remove unsafe arcs?
		checkRemoveTransUnsafeArcsMode(myLog, msg);

		try {
			extractSrcDestPaths(args);
		} catch (IOException e1) {
			myLog.error("Could not successfully extract all source files paths. See log.");
			myLog.error(e1.getMessage());
			if (MainPNML2BPN.isDebug) {
				e1.printStackTrace();
			}
		}
		//long startTime = System.nanoTime();
		initSignatureMessage();
		PNMLExporter pe = new PNML2BPNFactory().createExporter();
		org.slf4j.Logger jr = LoggerFactory.getLogger(pe.getClass()
				.getCanonicalName());
		// TODO : optimize with threads
		boolean error = false;
		for (int i = 0; i < pathSrc.size(); i++) {
			try {
				pe.exportPNML(new File(pathSrc.get(i)),
						new File(pathDest.get(i)), jr);
			} catch (PNMLImportExportException | InterruptedException
					| IOException e) {
				myLog.error(e.getMessage());
				MainPNML2BPN.printStackTrace(e);
				error |= true;
			}
		}
		long endTime = System.nanoTime();

		if (!error) {
			msg.append("Finished successfully.");
			myLog.info(msg.toString());
		} else {
			msg.append("Finished in error.");
			if (!MainPNML2BPN.isDebug) {
				msg.append(
						" Activate debug mode to print stacktraces, like so: export ")
						.append(PNML2BPN_DEBUG).append("=true");
			}
			myLog.error(msg.toString());
		}

		msg = null;
		myLog.info("PNML to BPN took {} seconds.",
				(endTime - startTime) / 1.0e9);
		myLog.info(signatureMesg.toString());
		LoggerContext loggerContext = (LoggerContext) LoggerFactory
				.getILoggerFactory();
		loggerContext.stop();
		if (error) {
			System.exit(-1);
		}
	}

	/**
	 * Initialises signature message.
	 */
	private static void initSignatureMessage() {
		signatureMesg = new StringBuilder();
		signatureMesg.append(COLWS).append("generated by pnml2bpn version ").append(VERSION);
		if (isOption) {
			signatureMesg.append(" with options ");
			if (isForceBPNGen) {
				signatureMesg.append(WSDASH).append(FORCE_BPN_GENERATION);
			}
			if (!isBoundsChecking) {
				signatureMesg.append(WSDASH).append(BOUNDS_CHECKING);
			}
			if (isGenerateUnsafe) {
				signatureMesg.append(WSDASH).append(GENERATE_UNSAFE);
			}
			if (isRemoveTransUnsafeArcs) {
				signatureMesg.append(WSDASH).append(REMOVE_TRANS_UNSAFE_ARCS);
			}
			if (!isCamiTmpDelete) {
				signatureMesg.append(WSDASH).append(CAMI_TMP_KEEP);
			}
			
		} else {
			signatureMesg.append(" with default values for options");
		}
		
	}

	private static void checkGenerateUnsafeMode(Logger myLog, StringBuilder msg) {
		String genUnsafe = System.getProperty(GENERATE_UNSAFE);
		if (genUnsafe != null && Boolean.valueOf(genUnsafe)) {
			isGenerateUnsafe = true;
			isOption = true;
			myLog.warn("Generation of unsafe (structural) BPN enabled.");
		} else if (genUnsafe == null) {
			isGenerateUnsafe = false;
			msg.append(
					"Generation of unsafe BPN (structural only) not set. Default is false. If you want to "
					+ "generate unsafe (structural) BPN, then invoke this program with ")
					.append(GENERATE_UNSAFE)
					.append(" property like so: java -D")
					.append(GENERATE_UNSAFE)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		} else {
			isGenerateUnsafe = false;
			myLog.info("Generation of unsafe (structural) BPN  disabled.");
		}
	}

	private static void checkRemoveTransUnsafeArcsMode(Logger myLog,
			StringBuilder msg) {
		String removeua = System.getProperty(REMOVE_TRANS_UNSAFE_ARCS);
		if (removeua != null && Boolean.valueOf(removeua)) {
			isRemoveTransUnsafeArcs = true;
			isOption = true;
			myLog.warn("Removal of transitions connecte to unsafe arcs enabled.");
		} else if (removeua == null) {
			isRemoveTransUnsafeArcs = false;
			msg.append(
					"Remove transitions of unsafe arcs not set. Default is false. If you want to "
					+ "remove transitions of unsafe arcs, then invoke this program with ")
					.append(REMOVE_TRANS_UNSAFE_ARCS)
					.append(" property like so: java -D")
					.append(REMOVE_TRANS_UNSAFE_ARCS)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		} else {
			isRemoveTransUnsafeArcs = false;
			myLog.warn("Remove transitions of unsafe arcs disabled.");
		}
		
	}

	private static void checkBoundsCheckingMode(org.slf4j.Logger myLog,
			StringBuilder msg) {
		String boundsChecking = System.getProperty(BOUNDS_CHECKING);
		if (boundsChecking != null && Boolean.valueOf(boundsChecking)) {
			isBoundsChecking = true;
			myLog.warn("Bounds checking enabled.");
		} else if (boundsChecking == null) {
			isBoundsChecking = true;
			msg.append(
					"Bounds checking not set. Default is true. If you want to disable bounds checking, then invoke this program with ")
					.append(BOUNDS_CHECKING)
					.append(" property like so: java -D")
					.append(BOUNDS_CHECKING)
					.append("=false [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		} else {
			isBoundsChecking = false;
			isOption = true;
			myLog.warn("Bounds checking disabled.");
		}
	}

	/**
	 * @param myLog
	 * @param msg
	 */
	private static void checkForceBPNGenMode(org.slf4j.Logger myLog,
			StringBuilder msg) {
		String forceBpnGen = System.getProperty(FORCE_BPN_GENERATION);
		if (forceBpnGen != null && Boolean.valueOf(forceBpnGen)) {
			isForceBPNGen = true;
			isOption = true;
			myLog.warn("Force BPN generation enabled.");
		} else {
			isForceBPNGen = false;
			msg.append(
					"Forcing BPN generation not set. Default is false. If you want to force BPN generation for non 1-Safe nets, then invoke this program with ")
					.append(FORCE_BPN_GENERATION)
					.append(" property like so: java -D")
					.append(FORCE_BPN_GENERATION)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		}
	}

	/**
	 * @param myLog
	 * @param msg
	 */
	private static void checkCamiKeepingMode(org.slf4j.Logger myLog,
			StringBuilder msg) {
		String keep = System.getProperty(CAMI_TMP_KEEP);
		if (keep != null && Boolean.valueOf(keep)) {
			isCamiTmpDelete = false;
			isOption = true;
			myLog.warn("Keep temporary Cami enabled.");
		} else {
			isCamiTmpDelete = true;
			msg.append(
					"Keeping temporary Cami file property not set. If you want to keep temporary Cami file then invoke this program with ")
					.append(CAMI_TMP_KEEP)
					.append(" property like so: java -D")
					.append(CAMI_TMP_KEEP)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		}
	}

	/**
	 * @param myLog
	 * @param msg
	 */
	private static void checkDebugMode(org.slf4j.Logger myLog, StringBuilder msg) {
		String debug = System.getenv(PNML2BPN_DEBUG);
		if ("true".equalsIgnoreCase(debug)) {
			setDebug(true);
		} else {
			setDebug(false);
			msg.append(
					"Debug mode not set. If you want to activate the debug mode (print stackstaces in case of errors), then set the ")
					.append(PNML2BPN_DEBUG)
					.append(" environnement variable like so: export ")
					.append(PNML2BPN_DEBUG).append("=true.");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		}
	}

	/**
	 * Extracts PNML files (scans directories recursively) from command-line
	 * arguments.
	 * 
	 * @param args
	 * @throws IOException
	 */
	private static void extractSrcDestPaths(String[] args) throws IOException {
		pathDest = new ArrayList<String>();
		pathSrc = new ArrayList<String>();
		File srcf;
		File[] srcFiles;
		pff = new PNMLFilenameFilter();
		dff = new DirFileFilter();
		for (String s : args) {
			srcf = new File(s);
			if (srcf.isFile()) {
				pathSrc.add(s);
				pathDest.add(s.replaceAll(PNML_EXT, BPN_EXT));
			} else if (srcf.isDirectory()) {
				srcFiles = extractSrcFiles(srcf, pff, dff);
				for (File f : srcFiles) {
					pathSrc.add(f.getCanonicalPath());
					pathDest.add(f.getCanonicalPath().replaceAll(PNML_EXT,
							BPN_EXT));
				}
			}
		}
	}

	private static File[] extractSrcFiles(File srcf, PNMLFilenameFilter pff,
			DirFileFilter dff) {
		List<File> res = new ArrayList<File>();

		// filter PNML files
		File[] pfiles = srcf.listFiles(pff);
		res.addAll(Arrays.asList(pfiles));

		// filter directories
		pfiles = srcf.listFiles(dff);
		for (File f : pfiles) {
			res.addAll(Arrays.asList(extractSrcFiles(f, pff, dff)));
		}

		return res.toArray(new File[0]);
	}

	private static final class PNMLFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(PNML_EXT);
		}
	}

	private static final class DirFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	}

	/**
	 * Returns true if debug mode is set.
	 * 
	 * @return
	 */
	public static boolean isDebug() {
		return isDebug;
	}

	/**
	 * Sets the debug mode according to parameter: enable (true) or disable
	 * (false).
	 * 
	 * @param isDebug
	 */
	public static synchronized void setDebug(boolean isDebug) {
		MainPNML2BPN.isDebug = isDebug;
	}

	/**
	 * Returns true if temporary Cami file should be deleted, false otherwise.
	 * 
	 * @return
	 */
	public static synchronized boolean isCamiTmpDelete() {
		return isCamiTmpDelete;
	}

	/**
	 * Returns true if BPN generation is forced, even if the net is not 1-safe.
	 * 
	 * @return
	 */
	public static synchronized boolean isForceBPNGen() {
		return isForceBPNGen;
	}

	/**
	 * Returns true if bounds checking is enabled (default), false otherwise.
	 * 
	 * @return
	 */
	public static synchronized boolean isBoundsChecking() {
		return isBoundsChecking;
	}
	
	/**
	 * Returns true if user has asked for the removal of transitions
	 * of unsafe arcs (incoming or outgoing).
	 * @return
	 */
	public static synchronized boolean isRemoveTransUnsafeArcs() {
		return isRemoveTransUnsafeArcs;
	}
	
	/**
	 * Returns true if user has asked for the generation of unsafe 
	 * BPN (structural.bpn) in the case of unsafe initial place(s) or arc(s). 
	 * @return
	 */
	public static synchronized boolean isGenerateUnsafe() {
		return isGenerateUnsafe;
	}
	
	public static synchronized void appendMesgLineToSignature(String msg) {
		signatureMesg.append(NL).append(COLWS).append(msg);
	}

	/**
	 * Prints the stack trace of the exception passed as parameter.
	 * 
	 * @param e
	 */
	public static synchronized void printStackTrace(Exception e) {
		if (MainPNML2BPN.isDebug) {
			e.printStackTrace();
		}
	}

}
