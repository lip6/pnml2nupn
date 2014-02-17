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
package fr.lip6.move.pnml2bpn.export.impl;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

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

import fr.lip6.move.pnml2bpn.MainPNML2BPN;
import fr.lip6.move.pnml2bpn.exceptions.InternalException;
import fr.lip6.move.pnml2bpn.exceptions.InvalidNetException;
import fr.lip6.move.pnml2bpn.exceptions.InvalidPNMLTypeException;
import fr.lip6.move.pnml2bpn.exceptions.InvalidSafeNetException;
import fr.lip6.move.pnml2bpn.exceptions.PNMLImportExportException;
import fr.lip6.move.pnml2bpn.export.PNMLExporter;
import fr.lip6.move.pnml2bpn.utils.PNML2BPNUtils;
import fr.lip6.move.pnml2bpn.utils.SafePNChecker;

/**
 * Actual PNML 2 BPN exporter.
 * 
 * @author lom
 * 
 */
public final class PNML2BPNExporter implements PNMLExporter {

	private static final String TRANS_EXT = ".trans";
	private static final String STATES_EXT = ".places";
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

	private Logger log = null;

	private Object2LongOpenHashMap<String> placesId2bpnMap = null;

	private Object2LongOpenHashMap<String> trId2bpnMap = null;
	private Long2ObjectOpenHashMap<LongBigArrayBigList> tr2OutPlacesMap = null;
	private Long2ObjectOpenHashMap<LongBigArrayBigList> tr2InPlacesMap = null;
	private File currentInputFile = null;
	private SafePNChecker spnc = null;

	public PNML2BPNExporter() {
		spnc = new SafePNChecker();
	}

	@Override
	public void exportPNML(URI inFile, URI outFile, Logger journal)
			throws PNMLImportExportException, InterruptedException, IOException {
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public void exportPNML(File inFile, File outFile, Logger journal)
			throws PNMLImportExportException, InterruptedException, IOException {
		initLog(journal);
		export(inFile, outFile, journal);
	}

	@Override
	public void exportPNML(String inFile, String outFile, Logger journal)
			throws PNMLImportExportException, InterruptedException, IOException {
		initLog(journal);
		export(new File(inFile), new File(outFile), journal);
	}

	/**
	 * @param journal
	 */
	private void initLog(Logger journal) {
		this.log = journal;
	}

	private void export(File inFile, File outFile, Logger journal)
			throws PNMLImportExportException, InterruptedException, IOException {
		initLog(journal);
		try {
			this.currentInputFile = inFile;
			journal.info("Checking preconditions on input file format: {} ",
					inFile.getCanonicalPath());
			PNML2BPNUtils.checkIsPnmlFile(inFile);
			log.info("Exporting into BPN: {}", inFile.getCanonicalPath());
			/*
			 * String sessionId = "pnml2bpn" + Thread.currentThread().getId();
			 * PNMLUtils.createWorkspace(sessionId); HLAPIRootClass rootClass =
			 * PNMLUtils.importPnmlDocument(inFile, false); if
			 * (!PNType.PTNET.equals(PNMLUtils.determineNetType(rootClass))) {
			 * String message =
			 * "This translation tool only support P/T Nets. The PNML you provided does not have a P/T Net root."
			 * ; journal.error(message); throw new
			 * PNMLImportExportException(message); }
			 */
			translateIntoBPN(inFile, outFile, journal);

		} catch (ValidationException
				| fr.lip6.move.pnml2bpn.exceptions.InvalidFileTypeException
				| fr.lip6.move.pnml2bpn.exceptions.InvalidFileException
				| InternalException | InvalidPNMLTypeException e) {
			// journal.error(e.getMessage());
			// MainPNML2BPN.printStackTrace(e);
			throw new PNMLImportExportException(e);
		} catch (IOException e) {
			throw e;
		}

	}

	private void translateIntoBPN(File inFile, File outFile, Logger journal)
			throws InvalidPNMLTypeException, InterruptedException,
			PNMLImportExportException, IOException {
		XMLMemMappedBuffer xb = new XMLMemMappedBuffer();
		VTDGenHuge vg = new VTDGenHuge();
		BlockingQueue<String> bpnQueue = null;
		BlockingQueue<String> tsQueue = null;
		BlockingQueue<String> psQueue = null;
		OutChannelBean ocbBpn = null;
		OutChannelBean ocbTs = null;
		OutChannelBean ocbPs = null;
		File outTSFile = null;
		File outPSFile = null;
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
								+ this.currentInputFile.getCanonicalPath());
			}
			// The net must be 1-safe, if bounds checking is enabled.
			if (MainPNML2BPN.isBoundsChecking()) {
				log.info("Checking it is 1-Safe.");
				if (!isNet1Safe()) {
					if (MainPNML2BPN.isForceBPNGen()) {
						journal.warn(
								"The net(s) in the submitted document is not 1-safe, but forced BPN generation is set: {}",
								this.currentInputFile.getCanonicalPath());
						journal.warn("Continuing BPN generation.");
					} else {
						throw new InvalidSafeNetException(
								"The net(s) in the submitted document is not 1-safe: "
										+ this.currentInputFile
												.getCanonicalPath());
					}
				} else {
					log.info("Net appears to be 1-Safe.");
				}
			} else {
				log.info("Bounds checking is disabled. I don't know if the net is 1-safe, or not.");
			}
			// Open BPN and mapping files channels, and init write queues
			outTSFile = new File(PNML2BPNUtils.extractBaseName(outFile
					.getCanonicalPath()) + TRANS_EXT);
			outPSFile = new File(PNML2BPNUtils.extractBaseName(outFile
					.getCanonicalPath()) + STATES_EXT);
			// Channels for BPN, transitions and places id mapping
			ocbBpn = PNML2BPNUtils.openOutChannel(outFile);
			ocbTs = PNML2BPNUtils.openOutChannel(outTSFile);
			ocbPs = PNML2BPNUtils.openOutChannel(outPSFile);
			// Queues for BPN, transitions and places id mapping
			bpnQueue = initQueue();
			tsQueue = initQueue();
			psQueue = initQueue();
			// Start writers
			Thread bpnWriter = startWriter(ocbBpn, bpnQueue);
			Thread tsWriter = startWriter(ocbTs, tsQueue);
			Thread psWriter = startWriter(ocbPs, psQueue);

