/**
 *  Copyright 2014-2016 Université Paris Nanterre and Sorbonne Université,
 * 							 CNRS, LIP6
 *
 *  All rights reserved.   This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Project leader / Initial Contributor:
 *    Lom Messan Hillah - <lom-messan.hillah@lip6.fr>
 *
 *
 *  Mailing list:
 *    lom-messan.hillah@lip6.fr
 */
package fr.lip6.move.pnml2nupn;

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
import fr.lip6.move.pnml2nupn.exceptions.EarlyStopException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidPNMLTypeException;
import fr.lip6.move.pnml2nupn.exceptions.PNMLImportExportException;
import fr.lip6.move.pnml2nupn.export.PNML2NUPNFactory;
import fr.lip6.move.pnml2nupn.export.PNML2NUPNExporter;

/**
 * Main class for command-line invocation.
 * 
 * @author lom
 * 
 */
public final class MainPNML2NUPN {

	private static final String DOT = ".";
	public static final String NL = "\n";
	public static final String EQ = "=";
	public static final String WS = " ";
	public static final String COLWS = ":" + WS;
	public static final String WSDASH = " -";
	public static final String XP = "!";
	public static final String TOOL_NAME = "pnml2nupn";
	public static final String VERSION = "2.1.0";
	public static final String CREATOR = "creator";
	public static final String UNIT_SAFE = "unit_safe";
	public static final String BOUNDS = "cosyverif/bounds 1.0";
	public static final String PRAGMA_CREATOR = XP + CREATOR + WS + TOOL_NAME + WS + VERSION;
	public static final String PRAGMA_UNIT_SAFE_BY_CREATOR = XP + UNIT_SAFE;
	public static final String PRAGMA_UNIT_SAFE_BY_BOUNDS = XP + UNIT_SAFE + WS + BOUNDS;
	public static final String PRAGMA_MULTIPLE_INIT_TOKEN = XP + "multiple_initial_tokens" + WS;
	public static final String PRAGMA_MULTIPLE_ARCS = XP + "multiple_arcs" + WS;
	public static final String NUPN = "nupn";
	public static final String PNML_EXT = "pnml";
	public static final String PNML2NUPN_DEBUG = "PNML2NUPN_DEBUG";
	public static final String CAMI_TMP_KEEP = "cami.tmp.keep";
	/**
	 * Force NUPN Generation works by default for the case where bounds checking
	 * is disabled.
	 */
	public static final String FORCE_NUPN_GENERATION = "force.nupn.generation";
	/**
	 * Bounds checking property.
	 */
	public static final String UNIT_SAFENESS_CHECKING = "unit.safeness.checking";
	/**
	 * Stop after unit-safeness checking, whatever the result found.
	 */
	public static final String UNIT_SAFENESS_CHECKING_ONLY = "unit.safeness.checking.only";

	public static final String HAS_UNSAFE_ARCS = "has.unsafe.arcs";
	
	/**
	 * Preserve NUPN toolspecific section when encountered during the generation - best effort mode.
	 * Mixed generation strategy: naive one combined with NUPN information.
	 */
	public static final String PRESERVE_NUPN_MIX = "preserve.nupn.mix";
	/**
	 * Preserve NUPN toolspecific section right from the beginning by looking for it first - native mode
	 */
	public static final String PRESERVE_NUPN_NATIVE = "preserve.nupn.native";

	private static StringBuilder signatureMesg;

	private static boolean isDebug;

	private static List<String> pathDest;
	private static List<String> pathSrc;
	private static PNMLFilenameFilter pff;
	private static DirFileFilter dff;
	private static boolean isOption;
	private static boolean isCamiTmpDelete;
	private static boolean isForceNUPNGen;
	private static boolean isUnitSafeChecking;
	private static boolean isUnitSafeCheckingOnly;
	private static boolean isRemoveTransUnsafeArcs;
	private static boolean isGenerateUnsafe;
	private static boolean isHasUnsafeArcs;
	private static boolean isPreserveNupnMix;
	private static boolean isPreserveNupnNative;

