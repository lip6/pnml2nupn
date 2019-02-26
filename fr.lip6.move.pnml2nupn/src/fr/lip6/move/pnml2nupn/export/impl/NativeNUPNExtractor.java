package fr.lip6.move.pnml2nupn.export.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;

import com.ximpleware.extended.AutoPilotHuge;
import com.ximpleware.extended.NavExceptionHuge;
import com.ximpleware.extended.VTDNavHuge;
import com.ximpleware.extended.XPathEvalExceptionHuge;
import com.ximpleware.extended.XPathParseExceptionHuge;

import fr.lip6.move.pnml2nupn.MainPNML2NUPN;
import fr.lip6.move.pnml2nupn.exceptions.PNMLImportExportException;
import fr.lip6.move.pnml2nupn.utils.PNML2NUPNUtils;
import it.unimi.dsi.fastutil.BigArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;

public final class NativeNUPNExtractor {

	private File inFile, outFile;
	private File outTSFile, outPSFile;
	private Logger log;
	private OutChannelBean ocbNupn, ocbTs, ocbPs;
	private BlockingQueue<String> nupnQueue, tsQueue, psQueue;
	private Thread nupnWriter, tsWriter, psWriter;
	private VTDNavHuge vn;
	private AutoPilotHuge ap;
	private long nbUnits, nbTrans, nbPlaces, nbArcs;
	private String rootUnitId;
	private boolean isSafe;
	private ObjectBigArrayBigList<String> markedPlaces;
	private Object2LongOpenHashMap<String> plId2nupnMap;
	private Object2LongOpenHashMap<String> trId2nupnMap;
	private Long2ObjectOpenHashMap<LongBigArrayBigList> tr2OutPlacesMap;
	private Long2ObjectOpenHashMap<LongBigArrayBigList> tr2InPlacesMap;
	private Object2LongOpenHashMap<String> unitsIdMap;
	private LongBigArrayBigList markedPlacesNupnId;
	private ObjectBigArrayBigList<String> nupnLines;
	private long nupnPlIdGen, nupnTrIdGen, nupnUnitIdGen, initPlacesCount;
	private final StringBuilder nupnsb;

	public NativeNUPNExtractor(File input, File output, Logger journal) {
		this.inFile = input;
		this.outFile = output;
		this.log = journal;
		nupnsb = new StringBuilder();
	}

	public void extractNUPN(VTDNavHuge vn, AutoPilotHuge ap) {
		this.vn = vn;
		this.ap = ap;
		extractNUPN();
	}

	public void extractNUPN() {
		log.info("Starting the extraction of native NUPN from PNML");

		try {
			checkAndSetNavAutopilot();
			initDataStructures();
			openIOChannels();
			startWriters();

			// Insert creator pragma
			PNML2NUPNUtils.insertCreatorPragma(nupnQueue);
			//collectPlaces();
			//collectInitialPlaces();
			extractSizes();
			extractStructure();
			
			extractUnits();
			collectInitialPlaces();
			
			// check unsafe arcs in order to writer multiple arcs pragma 
			
			writeNUPNPlaces();
			writeInitialPlaces();
			writeUnits();
			//collectTransitions();
			//writeTransitions();


		} catch (InterruptedException | PNMLImportExportException | IOException e) {
			PNML2NUPNUtils.printStackTrace(e);
		}
	}


