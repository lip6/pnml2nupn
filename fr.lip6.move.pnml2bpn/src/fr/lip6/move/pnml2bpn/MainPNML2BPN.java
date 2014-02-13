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

	private static final String BPN_EXT = ".bpn";
	private static final String PNML_EXT = ".pnml";
	private static final String PNML2BPN_DEBUG = "PNML2BPN_DEBUG";
	private static final String CAMI_TMP_DELETE = "cami.tmp.delete";
	private static final String FORCE_BPN_GENERATION = "force.bpn.generation";
	private static boolean isDebug;

	private static List<String> pathDest;
	private static List<String> pathSrc;
	private static PNMLFilenameFilter pff;
	private static DirFileFilter dff;
	private static boolean isCamiTmpDelete;
	private static boolean isForceBPNGen;
	
	private MainPNML2BPN() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		org.slf4j.Logger myLog = LoggerFactory.getLogger(MainPNML2BPN.class
				.getCanonicalName());
		StringBuilder msg = new StringBuilder();
		if (args.length < 1) {
			myLog.error("At least the path to one PNML P/T file is expected. You may provide a file, a directory, or a mix of several of these.");
			return;
		}
		// Debug mode?
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
		// Keep Cami property
		String remove = System.getProperty(CAMI_TMP_DELETE);
		if (remove != null && !Boolean.valueOf(remove)) {
			isCamiTmpDelete = false;
		} else {
			isCamiTmpDelete = true;
			msg.append(
					"Keeping temporary Cami file property not set. If you want to keep temporary Cami file then invoke this program with ")
					.append(CAMI_TMP_DELETE)
					.append(" property like so: java -D")
					.append(CAMI_TMP_DELETE).append("=false [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		}
		// Force BPN generation property
		String forceBpnGen = System.getProperty(FORCE_BPN_GENERATION);
		if (forceBpnGen != null && !Boolean.valueOf(forceBpnGen)) {
			isForceBPNGen = false;
		} else {
			isForceBPNGen = true;
			msg.append(
					"Forcing BPN generation not set. If you want to force BPN generation for non 1-Safe nets, then invoke this program with ")
					.append(FORCE_BPN_GENERATION)
					.append(" property like so: java -D")
					.append(FORCE_BPN_GENERATION).append("=false [JVM OPTIONS] -jar ...");
			myLog.warn(msg.toString());
			msg.delete(0, msg.length());
		}
		
		try {
			extractSrcDestPaths(args);
		} catch (IOException e1) {
			myLog.error("Could not successfully extract all source files paths. See log.");
			myLog.error(e1.getMessage());
			if(MainPNML2BPN.isDebug) {
				e1.printStackTrace();
			}
			
		}
		long startTime = System.nanoTime();
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
		LoggerContext loggerContext = (LoggerContext) LoggerFactory
				.getILoggerFactory();
		loggerContext.stop();
		if (error) {
			System.exit(-1);
		}
	}

	/**
	 * Extracts PNML files (scans directories recursively) from command-line arguments.
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
					pathDest.add(f.getCanonicalPath().replaceAll(PNML_EXT, BPN_EXT));
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
		for (File f: pfiles) {
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

	public static boolean isDebug() {
		return isDebug;
	}

	public static synchronized void setDebug(boolean isDebug) {
		MainPNML2BPN.isDebug = isDebug;
	}
	
	public static synchronized boolean isCamiTmpDelete() {
		return isCamiTmpDelete;
	}
	
	public static synchronized boolean isForceBPNGen() {
		return isForceBPNGen;
	}

	public static synchronized void printStackTrace(Exception e) {
		if (MainPNML2BPN.isDebug) {
			e.printStackTrace();
		}
	}

}