	private MainPNML2NUPN() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.nanoTime();
		org.slf4j.Logger myLog = LoggerFactory.getLogger(MainPNML2NUPN.class.getCanonicalName());
		StringBuilder msg = new StringBuilder();
		if (args.length < 1) {
			myLog.error("The path to at least one PNML P/T file is expected. You may provide a file, a directory, or a mix of several of these.");
			return;
		}
		// Debug mode?
		checkDebugMode(myLog, msg);
		// Keep Cami property
		checkCamiKeepingMode(myLog, msg);
		// Force NUPN generation property
		checkForceNUPNGenMode(myLog, msg);
		// Unit safeness checking property
		checkUnitSafeCheckingMode(myLog, msg);
		// Unit safeness checking only property
		checkUnitSafeCheckingOnlyMode(myLog, msg);
		// Has unsafe arcs?
		checkHashUnsafeArcsMode(myLog, msg);
		// Preserve NUPN tool info in mixed mode?
		checkPreserveNUPMix(myLog, msg);
		// Preserve NUNPN info in native mode?
		checkPreserveNUPNative(myLog, msg);
		try {
			extractSrcDestPaths(args);
		} catch (IOException e1) {
			myLog.error("Could not successfully extract all source files paths. See log.");
			myLog.error(e1.getMessage());
			if (MainPNML2NUPN.isDebug) {
				e1.printStackTrace();
			}
		}
		initSignatureMessage();
		PNML2NUPNExporter pe = PNML2NUPNFactory.instance().createExporter();
		org.slf4j.Logger jr = LoggerFactory.getLogger(pe.getClass().getCanonicalName());
		// TODO : optimize with threads
		boolean error = false;
		for (int i = 0; i < pathSrc.size(); i++) {
			try {
				// Option exclusive of the others
				if (isHasUnsafeArcs) {
					pe.hasUnsafeArcs(pathSrc.get(i), pathDest.get(i), jr);
				} else {
					pe.export2NUPN(new File(pathSrc.get(i)), new File(pathDest.get(i)), jr);
				}
			} catch (PNMLImportExportException | InterruptedException | IOException | InvalidPNMLTypeException e) {
				myLog.error(e.getMessage());
				MainPNML2NUPN.printStackTrace(e);
				error |= true;
			} catch (EarlyStopException e) {
				myLog.warn(e.getMessage());
			}
		}

