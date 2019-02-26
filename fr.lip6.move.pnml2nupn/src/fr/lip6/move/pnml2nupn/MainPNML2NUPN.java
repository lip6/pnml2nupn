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
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import fr.lip6.move.pnml2nupn.exceptions.EarlyStopException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidPNMLTypeException;
import fr.lip6.move.pnml2nupn.exceptions.PNMLImportExportException;
import fr.lip6.move.pnml2nupn.export.PNML2NUPNExporter;
import fr.lip6.move.pnml2nupn.export.PNML2NUPNFactory;
import fr.lip6.move.pnml2nupn.utils.PNML2NUPNUtils;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

/**
 * Main class for command-line invocation.
 * 
 */
public final class MainPNML2NUPN {

	private static final String APPPROP_FILE_NAME="application.properties";
	private static final String OPTPROP_FILE_NAME="options.properties";
	private static final String EXCLOPTPROP_FILE_NAME="options-exclusive.properties";
	private static final String OPTDESCPROP_FILE_NAME = "optionsdescription.properties";
	public static final String DOT = ".";
	public static final String NL = "\n";
	public static final String EQ = "=";
	public static final String WS = " ";
	public static final String COLWS = ":" + WS;
	public static final String WSDASH = " -";
	public static final String XP = "!";
	public static final String TOOL_NAME_PROP = "mytool.name";
	public static final String TOOL_VERSION_PROP = "mytool.version";
	public static final String CREATOR = "creator";
	public static final String UNIT_SAFE = "unit_safe";
	public static final String BOUNDS = "cosyverif/bounds 1.0";
	public static final String PRAGMA_CREATOR_PREFIX = XP + CREATOR + WS;
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
	/**
	 * Application properties
	 */
	private static Properties appProperties;
	/**
	 * Command line options and their description
	 */
	private static Properties optProperties;
	private static Properties exclusiveOptProperties;
	private static Properties optDescProperties;
	private static Object2BooleanOpenHashMap<String> optionsMap;

	private static boolean isDebug;

	private static List<String> pathDest;
	private static List<String> pathSrc;
	private static PNMLFilenameFilter pff;
	private static DirFileFilter dff;

	private static boolean error;
	private static org.slf4j.Logger myLog ;
	

