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
package fr.lip6.move.pnml2nupn.export.impl;

import it.unimi.dsi.fastutil.ints.IntBigArrayBigList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.bind.ValidationException;

import org.slf4j.Logger;

import com.ximpleware.extended.AutoPilotHuge;
import com.ximpleware.extended.NavExceptionHuge;
import com.ximpleware.extended.ParseExceptionHuge;
import com.ximpleware.extended.VTDGenHuge;
import com.ximpleware.extended.VTDNavHuge;
import com.ximpleware.extended.XMLMemMappedBuffer;
import com.ximpleware.extended.XPathEvalExceptionHuge;
import com.ximpleware.extended.XPathParseExceptionHuge;

import fr.lip6.move.pnml2nupn.MainPNML2NUPN;
import fr.lip6.move.pnml2nupn.exceptions.EarlyStopException;
import fr.lip6.move.pnml2nupn.exceptions.InternalException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidNetException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidPNMLTypeException;
import fr.lip6.move.pnml2nupn.exceptions.InvalidSafeNetException;
import fr.lip6.move.pnml2nupn.exceptions.PNMLImportExportException;
import fr.lip6.move.pnml2nupn.export.PNMLExporter;
import fr.lip6.move.pnml2nupn.utils.PNML2NUPNUtils;
import fr.lip6.move.pnml2nupn.utils.SafePNChecker;

/**
 * Actual PNML 2 BPN exporter.
 * 
 * @author lom
 * 
 */
public final class PNML2NUPNExporter implements PNMLExporter {

	private static final String TEXT = "text";
	private static final String TRANS_EXT = ".trans";
	private static final String STATES_EXT = ".places";
	private static final String UNSAFE_ARC = ".unsafe.arcs";
	private static final String STOP = "STOP";
	private static final String CANCEL = "CANCEL";
	private static final String NL = "\n";
	private static final String HK = "#";
	private static final String PLACES = "places";
	private static final String UNITS = "units";
	private static final String U = "U";
	private static final String INIT_PLACE = "initial place";
	private static final String INIT_PLACES = "initial places";
	private static final String ROOT_UNIT = "root unit";
	private static final String TRANSITIONS = "transitions";
	private static final String T = "T";
	private static final String WS = " ";
	private static final String ZERO = "0";
	private static final String ONE = "1";
	private static final String DOTS = "...";
	private static final String COMMA = ",";
	private static final String COMMAWS = COMMA + WS;
	private static final String STRUCTURE = "structure";

	private Logger log = null;

	private Object2LongOpenHashMap<String> placesId2bpnMap = null;

	private Object2LongOpenHashMap<String> trId2bpnMap = null;
	private Long2ObjectOpenHashMap<LongBigArrayBigList> tr2OutPlacesMap = null;
	private Long2ObjectOpenHashMap<LongBigArrayBigList> tr2InPlacesMap = null;
	private Object2ObjectOpenHashMap<String, IntBigArrayBigList> tr2InUnsafeArcsMap = null;
	private Object2ObjectOpenHashMap<String, IntBigArrayBigList> tr2OutUnsafeArcsMap = null;
	private ObjectSet<String> unsafeNodes = null;
	private Object2IntOpenHashMap<String> unsafeArcsMap = null;

	private File currentInputFile = null;
	private SafePNChecker spnc = null;
	private long nbUnsafeArcs, nbUnsafePlaces, nbUnsafeTrans;
	// For places id count
	private long iDCount;
	private boolean unsafePlaces, unsafeArcs, unsafeTrans;
	/* For the NuPN file */
	private BlockingQueue<String> nupnQueue = null;
	/* For Transitions mapping NuPN - PNML */
	private BlockingQueue<String> tsQueue = null;
	/* For Places mapping NuPN - PNML */
	private BlockingQueue<String> psQueue = null;
	/* For unsafe arcs */
	private BlockingQueue<String> uaQueue = null;
	private OutChannelBean ocbBpn = null;
	private OutChannelBean ocbTs = null;
	private OutChannelBean ocbPs = null;
	private OutChannelBean ocbUA = null;
	private File outTSFile = null;
	private File outPSFile = null;
	private File outUAFile = null;

	public PNML2NUPNExporter() {
		spnc = new SafePNChecker();
	}