		if (!error) {
			msg.append("Finished successfully.");
			myLog.info(msg.toString());
		} else {
			msg.append("Finished in error.");
			if (!MainPNML2NUPN.isDebug) {
				msg.append(" Activate debug mode to print stacktraces, like so: export ").append(PNML2NUPN_DEBUG)
						.append("=true");
			}
			myLog.error(msg.toString());
		}
		myLog.info(signatureMesg.toString());
		long endTime = System.nanoTime();
		myLog.info("PNML to NUPN took {} seconds.", (endTime - startTime) / 1.0e9);
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
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
		signatureMesg.append(COLWS).append("generated by pnml2nupn version ").append(VERSION);
		if (isOption) {
			signatureMesg.append(" with options");
			if (isForceNUPNGen) {
				signatureMesg.append(WSDASH).append(FORCE_NUPN_GENERATION).append(EQ).append(isForceNUPNGen);
			}
			if (!isUnitSafeChecking) {
				signatureMesg.append(WSDASH).append(UNIT_SAFENESS_CHECKING).append(EQ).append(isUnitSafeChecking);
			}
			if (!isUnitSafeCheckingOnly) {
				signatureMesg.append(WSDASH).append(UNIT_SAFENESS_CHECKING_ONLY).append(EQ).append(isUnitSafeCheckingOnly);
			}
			if (!isCamiTmpDelete) {
				signatureMesg.append(WSDASH).append(CAMI_TMP_KEEP).append(EQ).append(isCamiTmpDelete);
			}
			if (isHasUnsafeArcs) {
				signatureMesg.append(WSDASH).append(HAS_UNSAFE_ARCS).append(EQ).append(isHasUnsafeArcs);
			}
			if (isPreserveNupnMix) {
				signatureMesg.append(WSDASH).append(PRESERVE_NUPN_MIX).append(EQ).append(isPreserveNupnMix);
			}
			if (isPreserveNupnNative) {
				signatureMesg.append(WSDASH).append(PRESERVE_NUPN_NATIVE).append(EQ).append(isPreserveNupnNative);
			}

		} else {
			signatureMesg.append(" with default values for all options.");
		}
	}

	private static void checkHashUnsafeArcsMode(Logger myLog, StringBuilder msg) {
		String unsafeArcs = System.getProperty(HAS_UNSAFE_ARCS);
		if (unsafeArcs != null && Boolean.valueOf(unsafeArcs)) {
			isHasUnsafeArcs = true;
			isOption = true;
			myLog.warn("You have requested to find if there are unsafe arcs. This option is exclusive of all the others.");
		} else if (unsafeArcs == null) {
			isHasUnsafeArcs = false;
			msg.append(
					"Request to find unsafe arcs (exclusive option) not set. Default is false. If you want to "
							+ "set this property (exclusive of the others, then invoke this program with ")
					.append(HAS_UNSAFE_ARCS).append(" property like so: java -D").append(HAS_UNSAFE_ARCS)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		} else {
			isHasUnsafeArcs = false;
			myLog.info("No request to find unsafe arcs (exclusive option).");
		}
	}

	private static void checkUnitSafeCheckingMode(org.slf4j.Logger myLog, StringBuilder msg) {
		String boundsChecking = System.getProperty(UNIT_SAFENESS_CHECKING);
		if (boundsChecking != null && Boolean.valueOf(boundsChecking)) {
			isUnitSafeChecking = true;
			myLog.warn("Unit safeness checking enabled.");
		} else if (boundsChecking == null) {
			isUnitSafeChecking = false;
			msg.append(
					"Unit safeness checking not set. Default is false. If you want to enable unit safeness checking, then invoke this program with ")
					.append(UNIT_SAFENESS_CHECKING).append(" property like so: java -D").append(UNIT_SAFENESS_CHECKING)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		} else {
			isUnitSafeChecking = false;
			isOption = true;
			myLog.warn("Unit safeness checking disabled.");
		}
	}

	private static void checkUnitSafeCheckingOnlyMode(Logger myLog, StringBuilder msg) {
		String usCheckOnly = System.getProperty(UNIT_SAFENESS_CHECKING_ONLY);
		if (usCheckOnly != null && Boolean.valueOf(usCheckOnly)) {
			isUnitSafeCheckingOnly = true;
			myLog.warn("Unit safeness checking only enabled.");
		} else if (usCheckOnly == null) {
			isUnitSafeCheckingOnly = false;
			msg.append(
					"Unit safeness checking only not set. Default is false. If you want to enable unit safeness checking only, then invoke this program with ")
					.append(UNIT_SAFENESS_CHECKING_ONLY).append(" property like so: java -D").append(UNIT_SAFENESS_CHECKING_ONLY)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		} else {
			isUnitSafeCheckingOnly = false;
			isOption = true;
			myLog.warn("Unit safeness checking only disabled.");
		}
		
	}
	
	/**
	 * @param myLog
	 * @param msg
	 */
	private static void checkForceNUPNGenMode(org.slf4j.Logger myLog, StringBuilder msg) {
		String forceBpnGen = System.getProperty(FORCE_NUPN_GENERATION);
		if (forceBpnGen != null && Boolean.valueOf(forceBpnGen)) {
			isForceNUPNGen = true;
			isOption = true;
			myLog.warn("Force NUPN generation enabled.");
		} else {
			isForceNUPNGen = false;
			msg.append(
					"Forcing NUPN generation not set. Default is false. If you want to force NUPN generation for non 1-Safe nets, then invoke this program with ")
					.append(FORCE_NUPN_GENERATION).append(" property like so: java -D").append(FORCE_NUPN_GENERATION)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		}
	}

	/**
	 * @param myLog
	 * @param msg
	 */
	private static void checkCamiKeepingMode(org.slf4j.Logger myLog, StringBuilder msg) {
		String keep = System.getProperty(CAMI_TMP_KEEP);
		if (keep != null && Boolean.valueOf(keep)) {
			isCamiTmpDelete = false;
			isOption = true;
			myLog.warn("Keep temporary Cami enabled.");
		} else {
			isCamiTmpDelete = true;
			msg.append(
					"Keeping temporary Cami file property not set. If you want to keep temporary Cami file then invoke this program with ")
					.append(CAMI_TMP_KEEP).append(" property like so: java -D").append(CAMI_TMP_KEEP)
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
		String debug = System.getenv(PNML2NUPN_DEBUG);
		if ("true".equalsIgnoreCase(debug)) {
			setDebug(true);
		} else {
			setDebug(false);
			msg.append(
					"Debug mode not set. If you want to activate the debug mode (print stacktraces in case of errors), then set the ")
					.append(PNML2NUPN_DEBUG).append(" environment variable like so: export ").append(PNML2NUPN_DEBUG)
					.append("=true.");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		}
	}
	
	private static void checkPreserveNUPMix(org.slf4j.Logger myLog, StringBuilder msg) {
		String preserveNupnMix = System.getProperty(PRESERVE_NUPN_MIX);
		if (preserveNupnMix != null && Boolean.valueOf(preserveNupnMix)) {
			isPreserveNupnMix = true;
			isOption = true;
			myLog.warn("Preserve NUPN tool specific info in mixed mode enabled");
		} else {
			isPreserveNupnMix = false;
			msg.append(
					"Preserving NUPN tool specific info in mixed mode not set. Default is false. If you want to preserve NUPN tool specific info when in naive generation mode, then invoke this program with ")
					.append(PRESERVE_NUPN_MIX).append(" property like so: java -D").append(PRESERVE_NUPN_MIX)
					.append("=true [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		}
	}
	
	private static void checkPreserveNUPNative(org.slf4j.Logger myLog, StringBuilder msg) {
		String preserveNupnNative = System.getProperty(PRESERVE_NUPN_NATIVE);
		if (preserveNupnNative != null && Boolean.valueOf(preserveNupnNative)) {
			isPreserveNupnNative = true;
			isOption = true;
			myLog.warn("Preserve NUPN tool specific info in native mode enabled");
		} else {
			isPreserveNupnNative = false;
			msg.append(
					"Preserving NUPN tool specific info in native mode not set. Default is false. If you want to preserve NUPN tool specific info right from the beginning, then invoke this program with ")
					.append(PRESERVE_NUPN_NATIVE).append(" property like so: java -D").append(PRESERVE_NUPN_NATIVE)
					.append("=true [JVM OPTIONS] -jar ...");
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
		String src, dest;
		pff = new PNMLFilenameFilter();
		dff = new DirFileFilter();
		for (String s : args) {
			srcf = new File(s);
			if (srcf.isFile()) {
				pathSrc.add(s);
				pathDest.add(s.substring(0, s.lastIndexOf(DOT) + 1).concat(NUPN));
			} else if (srcf.isDirectory()) {
				srcFiles = extractSrcFiles(srcf, pff, dff);
				for (File f : srcFiles) {
					src = f.getCanonicalPath();
					pathSrc.add(src);
					dest = src.substring(0, src.lastIndexOf(DOT) + 1) + NUPN;
					pathDest.add(dest);
				}
			}
		}
	}

	private static File[] extractSrcFiles(File srcf, PNMLFilenameFilter pff, DirFileFilter dff) {
		List<File> res = new ArrayList<File>();

		// filter PNML files
		File[] pfiles = srcf.listFiles(pff);
		res.addAll(Arrays.asList(pfiles));

		// filter directories
		pfiles = srcf.listFiles(dff);
		if (pfiles != null) {
			for (File f : pfiles) {
				res.addAll(Arrays.asList(extractSrcFiles(f, pff, dff)));
			}
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
		MainPNML2NUPN.isDebug = isDebug;
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
	public static synchronized boolean isForceNUPNGen() {
		return isForceNUPNGen;
	}

	/**
	 * Returns true if unit safeness checking is enabled (default), false otherwise.
	 * 
	 * @return
	 */
	public static synchronized boolean isUnitSafenessChecking() {
		return isUnitSafeChecking;
	}
	
	/**
	 * Returns true if unit safeness checking only is enabled, false otherwise (default).
	 * 
	 * @return
	 */
	public static synchronized boolean isUnitSafenessCheckingOnly() {
		return isUnitSafeCheckingOnly;
	}

	/**
	 * Returns true if user has asked for the removal of transitions of unsafe
	 * arcs (incoming or outgoing).
	 * 
	 * @return
	 */
	public static synchronized boolean isRemoveTransUnsafeArcs() {
		return isRemoveTransUnsafeArcs;
	}

	/**
	 * Returns true if user has asked for the generation of unsafe BPN
	 * (structural.bpn) in the case of unsafe initial place(s) or arc(s).
	 * 
	 * @return
	 */
	public static synchronized boolean isGenerateUnsafe() {
		return isGenerateUnsafe;
	}

	public static boolean isPreserveNupnMix() {
		return isPreserveNupnMix;
	}
	
	public static boolean isPreserveNupnNative() {
		return isPreserveNupnNative;
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
		if (MainPNML2NUPN.isDebug) {
			e.printStackTrace();
		}
	}
}
