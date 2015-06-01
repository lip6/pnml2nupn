/**
 *  Copyright 2014-2015 Université Paris Ouest and Sorbonne Universités,
 * 							Univ. Paris 06 - CNRS UMR
 * 							7606 (LIP6)
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.sun.org.apache.regexp.internal.recompile;

import fr.lip6.move.pnml.cpnami.cami.CamiFactory;
import fr.lip6.move.pnml.cpnami.cami.Runner;
import fr.lip6.move.pnml.cpnami.cami.model.Ca;
import fr.lip6.move.pnml.cpnami.exceptions.CamiException;
import fr.lip6.move.pnml2nupn.MainPNML2NUPN;
import fr.lip6.move.pnml2nupn.exceptions.PNMLImportExportException;

public final class SafePNChecker {

	private static final String SEP = ", ";
	private static final String CAMI_EXT = ".cami";
	private static final String PNML_EXT = ".pnml";
	private static final String UNKNOWN_CAMI_COMMAND = "Unknown CAMI command";
	private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
	private static final String TMP_DIR = System.getProperty("user.dir") + "/tmp/";
	private static final int ONE = 1;
	private static final int ZERO = 0;
	private static final String UNSAFE_PLACES_NB_REPORT = "unsafe.places.nb.report";
	private static String P2C_OPT = "-p2c";
	private static String BOUNDS = "bounds";
	private static String EXE = ".exe";
	private static String BOUNDS_INPACKAGE = "main/resources/bounds";
	private static String BOUNDS_PATTERN = "(.*)\\s\\[(\\d+).{3}(\\d+|o{2})\\]$";

	private String pnmlDocPath;
	private StringBuffer explain;
	private Timer timer;
	private int boundsExitValue;
	private org.slf4j.Logger log;

	public SafePNChecker(String pnmlDocumentPath) {
		this.setPnmlDocPath(pnmlDocumentPath);
		boundsExitValue = -99;
		initLog();
	}

	public SafePNChecker() {
		this.pnmlDocPath = null;
		initLog();
	}
	
	public String getPnmlDocPath() {
		return pnmlDocPath;
	}

	public void setPnmlDocPath(String pnmlDocPath) {
		this.pnmlDocPath = pnmlDocPath;
	}

	public boolean isNet1Safe() throws PNMLImportExportException {
		boolean res = false;
		if (pnmlDocPath != null) {
			File camiFile = transformPnml2Cami();
			File tmpBoundFile = createJavaTmpBoundsFile();
			try {
				PNML2NUPNUtils.extractFile(BOUNDS_INPACKAGE, tmpBoundFile.getCanonicalPath());
				tmpBoundFile.setExecutable(true);
				res = checkNetIs1Safe(camiFile, tmpBoundFile);

			} catch (IOException | ExecutionException e) {
				throw new PNMLImportExportException(e.getMessage(), e.getCause());
			} finally {
				deleteCamiFile(camiFile);
				cleanTmp(tmpBoundFile);
			}
		}

		return res;
	}
	
	public String getExplanation() {
		return explain.toString();
	}

	private void initLog() {
		log = LoggerFactory.getLogger(SafePNChecker.class.getCanonicalName());
		explain = new StringBuffer();
	}
	
	private void deleteCamiFile(File camiFile) {
		if (MainPNML2NUPN.isCamiTmpDelete()) {
			camiFile.delete();
		}
	}

	private boolean checkNetIs1Safe(File camiFile, File tmpBoundFile) throws IOException, ExecutionException {
		boolean totalRes = true;
		boolean ligneRes;
		List<String> command = new ArrayList<String>();
		command.add(tmpBoundFile.getCanonicalPath());
		command.add(camiFile.getCanonicalPath());
		command.add("-i");
		int nbUnsafePlaces = 0; // -1 means all unsafe places must be reported
		String unsafeArcs = System.getProperty(UNSAFE_PLACES_NB_REPORT);
		if (unsafeArcs != null) {
			try {
				nbUnsafePlaces = Integer.valueOf(unsafeArcs).intValue();
			} catch (NumberFormatException e) {
				log.error("Could not convert nb of unsafe places to report from the input string");
			}
		}
		String line;
		Matcher m;
		long startTime = System.nanoTime();
		log.info("Launching the Bounds tool.");
		ProcessBuilder pb = new ProcessBuilder(command);
		Process pr = pb.start();
		InputStream stdOut = pr.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(stdOut)));
		Pattern p = Pattern.compile(BOUNDS_PATTERN);
		line = br.readLine();
		if (line != null && UNKNOWN_CAMI_COMMAND.equalsIgnoreCase(line)) {
			totalRes = false;
		} else if (line != null) {
			do {
				m = p.matcher(line);
				if (m.matches()) {
					ligneRes = (checkBoundIsZeroOrOne(m.group(2)) && checkBoundIsZeroOrOne(m.group(3)));
					totalRes &= ligneRes;
					if (!ligneRes) {
						if (nbUnsafePlaces > 0) {
							explain.append(line).append(SEP);
							nbUnsafePlaces--;
						} else if (nbUnsafePlaces == -1) {
							explain.append(line).append(SEP);
						}
					}
				}
			} while ((line = br.readLine()) != null);
		}
		br.close();
		try {
			// wait for finishing bounds executable during a determined amount
			// of time.
			/*
			 * timer = new Timer(); TimerTask tt = new
			 * StopWaitingTask(Thread.currentThread()); timer.schedule(tt,
			 * WAITING_TIME);
			 */
			boundsExitValue = pr.waitFor();
			/*
			 * tt.cancel(); timer.cancel();
			 */
			long endTime = System.nanoTime();
			log.info("The Bounds tool took {} seconds to perform the check.", (endTime - startTime) / 1.0e9);
		} catch (InterruptedException e) {
			// Thread.currentThread().interrupt();
			totalRes = false;
			throw new ExecutionException("Interrupted while waiting for the Bounds tool to complete", e.getCause());
		}
		if (explain.length() != 0) {
			explain.delete(explain.length() - 2, explain.length());
		}
		return totalRes;
	}

	class StopWaitingTask extends TimerTask {
		Thread parent;

		public StopWaitingTask(Thread launcher) {
			parent = launcher;
		}

		@Override
		public void run() {
			if (boundsExitValue == -99) {
				timer.cancel();
				parent.interrupt();
			}
		}
	}

	private boolean checkBoundIsZeroOrOne(String bound) {
		return checkBoundIsZero(bound) || checkBoundIsOne(bound);
	}

	private boolean checkBoundIsZero(String bound) {
		try {
			int val = Integer.valueOf(bound).intValue();
			return val == ZERO;
		} catch (NumberFormatException e) {
			if (MainPNML2NUPN.isDebug())
				log.warn("While checking a bound is equal to 0: could not convert bound string {} to a number.", bound);
			return false;
		}
	}

	private boolean checkBoundIsOne(String bound) {
		try {
			int val = Integer.valueOf(bound).intValue();
			return val == ONE;
		} catch (NumberFormatException e) {
			if (MainPNML2NUPN.isDebug())
				log.warn("While checking a bound is equal to 1: could not convert bound string {} to a number.", bound);
			return false;
		}
	}

	private File createJavaTmpBoundsFile() {
		File tmpExeFile = null;
		boolean successful = true;
		String tmp = System.getProperty(JAVA_IO_TMPDIR);
		if (tmp != null) {
			try {
				tmpExeFile = File.createTempFile(BOUNDS, EXE);
			} catch (IOException e) {
				successful = false;
				e.printStackTrace();
			}

		} else { // default behaviour, will work on Linux and Mac only.
			tmp = TMP_DIR;

			successful = false;
		}
		if (!successful) {
			File tmpDir = new File(tmp);
			if (!tmpDir.exists()) {
				tmpDir.mkdir();
			}
			tmpExeFile = new File(tmp + BOUNDS);
		}

		return tmpExeFile;
	}

	@SuppressWarnings("unused")
	private File createSysTmpBoundsFile() {
		// default behaviour, will work on Linux and Mac.
		File dir = new File(TMP_DIR);
		if (!dir.exists()) {
			dir.mkdir();
		}
		File tmpExeFile = new File(TMP_DIR + BOUNDS + EXE);
		return tmpExeFile;
	}

	private void cleanTmp(File f) {
		f.delete();
		File tmpDir = new File(TMP_DIR);
		if (tmpDir.exists()) {
			tmpDir.delete();
		}
	}

	/**
	 * Translates PNML P/T document into Cami The path to the PNML document must
	 * be set with {@link #setPnmlDocPath(String)}, or passed to the
	 * constructor.
	 * 
	 * @throws PNMLImportExportException
	 * 
	 */
	private File transformPnml2Cami() throws PNMLImportExportException {
		String[] arg = new String[] { P2C_OPT, pnmlDocPath };

		final Runner camiRunner = CamiFactory.SINSTANCE.createRunner();
		try {
			camiRunner.run(arg);
			// successful get the path to cami file
			int pnmlLI = pnmlDocPath.lastIndexOf(PNML_EXT);
			String camiFilePath = pnmlDocPath.substring(0, pnmlLI) + CAMI_EXT;
			File camiFile = new File(camiFilePath);
			if (!camiFile.exists()) {
				throw new CamiException("Transformation to Cami finished, but Cami file does not exist: "
						+ camiFile.getCanonicalPath());
			}
			return camiFile;
		} catch (CamiException | IOException e) {
			throw new PNMLImportExportException(e.getMessage(), e.getCause());
		}
	}

	public static void main(String[] args) {
		// PNML2BPNUtils.extractFile("main/resources/bounds",
		// "resources/bounds");
		Pattern p = Pattern.compile(BOUNDS_PATTERN);
		Matcher m = p.matcher(args[0] + " " + args[1]);
		System.out.println(args[0] + " " + args[1]);
		boolean b = m.matches();
		if (b) {
			System.out.println("Matches!");
			System.out.println(m.group(2) + "..." + m.group(3));
		} else {
			System.out.println("Does not match!");
		}
	}

}