	@Override
	public void exportPNML(URI inFile, URI outFile, Logger journal) throws PNMLImportExportException,
			InterruptedException, IOException {
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public void exportPNML(File inFile, File outFile, Logger journal) throws PNMLImportExportException,
			InterruptedException, IOException {
		initLog(journal);
		export(inFile, outFile, journal);
	}

	@Override
	public void exportPNML(String inFile, String outFile, Logger journal) throws PNMLImportExportException,
			InterruptedException, IOException {
		initLog(journal);
		export(new File(inFile), new File(outFile), journal);
	}

	@Override
	public void hasUnsafeArcs(String inFile, String outFile, Logger journal) throws InvalidPNMLTypeException,
			IOException, PNMLImportExportException {
		checkHasUnsafeArcs(new File(inFile), new File(outFile), journal);

	}

	private void checkHasUnsafeArcs(File inFile, File outFile, Logger journal) throws InvalidPNMLTypeException,
			IOException, PNMLImportExportException {
		XMLMemMappedBuffer xb = new XMLMemMappedBuffer();
		VTDGenHuge vg = new VTDGenHuge();
		long nbUnsArcs = 0L;
		initLog(journal);
		try {
			xb.readFile(inFile.getCanonicalPath());
			vg.setDoc(xb);
			vg.parse(true);
			VTDNavHuge vn = vg.getNav();
			AutoPilotHuge ap = new AutoPilotHuge(vn);
			log.info("Checking it is a PT Net.");
			if (!isPTNet(ap, vn)) {
				throw new InvalidPNMLTypeException(
						"The contained Petri net(s) in the following file is not a P/T Net. Only P/T Nets are supported: "
								+ inFile.getCanonicalPath());
			}
			outUAFile = new File(PNML2NUPNUtils.extractBaseName(outFile.getCanonicalPath()) + UNSAFE_ARC);
			ocbUA = PNML2NUPNUtils.openOutChannel(outUAFile);
			uaQueue = initQueue();
			Thread uaWriter = startWriter(ocbUA, uaQueue);

			// Check inscriptions > 1
			ap.resetXPath();
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.UNSAFE_ARCS);
			StringBuilder unsafeArcsId = new StringBuilder();
			int val;
			String id, src, trg;
			while ((ap.evalXPath()) != -1) {
				vn.push();
				vn.toElement(VTDNavHuge.FIRST_CHILD);
				while (!vn.matchElement(TEXT)) {
					vn.toElement(VTDNavHuge.NEXT_SIBLING);
				}
				val = Integer.parseInt(vn.toString(vn.getText()).trim());
				vn.toElement(VTDNavHuge.PARENT);
				vn.toElement(VTDNavHuge.PARENT);
				id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
				if (id != null) {
					src = vn.toString(vn.getAttrVal(PNMLPaths.SRC_ATTR));
					trg = vn.toString(vn.getAttrVal(PNMLPaths.TRG_ATTR));
					unsafeArcsId.append(src + WS + id + WS + trg + WS + HK + val + NL);
					uaQueue.put(unsafeArcsId.toString());
					nbUnsArcs++;
				}
				vn.pop();
				unsafeArcsId.delete(0, unsafeArcsId.length());
			}
			if (nbUnsArcs > 0) {
				journal.warn("There are {} unsafe arcs in this net.", nbUnsArcs);
			} else {
				log.info("There are no unsafe arcs in this net.");
			}
			stopWriter(uaQueue);
			uaWriter.join();
			closeChannel(ocbUA);
			if (nbUnsArcs > 0) {
				log.info("See unsafe arcs files: {}", outUAFile.getCanonicalPath());
			} else {
				outUAFile.delete();
			}
		} catch (ParseExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge | NavExceptionHuge
				| InterruptedException e) {
			try {
				emergencyStop(outFile);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			throw new PNMLImportExportException(e);
		}
	}

	private void export(File inFile, File outFile, Logger journal) throws PNMLImportExportException,
			InterruptedException, IOException {
		initLog(journal);
		try {
			this.currentInputFile = inFile;
			journal.info("Checking preconditions on input file format: {} ", inFile.getCanonicalPath());
			PNML2NUPNUtils.checkIsPnmlFile(inFile);
			log.info("Exporting into NUPN: {}", inFile.getCanonicalPath());
			translateIntoNUPN(inFile, outFile, journal);
		} catch (ValidationException | fr.lip6.move.pnml2nupn.exceptions.InvalidFileTypeException
				| fr.lip6.move.pnml2nupn.exceptions.InvalidFileException | InternalException | InvalidPNMLTypeException e) {
			throw new PNMLImportExportException(e);
		} catch (IOException e) {
			throw e;
		}

	}

	private void translateIntoNUPN(File inFile, File outFile, Logger journal) throws InvalidPNMLTypeException,
			InterruptedException, PNMLImportExportException, IOException {
		XMLMemMappedBuffer xb = new XMLMemMappedBuffer();
		VTDGenHuge vg = new VTDGenHuge();
		boolean isSafe = false;
		try {
			xb.readFile(inFile.getCanonicalPath());
			vg.setDoc(xb);
			vg.parse(true);

			VTDNavHuge vn = vg.getNav();

			AutoPilotHuge ap = new AutoPilotHuge(vn);
			log.info("Checking it is a PT Net.");
			if (!isPTNet(ap, vn)) {
				throw new InvalidPNMLTypeException(
						"The Petri net in the following file is not a P/T Net. Only P/T Nets are supported: "
								+ this.currentInputFile.getCanonicalPath());
			}
			// The net must be 1-safe, if bounds checking is enabled.
			if (MainPNML2NUPN.isUnitSafenessChecking()) {
				log.info("Checking the net is 1-Safe.");
				if (!(isSafe = isNet1Safe())) {
					if (MainPNML2NUPN.isForceNUPNGen() && !MainPNML2NUPN.isUnitSafenessCheckingOnly()) {
						journal.warn(
								"The net in the submitted document is not 1-safe, but forced NUPN generation is set: {}",
								this.currentInputFile.getCanonicalPath());
						journal.warn("\nUNSAFE PLACES: {}", generateUnsafePlacesReport());
						journal.warn("Continuing NUPN generation.");
					} else {
						journal.error("The net in the submitted document is not 1-safe (according to the Bounds tool): "
								+ this.currentInputFile.getCanonicalPath());
						journal.error("\nUNSAFE PLACES: {}", generateUnsafePlacesReport());
						if (!MainPNML2NUPN.isUnitSafenessCheckingOnly()) {
							throw new InvalidSafeNetException("Unsafe net in "
									+ this.currentInputFile.getCanonicalPath());
						}
					}
				} else {
					log.info("Submitted Net appears to be 1-Safe (according to the Bounds tool): {}",
							this.currentInputFile.getCanonicalPath());
				}
				if (MainPNML2NUPN.isUnitSafenessCheckingOnly()) {
					journal.info("Unit safeness checking only requested. Will stop here.");
					throw new EarlyStopException("Unit safeness checking only on "
							+ this.currentInputFile.getCanonicalPath());
				}
			} else {
				log.warn("Bounds checking is disabled. I don't know if the net is 1-Safe.");
			}
			// Open NUPN and mapping files channels, and init write queues
			outTSFile = new File(PNML2NUPNUtils.extractBaseName(outFile.getCanonicalPath()) + TRANS_EXT);
			outPSFile = new File(PNML2NUPNUtils.extractBaseName(outFile.getCanonicalPath()) + STATES_EXT);
			// Channels for NuPN, transitions and places id mapping
			ocbBpn = PNML2NUPNUtils.openOutChannel(outFile);
			ocbTs = PNML2NUPNUtils.openOutChannel(outTSFile);
			ocbPs = PNML2NUPNUtils.openOutChannel(outPSFile);
			// Queues for NUPN, transitions and places id mapping
			nupnQueue = initQueue();
			tsQueue = initQueue();
			psQueue = initQueue();

			// Start writers
			Thread nupnWriter = startWriter(ocbBpn, nupnQueue);
			Thread tsWriter = startWriter(ocbTs, tsQueue);
			Thread psWriter = startWriter(ocbPs, psQueue);

			// Insert creator pragma (since 1.3.0)
			insertCreatorPragma(nupnQueue);
			// Insert unit_safe pragma if necessary (since 1.4.1)
			if (MainPNML2NUPN.isUnitSafenessChecking() && isSafe) {
				insertUnitSafePragma(nupnQueue);
			}

			// Init data type for places id and transitions
			initPlacesMap();
			initUnsafeArcsMap();
			initTransitionsMaps();
			initUnsafeTransMaps();

			// export places
			log.info("Exporting places.");
			exportPlacesIntoUnits(ap, vn, nupnQueue, psQueue);

			// export transitions
			log.info("Exporting transitions.");
			// exportTransitions(ap, vn, bpnQueue, tsQueue);
			exportTransitions130(ap, vn, nupnQueue);

			// Stop Writers
			stopWriters(nupnQueue, tsQueue, psQueue);
			// stopWriter(uaQueue);
			nupnWriter.join();
			tsWriter.join();
			psWriter.join();
			// Close channels
			closeChannels(ocbBpn, ocbTs, ocbPs);
			// clear maps
			clearAllCollections();
			log.info("See NUPN and mapping files: {}, {} and {}", outFile.getCanonicalPath(),
					outTSFile.getCanonicalPath(), outPSFile.getCanonicalPath());
		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge | ParseExceptionHuge
				| InvalidSafeNetException | InternalException | InvalidNetException | EarlyStopException e) {
			emergencyStop(outFile);
			throw new PNMLImportExportException(e);
		} catch (InterruptedException e) {
			emergencyStop(outFile);
			throw e;
		} catch (IOException e) {
			emergencyStop(outFile);
			throw e;
		}
	}

	private void insertUnitSafePragma(BlockingQueue<String> nupnQueue2) throws InterruptedException {
		insertPragma(MainPNML2NUPN.PRAGMA_UNIT_SAFE + NL, nupnQueue);
	}

	private void insertCreatorPragma(BlockingQueue<String> nupnQueue) throws InterruptedException {
		insertPragma(MainPNML2NUPN.PRAGMA_CREATOR + NL, nupnQueue);
	}

	private void insertPragma(String pragma, BlockingQueue<String> nupnQueue) throws InterruptedException {
		nupnQueue.put(pragma);
	}

	/**
	 * @param journal
	 */
	private void initLog(Logger journal) {
		this.log = journal;
	}

	private BlockingQueue<String> initQueue() {
		BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
		return queue;
	}

	/**
	 * Initializes internal data structures for transitions.
	 */
	private void initTransitionsMaps() {
		if (trId2bpnMap == null) {
			trId2bpnMap = new Object2LongOpenHashMap<String>();
			trId2bpnMap.defaultReturnValue(-1L);
		}
		if (tr2InPlacesMap == null) {
			tr2InPlacesMap = new Long2ObjectOpenHashMap<>();
			tr2InPlacesMap.defaultReturnValue(null);
		}
		if (tr2OutPlacesMap == null) {
			tr2OutPlacesMap = new Long2ObjectOpenHashMap<>();
			tr2OutPlacesMap.defaultReturnValue(null);
		}
	}

	/**
	 * Initialises the data structure for unsafe transitions.
	 */
	private void initUnsafeTransMaps() {
		if (tr2InUnsafeArcsMap == null) {
			tr2InUnsafeArcsMap = new Object2ObjectOpenHashMap<String, IntBigArrayBigList>();
			tr2InUnsafeArcsMap.defaultReturnValue(null);
		}
		if (tr2OutUnsafeArcsMap == null) {
			tr2OutUnsafeArcsMap = new Object2ObjectOpenHashMap<String, IntBigArrayBigList>();
			tr2OutUnsafeArcsMap.defaultReturnValue(null);
		}

	}

	/**
	 * Initializes internal data structures for places.
	 */
	private void initPlacesMap() {
		iDCount = 0L;
		if (placesId2bpnMap == null) {
			placesId2bpnMap = new Object2LongOpenHashMap<String>();
			placesId2bpnMap.defaultReturnValue(-1L);
		}
	}

	/**
	 * Initializes internal data structures for arcs.
	 */
	private void initUnsafeArcsMap() {
		if (unsafeArcsMap == null) {
			unsafeArcsMap = new Object2IntOpenHashMap<>();
			unsafeArcsMap.defaultReturnValue(-1);
		}
		if (unsafeNodes == null) {
			unsafeNodes = new ObjectOpenHashSet<>();
		}
	}

	private Thread startWriter(OutChannelBean ocb, BlockingQueue<String> queue) {
		Thread t = new Thread(new NUPNWriter(ocb, queue));
		t.start();
		return t;
	}

	/**
	 * Export transitions into NUPN (since 1.3.0)
	 * 
	 * @param ap
	 * @param vn
	 * @param bpnQueue
	 * @param tsQueue
	 * @throws XPathParseExceptionHuge
	 * @throws NavExceptionHuge
	 * @throws InterruptedException
	 * @throws XPathEvalExceptionHuge
	 */
	private void exportTransitions130(AutoPilotHuge ap, VTDNavHuge vn, BlockingQueue<String> bpnQueue)
			throws XPathParseExceptionHuge, NavExceptionHuge, InterruptedException, XPathEvalExceptionHuge {
		long nb = trId2bpnMap.size();
		StringBuilder bpnsb = new StringBuilder();
		bpnsb.append(TRANSITIONS).append(WS).append(HK).append(nb).append(WS).append(ZERO).append(DOTS).append(nb - 1L)
				.append(NL);
		bpnQueue.put(bpnsb.toString());
		bpnsb.delete(0, bpnsb.length());

		LongCollection allTr = new LongRBTreeSet(trId2bpnMap.values());

		for (long trId : allTr) {
			bpnsb.append(T).append(trId);
			buildConnectedPlaces2Transition(bpnsb, trId, tr2InPlacesMap);
			buildConnectedPlaces2Transition(bpnsb, trId, tr2OutPlacesMap);
			bpnsb.append(NL);
			bpnQueue.put(bpnsb.toString());
			bpnsb.delete(0, bpnsb.length());
		}
		bpnsb.delete(0, bpnsb.length());
		bpnsb = null;
	}

	/**
	 * @param bpnsb
	 * @param trId
	 */
	private void buildConnectedPlaces2Transition(StringBuilder bpnsb, long trId,
			Long2ObjectOpenHashMap<LongBigArrayBigList> tr2PlacesMap) {
		LongBigArrayBigList pls;
		long plsSize;
		pls = tr2PlacesMap.get(trId);
		if (pls != null) {
			plsSize = pls.size64();
		} else { // no place in input or output list of this transition
			plsSize = 0L;
		}
		bpnsb.append(WS).append(HK).append(plsSize);

		if (plsSize > 0L) {
			for (long plId : pls) {
				bpnsb.append(WS).append(plId);
			}
		}
	}

	/**
	 * Build transitions collections, collecting unsafe arcs and corresponding
	 * transitions.
	 * 
	 * @param ap
	 * @param vn
	 */
	private void buildTransitions(AutoPilotHuge ap, VTDNavHuge vn) throws XPathParseExceptionHuge, NavExceptionHuge,
			InterruptedException, XPathEvalExceptionHuge {
		String arc, src, trg, id;
		long count = 0L;
		long tId, pId;
		int arcInsc = 0;
		LongBigArrayBigList pls = null;
		IntBigArrayBigList arcVals = null;
		// nbUnsafeTrans = 0L;

		ap.selectXPath(PNMLPaths.TRANSITIONS_PATH);
		while ((ap.evalXPath()) != -1) {
			vn.push();
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			tId = count++;
			trId2bpnMap.put(id, tId);
			tsQueue.put(tId + WS + id + NL);
			vn.pop();
		}

		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		ap.selectXPath(PNMLPaths.ARCS_PATH);
		while ((ap.evalXPath()) != -1) {
			vn.push();
			pls = null;
			arc = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			src = vn.toString(vn.getAttrVal(PNMLPaths.SRC_ATTR));
			trg = vn.toString(vn.getAttrVal(PNMLPaths.TRG_ATTR));

			tId = trId2bpnMap.getLong(src);
			if (tId == -1L) { // transition is the target
				tId = trId2bpnMap.getLong(trg);
				if (tId == -1L) {
					tId = count++;
					trId2bpnMap.put(trg, tId);
					tsQueue.put(tId + WS + trg + NL);
				}
				pls = tr2InPlacesMap.get(tId);
				if (pls == null) {
					pls = new LongBigArrayBigList();
					tr2InPlacesMap.put(tId, pls);
				}
				// associate the input place
				pId = placesId2bpnMap.getLong(src);
				pls.add(pId);
				// Unsafe node ?
				if (unsafeNodes.contains(trg)) {
					arcInsc = unsafeArcsMap.getInt(arc);
					if (arcInsc != -1) {
						arcVals = tr2InUnsafeArcsMap.get(trg);
						if (arcVals == null) {
							arcVals = new IntBigArrayBigList();
							tr2InUnsafeArcsMap.put(trg, arcVals);
						}
						arcVals.add(arcInsc);
					}
				}

			} else {// transition is the source
				pls = tr2OutPlacesMap.get(tId);
				if (pls == null) {
					pls = new LongBigArrayBigList();
					tr2OutPlacesMap.put(tId, pls);
				}
				pId = placesId2bpnMap.getLong(trg);
				pls.add(pId);
				if (unsafeNodes.contains(src)) {
					arcInsc = unsafeArcsMap.getInt(arc);
					if (arcInsc != -1) {
						arcVals = tr2OutUnsafeArcsMap.get(src);
						if (arcVals == null) {
							arcVals = new IntBigArrayBigList();
							tr2OutUnsafeArcsMap.put(src, arcVals);
						}
						arcVals.add(arcInsc);
					}
				}
			}
			vn.pop();
		}
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
	}

	/**
	 * Builds unsafe arcs pragma
	 * 
	 * @param nupnQueue
	 * @throws InterruptedException
	 */
	private void buildUnsafeArcsPragma(BlockingQueue<String> nupnQueue) throws InterruptedException {
		IntBigArrayBigList arcVals = null;
		long nbTransIn = 0L, nbTransOut = 0L, nbTransInOut = 0L;
		int minValIn = -1, minValOut = -1, maxValIn = -1, maxValOut = -1;
		long minDiff = -1000000L, maxDiff = -10000000L, diff = 0L, inValT = 0L, outValT = 0L;

		nbUnsafeTrans = 0L;
		if (unsafeTrans) {
			StringBuilder warnMsg = new StringBuilder();
			for (String s : tr2InUnsafeArcsMap.keySet()) {
				arcVals = tr2InUnsafeArcsMap.get(s);
				warnMsg.append("Transition ").append(s).append(" is unsafe because it has ").append(arcVals.size64())
						.append(" incoming arc(s) with respective valuation(s):");
				inValT = 0L;
				outValT = 0L;
				for (int v : arcVals) {
					warnMsg.append(WS).append(v);
					if (minValIn == -1 && v > maxValIn && v > minValIn) {
						minValIn = v;
						maxValIn = v;
					} else if (v < minValIn) {
						minValIn = v;
					} else if (v > maxValIn) {
						maxValIn = v;
					}
					inValT += v;
				}

				arcVals = tr2OutUnsafeArcsMap.get(s);
				if (arcVals != null) {
					warnMsg.append(", and ").append(arcVals.size64())
							.append(" outgoing arc(s) with respective valuation(s):");
					nbTransInOut++;
					for (int v : arcVals) {
						warnMsg.append(WS).append(v);
						if (minValOut == -1 && v > maxValOut && v > minValOut) {
							minValOut = v;
							maxValOut = v;
						} else if (v < minValOut) {
							minValOut = v;
						} else if (v > maxValOut) {
							maxValOut = v;
						}
						outValT += v;
					}
					tr2OutUnsafeArcsMap.remove(s);
				} else {
					nbTransIn++;
				}
				nbUnsafeTrans++;
				log.warn(warnMsg.toString());
				warnMsg.delete(0, warnMsg.length());
				diff = outValT - inValT;
				if (minDiff == -1000000L && diff > maxDiff && diff > minDiff) {
					minDiff = diff;
					maxDiff = diff;
				} else if (diff < minDiff) {
					minDiff = diff;
				} else if (diff > maxDiff) {
					maxDiff = diff;
				}
			}

			for (String s : tr2OutUnsafeArcsMap.keySet()) {
				arcVals = tr2OutUnsafeArcsMap.get(s);
				warnMsg.append("Transition ").append(s).append(" is unsafe because it has ").append(arcVals.size64())
						.append(" outgoing arc(s) with respective valuation(s):");
				nbTransOut++;
				inValT = 0L;
				outValT = 0L;
				for (int v : arcVals) {
					warnMsg.append(WS).append(v);
					if (minValOut == -1 && v > maxValOut && v > minValOut) {
						minValOut = v;
						maxValOut = v;
					} else if (v < minValOut) {
						minValOut = v;
					} else if (v > maxValOut) {
						maxValOut = v;
					}
					outValT += v;
				}
				nbUnsafeTrans++;
				log.warn(warnMsg.toString());
				warnMsg.delete(0, warnMsg.length());
				diff = outValT - inValT;
				if (minDiff == -1000000L && diff > maxDiff && diff > minDiff) {
					minDiff = diff;
					maxDiff = diff;
				} else if (diff < minDiff) {
					minDiff = diff;
				} else if (diff > maxDiff) {
					maxDiff = diff;
				}
			}
			// Write pragma
			StringBuffer multArcsPrama = new StringBuffer();
			multArcsPrama.append(MainPNML2NUPN.PRAGMA_MULTIPLE_ARCS)
			// .append(HK + nbUnsafeArcs).append(WS)
					.append(HK + nbTransIn).append(WS).append(HK + nbTransOut).append(WS).append(HK + nbTransInOut);

			if (nbTransIn == 0L && nbTransInOut == 0L) {
				multArcsPrama.append(WS).append(ONE).append(DOTS).append(ZERO);
			} else {
				multArcsPrama.append(WS).append(minValIn).append(DOTS).append(maxValIn);
			}

			if (nbTransOut == 0L && nbTransInOut == 0L) {
				multArcsPrama.append(WS).append(ONE).append(DOTS).append(ZERO);
			} else {
				multArcsPrama.append(WS).append(minValOut).append(DOTS).append(maxValOut);
			}
			multArcsPrama.append(WS).append(minDiff).append(DOTS).append(maxDiff);
			multArcsPrama.append(NL);
			nupnQueue.put(multArcsPrama.toString());

			// Write unsafe arcs and transitions info in signature message
			MainPNML2NUPN.appendMesgLineToSignature("There are " + nbUnsafeArcs + " unsafe arcs with inscriptions > 1");
			MainPNML2NUPN.appendMesgLineToSignature("There are " + nbUnsafeTrans
					+ " transitions connected to the unsafe arcs");
		}
	}

	private void exportPlacesIntoUnits(AutoPilotHuge ap, VTDNavHuge vn, BlockingQueue<String> nupnQueue,
			BlockingQueue<String> psQueue) throws XPathParseExceptionHuge, XPathEvalExceptionHuge, NavExceptionHuge,
			InvalidSafeNetException, InternalException, InterruptedException, InvalidNetException, IOException {
		// long iDCount = 0L;
		long nbMarkedPlaces = 0L;
		int minMarking = 0, maxMarking = 0, mkg, totalMkg = 0;
		unsafePlaces = false;
		unsafeArcs = false;
		nbUnsafePlaces = 0L;
		nbUnsafeArcs = 0L;
		String id;
		ap.selectXPath(PNMLPaths.COUNT_MARKED_PLACES);
		nbMarkedPlaces = (long) ap.evalXPathToNumber();

		// Exit point if there is no initial place in the net
		if (nbMarkedPlaces == 0L) {
			throw new InvalidNetException("Error: there is no initial place in this net!");
		}
		// Check initial markings > 1. No more exit point since 1.3.0
		// (generate.unsafe property must be removed)
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		ap.selectXPath(PNMLPaths.UNSAFE_MARKED_PLACES);
		StringBuilder unsafePlacesId = new StringBuilder();
		while ((ap.evalXPath()) != -1) {
			vn.push();
			vn.toElement(VTDNavHuge.FIRST_CHILD);
			while (!vn.matchElement(TEXT)) {
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
			}
			mkg = Integer.parseInt(vn.toString(vn.getText()).trim());
			if (minMarking == 0 && mkg > maxMarking && mkg > minMarking) {
				minMarking = mkg;
				maxMarking = mkg;
			} else if (mkg < minMarking) {
				minMarking = mkg;
			} else if (mkg > maxMarking) {
				maxMarking = mkg;
			}
			vn.toElement(VTDNavHuge.PARENT);
			vn.toElement(VTDNavHuge.PARENT);
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			if (id != null) {
				unsafePlacesId.append(id + COMMAWS);
				nbUnsafePlaces++;
			}
			vn.pop();
		}

		// Check inscriptions > 1
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		ap.selectXPath(PNMLPaths.UNSAFE_ARCS);
		int val;
		String src, trg;
		while ((ap.evalXPath()) != -1) {
			vn.push();
			vn.toElement(VTDNavHuge.FIRST_CHILD);
			while (!vn.matchElement(TEXT)) {
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
			}
			val = Integer.parseInt(vn.toString(vn.getText()).trim());
			vn.toElement(VTDNavHuge.PARENT);
			vn.toElement(VTDNavHuge.PARENT);
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			if (id != null) {
				src = vn.toString(vn.getAttrVal(PNMLPaths.SRC_ATTR));
				unsafeNodes.add(src);
				trg = vn.toString(vn.getAttrVal(PNMLPaths.TRG_ATTR));
				unsafeNodes.add(trg);
				log.warn("Unsafe arc: {}", src + WS + id + WS + trg + WS + HK + val);
				unsafeArcsMap.put(id, val);
				nbUnsafeArcs++;
			}
			vn.pop();
		}
		if (nbUnsafeArcs > 0) {
			unsafeArcs = true;
			unsafeTrans = true;
			log.warn("There are {} unsafe arcs in this net.", nbUnsafeArcs);
		}

		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);

		// Parse all the places, to have ordered ids according to order of
		// appearance in the PNML file.
		long pId;
		ap.selectXPath(PNMLPaths.PLACES_PATH);
		while ((ap.evalXPath()) != -1) {
			vn.push();
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			pId = iDCount++;
			placesId2bpnMap.put(id, pId);
			vn.pop();
		}
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);

