/**
 *  Copyright 2014-2016 Université Paris Nanterre and Sorbonne Université,
 *  					CNRS, LIP6
 *
 *  All rights reserved.   This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Project leader / Initial Contributor:
 *    Lom Messan Hillah - <lom-messan.hillah@lip6.fr>
 *
 *  Mailing list:
 *    lom-messan.hillah@lip6.fr
 */
package fr.lip6.move.pnml2nupn.export.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

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
import fr.lip6.move.pnml2nupn.export.PNML2NUPNExporter;
import fr.lip6.move.pnml2nupn.utils.PNML2NUPNUtils;
import fr.lip6.move.pnml2nupn.utils.SafePNChecker;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * Actual PNML 2 BPN exporter.
 * 
 * @author lom
 * 
 */
public final class PNML2NUPNExporterImpl implements PNML2NUPNExporter {

	private Logger log = null;

	private Object2LongOpenHashMap<String> placesId2NupnMap = null;
	private Object2LongOpenHashMap<String> trId2NupnMap = null;
	private Long2ObjectOpenHashMap<LongBigArrayBigList> tr2OutPlacesMap = null;
	private Long2ObjectOpenHashMap<LongBigArrayBigList> tr2InPlacesMap = null;
	private Object2ObjectOpenHashMap<String, LongBigArrayBigList> tr2InAllArcsMap = null;
	private Object2ObjectOpenHashMap<String, LongBigArrayBigList> tr2OutAllArcsMap = null;
	private Object2ObjectOpenHashMap<String, LongBigArrayBigList> tr2InUnsafeArcsMap = null;
	private Object2ObjectOpenHashMap<String, LongBigArrayBigList> tr2OutUnsafeArcsMap = null;
	private ObjectSet<String> unsafeNodes = null;
	private Object2LongOpenHashMap<String> unsafeArcsMap = null;

	private File currentInputFile = null;
	private SafePNChecker spnc = null;
	private long nbUnsafeArcs, nbUnsafePlaces, nbUnsafeTrans;
	private long nbTransIn = 0L, nbTransOut = 0L, nbTransInOut = 0L;
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
	/*Navigation in the XML*/
	private VTDNavHuge vn;
	private AutoPilotHuge ap;
	/*NUPN tool specific section in the PNML?*/
	private boolean hasNUPNToolspecific;

	public PNML2NUPNExporterImpl() {
		spnc = new SafePNChecker();
	}