	private void writeUnits() throws InterruptedException {
		int nbUnits = unitsIdMap.size();
		nupnsb.append(NUPNConstants.UNITS).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbUnits)
			.append(NUPNConstants.WS).append(NUPNConstants.ZERO).append(NUPNConstants.DOTS)
			.append(nbUnits - 1).append(NUPNConstants.NL);
		nupnsb.append(NUPNConstants.ROOT_UNIT).append(NUPNConstants.WS).append(unitsIdMap.getLong(rootUnitId))
			.append(NUPNConstants.NL);
		nupnQueue.put(nupnsb.toString());
		for (String l: nupnLines) {
			nupnQueue.put(l);
		}
		clearNUPNStringBuilder();
	}
		

	private void writeInitialPlaces() throws InterruptedException {
		long nbMarkedPlaces = markedPlaces.size64();
		if (nbMarkedPlaces > 1) {
			nupnsb.append(NUPNConstants.INIT_PLACES).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbMarkedPlaces);
			for (String pId : markedPlaces) {
				nupnsb.append(NUPNConstants.WS).append(plId2nupnMap.getLong(pId));
			}
		} else {
			nupnsb.append(NUPNConstants.INIT_PLACE).append(NUPNConstants.WS).append(plId2nupnMap.getLong(markedPlaces.get(0)));
		}
		nupnsb.append(NUPNConstants.NL);
		nupnQueue.put(nupnsb.toString());
		clearNUPNStringBuilder();
	}

	private void collectInitialPlaces() throws PNMLImportExportException {
		log.info("Collecting initially marked places...");
		String placeId;
		long plNupnId;
		long mkg, totalMkg = 0L;
		ObjectBigArrayBigList<String> unsafePlaces = new ObjectBigArrayBigList<>();
		
		LongArrayList minMarking = new LongArrayList(2);
		LongArrayList maxMarking = new LongArrayList(2);
		try {
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.MARKED_PLACES);
			while((ap.evalXPath()) != -1) {
				vn.push();
				placeId = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
				plNupnId = plId2nupnMap.getLong(placeId);
				if (plNupnId == -1L) {
					log.error("Marked place {} was not reported in the NUPN toolspecific section!", placeId);
				}
				markedPlaces.add(placeId);
				markedPlacesNupnId.add(plNupnId);
				vn.toElement(VTDNavHuge.FIRST_CHILD);
				while (!vn.matchElement(NUPNConstants.TEXT)) {
					vn.toElement(VTDNavHuge.NEXT_SIBLING);
				}
				totalMkg += mkg = Long.parseLong(vn.toString(vn.getText()).trim());
				
				vn.toElement(VTDNavHuge.PARENT);
				vn.toElement(VTDNavHuge.PARENT);
				if (mkg > 1) {
					unsafePlaces.add(placeId);
					PNML2NUPNUtils.setMin(mkg, minMarking);
					PNML2NUPNUtils.setMax(mkg, maxMarking);
				}
				vn.pop();
			}
			log.info("Initial place(s): {}", markedPlaces.toString());
			long nbUnsafePlaces = unsafePlaces.size64();
			long nbMarkedPlaces = markedPlaces.size64();
			if (nbUnsafePlaces> 0) {
				nupnsb.append(MainPNML2NUPN.PRAGMA_MULTIPLE_INIT_TOKEN).append(NUPNConstants.HK).append(totalMkg)
					.append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbUnsafePlaces).append(NUPNConstants.WS)
					.append(minMarking.getLong(0)).append(NUPNConstants.DOTS).append(maxMarking.getLong(0)).append(NUPNConstants.NL);
				nupnQueue.put(nupnsb.toString());
				clearNUPNStringBuilder();
				log.warn("There are {} unsafe initial places in this net.", nbUnsafePlaces);
				log.warn("Unsafe initial places: {}", unsafePlaces.toString());
			
				log.info("Checking invariant 'total nb of tokens > nb initial places': {}", totalMkg > nbMarkedPlaces);
				log.info("Checking invariant 'nb unsafe initial places <= nb initial places': {}",
						nbUnsafePlaces <= nbMarkedPlaces);
				log.info(
						"Checking invariant '(nb_init - nb_places) + (nb_places * min) <= nb_tokens <= (nb_init - nb_places) + (nb_places * max)': {}",
						(nbMarkedPlaces - nbUnsafePlaces) + (nbUnsafePlaces* minMarking.getLong(0)) <= totalMkg
						&& totalMkg <= (nbMarkedPlaces - nbUnsafePlaces) + (nbUnsafePlaces * maxMarking.getLong(0)));
				MainPNML2NUPN.appendMesgLineToSignature("decreased to one the marking of " + nbUnsafePlaces
						+ " initial places");
			}
		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge | InterruptedException e) {
			throw new PNMLImportExportException(e);
		}
		ap.resetXPath();
	}

	private void extractSizes() throws PNMLImportExportException {
		try {
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.NUPN_SIZE);
			while ((ap.evalXPath()) != -1) {
				vn.push();
				nbPlaces = Long.parseLong(vn.toString(vn.getAttrVal(PNMLPaths.PLACES_ATTR)));
				nbTrans = Long.parseLong(vn.toString(vn.getAttrVal(PNMLPaths.TRANS_ATTR)));
				nbArcs = Long.parseLong(vn.toString(vn.getAttrVal(PNMLPaths.ARCS_ATTR)));
				vn.pop();
			}
			log.info("Nb places = {}; nb transitions = {}; nb arcs = {}", nbPlaces, nbTrans, nbArcs);

		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge e) {
			throw new PNMLImportExportException(e);
		}
		ap.resetXPath();
	}

	private void extractStructure() throws PNMLImportExportException {
		try {
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.NUPN_STRUCTURE);
			while ((ap.evalXPath()) != -1) {
				vn.push();
				nbUnits = Long.parseLong(vn.toString(vn.getAttrVal(PNMLPaths.UNITS_ATTR)));
				rootUnitId = vn.toString(vn.getAttrVal(PNMLPaths.ROOT_ATTR));
				plId2nupnMap.put(rootUnitId, nupnUnitIdGen++);
				isSafe = Boolean.valueOf((vn.toString(vn.getAttrVal(PNMLPaths.SAFE_ATTR))));
				vn.pop();
			}
			log.info("Nb units = {}; root unit id = {}; is Safe = {}", nbUnits, rootUnitId, isSafe);
			if (isSafe) {
				insertUnitSafePragma();
			}
		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge | InterruptedException e) {
			throw new PNMLImportExportException(e);
		}
		ap.resetXPath();
	}

	private void writeNUPNPlaces() throws PNMLImportExportException {
		ap.resetXPath();
		try {
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.COUNT_PLACES_PATH);
			long nbPl = (long) ap.evalXPathToNumber();
			if (nbPl != nbPlaces) {
				log.error("The number of places I counted in the PNML file ({}) is not equal to the number reported ({}) in the NUPN size element!", nbPl, nbPlaces);
				log.warn("I will output in the NUPN the number reported in the NUPN size element.");
			}
			nupnsb.append(NUPNConstants.PLACES).append(NUPNConstants.WS).append(NUPNConstants.HK).append(nbPlaces)
			.append(NUPNConstants.WS).append(NUPNConstants.ZERO).append(NUPNConstants.DOTS).append(nbPlaces - 1)
			.append(NUPNConstants.NL);
			nupnQueue.put(nupnsb.toString());
			clearNUPNStringBuilder();
		} catch (NavExceptionHuge | XPathParseExceptionHuge | InterruptedException e) {
			throw new PNMLImportExportException(e);
		}
	}

	private void extractUnits() throws PNMLImportExportException {
		try {
			String places = "", subunits = "";
			String[] elemId;
			String unitSId;
			long unitLId;
			long plId;
			LongList placesIntId = new LongArrayList();
			final StringBuilder psmapping = new StringBuilder();
			vn.toElement(VTDNavHuge.ROOT);
			ap.selectXPath(PNMLPaths.NUPN_UNIT);
			while ((ap.evalXPath()) != -1) {
				vn.push();
				unitSId = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
				unitLId = unitsIdMap.getLong(unitSId);
				if (unitLId == -1L) {
					unitLId = nupnUnitIdGen++;
					unitsIdMap.put(unitSId, unitLId);
				}
				nupnsb.append("U").append(unitLId);
				// places
				vn.toElement(VTDNavHuge.FIRST_CHILD);
				if (vn.getText() != -1) {
					places = vn.toString(vn.getText()).trim();
				}
				if (!places.isEmpty()) {
					elemId = places.split(NUPNConstants.WS);
					for (String s : elemId) {
						plId = plId2nupnMap.getLong(s);
						if (plId == -1L) {
							plId = nupnPlIdGen++;
							plId2nupnMap.put(s, plId);
						}
						placesIntId.add(plId);
						psmapping.append(plId).append(NUPNConstants.WS).append(s).append(NUPNConstants.NL);
						psQueue.put(psmapping.toString());
						psmapping.delete(0, psmapping.length());
					}
					nupnsb.append(NUPNConstants.WS).append(NUPNConstants.HK).append(placesIntId.size());
					PNML2NUPNUtils.debug("Collected places in unit {} ({}): {} ", log, unitSId, unitLId, placesIntId.toString());
					if (placesIntId.size() > 1) {
						nupnsb.append(NUPNConstants.WS).append(placesIntId.getLong(0)).append(NUPNConstants.DOTS).append(placesIntId.getLong(placesIntId.size() - 1));
						// TODO: check and remove this line - placesIntId.stream().forEach(i -> nupnsb.append(NUPNConstants.WS).append(i));
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
							unitLId = nupnUnitIdGen++;
							unitsIdMap.put(s, unitLId);
						}
						nupnsb.append(NUPNConstants.WS).append(unitLId);
					}
				} else {
					nupnsb.append(NUPNConstants.WS).append(NUPNConstants.HK).append(NUPNConstants.ZERO);
				}
				nupnsb.append(NUPNConstants.NL);
				
				nupnLines.add(nupnsb.toString());
				clearNUPNStringBuilder();
				placesIntId.clear();
				subunits = "";
				places = "";
				vn.pop();
			}
		} catch (NavExceptionHuge | XPathParseExceptionHuge | XPathEvalExceptionHuge | InterruptedException e) {
			throw new PNMLImportExportException(e);
		}
		ap.resetXPath();
	}

	private void insertUnitSafePragma() throws InterruptedException {
		PNML2NUPNUtils.insertPragma(MainPNML2NUPN.PRAGMA_UNIT_SAFE_BY_CREATOR + NUPNConstants.NL, nupnQueue);
	}

	private void checkAndSetNavAutopilot() throws PNMLImportExportException {
		if (vn == null || ap == null) {
			vn = PNML2NUPNUtils.openXMLStream(inFile).getNav();
			ap = new AutoPilotHuge(vn);
		}
	}

	private void openIOChannels() throws IOException {
		outTSFile = new File(PNML2NUPNUtils.extractBaseName(outFile.getCanonicalPath()) + NUPNConstants.TRANS_EXT);
		outPSFile = new File(PNML2NUPNUtils.extractBaseName(outFile.getCanonicalPath()) + NUPNConstants.STATES_EXT);
		ocbNupn = PNML2NUPNUtils.openOutChannel(outFile);
		nupnQueue = PNML2NUPNUtils.initQueue();
		tsQueue = PNML2NUPNUtils.initQueue();
		psQueue = PNML2NUPNUtils.initQueue();

	}

	private void startWriters() {
		nupnWriter = PNML2NUPNUtils.startWriter(ocbNupn, nupnQueue);
		tsWriter = PNML2NUPNUtils.startWriter(ocbTs, tsQueue);
		psWriter = PNML2NUPNUtils.startWriter(ocbPs, psQueue);
	}

	private void emergencyStop(File outFile) throws InterruptedException, IOException {
		stop(outFile);
		log.error("Emergency stop. Cancelled the translation and released opened resources.");
	}

	private void clearNUPNStringBuilder() {
		nupnsb.delete(0, nupnsb.length());
	}

	private void stop(File outFile) throws InterruptedException, IOException {
		PNML2NUPNUtils.cancelWriters(nupnQueue, tsQueue, psQueue);
		PNML2NUPNUtils.closeChannels(ocbNupn, ocbTs, ocbPs);
		PNML2NUPNUtils.deleteOutputFiles(outFile, outTSFile, outPSFile);
	}

	private void initDataStructures() {
		if (trId2nupnMap == null) {
			trId2nupnMap = new Object2LongOpenHashMap<String>();
			trId2nupnMap.defaultReturnValue(-1L);
		}
		if (tr2InPlacesMap == null) {
			tr2InPlacesMap = new Long2ObjectOpenHashMap<>();
			tr2InPlacesMap.defaultReturnValue(null);
		}
		if (tr2OutPlacesMap == null) {
			tr2OutPlacesMap = new Long2ObjectOpenHashMap<>();
			tr2OutPlacesMap.defaultReturnValue(null);
		}
		if (plId2nupnMap == null) {
			plId2nupnMap = new Object2LongOpenHashMap<String>();
			plId2nupnMap.defaultReturnValue(-1L);
		}
		if (unitsIdMap == null) {
			unitsIdMap = new Object2LongOpenHashMap<>();
			unitsIdMap.defaultReturnValue(-1L);
		}
		markedPlaces = new ObjectBigArrayBigList<>();
		markedPlacesNupnId = new LongBigArrayBigList();
		nupnLines = new ObjectBigArrayBigList<>();
		initPlacesCount = 0L;
		nupnPlIdGen = 0L;
		nupnTrIdGen = 0L;
		nupnUnitIdGen = 0L;
	}


	/**
	 * Clears all internal data structures for places and transitions.
	 */
	private void clearDataStructures() {
		plId2nupnMap.clear();
		trId2nupnMap.clear();
		tr2InPlacesMap.clear();
		tr2OutPlacesMap.clear();
		unitsIdMap.clear();
		markedPlacesNupnId.clear();
	}

}