			// Init data type for places id and export places
			initPlacesMap();
			log.info("Exporting places.");
			exportPlacesIntoUnits(ap, vn, bpnQueue, psQueue);

			// Init data type for transitions id and export transitions
			initTransitionsMaps();
			log.info("Exporting transitions.");
			exportTransitions(ap, vn, bpnQueue, tsQueue);

			// Stop Writers
			stopWriters(bpnQueue, tsQueue, psQueue);
			bpnWriter.join();
			tsWriter.join();
			psWriter.join();
			// Close channels
			closeChannels(ocbBpn, ocbTs, ocbPs);
			// clear maps
			clearAllMaps();
			log.info("See BPN and mapping files: {}, {} and {}",
					outFile.getCanonicalPath(), outTSFile.getCanonicalPath(),
					outPSFile.getCanonicalPath());
		} catch (NavExceptionHuge | XPathParseExceptionHuge
				| XPathEvalExceptionHuge | ParseExceptionHuge
				| InvalidSafeNetException | InternalException
				| InvalidNetException e) {
			emergencyStop(outFile, bpnQueue, tsQueue, psQueue, ocbBpn, ocbTs,
					ocbPs, outTSFile, outPSFile, journal);
			throw new PNMLImportExportException(e);
		} catch (InterruptedException e) {
			emergencyStop(outFile, bpnQueue, tsQueue, psQueue, ocbBpn, ocbTs,
					ocbPs, outTSFile, outPSFile, journal);
			throw e;
		} catch (IOException e) {
			emergencyStop(outFile, bpnQueue, tsQueue, psQueue, ocbBpn, ocbTs,
					ocbPs, outTSFile, outPSFile, journal);
			throw e;
		}
	}

	/**
	 * Emergency stop.
	 * 
	 * @param outFile
	 * @param bpnQueue
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
	private void emergencyStop(File outFile, BlockingQueue<String> bpnQueue,
			BlockingQueue<String> tsQueue, BlockingQueue<String> psQueue,
			OutChannelBean ocbBpn, OutChannelBean ocbTs, OutChannelBean ocbPs,
			File outTSFile, File outPSFile, Logger journal)
			throws InterruptedException, IOException {

		cancelWriters(bpnQueue, tsQueue, psQueue);
		closeChannels(ocbBpn, ocbTs, ocbPs);
		deleteOutputFiles(outFile, outTSFile, outPSFile);
		journal.error("Emergency stop. Cancelled the translation and released opened resources.");
	}

	/**
	 * Emergency stop actions.
	 * 
	 * @param outFile
	 * @param outTSFile
	 * @param outPSFile
	 */
	private void deleteOutputFiles(File outFile, File outTSFile, File outPSFile) {
		if (outFile != null && outFile.exists()) {
			outFile.delete();
		}
		if (outPSFile != null && outPSFile.exists()) {
			outPSFile.delete();
		}
		if (outTSFile != null && outTSFile.exists()) {
			outTSFile.delete();
		}
	}

	/**
	 * Close output channels.
	 * 
	 * @param ocbBpn
	 * @param ocbTs
	 * @param ocbPs
	 * @throws IOException
	 */
	private void closeChannels(OutChannelBean ocbBpn, OutChannelBean ocbTs,
			OutChannelBean ocbPs) throws IOException {
		PNML2BPNUtils.closeOutChannel(ocbBpn);
		PNML2BPNUtils.closeOutChannel(ocbTs);
		PNML2BPNUtils.closeOutChannel(ocbPs);
	}

	/**
	 * Cancels writers in case of emergency stop.
	 * 
	 * @param bpnQueue
	 * @param tsQueue
	 * @param psQueue
	 * @throws InterruptedException
	 */
	private void cancelWriters(BlockingQueue<String> bpnQueue,
			BlockingQueue<String> tsQueue, BlockingQueue<String> psQueue)
			throws InterruptedException {
		if (bpnQueue != null) {
			bpnQueue.put(CANCEL);
		}
		if (tsQueue != null) {
			tsQueue.put(CANCEL);
		}
		if (psQueue != null) {
			psQueue.put(CANCEL);
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
	private void stopWriters(BlockingQueue<String> bpnQueue,
			BlockingQueue<String> tsQueue, BlockingQueue<String> psQueue)
			throws InterruptedException {
		bpnQueue.put(STOP);
		tsQueue.put(STOP);
		psQueue.put(STOP);
	}

	/**
	 * Clears all internal data structures for places and transitions.
	 */
	private void clearAllMaps() {
		placesId2bpnMap.clear();
		trId2bpnMap.clear();
		tr2InPlacesMap.clear();
		tr2OutPlacesMap.clear();
	}

	/**
	 * Initialize internal data structures for transitions.
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
	 * Initialize internal data structures for places.
	 */
	private void initPlacesMap() {
		if (placesId2bpnMap == null) {
			placesId2bpnMap = new Object2LongOpenHashMap<String>();
			placesId2bpnMap.defaultReturnValue(-1L);
		}
	}

	private void exportTransitions(AutoPilotHuge ap, VTDNavHuge vn,
			BlockingQueue<String> bpnQueue, BlockingQueue<String> tsQueue)
			throws XPathParseExceptionHuge, NavExceptionHuge,
			InterruptedException, XPathEvalExceptionHuge {
		// count transitions
		ap.selectXPath(PNMLPaths.COUNT_TRANSITIONS_PATH);
		long nb = (long) ap.evalXPathToNumber();
		StringBuilder bpnsb = new StringBuilder();
		bpnsb.append(TRANSITIONS).append(WS).append(HK).append(nb).append(WS)
				.append(ZERO).append(DOTS).append(nb - 1).append(NL);
		bpnQueue.put(bpnsb.toString());
		bpnsb.delete(0, bpnsb.length());
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);

		// Handle transitions through arcs
		String src, trg;
		long count = 0L;
		long tId, pId;
		LongBigArrayBigList pls = null;
		ap.selectXPath(PNMLPaths.ARCS_PATH);
		// @deprecated tsQueue.put(TRANSITIONS_MAPPING_MSG + NL);
		while ((ap.evalXPath()) != -1) {
			pls = null;
			src = vn.toString(vn.getAttrVal(PNMLPaths.SRC_ATTR));
			trg = vn.toString(vn.getAttrVal(PNMLPaths.TRG_ATTR));
			pId = placesId2bpnMap.getLong(src);
			if (pId != -1L) { // transition is the target
				tId = trId2bpnMap.getLong(trg);
				if (tId != -1L) {
					pls = tr2InPlacesMap.get(tId);
				} else {
					tId = count++;
					trId2bpnMap.put(trg, tId);
					tsQueue.put(tId + WS + trg + NL);
				}
				if (pls == null) {
					pls = new LongBigArrayBigList();
					tr2InPlacesMap.put(tId, pls);
				}
			} else {// transition is the source
				pId = placesId2bpnMap.getLong(trg);
				tId = trId2bpnMap.getLong(src);
				if (tId != -1L) {
					pls = tr2OutPlacesMap.get(tId);
				} else {
					tId = count++;
					trId2bpnMap.put(src, tId);
					tsQueue.put(tId + WS + src + NL);
				}
				if (pls == null) {
					pls = new LongBigArrayBigList();
					tr2OutPlacesMap.put(tId, pls);
				}
			}
			pls.add(pId);
		}
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
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
	private void buildConnectedPlaces2Transition(StringBuilder bpnsb,
			long trId, Long2ObjectOpenHashMap<LongBigArrayBigList> tr2PlacesMap) {
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

	private BlockingQueue<String> initQueue() {
		BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
		return queue;
	}

	private Thread startWriter(OutChannelBean ocb, BlockingQueue<String> queue) {
		Thread t = new Thread(new BPNWriter(ocb, queue));
		t.start();
		return t;
	}

	private void exportPlacesIntoUnits(AutoPilotHuge ap, VTDNavHuge vn,
			BlockingQueue<String> bpnQueue, BlockingQueue<String> tsQueue)
			throws XPathParseExceptionHuge, XPathEvalExceptionHuge,
			NavExceptionHuge, InvalidSafeNetException, InternalException,
			InterruptedException, InvalidNetException {
		long iDCount = 0L;
		long nbMarkedPlaces = 0L;
		ap.selectXPath(PNMLPaths.COUNT_MARKED_PLACES);
		nbMarkedPlaces = (long) ap.evalXPathToNumber();

		// FIXME: exit point if there is no initial place in the net
		if (nbMarkedPlaces == 0L) {
			throw new InvalidNetException(
					"Error: there is no initial place in this net!");
		}

		// FIXME: several initial are now accepted (since 1.1.10)
		// places
		if (nbMarkedPlaces > 1) {
			//log.error("Attention: there are several initial places and no solution yet for the correct encoding of the resulting BPN.");
			//throw new InvalidNetException(
			//		"No solution yet to export into BPN the case of several initial places");
			log.warn("There are several initial places in this net.");
		}

		// select initial places
		ap.resetXPath();
		ap.selectXPath(PNMLPaths.MARKED_PLACES);
		String id;
		List<Long> initPlaces = new ArrayList<>();
		// FIXME Check: do we need to clone the vn for using it in the loop?
		// (Case of several initial places...)
		while ((ap.evalXPath()) != -1) {
			vn.push();
			vn.toElement(VTDNavHuge.PARENT);
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			if (id != null) {
				try {
					tsQueue.put(iDCount + WS + id + NL);
					placesId2bpnMap.put(id, iDCount);
					initPlaces.add(iDCount);
					iDCount++;
				} catch (InterruptedException e) {
					log.error(e.getMessage());
					throw new InternalException(e.getMessage());

				}
			}
			vn.pop();
		}

		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);

		// count all places
		ap.selectXPath(PNMLPaths.COUNT_PLACES_PATH);
		long nb = (long) ap.evalXPathToNumber();
		StringBuilder bpnsb = new StringBuilder();
		// Write Number of places
		bpnsb.append(PLACES).append(WS).append(HK).append(nb).append(WS)
				.append(ZERO).append(DOTS).append(nb - 1).append(NL);
		// Handle case where there are several initial places
		if (initPlaces.size() > 1) {
			bpnsb.append(INIT_PLACES).append(WS).append(HK)
					.append(initPlaces.size());
			for (Long l : initPlaces) {
				bpnsb.append(WS).append(l);
			}
		} else {
			bpnsb.append(INIT_PLACE).append(WS).append(ZERO);
		}
		bpnsb.append(NL);
		// Write the number of Units. Check case there is just one place.
		if (nb > 1) {
			bpnsb.append(UNITS).append(WS).append(HK).append(nb + 1).append(WS)
					.append(ZERO).append(DOTS).append(nb).append(NL);
		} else {
			bpnsb.append(UNITS).append(WS).append(HK).append(nb).append(WS)
					.append(ZERO).append(DOTS).append(nb - 1).append(NL);
		}

		// Root unit declaration - id is N - 1. Check case there is just one place.
		if (nb > 1) {
			bpnsb.append(ROOT_UNIT).append(WS).append(nb).append(NL);
		} else {
			bpnsb.append(ROOT_UNIT).append(WS).append(nb - 1).append(NL);
		}
		bpnQueue.put(bpnsb.toString());
		bpnsb.delete(0, bpnsb.length());

		// One place per unit, keep track of their PNML id in ts file
		// First the initial places
		long count = 0L;
		for (Long l : initPlaces) {
			bpnsb.append(U).append(count).append(WS).append(HK).append(ONE)
					.append(WS).append(l).append(DOTS).append(l).append(WS)
					.append(HK).append(ZERO).append(NL);
			bpnQueue.put(bpnsb.toString());
			count++;
			bpnsb.delete(0, bpnsb.length());
		}

		// Then the rest
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		ap.selectXPath(PNMLPaths.PLACES_PATH_EXCEPT_MKG);
		StringBuilder tsmapping = new StringBuilder();
		while ((ap.evalXPath()) != -1) {

			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			tsmapping.append(iDCount).append(WS).append(id).append(NL);
			tsQueue.put(tsmapping.toString());
			bpnsb.append(U).append(count).append(WS).append(HK).append(ONE)
					.append(WS).append(iDCount).append(DOTS).append(iDCount)
					.append(WS).append(HK).append(ZERO).append(NL);
			bpnQueue.put(bpnsb.toString());
			placesId2bpnMap.put(id, iDCount);
			bpnsb.delete(0, bpnsb.length());
			tsmapping.delete(0, tsmapping.length());
			count++;
			iDCount++;
		}
		tsmapping = null;
		// / Root Unit N and its subunits. Check case there is just one place.
		if (nb > 1) {
			bpnsb.append(U).append(nb).append(WS).append(HK).append(ZERO)
					.append(WS).append(ONE).append(DOTS).append(ZERO)
					.append(WS).append(HK).append(nb);
			for (count = 0L; count < nb; count++) {
				bpnsb.append(WS).append(count);
			}
		} else if (nb == 1) {
			// DO NOTHING, already handled above.
			log.warn("We encountered the case where there is just one place in the net.");
		} else { // FIXME This case should not happen.
			bpnsb.append(U).append(nb).append(WS).append(HK).append(ZERO)
					.append(WS).append(ONE).append(DOTS).append(ZERO)
					.append(WS).append(HK).append(ZERO);
			log.error("We encountered the case where there is no place at all in the net.");
			log.error("This violates the rules stating that root unit must have at least 2 sub-units, if it does not contain any place.");
			throw new InvalidNetException(
					"No place in the net! See error messages above.");
		}
		bpnsb.append(NL);
		bpnQueue.put(bpnsb.toString());
		bpnsb.delete(0, bpnsb.length());
		bpnsb = null;
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
	}

	private boolean isPTNet(AutoPilotHuge ap, VTDNavHuge vn)
			throws XPathParseExceptionHuge, XPathEvalExceptionHuge,
			NavExceptionHuge {
		boolean result = true;
		ap.selectXPath(PNMLPaths.NETS_PATH);
		while ((ap.evalXPath()) != -1) {
			String netType = vn.toString(vn.getAttrVal(PNMLPaths.TYPE_ATTR));
			log.info("Discovered net type: {}", netType);
			if (!netType.endsWith(PNMLPaths.PTNET_TYPE)) {
				result = false;
				break;
			}
		}
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		return result;
	}

	@SuppressWarnings("unused")
	private boolean isNet1Safe(AutoPilotHuge ap, VTDNavHuge vn)
			throws XPathParseExceptionHuge, NavExceptionHuge,
			NumberFormatException, XPathEvalExceptionHuge {
		boolean result = true;
		long count = 0L;
		String mkg;
		ap.selectXPath(PNMLPaths.MARKED_PLACES);
		while ((ap.evalXPath()) != -1) {
			mkg = vn.toString(vn.getText());
			if (Integer.valueOf(mkg) == 1) {
				count++;
			} else {
				break;
			}
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
}