		// select initial places, assign ids to them
		ap.selectXPath(PNMLPaths.MARKED_PLACES);
		List<Long> initPlaces = new ArrayList<>();
		StringBuilder initPlacesId = new StringBuilder();
		while ((ap.evalXPath()) != -1) {
			vn.push();
			vn.toElement(VTDNavHuge.FIRST_CHILD);
			while (!vn.matchElement(TEXT)) {
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
			}
			totalMkg += Integer.parseInt(vn.toString(vn.getText()).trim());
			vn.toElement(VTDNavHuge.PARENT);
			vn.toElement(VTDNavHuge.PARENT);
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			if (id != null) {
				try {
					// pId = iDCount++;
					pId = placesId2bpnMap.getLong(id);
					// placesId2bpnMap.put(id, pId);
					initPlaces.add(pId);
					psQueue.put(pId + WS + id + NL);
					initPlacesId.append(id + COMMAWS);
				} catch (InterruptedException e) {
					log.error(e.getMessage());
					throw new InternalException(e.getMessage());
				}
			}
			vn.pop();
		}

		if (nbUnsafePlaces > 0) {
			unsafePlaces = true;
			nupnQueue.put(MainPNML2NUPN.PRAGMA_MULTIPLE_INIT_TOKEN + HK + totalMkg + WS + HK + nbUnsafePlaces + WS
					+ minMarking + DOTS + maxMarking + NL);
			log.warn("There are {} unsafe initial places in this net.", nbUnsafePlaces);
			unsafePlacesId.delete(unsafePlacesId.length() - 2, unsafePlacesId.length());
			log.warn("Unsafe initial places: {}", unsafePlacesId.toString());
		}