	@Override
	public void export2NUPN(URI inFile, URI outFile, Logger journal) throws PNMLImportExportException,
	InterruptedException, IOException {
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	@Override
	public void export2NUPN(File inFile, File outFile, Logger journal) throws PNMLImportExportException,
	InterruptedException, IOException, EarlyStopException {
		initLog(journal);
		export(inFile, outFile, journal);
	}

	@Override
	public void export2NUPN(String inFile, String outFile, Logger journal) throws PNMLImportExportException,
	InterruptedException, IOException, EarlyStopException {
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
			outUAFile = new File(PNML2NUPNUtils.extractBaseName(outFile.getCanonicalPath()) + NUPNConstants.UNSAFE_ARC);
			ocbUA = PNML2NUPNUtils.openOutChannel(outUAFile);
			uaQueue = PNML2NUPNUtils.initQueue();
			Thread uaWriter = PNML2NUPNUtils.startWriter(ocbUA, uaQueue);

			// Check inscriptions > 1
			ap.resetXPath();
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.UNSAFE_ARCS);
			StringBuilder unsafeArcsId = new StringBuilder();
			long val;
			String id, src, trg;
			while ((ap.evalXPath()) != -1) {
				vn.push();
				vn.toElement(VTDNavHuge.FIRST_CHILD);
				while (!vn.matchElement(NUPNConstants.TEXT)) {
					vn.toElement(VTDNavHuge.NEXT_SIBLING);
				}
				val = Long.parseLong(vn.toString(vn.getText()).trim());
				vn.toElement(VTDNavHuge.PARENT);
				vn.toElement(VTDNavHuge.PARENT);
				id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
				if (id != null) {
					src = vn.toString(vn.getAttrVal(PNMLPaths.SRC_ATTR));
					trg = vn.toString(vn.getAttrVal(PNMLPaths.TRG_ATTR));
					unsafeArcsId.append(src + NUPNConstants.WS + id + NUPNConstants.WS + trg + NUPNConstants.WS + NUPNConstants.HK + val + NUPNConstants.NL);
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
			PNML2NUPNUtils.stopWriter(uaQueue);
			uaWriter.join();
			PNML2NUPNUtils.closeChannel(ocbUA);
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
	InterruptedException, IOException, EarlyStopException {
		initLog(journal);
		try {
			this.currentInputFile = inFile;
			journal.info("Checking preconditions on input file format: {} ", inFile.getCanonicalPath());
			PNML2NUPNUtils.checkIsPnmlFile(inFile);
			log.info("Exporting into NUPN: {}", inFile.getCanonicalPath());
			openXMLStream(inFile);
			boolean hasNupnToolInfo = hasNUPNToolSpecificSection(inFile);
			/*
			if (MainPNML2NUPN.isPreserveNupnNative() && hasNupnToolInfo) {
				journal.info("NUPN extraction in native mode requested, and there is a NUPN tool specific section.");
				NativeNUPNExtractor nupnExtractor = new NativeNUPNExtractor(inFile, outFile, journal);
				nupnExtractor.extractNUPN(vn, ap);
			} else {
				translateIntoNUPN(inFile, outFile, journal);
			}*/
			translateIntoNUPN(inFile, outFile, journal);
		} catch (ValidationException | fr.lip6.move.pnml2nupn.exceptions.InvalidFileTypeException
				| fr.lip6.move.pnml2nupn.exceptions.InvalidFileException | InternalException | InvalidPNMLTypeException e) {
			throw new PNMLImportExportException(e);
		} catch (IOException e) {
			throw e;
		}
	}

	private void openXMLStream(File inFile) throws PNMLImportExportException {
		vn = PNML2NUPNUtils.openXMLStream(inFile).getNav();
		ap = new AutoPilotHuge(vn);
	}

	private boolean hasNUPNToolSpecificSection(File inFile) throws PNMLImportExportException {
		boolean hasNUPNToolspecific = false;
		ap.resetXPath();
		try {
			log.info("Checking for the presence of a NUPN tool specific section...");
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.NUPN_TOOL_SPECIFIC);
			String version;
			while ((ap.evalXPath()) != -1) {
				vn.push();
				version = vn.toString(vn.getAttrVal(PNMLPaths.VERSION_ATTR));
				if (NUPNConstants.NUPN_SUPPORTED_VERSION.equals(version)) {
					hasNUPNToolspecific = true;
					log.info("NUPN toolspecific section detected in the PNML, version: {}", version);
					vn.pop();
					break;
				} else {
					log.warn("NUPN toolspecific section detected in the PNML, but version not supported: {}", version);
					log.warn("I support NUPN toolspecific version {}", NUPNConstants.NUPN_SUPPORTED_VERSION);
				}
				vn.pop();
			}
		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge e) {
			throw new PNMLImportExportException(e);
		}
		ap.resetXPath();
		this.hasNUPNToolspecific = hasNUPNToolspecific;
		if (!hasNUPNToolspecific) {
			log.info("No NUPN tool specific section in this PNML file.");
		}
		return hasNUPNToolspecific;
	}

	private void translateIntoNUPN(File inFile, File outFile, Logger journal) throws InvalidPNMLTypeException,
	InterruptedException, PNMLImportExportException, IOException, EarlyStopException {

		boolean isSafe = false;
		try {

			log.info("Checking it is a PT Net.");
			if (!isPTNet(ap, vn)) {
				throw new InvalidPNMLTypeException(
						"The net in the document is not a P/T Net. Only P/T Nets are supported: "
								+ this.currentInputFile.getCanonicalPath());
			}
			// The net must be 1-safe, if bounds checking is enabled.
			if (MainPNML2NUPN.isUnitSafenessChecking()) {
				log.info("Checking if this net is 1-Safe.");
				if (!(isSafe = isNet1Safe())) {
					if (SafePNChecker.isBoundsVerdictInconclusive()) {
						journal.warn("This net cannot be proven 1-safe or unsafe (the Bounds tool could not compute the bounds using structural analysis of place bounds): "
								+ this.currentInputFile.getCanonicalPath());
					} else {
						journal.error("This net is not 1-safe (proven by the Bounds tool using structural analysis of place bounds): "
								+ this.currentInputFile.getCanonicalPath());
						journal.error("\nUNSAFE PLACES: {}", generateUnsafePlacesReport());
					}

					if (MainPNML2NUPN.isForceNUPNGen() && !MainPNML2NUPN.isUnitSafenessCheckingOnly()) {
						journal.warn("Forced NUPN generation is set => Continuing NUPN generation.");
					} else {
						if (!MainPNML2NUPN.isUnitSafenessCheckingOnly()) {
							if (SafePNChecker.isBoundsVerdictInconclusive()) {
								journal.warn("Potentially unsafe net (inconclusive verdict by the Bounds tool) => Continuing NUPN generation.");
							} else {
								throw new InvalidSafeNetException("Unsafe net in "
										+ this.currentInputFile.getCanonicalPath());
							}
						}
					}
				} else {
					log.info(
							"This net is 1-safe (proven by the Bounds tool using structural analysis of place bounds): {}",
							this.currentInputFile.getCanonicalPath());
				}
				if (MainPNML2NUPN.isUnitSafenessCheckingOnly()) {
					journal.info("Unit safeness checking only requested. Will stop here.");
					throw new EarlyStopException("Unit safeness checking only requested on "
							+ this.currentInputFile.getCanonicalPath());
				}
			} else {
				log.warn("Unit safeness checking is disabled. I don't know if this net is 1-Safe.");
			}
			// Open NUPN and mapping files channels, and init write queues
			outTSFile = new File(PNML2NUPNUtils.extractBaseName(outFile.getCanonicalPath()) + NUPNConstants.TRANS_EXT);
			outPSFile = new File(PNML2NUPNUtils.extractBaseName(outFile.getCanonicalPath()) + NUPNConstants.STATES_EXT);
			// Channels for NuPN, transitions and places id mapping
			ocbBpn = PNML2NUPNUtils.openOutChannel(outFile);
			ocbTs = PNML2NUPNUtils.openOutChannel(outTSFile);
			ocbPs = PNML2NUPNUtils.openOutChannel(outPSFile);
			// Queues for NUPN, transitions and places id mapping
			nupnQueue = PNML2NUPNUtils.initQueue();
			tsQueue = PNML2NUPNUtils.initQueue();
			psQueue = PNML2NUPNUtils.initQueue();

			// Start writers
			Thread nupnWriter = PNML2NUPNUtils.startWriter(ocbBpn, nupnQueue);
			Thread tsWriter = PNML2NUPNUtils.startWriter(ocbTs, tsQueue);
			Thread psWriter = PNML2NUPNUtils.startWriter(ocbPs, psQueue);

			// Insert creator pragma (since 1.3.0)
			PNML2NUPNUtils.insertCreatorPragma(nupnQueue);
			// Insert unit_safe pragma if necessary (since 1.4.1)
			if (MainPNML2NUPN.isUnitSafenessChecking() && isSafe) {
				insertUnitSafePragma();
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
			PNML2NUPNUtils.stopWriters(nupnQueue, tsQueue, psQueue);
			// stopWriter(uaQueue);
			nupnWriter.join();
			tsWriter.join();
			psWriter.join();
			// Close channels
			PNML2NUPNUtils.closeChannels(ocbBpn, ocbTs, ocbPs);
			// clear maps
			clearAllCollections();
			log.info("See NUPN and mapping files: {}, {} and {}", outFile.getCanonicalPath(),
					outTSFile.getCanonicalPath(), outPSFile.getCanonicalPath());
		} catch (EarlyStopException e) {
			normalStop(outFile);
			throw e;
		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge 
				| InvalidSafeNetException | InternalException | InvalidNetException e) {
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
		long nb = trId2NupnMap.size();
		StringBuilder bpnsb = new StringBuilder();
		bpnsb.append(NUPNConstants.TRANSITIONS).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nb).append(NUPNConstants.WS).append(NUPNConstants.ZERO).append(NUPNConstants.DOTS).append(nb - 1L)
		.append(NUPNConstants.NL);
		bpnQueue.put(bpnsb.toString());
		bpnsb.delete(0, bpnsb.length());

		LongCollection allTr = new LongRBTreeSet(trId2NupnMap.values());

		for (long trId : allTr) {
			bpnsb.append(NUPNConstants.T).append(trId);
			buildConnectedPlaces2Transition(bpnsb, trId, tr2InPlacesMap);
			buildConnectedPlaces2Transition(bpnsb, trId, tr2OutPlacesMap);
			bpnsb.append(NUPNConstants.NL);
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
		bpnsb.append(NUPNConstants.WS).append(NUPNConstants.HK).append(plsSize);

		if (plsSize > 0L) {
			for (long plId : pls) {
				bpnsb.append(NUPNConstants.WS).append(plId);
			}
		}
	}

	private void mapInputArcToTransition(String targetTrId, long inscription) {
		LongBigArrayBigList allArcVals = tr2InAllArcsMap.get(targetTrId);
		if (allArcVals == null) {
			allArcVals = new LongBigArrayBigList();
		}
		allArcVals.add(inscription);
		tr2InAllArcsMap.put(targetTrId, allArcVals);
	}

	private void mapOutputArcToTransition(String sourceTrId, long inscription) {
		LongBigArrayBigList allArcVals = tr2OutAllArcsMap.get(sourceTrId);
		if (allArcVals == null) {
			allArcVals = new LongBigArrayBigList();
		}
		allArcVals.add(inscription);
		tr2OutAllArcsMap.put(sourceTrId, allArcVals);
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
		long arcInsc = 0;
		boolean foundInsc;
		LongBigArrayBigList pls = null;
		LongBigArrayBigList arcVals = null;

		ap.selectXPath(PNMLPaths.TRANSITIONS_PATH);
		while ((ap.evalXPath()) != -1) {
			vn.push();
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			tId = count++;
			trId2NupnMap.put(id, tId);
			tsQueue.put(tId + NUPNConstants.WS + id + NUPNConstants.NL);
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

			tId = trId2NupnMap.getLong(src);
			if (tId == -1L) { // transition is the target
				tId = trId2NupnMap.getLong(trg);
				if (tId == -1L) {
					tId = count++;
					trId2NupnMap.put(trg, tId);
					tsQueue.put(tId + NUPNConstants.WS + trg + NUPNConstants.NL);
				}
				pls = tr2InPlacesMap.get(tId);
				if (pls == null) {
					pls = new LongBigArrayBigList();
					tr2InPlacesMap.put(tId, pls);
				}
				// associate the input place
				pId = placesId2NupnMap.getLong(src);
				pls.add(pId);
				// Unsafe node ?
				if (unsafeNodes.contains(trg)) {
					arcInsc = unsafeArcsMap.getLong(arc);
					if (arcInsc != -1) {
						arcVals = tr2InUnsafeArcsMap.get(trg);
						if (arcVals == null) {
							arcVals = new LongBigArrayBigList();
							tr2InUnsafeArcsMap.put(trg, arcVals);
						}
						arcVals.add(arcInsc);
					}
				}
				// looking for inscription
				if (vn.toElement(VTDNavHuge.FIRST_CHILD)) {
					while (!(foundInsc = vn.matchElement(NUPNConstants.INSCRIPTION))) {
						if (!vn.toElement(VTDNavHuge.NEXT_SIBLING)){
							break;
						}
					}
					if (foundInsc){
						vn.toElement(VTDNavHuge.FIRST_CHILD);
						while (!vn.matchElement(NUPNConstants.TEXT)) {
							vn.toElement(VTDNavHuge.NEXT_SIBLING);
						}
						arcInsc = Long.parseLong(vn.toString(vn.getText()).trim());
						// map the transition to all input arcs
						mapInputArcToTransition(trg, arcInsc);
						vn.toElement(VTDNavHuge.PARENT);
					}else {
						mapInputArcToTransition(trg, 1L);
					}
					vn.toElement(VTDNavHuge.PARENT);
				} else {
					mapInputArcToTransition(trg, 1L);
				}

			} else {// transition is the source
				pls = tr2OutPlacesMap.get(tId);
				if (pls == null) {
					pls = new LongBigArrayBigList();
					tr2OutPlacesMap.put(tId, pls);
				}
				pId = placesId2NupnMap.getLong(trg);
				pls.add(pId);
				if (unsafeNodes.contains(src)) {
					arcInsc = unsafeArcsMap.getLong(arc);
					if (arcInsc != -1) {
						arcVals = tr2OutUnsafeArcsMap.get(src);
						if (arcVals == null) {
							arcVals = new LongBigArrayBigList();
							tr2OutUnsafeArcsMap.put(src, arcVals);
						}
						arcVals.add(arcInsc);
					}
				}
				// looking for inscription
				if (vn.toElement(VTDNavHuge.FIRST_CHILD)) {
					while (!(foundInsc = vn.matchElement(NUPNConstants.INSCRIPTION))) {
						if (!vn.toElement(VTDNavHuge.NEXT_SIBLING)){
							break;
						}
					}
					if (foundInsc){
						vn.toElement(VTDNavHuge.FIRST_CHILD);
						while (!vn.matchElement(NUPNConstants.TEXT)) {
							vn.toElement(VTDNavHuge.NEXT_SIBLING);
						}
						arcInsc = Long.parseLong(vn.toString(vn.getText()).trim());
						// map the transition to all output arcs
						mapOutputArcToTransition(src, arcInsc);
						vn.toElement(VTDNavHuge.PARENT);
					} else {
						mapOutputArcToTransition(src, 1L);
					}
					vn.toElement(VTDNavHuge.PARENT);
				} else {
					mapOutputArcToTransition(src, 1L);
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
		LongBigArrayBigList arcVals = null;
		LongArrayList minValIn = new LongArrayList(2);
		LongArrayList minValOut = new LongArrayList(2);
		LongArrayList maxValIn = new LongArrayList(2);
		LongArrayList maxValOut = new LongArrayList(2);

		LongArrayList minAllDiff = new LongArrayList(2);
		LongArrayList maxAllDiff = new LongArrayList(2);
		//long minDiff = 0L, maxDiff = 0L;
		long diff = 0L;
		long inValT = 0L, outValT = 0L;

		nbUnsafeTrans = 0L;
		if (unsafeTrans) {
			StringBuilder warnMsg = new StringBuilder();
			for (String s : tr2InUnsafeArcsMap.keySet()) {
				arcVals = tr2InUnsafeArcsMap.get(s);
				inValT = 0L;
				outValT = 0L;
				warnMsg.append("Transition ").append(s).append(" is unsafe because it has ").append(arcVals.size64())
				.append(" unsafe incoming arc(s) with respective valuation(s):");
				for (long v : arcVals) {
					warnMsg.append(NUPNConstants.WS).append(v);
					PNML2NUPNUtils.setMin(v, minValIn);
					PNML2NUPNUtils.setMax(v, maxValIn);
					/*minValIn = Math.min(minValIn, v);
					maxValIn = Math.max(maxValIn, v);*/
					inValT += v;
				}	
				// they could also have incoming arcs with valuation = 1...
				arcVals = tr2InAllArcsMap.get(s);
				if (arcVals != null) {
					long arcValsSafe = arcVals.stream().filter(v -> v == 1L).map(x -> x).reduce(0L, (a, b) -> a + b);
					if (arcValsSafe > 0L) {
						inValT += arcValsSafe;
						warnMsg.append(", safe incoming arc(s) with respective valuation(s):");
						arcVals.stream().filter(v -> v == 1).forEach(v -> {
							warnMsg.append(NUPNConstants.WS).append(v);
						});
					}
					tr2InAllArcsMap.remove(s);
				}

				arcVals = tr2OutUnsafeArcsMap.get(s);
				if (arcVals != null) {
					nbTransInOut++;
					warnMsg.append(", ").append(arcVals.size64())
					.append(" unsafe outgoing arc(s) with respective valuation(s):");
					for (long v : arcVals) {
						warnMsg.append(NUPNConstants.WS).append(v);
						PNML2NUPNUtils.setMin(v, minValOut);
						PNML2NUPNUtils.setMax(v, maxValOut);
						/*minValOut = Math.min(minValOut, v);
						maxValOut = Math.max(maxValOut, v);*/
						outValT += v;
					}
					tr2OutUnsafeArcsMap.remove(s);
					//tr2InAllArcsMap.remove(s);
					//tr2OutAllArcsMap.remove(s);
				} else {
					nbTransIn++;	
				}

				// they could also have outgoing arcs with valuation = 1...
				arcVals = tr2OutAllArcsMap.get(s);
				if (arcVals != null) {
					long arcValsSafe = arcVals.stream().filter(v -> v == 1L).map(x -> x).reduce(0L, (a, b) -> a + b);
					if (arcValsSafe > 0L) {
						outValT += arcValsSafe;
						warnMsg.append(", and safe outgoing arc(s) with respective valuation(s):");
						arcVals.stream().filter(v -> v == 1).forEach(v -> {
							warnMsg.append(NUPNConstants.WS).append(v);
						});
					}
					tr2OutAllArcsMap.remove(s);
				}

				nbUnsafeTrans++;

				diff = outValT - inValT;
				PNML2NUPNUtils.setMin(diff, minAllDiff);
				PNML2NUPNUtils.setMax(diff, maxAllDiff);

				if (diff == 0) {
					int replacementStartIndex = 12 + s.length();
					warnMsg.replace(replacementStartIndex, replacementStartIndex + 3, "might be ");
					PNML2NUPNUtils.debug(warnMsg.toString(), log);
				} else {
					log.warn(warnMsg.toString());
				}

				warnMsg.delete(0, warnMsg.length());
				PNML2NUPNUtils.debug("Diff for transition {}: outVal({}) - inVal({}) = {}", log, s, s, s, diff);
			}

			tr2InUnsafeArcsMap.clear();

			for (String s : tr2OutUnsafeArcsMap.keySet()) {
				arcVals = tr2OutUnsafeArcsMap.get(s);
				inValT = 0L;
				outValT = 0L;
				warnMsg.append("Transition ").append(s).append(" is unsafe because it has ").append(arcVals.size64())
				.append(" unsafe outgoing arc(s) with respective valuation(s):");
				nbTransOut++;
				for (long v : arcVals) {
					warnMsg.append(NUPNConstants.WS).append(v);
					PNML2NUPNUtils.setMin(v, minValOut);
					PNML2NUPNUtils.setMax(v, maxValOut);
					outValT += v;
				}
				// they could also have outgoing arcs with valuation = 1...
				arcVals = tr2OutAllArcsMap.get(s);
				if (arcVals != null) {
					long arcValsSafe = arcVals.stream().filter(v -> v == 1).map(x -> x).reduce(0L, (a, b) -> a + b);
					if (arcValsSafe > 0L) {
						outValT += arcValsSafe;
						warnMsg.append(", safe outgoing arc(s) with respective valuation(s):");	
						arcVals.stream().filter(v -> v == 1).forEach(v -> {
							warnMsg.append(NUPNConstants.WS).append(v);
						});
					}
					tr2OutAllArcsMap.remove(s);
				}
				// they could also have incoming arcs with valuation = 1...
				arcVals = tr2InAllArcsMap.get(s);
				if (arcVals != null) {
					warnMsg.append(", and ").append(arcVals.size64())
					.append(" safe incoming arc(s) with respective valuation(s):");
					long arcValsSafe = arcVals.stream().filter(v -> v == 1).map(x -> x).reduce(0L, (a, b) -> a + b);
					if (arcValsSafe > 0L) {
						inValT += arcValsSafe;
						arcVals.stream().filter(v -> v == 1).forEach(v -> {
							warnMsg.append(NUPNConstants.WS).append(v);
						});
					}
					tr2InAllArcsMap.remove(s);
				}
				nbUnsafeTrans++;

				diff = outValT - inValT;
				PNML2NUPNUtils.setMin(diff, minAllDiff);
				PNML2NUPNUtils.setMax(diff, maxAllDiff);

				if (diff == 0) {
					int replacementStartIndex = 12 + s.length();
					warnMsg.replace(replacementStartIndex, replacementStartIndex + 3, "might be ");
					PNML2NUPNUtils.debug(warnMsg.toString(), log);
				} else {
					log.warn(warnMsg.toString());
				}
				warnMsg.delete(0, warnMsg.length());
				//debug("Diff for transition {}: Min-Diff= {}; Max-Diff= {}", s, minDiff, maxDiff);
				PNML2NUPNUtils.debug("Diff for transition {}: outVal({}) - inVal({}) = {}", log, s, s, s, diff);
			}

			tr2OutUnsafeArcsMap.clear();

			// process the rest of the transitions (safe ones) to compute min-diff and max-diff
			for (String s : tr2InAllArcsMap.keySet()) {
				arcVals = tr2InAllArcsMap.get(s);
				inValT = 0L;
				outValT = 0L;
				for (long v : arcVals) {
					inValT += v;
				}
				arcVals = tr2OutAllArcsMap.get(s);
				if (arcVals != null) {
					for (long v : arcVals) {
						outValT += v;
					}
					tr2OutAllArcsMap.remove(s);
				}
				diff = outValT - inValT;
				PNML2NUPNUtils.setMin(diff, minAllDiff);
				PNML2NUPNUtils.setMax(diff, maxAllDiff);
				PNML2NUPNUtils.debug("Diff for transition {}: outVal({}) - inVal({}) = {}", log, s, s, s, diff);
			}

			for (String s : tr2OutAllArcsMap.keySet()) {
				arcVals = tr2OutAllArcsMap.get(s);
				inValT = 0L;
				outValT = 0L;
				for (long v : arcVals) {
					outValT += v;
				}
				diff = outValT - inValT;
				PNML2NUPNUtils.setMin(diff, minAllDiff);
				PNML2NUPNUtils.setMax(diff, maxAllDiff);
				PNML2NUPNUtils.debug("Diff for transition {}: outVal({}) - inVal({}) = {}", log, s, s, s, diff);
			}

			// Write pragma
			StringBuffer multArcsPrama = new StringBuffer();
			multArcsPrama.append(MainPNML2NUPN.PRAGMA_MULTIPLE_ARCS)
			.append(NUPNConstants.HK + nbTransIn).append(NUPNConstants.WS).append(NUPNConstants.HK + nbTransOut).append(NUPNConstants.WS).append(NUPNConstants.HK + nbTransInOut);

			if (nbTransIn == 0L && nbTransInOut == 0L) {
				multArcsPrama.append(NUPNConstants.WS).append(NUPNConstants.ONE).append(NUPNConstants.DOTS).append(NUPNConstants.ZERO);
			} else {
				multArcsPrama.append(NUPNConstants.WS).append(minValIn.getLong(0)).append(NUPNConstants.DOTS).append(maxValIn.getLong(0));
			}

			if (nbTransOut == 0L && nbTransInOut == 0L) {
				multArcsPrama.append(NUPNConstants.WS).append(NUPNConstants.ONE).append(NUPNConstants.DOTS).append(NUPNConstants.ZERO);
			} else {
				multArcsPrama.append(NUPNConstants.WS).append(minValOut.getLong(0)).append(NUPNConstants.DOTS).append(maxValOut.getLong(0));
			}
			//multArcsPrama.append(NUPNConstants.WS).append(minDiff).append(NUPNConstants.DOTS).append(maxDiff);
			multArcsPrama.append(NUPNConstants.WS).append(minAllDiff.getLong(0)).append(NUPNConstants.DOTS).append(maxAllDiff.getLong(0));
			multArcsPrama.append(NUPNConstants.NL);
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
		long minMarking = 0, maxMarking = 0, mkg, totalMkg = 0;
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
			while (!vn.matchElement(NUPNConstants.TEXT)) {
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
			}
			mkg = Long.parseLong(vn.toString(vn.getText()).trim());
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
				unsafePlacesId.append(id + NUPNConstants.COMMAWS);
				nbUnsafePlaces++;
			}
			vn.pop();
		}

		// Check inscriptions > 1
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		ap.selectXPath(PNMLPaths.UNSAFE_ARCS);
		long val;
		String src, trg;
		while ((ap.evalXPath()) != -1) {
			vn.push();
			vn.toElement(VTDNavHuge.FIRST_CHILD);
			while (!vn.matchElement(NUPNConstants.TEXT)) {
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
			}
			val = Long.parseLong(vn.toString(vn.getText()).trim());
			vn.toElement(VTDNavHuge.PARENT);
			vn.toElement(VTDNavHuge.PARENT);
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			if (id != null) {
				src = vn.toString(vn.getAttrVal(PNMLPaths.SRC_ATTR));
				unsafeNodes.add(src);
				trg = vn.toString(vn.getAttrVal(PNMLPaths.TRG_ATTR));
				unsafeNodes.add(trg);
				log.warn("Unsafe arc: {}", src + NUPNConstants.WS + id + NUPNConstants.WS + trg + NUPNConstants.WS + NUPNConstants.HK + val);
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
			placesId2NupnMap.put(id, pId);
			vn.pop();
		}

		// select initial places, assign ids to them
		ap.resetXPath();
		vn.toElement(VTDNavHuge.ROOT);
		ap.selectXPath(PNMLPaths.MARKED_PLACES);
		List<Long> initPlaces = new ArrayList<>();
		StringBuilder initPlacesId = new StringBuilder();
		while ((ap.evalXPath()) != -1) {
			vn.push();
			vn.toElement(VTDNavHuge.FIRST_CHILD);
			while (!vn.matchElement(NUPNConstants.TEXT)) {
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
			}
			totalMkg += Long.parseLong(vn.toString(vn.getText()).trim());
			vn.toElement(VTDNavHuge.PARENT);
			vn.toElement(VTDNavHuge.PARENT);
			id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
			if (id != null) {
				try {
					// pId = iDCount++;
					pId = placesId2NupnMap.getLong(id);
					// placesId2bpnMap.put(id, pId);
					initPlaces.add(pId);
					if (!hasNUPNToolspecific)
						psQueue.put(pId + NUPNConstants.WS + id + NUPNConstants.NL);
					initPlacesId.append(id + NUPNConstants.COMMAWS);
				} catch (InterruptedException e) {
					log.error(e.getMessage());
					throw new InternalException(e.getMessage());
				}
			}
			vn.pop();
		}

		if (nbUnsafePlaces > 0) {
			unsafePlaces = true;
			nupnQueue.put(MainPNML2NUPN.PRAGMA_MULTIPLE_INIT_TOKEN + NUPNConstants.HK + totalMkg + NUPNConstants.WS + NUPNConstants.HK + nbUnsafePlaces + NUPNConstants.WS
					+ minMarking + NUPNConstants.DOTS + maxMarking + NUPNConstants.NL);
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
		ap.selectXPath(PNMLPaths.COUNT_PLACES_PATH);
		long nbPl = (long) ap.evalXPathToNumber();
		final StringBuilder nupnsb = new StringBuilder();
		// Write Number of places
		nupnsb.append(NUPNConstants.PLACES).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbPl).append(NUPNConstants.WS).append(NUPNConstants.ZERO).append(NUPNConstants.DOTS).append(nbPl - 1)
		.append(NUPNConstants.NL);
		// Output initial places
		if (initPlaces.size() > 1) {
			nupnsb.append(NUPNConstants.INIT_PLACES).append(NUPNConstants.WS).append(NUPNConstants.HK).append(initPlaces.size());
			for (Long l : initPlaces) {
				nupnsb.append(NUPNConstants.WS).append(l);
			}
		} else {
			nupnsb.append(NUPNConstants.INIT_PLACE).append(NUPNConstants.WS).append(initPlaces.get(0));
		}
		nupnsb.append(NUPNConstants.NL);

		// to write PNML places id mapping to NUPN id
		final StringBuilder psmapping = new StringBuilder();
		// If there is nupn toolspecific, use that info to build units
		if (hasNUPNToolspecific && MainPNML2NUPN.isPreserveNupnMix()) {
			log.info("NUPN tool specific section detected in the PNML.");
			log.info("Mixed generation strategy requested. Will use the NUPN structure provided in that section to build units.");
			String rootUn;
			Object2LongOpenHashMap<String> unitsIdMap = new Object2LongOpenHashMap<>(); 
			unitsIdMap.defaultReturnValue(-1L);
			long unitIDGen = 0L;
			ap.resetXPath();
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.NUPN_STRUCTURE);
			
			while ((ap.evalXPath()) != -1) {
				vn.push();
				// write number of units
				int nbUn = Integer.valueOf(vn.toString(vn.getAttrVal(PNMLPaths.UNITS_ATTR))).intValue();
				nupnsb.append(NUPNConstants.UNITS).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbUn).append(NUPNConstants.WS).append(NUPNConstants.ZERO).append(NUPNConstants.DOTS)
				.append(nbUn - 1).append(NUPNConstants.NL);
				// write root unit
				rootUn = vn.toString(vn.getAttrVal(PNMLPaths.ROOT_ATTR));
				long rootUnNb = unitIDGen++;
				unitsIdMap.put(rootUn, rootUnNb);
				nupnsb.append(NUPNConstants.ROOT_UNIT).append(NUPNConstants.WS).append(rootUnNb).append(NUPNConstants.NL);
				nupnQueue.put(nupnsb.toString());
				nupnsb.delete(0, nupnsb.length());
				vn.pop();
			}
			// write each unit
			ap.resetXPath();
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.NUPN_UNIT);
			String places = "", subunits = "";
			String[] elemId;
			String unitSId;
			long unitLId;
			long plId;
			LongList placesIntId = new LongArrayList();
			LongSortedSet faultyIds = new LongAVLTreeSet();
			boolean doubleCheck;
			while ((ap.evalXPath()) != -1) {
				vn.push();
				unitSId = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
				unitLId = unitsIdMap.getLong(unitSId);
				if (unitLId == -1L) {
					unitLId = unitIDGen++;
					unitsIdMap.put(unitSId, unitLId);
				}
				nupnsb.append(NUPNConstants.U).append(unitLId);
				// places
				vn.toElement(VTDNavHuge.FIRST_CHILD);
				if (vn.getText() != -1) {
					places = vn.toString(vn.getText()).trim();
				}
				if (!places.isEmpty()) {
					elemId = places.split(NUPNConstants.WS);
					for (String s : elemId) {
						plId = placesId2NupnMap.getLong(s);
						if (plId != -1L) {
							placesIntId.add(plId);
							psmapping.append(plId).append(NUPNConstants.WS).append(s).append(NUPNConstants.NL);
							psQueue.put(psmapping.toString());
							psmapping.delete(0, psmapping.length());
						}
					}
					nupnsb.append(NUPNConstants.WS).append(NUPNConstants.HK).append(placesIntId.size());
					if (placesIntId.size() > 1) {
						// calculate arithmetic progression of the nupn Ids to check if they are incremental in the unit
						placesIntId.sort(null);
						long sumArrProg = PNML2NUPNUtils.arithmeticProgression(placesIntId);
						PNML2NUPNUtils.debug("Assumed arithmetic progression of places Ids in unit {} ({}): {}", log, unitSId, unitLId, sumArrProg);
						long sumCheck = PNML2NUPNUtils.sum(placesIntId);
						PNML2NUPNUtils.debug("Manual sum of the places Ids in unit {} ({}): {} ", log, unitSId, unitLId, sumCheck);
						if (sumArrProg != sumCheck) {
							// double check
							doubleCheck = PNML2NUPNUtils.isArithmeticProgressionOnNUPNIds(placesIntId, faultyIds);
							log.error("The arithmetic progression of places Ids in unit {} ({}) is not satisfied! Double check result is (false -> no arithmetic progression): {}", unitSId, unitLId, doubleCheck); 
							log.error("List of consecutive places IDs not being in arithmetic progression in unit {} ({}): {}", unitSId, unitLId, faultyIds.toString());
							log.error("Consequently, the NUPN output for units will not be syntax-compliant.");
							placesIntId.stream().forEach(i -> nupnsb.append(NUPNConstants.WS).append(i));
							faultyIds.clear();
						} else {
							nupnsb.append(NUPNConstants.WS).append(placesIntId.getLong(0)).append(NUPNConstants.DOTS).append(placesIntId.getLong(placesIntId.size() - 1));
						}
					} else {
						nupnsb.append(NUPNConstants.WS).append(placesIntId.getLong(0)).append(NUPNConstants.DOTS).append(placesIntId.getLong(0));
					}
				} else {
					nupnsb.append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ZERO).append(NUPNConstants.WS)
					.append(NUPNConstants.ONE).append(NUPNConstants.DOTS).append(NUPNConstants.ZERO);
				}
				// subunits
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
				if (vn.getText() != -1) {
					subunits = vn.toString(vn.getText()).trim();
				}
				if (!subunits.isEmpty()) {
					elemId = subunits.split(NUPNConstants.WS);
					nupnsb.append(NUPNConstants.WS).append(NUPNConstants.HK).append(elemId.length);
					for (String s : elemId) {
						unitLId = unitsIdMap.getLong(s);
						if (unitLId == -1L) {
							unitLId = unitIDGen++;
							unitsIdMap.put(s, unitLId);
						}
						nupnsb.append(NUPNConstants.WS).append(unitLId);
					}
				} else {
					nupnsb.append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ZERO);
				}
				nupnsb.append(NUPNConstants.NL);
				nupnQueue.put(nupnsb.toString());
				nupnsb.delete(0, nupnsb.length());
				placesIntId.clear();
				subunits = "";
				places = "";
				vn.toElement(VTDNavHuge.PARENT);
				vn.pop();
			}
		} else {
			if (hasNUPNToolspecific) {
				log.info("NUPN tool specific section was detected in the PNML.");
				log.info("However, no mixed generation strategy requested. Therefore the NUPN structure in that section will not be used.");
				log.info("Will continue with the default, naive generation strategy.");
			}
			// Write the number of Units. Check case there is just one place.
			if (nbPl > 1) {
				nupnsb.append(NUPNConstants.UNITS).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbPl + 1).append(NUPNConstants.WS).append(NUPNConstants.ZERO).append(NUPNConstants.DOTS)
				.append(nbPl).append(NUPNConstants.NL);
			} else {
				nupnsb.append(NUPNConstants.UNITS).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbPl).append(NUPNConstants.WS).append(NUPNConstants.ZERO).append(NUPNConstants.DOTS)
				.append(nbPl - 1).append(NUPNConstants.NL);
			}

			// Root unit declaration - id is N - 1. Check case there is just one
			// place.
			if (nbPl > 1) {
				nupnsb.append(NUPNConstants.ROOT_UNIT).append(NUPNConstants.WS).append(nbPl).append(NUPNConstants.NL);
			} else {
				nupnsb.append(NUPNConstants.ROOT_UNIT).append(NUPNConstants.WS).append(nbPl - 1).append(NUPNConstants.NL);
			}
			nupnQueue.put(nupnsb.toString());
			nupnsb.delete(0, nupnsb.length());

			// One place per unit, keep track of their PNML id in ts file
			// First the initial places
			long count = 0L;
			for (Long l : initPlaces) {
				nupnsb.append(NUPNConstants.U).append(count).append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ONE).append(NUPNConstants.WS).append(l).append(NUPNConstants.DOTS)
				.append(l).append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ZERO).append(NUPNConstants.NL);
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
				pId = placesId2NupnMap.getLong(id);
				/*
				 * if (pId == -1L) { pId = iDCount++; placesId2bpnMap.put(id,
				 * pId); }
				 */
				psmapping.append(pId).append(NUPNConstants.WS).append(id).append(NUPNConstants.NL);
				psQueue.put(psmapping.toString());
				nupnsb.append(NUPNConstants.U).append(count).append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ONE).append(NUPNConstants.WS).append(pId).append(NUPNConstants.DOTS)
				.append(pId).append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ZERO).append(NUPNConstants.NL);
				nupnQueue.put(nupnsb.toString());
				// placesId2bpnMap.put(id, iDCount);
				nupnsb.delete(0, nupnsb.length());
				psmapping.delete(0, psmapping.length());
				count++;
				// iDCount++;
				vn.pop();
			}
			// / Root Unit N and its subunits. Check case there is just one
			// place.
			if (nbPl > 1) {
				nupnsb.append(NUPNConstants.U).append(nbPl).append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ZERO).append(NUPNConstants.WS).append(NUPNConstants.ONE).append(NUPNConstants.DOTS)
				.append(NUPNConstants.ZERO).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbPl);
				for (count = 0L; count < nbPl; count++) {
					nupnsb.append(NUPNConstants.WS).append(count);
				}
			} else if (nbPl == 1) {
				// DO NOTHING, already handled above.
				log.warn("I encountered the case where there is just one place in the net.");
			} else { // FIXME This case should not happen.
				nupnsb.append(NUPNConstants.U).append(nbPl).append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ZERO).append(NUPNConstants.WS).append(NUPNConstants.ONE).append(NUPNConstants.DOTS)
				.append(NUPNConstants.ZERO).append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ZERO);
				log.error("I encountered the case where there is no place at all in the net.");
				log.error("This violates the rules stating that root unit must have at least 2 sub-units, if it does not contain any place.");
				throw new InvalidNetException("No place in the net! See error messages above.");
			}
			nupnsb.append(NUPNConstants.NL);
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
			if (Long.valueOf(mkg) == 1) {
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

	private void insertUnitSafePragma() throws InterruptedException {
		PNML2NUPNUtils.insertPragma(MainPNML2NUPN.PRAGMA_UNIT_SAFE_BY_BOUNDS + NUPNConstants.NL, nupnQueue);
	}



	/**
	 * @param journal
	 */
	private void initLog(Logger journal) {
		this.log = journal;
	}


	/**
	 * Initializes internal data structures for transitions.
	 */
	private void initTransitionsMaps() {
		if (trId2NupnMap == null) {
			trId2NupnMap = new Object2LongOpenHashMap<String>();
			trId2NupnMap.defaultReturnValue(-1L);
		}
		if (tr2InPlacesMap == null) {
			tr2InPlacesMap = new Long2ObjectOpenHashMap<>();
			tr2InPlacesMap.defaultReturnValue(null);
		}
		if (tr2OutPlacesMap == null) {
			tr2OutPlacesMap = new Long2ObjectOpenHashMap<>();
			tr2OutPlacesMap.defaultReturnValue(null);
		}
		if (tr2InAllArcsMap == null) {
			tr2InAllArcsMap = new Object2ObjectOpenHashMap<String, LongBigArrayBigList>();
			tr2InAllArcsMap.defaultReturnValue(null);
		}
		if (tr2OutAllArcsMap == null) {
			tr2OutAllArcsMap = new Object2ObjectOpenHashMap<String, LongBigArrayBigList>();
			tr2OutAllArcsMap.defaultReturnValue(null);
		}
	}

	/**
	 * Initialises the data structure for unsafe transitions.
	 */
	private void initUnsafeTransMaps() {
		if (tr2InUnsafeArcsMap == null) {
			tr2InUnsafeArcsMap = new Object2ObjectOpenHashMap<String, LongBigArrayBigList>();
			tr2InUnsafeArcsMap.defaultReturnValue(null);
		}
		if (tr2OutUnsafeArcsMap == null) {
			tr2OutUnsafeArcsMap = new Object2ObjectOpenHashMap<String, LongBigArrayBigList>();
			tr2OutUnsafeArcsMap.defaultReturnValue(null);
		}
	}

	/**
	 * Initializes internal data structures for places.
	 */
	private void initPlacesMap() {
		iDCount = 0L;
		if (placesId2NupnMap == null) {
			placesId2NupnMap = new Object2LongOpenHashMap<String>();
			placesId2NupnMap.defaultReturnValue(-1L);
		}
	}

	/**
	 * Initializes internal data structures for arcs.
	 */
	private void initUnsafeArcsMap() {
		if (unsafeArcsMap == null) {
			unsafeArcsMap = new Object2LongOpenHashMap<>();
			unsafeArcsMap.defaultReturnValue(-1);
		}
		if (unsafeNodes == null) {
			unsafeNodes = new ObjectOpenHashSet<>();
		}
	}

	/**
	 * Emergency stop.
	 * 
	 * @param outFile
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void emergencyStop(File outFile) throws InterruptedException, IOException {
		stop(outFile);
		log.error("Emergency stop. Cancelled the translation and released opened resources.");
	}

	/**
	 * Normal stop.
	 * 
	 * @param outFile
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void normalStop(File outFile) throws InterruptedException, IOException {
		stop(outFile);
	}

	/**
	 * Stop NUPN writers and releases opened resources
	 * 
	 * @param outFile
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void stop(File outFile) throws InterruptedException, IOException {
		PNML2NUPNUtils.cancelWriters(nupnQueue, tsQueue, psQueue);
		PNML2NUPNUtils.cancelWriter(uaQueue);
		PNML2NUPNUtils.closeChannels(ocbBpn, ocbTs, ocbPs);
		PNML2NUPNUtils.closeChannel(ocbUA);
		PNML2NUPNUtils.deleteOutputFiles(outFile, outTSFile, outPSFile);
		PNML2NUPNUtils.deleteOutputFile(outUAFile);
	}	


	/**
	 * Clears all internal data structures for places and transitions.
	 */
	private void clearAllCollections() {
		placesId2NupnMap.clear();
		trId2NupnMap.clear();
		tr2InPlacesMap.clear();
		tr2OutPlacesMap.clear();
		unsafeArcsMap.clear();
		tr2InUnsafeArcsMap.clear();
		tr2OutUnsafeArcsMap.clear();
		tr2InAllArcsMap.clear();
		tr2OutAllArcsMap.clear();
		unsafeNodes.clear();
	}

}	
