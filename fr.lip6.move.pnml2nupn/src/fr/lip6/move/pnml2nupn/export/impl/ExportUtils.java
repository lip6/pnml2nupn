package fr.lip6.move.pnml2nupn.export.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;

import com.ximpleware.extended.AutoPilotHuge;
import com.ximpleware.extended.NavExceptionHuge;
import com.ximpleware.extended.VTDNavHuge;
import com.ximpleware.extended.XPathEvalExceptionHuge;
import com.ximpleware.extended.XPathParseExceptionHuge;

import fr.lip6.move.pnml2nupn.MainPNML2NUPN;
import fr.lip6.move.pnml2nupn.utils.PNML2NUPNUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * Utility methods for exporters.
 *
 */
public final class ExportUtils {

	private ExportUtils() {
	}

	/**
	 * Returns true if the considered net is a PT net, false otherwise.
	 * 
	 * @param ap
	 * @param vn
	 * @param logger
	 * @return
	 * @throws XPathParseExceptionHuge
	 * @throws XPathEvalExceptionHuge
	 * @throws NavExceptionHuge
	 */
	public static boolean isPTNet(AutoPilotHuge ap, VTDNavHuge vn, Logger logger)
			throws XPathParseExceptionHuge, XPathEvalExceptionHuge, NavExceptionHuge {
		boolean result = true;
		ap.selectXPath(PNMLPaths.NETS_PATH);
		while ((ap.evalXPath()) != -1) {
			vn.push();
			String netType = vn.toString(vn.getAttrVal(PNMLPaths.TYPE_ATTR));
			logger.info("Discovered net type: {}", netType);
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

	/**
	 * Inserts unit safeness pragma (as reported by the unit safeness checking tool)
	 * @param nupnQueue the output queue
	 * @param toolName the name (and possibly the version) of the tool stating the unit safeness information.
	 * If null, a default is set {@link MainPNML2NUPN#PRAGMA_UNIT_SAFE_BY_UNKNOWN}
	 * 
	 * @throws InterruptedException
	 */
	public static void insertUnitSafePragma(BlockingQueue<String> nupnQueue, String toolName) throws InterruptedException {
		if (toolName != null) {
			PNML2NUPNUtils.insertPragma(toolName + NUPNConstants.NL, nupnQueue);
		} else {
			PNML2NUPNUtils.insertPragma(MainPNML2NUPN.PRAGMA_UNIT_SAFE_BY_UNKNOWN + NUPNConstants.NL, nupnQueue);
		}
	}

	/**
	 * Updates current label length only if new label length is strictly greater.
	 * 
	 * @param newLabel
	 */
	public static int updateLabelLength(String newLabel, int currentLength) {
		int newLength = newLabel.length();
		if (newLength > currentLength)
			return newLength;
		return currentLength;
	}

	/**
	 * Sets the labels line (i.e., header) in the NUPN.
	 * 
	 * @param nupnQueue
	 *            output queue
	 * @param labelLength
	 *            the greatest label length
	 * @param thereExistTransitions
	 *            are there any transition in the net?
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void setLabelsLine(BlockingQueue<String> nupnQueue, int labelLength, boolean thereExistTransitions)
			throws InterruptedException, IOException {
		String endOfLabel = NUPNConstants.WS + labelLength + NUPNConstants.NL;
		if (!thereExistTransitions) {
			nupnQueue.put(NUPNConstants.LABELS_1_1_0 + endOfLabel);
		} else {
			nupnQueue.put(NUPNConstants.LABELS_1_0_0 + endOfLabel);
		}
	}

	/**
	 * Append the content of a given file to a NUPN file
	 * 
	 * @param outPlacesFile
	 *            the file whose content is to be appended
	 * @param nupnQueue
	 *            the writing queue to the NUPN file
	 * @param logger
	 * @throws IOException
	 */
	public static void appendFileContentToNUPN(File outPlacesFile, BlockingQueue<String> nupnQueue, Logger logger)
			throws IOException {
		Files.lines(outPlacesFile.toPath()).forEach(l -> {
			try {
				nupnQueue.put(l + NUPNConstants.NL);
			} catch (InterruptedException e) {
				logger.error("Error while appending content of external file {} to NUPN file: {}",
						outPlacesFile.getAbsolutePath(), e.getMessage());
				PNML2NUPNUtils.printStackTrace(e);
			}
		});
	}

	/**
	 * Looks up and returns the PNML node id. 
	 * It maps the node id to its name before returning the id.
	 * @param vn
	 * @param useNodeName
	 * @param id2NameMap
	 * @return the PNML node id
	 * @throws NavExceptionHuge
	 */
	public static String getPNMLNodeId(VTDNavHuge vn, boolean useNodeName,
			Object2ObjectOpenHashMap<String, String> id2NameMap) throws NavExceptionHuge {
		String id = vn.toString(vn.getAttrVal(PNMLPaths.ID_ATTR));
		if (useNodeName) {
			vn.toElement(VTDNavHuge.FIRST_CHILD);
			while (!vn.matchElement(PNMLPaths.NAME_ELEMENT)) {
				vn.toElement(VTDNavHuge.NEXT_SIBLING);
			}
			if (vn.matchElement(PNMLPaths.NAME_ELEMENT)) {
				vn.toElement(VTDNavHuge.FIRST_CHILD);
				while (!vn.matchElement(PNMLPaths.TEXT_ELEMENT)) {
					vn.toElement(VTDNavHuge.NEXT_SIBLING);
				}
				String name = vn.toString(vn.getText()).trim();
				if (!id2NameMap.containsKey(id)) {
					id2NameMap.put(id, name);
				}
				vn.toElement(VTDNavHuge.PARENT);
				vn.toElement(VTDNavHuge.PARENT);
			} else {
				id2NameMap.put(id, NUPNConstants.NO_NAME_PREFIX);
				vn.toElement(VTDNavHuge.PARENT);
			}
		}
		return id;
	}

	/**
	 * Returns a PNML node Id or name according to the option
	 * {@link MainPNML2NUPN#USE_PLACE_NAMES} or
	 * {@link MainPNML2NUPN#USE_TRANSITION_NAMES}. If the option is set, returns the
	 * name from the map (or {@link NUPNConstants#NO_NAME_PREFIX} if empty name).
	 * 
	 * @param id
	 * @param useNodeName
	 * @param id2NameMap
	 * @return
	 */
	public static String getPNMLNodeIdOrName(String id, boolean useNodeName,
			Object2ObjectOpenHashMap<String, String> id2NameMap) {
		String name;
		if (useNodeName) {
			name = id2NameMap.get(id);
			if (name.isEmpty()) {
				name = NUPNConstants.NO_NAME_PREFIX;
			}
		} else {
			name = id;
		}
		return name;
	}

}