		// Several initial places are now accepted (since 1.1.10)
		if (nbMarkedPlaces > 1) {
			log.info("There are {} initial places in this net.", nbMarkedPlaces);
		}
		// Remove trailing comma and space, then display initial places
		initPlacesId.delete(initPlacesId.length() - 2, initPlacesId.length());
		log.info("Initial place(s): {}", initPlacesId.toString());

		if (nbUnsafePlaces > 0) {
			log.info("Checking invariant 'total nb of tokens > nb initial places': {}", totalMkg > nbMarkedPlaces);

			log.info("Checking invariant 'nb unsafe initial places <= nb initial places': {}",
					nbUnsafePlaces <= nbMarkedPlaces);

			log.info(
					"Checking invariant '(nb_init - nb_places) + (nb_places * min) <= nb_tokens <= (nb_init - nb_places) + (nb_places * max)': {}",
					(nbMarkedPlaces - nbUnsafePlaces) + (nbUnsafePlaces * minMarking) <= totalMkg
							&& totalMkg <= (nbMarkedPlaces - nbUnsafePlaces) + (nbUnsafePlaces * maxMarking));
		}

		if (unsafePlaces) {
			MainPNML2NUPN.appendMesgLineToSignature("decreased to one the marking of " + nbUnsafePlaces
					+ " initial places");
		}