	private MainPNML2NUPN() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.nanoTime();
		myLog = LoggerFactory.getLogger(MainPNML2NUPN.class.getCanonicalName());
		StringBuilder msg = new StringBuilder();
		error = false;
		if (args.length < 1) {
			myLog.error("The path to at least one PNML P/T file is expected. You may provide a file, a directory, or a mix of several of these.");
			return;
		}
		loadProperties();
		initCommandLineOptions();
		loadActualCommandLineOptions(msg);
		try {
			extractSrcDestPaths(args);
		} catch (IOException e1) {
			myLog.error("Could not successfully extract all source files paths. See log.");
			myLog.error(e1.getMessage());
			PNML2NUPNUtils.printStackTrace(e1);
		}
		initSignatureMessage();
		PNML2NUPNExporter pe = PNML2NUPNFactory.instance().createExporter();
		org.slf4j.Logger jr = LoggerFactory.getLogger(pe.getClass().getCanonicalName());
		// TODO : optimize with threads
		for (int i = 0; i < pathSrc.size(); i++) {
			try {
				// Option exclusive of the others
				if (optionsMap.getBoolean(HAS_UNSAFE_ARCS)) {
					pe.hasUnsafeArcs(pathSrc.get(i), pathDest.get(i), jr);
				} else {
					pe.export2NUPN(new File(pathSrc.get(i)), new File(pathDest.get(i)), jr);
				}
			} catch (PNMLImportExportException | InterruptedException | IOException | InvalidPNMLTypeException | EarlyStopException e) {
				myLog.error(e.getMessage());
				PNML2NUPNUtils.printStackTrace(e);
				error |= true;
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
		myLog.info("The translation took {} seconds.", (endTime - startTime) / 1.0e9);
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.stop();
		if (error) {
			System.exit(-1);
		}
	}

	private static void loadActualCommandLineOptions(StringBuilder msg) {
		// Debug mode?
		checkDebugMode(myLog, msg);
		optProperties.keySet().stream().map(p-> (String)p).sorted().forEachOrdered(p -> {
			checkCmdlineOption(p, msg, Boolean.valueOf(exclusiveOptProperties.getProperty(p)));
		});
	}

	private static void initCommandLineOptions() {
		optionsMap = new Object2BooleanOpenHashMap<>();
		optionsMap.defaultReturnValue(false);
	}

	private static void loadProperties() {
		appProperties = new Properties();
		optProperties = new Properties();
		exclusiveOptProperties = new Properties();
		optDescProperties = new Properties();
		try {
			appProperties.load(MainPNML2NUPN.class.getResourceAsStream(APPPROP_FILE_NAME));
			optProperties.load(MainPNML2NUPN.class.getResourceAsStream(OPTPROP_FILE_NAME));
			exclusiveOptProperties.load(MainPNML2NUPN.class.getResourceAsStream(EXCLOPTPROP_FILE_NAME));
			optDescProperties.load(MainPNML2NUPN.class.getResourceAsStream(OPTDESCPROP_FILE_NAME));
		} catch (IOException ex) {
			myLog.error("Could not get access to the properties file in the classpath.");
			myLog.error(ex.getMessage());
			PNML2NUPNUtils.printStackTrace(ex);
			error |= true;
		}
	}

	private static void initSignatureMessage() {
		signatureMesg = new StringBuilder();
		signatureMesg.append(COLWS).append("generated by ").append(appProperties.getProperty(TOOL_NAME_PROP))
		.append(" version ").append(appProperties.getProperty(TOOL_VERSION_PROP));
		signatureMesg.append(" with options");
		for (String key: optionsMap.keySet().stream().sorted().collect(Collectors.toList())) {
			signatureMesg.append(WSDASH).append(key).append(EQ).append(optionsMap.getBoolean(key));
		}
	}
	
	private static void checkCmdlineOption(String option, StringBuilder msg, boolean isExclusiveOption) {
		PNML2NUPNUtils.debug("Checking option {}", myLog, option);
		String optionStr = System.getProperty(option);
		String optionDesc = optDescProperties.getProperty(option);
		if (optionStr != null && Boolean.valueOf(optionStr)) {
			optionsMap.put(option, true);
			myLog.info("Option '{}' enabled", optionDesc);
		} else {
			optionsMap.put(option, false);
			msg.append("Option ").append("'").append(optionDesc).append("'").append(" not set.")
			 .append(" Default is false. If you want to enable that option, then invoke this program with the ")
			 .append(" corresponding property like so: java -D").append(option)
			 .append("=true [JVM OPTIONS] -jar ...");
			myLog.info(msg.toString());
			msg.delete(0, msg.length());
		}
		if (isExclusiveOption) {
			myLog.info("When set, this option ({}) disables all the others (therefore I will ignore the other options.)", option);
		}
	}
	
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
			myLog.info(msg.toString());
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
		return optionsMap.getBoolean(CAMI_TMP_KEEP);
	}

	/**
	 * Returns true if BPN generation is forced, even if the net is not 1-safe.
	 * 
	 * @return
	 */
	public static synchronized boolean isForceNUPNGen() {
		return optionsMap.getBoolean(FORCE_NUPN_GENERATION);
	}

	/**
	 * Returns true if unit safeness checking is enabled (default), false otherwise.
	 * 
	 * @return
	 */
	public static synchronized boolean isUnitSafenessChecking() {
		return optionsMap.getBoolean(UNIT_SAFENESS_CHECKING);
	}

	/**
	 * Returns true if unit safeness checking only is enabled, false otherwise (default).
	 * 
	 * @return
	 */
	public static synchronized boolean isUnitSafenessCheckingOnly() {
		return optionsMap.getBoolean(UNIT_SAFENESS_CHECKING_ONLY);
	}

	public static boolean isPreserveNupnMix() {
		return optionsMap.getBoolean(PRESERVE_NUPN_MIX);
	}

	public static boolean isPreserveNupnNative() {
		return optionsMap.getBoolean(PRESERVE_NUPN_NATIVE);
	}

	public static synchronized void appendMesgLineToSignature(String msg) {
		signatureMesg.append(NL).append(COLWS).append(msg);
	}

	public static String getPragmaCreator() {
		StringBuilder pragmaCreator = new StringBuilder();
		pragmaCreator.append(PRAGMA_CREATOR_PREFIX).append(appProperties.getProperty(TOOL_NAME_PROP))
		.append(WS).append(appProperties.getProperty(TOOL_VERSION_PROP));
		return pragmaCreator.toString();
	}
}