		// build transitions, to be able to write unsafe arcs pragma
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		buildTransitions(ap, vn);
		buildUnsafeArcsPragma(nupnQueue);

		// count all places
		// ap.resetXPath();
		// vn.toElement(VTDNavHuge.ROOT);
		ap.selectXPath(PNMLPaths.COUNT_PLACES_PATH);
		long nbPl = (long) ap.evalXPathToNumber();
		final StringBuilder nupnsb = new StringBuilder();
		// Write Number of places
		nupnsb.append(PLACES).append(WS).append(HK).append(nbPl).append(WS).append(ZERO).append(DOTS).append(nbPl - 1)
				.append(NL);
		// Handle case where there are several initial places
		if (initPlaces.size() > 1) {
			nupnsb.append(INIT_PLACES).append(WS).append(HK).append(initPlaces.size());
			for (Long l : initPlaces) {
				nupnsb.append(WS).append(l);
			}
		} else {
			nupnsb.append(INIT_PLACE).append(WS).append(ZERO);
		}
		nupnsb.append(NL);

		// Check existence of nupn toolspecific
		boolean hasNUPNToolspecific = false;
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		ap.selectXPath(PNMLPaths.TOOL_SPECIFIC);
		String tool, version;
		while ((ap.evalXPath()) != -1) {
			vn.push();
			tool = vn.toString(vn.getAttrVal(PNMLPaths.TOOL_ATTR));
			version = vn.toString(vn.getAttrVal(PNMLPaths.VERSION_ATTR));
			if ("nupn".equals(tool) && "1.1".equals(version)) {
				hasNUPNToolspecific = true;
				break;
			}
			vn.pop();
		}
		// to write PNML places id mapping to NUPN id
		final StringBuilder tsmapping = new StringBuilder();
		// If there is nupn toolspecific, use that info to build units
		if (hasNUPNToolspecific) {
			log.info("NUPN toolspecific element detected in the PNML. Will use that info to build units.");
			vn.toElement(VTDNavHuge.FIRST_CHILD);
			while (!vn.matchElement(STRUCTURE)) {
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
			}
			// write number of units
			int nbUn = Integer.valueOf(vn.toString(vn.getAttrVal(PNMLPaths.UNITS_ATTR))).intValue();
			nupnsb.append(UNITS).append(WS).append(HK).append(nbUn).append(WS).append(ZERO).append(DOTS)
					.append(nbUn - 1).append(NL);
			// write root unit
			int rootUn = Integer.valueOf(vn.toString(vn.getAttrVal(PNMLPaths.ROOT_ATTR)).substring(1)).intValue();
			nupnsb.append(ROOT_UNIT).append(WS).append(rootUn).append(NL);
			nupnQueue.put(nupnsb.toString());
			nupnsb.delete(0, nupnsb.length());
			// write each unit
			ap.resetXPath();
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.NUPN_UNIT);
			String places, subunits;
			String[] elemId;
			LongList placesIntId = new LongArrayList();
			long plId;
			while ((ap.evalXPath()) != -1) {
				vn.push();
				nupnsb.append(vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR)).toUpperCase());
				// places
				vn.toElement(VTDNavHuge.FIRST_CHILD);
				places = vn.toString(vn.getText()).trim();
				if (!places.isEmpty()) {
					elemId = places.split(WS);
					for (String s : elemId) {
						plId = placesId2bpnMap.getLong(s);
						if (plId != -1L) {
							placesIntId.add(plId);
							tsmapping.append(s).append(WS).append(plId).append(NL);
							psQueue.put(tsmapping.toString());
							tsmapping.delete(0, tsmapping.length());
						}
					}
					nupnsb.append(WS).append(HK).append(placesIntId.size());
					if (placesIntId.size() > 1) {
						placesIntId.stream().map(i -> nupnsb.append(WS).append(i));
					} else {
						nupnsb.append(WS).append(placesIntId.get(0)).append(DOTS).append(placesIntId.get(0));
					}
				} else {
					nupnsb.append(WS).append(HK).append(ZERO).append(WS).append(ONE).append(DOTS).append(ZERO);
				}
				// subunits
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
				subunits = vn.toString(vn.getText()).trim();
				if (!subunits.isEmpty()) {
					elemId = subunits.split(WS);
					nupnsb.append(WS).append(HK).append(elemId.length);
					for (String s : elemId) {
						nupnsb.append(WS).append(s.substring(1));
					}
					nupnsb.append(NL);
				} else {
					nupnsb.append(WS).append(HK).append(ZERO).append(NL);
				}
				nupnQueue.put(nupnsb.toString());
				nupnsb.delete(0, nupnsb.length());
				vn.toElement(VTDNavHuge.PARENT);
				vn.pop();
			}
		} else {

			// Write the number of Units. Check case there is just one place.
			if (nbPl > 1) {
				nupnsb.append(UNITS).append(WS).append(HK).append(nbPl + 1).append(WS).append(ZERO).append(DOTS)
						.append(nbPl).append(NL);
			} else {
				nupnsb.append(UNITS).append(WS).append(HK).append(nbPl).append(WS).append(ZERO).append(DOTS)
						.append(nbPl - 1).append(NL);
			}

			// Root unit declaration - id is N - 1. Check case there is just one
			// place.
			if (nbPl > 1) {
				nupnsb.append(ROOT_UNIT).append(WS).append(nbPl).append(NL);
			} else {
				nupnsb.append(ROOT_UNIT).append(WS).append(nbPl - 1).append(NL);
			}
			nupnQueue.put(nupnsb.toString());
			nupnsb.delete(0, nupnsb.length());

			// One place per unit, keep track of their PNML id in ts file
			// First the initial places
			long count = 0L;
			for (Long l : initPlaces) {
				nupnsb.append(U).append(count).append(WS).append(HK).append(ONE).append(WS).append(l).append(DOTS)
						.append(l).append(WS).append(HK).append(ZERO).append(NL);
				nupnQueue.put(nupnsb.toString());
				count++;
				nupnsb.delete(0, nupnsb.length());
			}

			// Then the rest
			ap.resetXPath();
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.PLACES_PATH_EXCEPT_MKG);
			
			while ((ap.evalXPath()) != -1) {
				vn.push();
				id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
				pId = placesId2bpnMap.getLong(id);
				/*
				 * if (pId == -1L) { pId = iDCount++; placesId2bpnMap.put(id,
				 * pId); }
				 */
				tsmapping.append(pId).append(WS).append(id).append(NL);
				psQueue.put(tsmapping.toString());
				nupnsb.append(U).append(count).append(WS).append(HK).append(ONE).append(WS).append(pId).append(DOTS)
						.append(pId).append(WS).append(HK).append(ZERO).append(NL);
				nupnQueue.put(nupnsb.toString());
				// placesId2bpnMap.put(id, iDCount);
				nupnsb.delete(0, nupnsb.length());
				tsmapping.delete(0, tsmapping.length());
				count++;
				// iDCount++;
				vn.pop();
			}
			// / Root Unit N and its subunits. Check case there is just one
			// place.
			if (nbPl > 1) {
				nupnsb.append(U).append(nbPl).append(WS).append(HK).append(ZERO).append(WS).append(ONE).append(DOTS)
						.append(ZERO).append(WS).append(HK).append(nbPl);
				for (count = 0L; count < nbPl; count++) {
					nupnsb.append(WS).append(count);
				}
			} else if (nbPl == 1) {
				// DO NOTHING, already handled above.
				log.warn("I encountered the case where there is just one place in the net.");
			} else { // FIXME This case should not happen.
				nupnsb.append(U).append(nbPl).append(WS).append(HK).append(ZERO).append(WS).append(ONE).append(DOTS)
						.append(ZERO).append(WS).append(HK).append(ZERO);
				log.error("I encountered the case where there is no place at all in the net.");
				log.error("This violates the rules stating that root unit must have at least 2 sub-units, if it does not contain any place.");
				throw new InvalidNetException("No place in the net! See error messages above.");
			}
			nupnsb.append(NL);
			nupnQueue.put(nupnsb.toString());
			nupnsb.delete(0, nupnsb.length());
		}
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
	}

	private boolean isPTNet(AutoPilotHuge ap, VTDNavHuge vn) throws XPathParseExceptionHuge, XPathEvalExceptionHuge,
			NavExceptionHuge {
		boolean result = true;
		ap.selectXPath(PNMLPaths.NETS_PATH);
		while ((ap.evalXPath()) != -1) {
			vn.push();
			String netType = vn.toString(vn.getAttrVal(PNMLPaths.TYPE_ATTR));
			log.info("Discovered net type: {}", netType);
			if (!netType.endsWith(PNMLPaths.PTNET_TYPE)) {
				result = false;
				break;
			}
			vn.pop();
		}
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		return result;
	}

	@SuppressWarnings("unused")
	private boolean isNet1Safe(AutoPilotHuge ap, VTDNavHuge vn) throws XPathParseExceptionHuge, NavExceptionHuge,
			NumberFormatException, XPathEvalExceptionHuge {
		boolean result = true;
		long count = 0L;
		String mkg;
		ap.selectXPath(PNMLPaths.MARKED_PLACES);
		while ((ap.evalXPath()) != -1) {
			vn.push();
			mkg = vn.toString(vn.getText());
			if (Integer.valueOf(mkg) == 1) {
				count++;
			} else {
				break;
			}
			vn.pop();
		}
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		if (count != 1L) {
			result = false;
		}
		return result;
	}

	private boolean isNet1Safe() throws IOException, PNMLImportExportException {
		spnc.setPnmlDocPath(this.currentInputFile.getCanonicalPath());
		boolean res = spnc.isNet1Safe();
		return res;
	}

	private String generateUnsafePlacesReport() {
		return spnc.getExplanation();
	}

	/**
	 * Emergency stop.
	 * 
	 * @param outFile
	 * @param nupnQueue
	 * @param tsQueue
	 * @param psQueue
	 * @param ocbBpn
	 * @param ocbTs
	 * @param ocbPs
	 * @param outTSFile
	 * @param outPSFile
	 * @param journal
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void emergencyStop(File outFile) throws InterruptedException, IOException {

		cancelWriters(nupnQueue, tsQueue, psQueue);
		cancelWriter(uaQueue);
		closeChannels(ocbBpn, ocbTs, ocbPs);
		closeChannel(ocbUA);
		deleteOutputFiles(outFile, outTSFile, outPSFile);
		deleteOutputFile(outUAFile);
		log.error("Emergency stop. Cancelled the translation and released opened resources.");
	}

	/**
	 * Emergency stop actions.
	 * 
	 * @param outFile
	 * @param outTSFile
	 * @param outPSFile
	 */
	private void deleteOutputFiles(File outFile, File outTSFile, File outPSFile) {
		deleteOutputFile(outFile);
		deleteOutputFile(outTSFile);
		deleteOutputFile(outPSFile);
	}

	/**
	 * Deletes an output file.
	 * 
	 * @param oFile
	 */
	private void deleteOutputFile(File oFile) {
		if (oFile != null && oFile.exists()) {
			oFile.delete();
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
	private void closeChannels(OutChannelBean ocbBpn, OutChannelBean ocbTs, OutChannelBean ocbPs) throws IOException {
		closeChannel(ocbBpn);
		closeChannel(ocbTs);
		closeChannel(ocbPs);
	}

	/**
	 * Closes an output channel.
	 * 
	 * @param cb
	 * @throws IOException
	 */
	private void closeChannel(OutChannelBean cb) throws IOException {
		PNML2NUPNUtils.closeOutChannel(cb);
	}

	/**
	 * Cancels writers in case of emergency stop.
	 * 
	 * @param bpnQueue
	 * @param tsQueue
	 * @param psQueue
	 * @throws InterruptedException
	 */
	private void cancelWriters(BlockingQueue<String> bpnQueue, BlockingQueue<String> tsQueue,
			BlockingQueue<String> psQueue) throws InterruptedException {
		cancelWriter(bpnQueue);
		cancelWriter(tsQueue);
		cancelWriter(psQueue);
	}

	/**
	 * Cancel a writer by sending a cancellation message to it.
	 * 
	 * @param queue
	 * @throws InterruptedException
	 */
	private void cancelWriter(BlockingQueue<String> queue) throws InterruptedException {
		if (queue != null) {
			queue.put(CANCEL);
		}
	}

	/**
	 * Normal stop.
	 * 
	 * @param bpnQueue
	 * @param tsQueue
	 * @param psQueue
	 * @throws InterruptedException
	 */
	private void stopWriters(BlockingQueue<String> bpnQueue, BlockingQueue<String> tsQueue,
			BlockingQueue<String> psQueue) throws InterruptedException {
		stopWriter(bpnQueue);
		stopWriter(tsQueue);
		stopWriter(psQueue);
	}

	/**
	 * Normal stop of a writer.
	 * 
	 * @param queue
	 * @throws InterruptedException
	 */
	private void stopWriter(BlockingQueue<String> queue) throws InterruptedException {
		queue.put(STOP);
	}

	/**
	 * Clears all internal data structures for places and transitions.
	 */
	private void clearAllCollections() {
		placesId2bpnMap.clear();
		trId2bpnMap.clear();
		tr2InPlacesMap.clear();
		tr2OutPlacesMap.clear();
		unsafeArcsMap.clear();
		tr2InUnsafeArcsMap.clear();
		tr2OutUnsafeArcsMap.clear();
		unsafeNodes.clear();
	}
}
