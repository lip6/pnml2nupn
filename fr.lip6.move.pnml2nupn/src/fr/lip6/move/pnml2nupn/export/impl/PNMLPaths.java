/**
 *  Copyright 2014-2016 Université Paris Ouest and Sorbonne Universités,
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

/**
 * A set of in-memory XPATH queries used to parse PNML.
 * @author lom
 *
 */
public final class PNMLPaths {

	private PNMLPaths() {
		super();
	}

	public static final String PTNET_TYPE = "ptnet";
	
	public static final String ID_ATTR = "id";
	
	public static final String SRC_ATTR = "source";
	
	public static final String TRG_ATTR = "target";
	
	public static final String TYPE_ATTR = "type";
	
	public static final String NETS_PATH = "/pnml/net";
	
	public static final String PAGES_PATH = NETS_PATH + "/page";
	
	public static final String PLACES_PATH = PAGES_PATH + "/place";
	
	public static final String MARKED_PLACES = PLACES_PATH + "/initialMarking[text > 0]";
	
	public static final String UNSAFE_MARKED_PLACES = PLACES_PATH + "/initialMarking[text > 1]";
	
	
	public static final String COUNT_MARKED_PLACES = "count(" + MARKED_PLACES + ")";
	
	public static final String PLACES_PATH_EXCEPT_MKG = PAGES_PATH + "/child::place[not(child::initialMarking)]" 
			+ " | " + PAGES_PATH  + "/child::place[child::initialMarking[text <= 0]]";
	
	public static final String TRANSITIONS_PATH = PAGES_PATH + "/transition";
	
	public static final String ARCS_PATH = PAGES_PATH + "/arc";
	
	public static final String UNSAFE_ARCS = ARCS_PATH + "/inscription[text > 1]";

	public static final String COUNT_PLACES_PATH = "count(" + PLACES_PATH + ")";

	public static final String COUNT_TRANSITIONS_PATH = "count(" + TRANSITIONS_PATH + ")";
	
	public static final String TOOL_SPECIFIC = PAGES_PATH + "/toolspecific";
	
	public static final String NUPN_TOOL_SPECIFIC = TOOL_SPECIFIC + "[@tool='nupn']";
	
	public static final String NUPN_SIZE = NUPN_TOOL_SPECIFIC + "/size";
	
	public static final String NUPN_STRUCTURE = NUPN_TOOL_SPECIFIC + "/structure";
	
	public static final String NUPN_UNIT = NUPN_STRUCTURE + "/unit";
	
	public static final String NUPN_UNIT_PLACES = NUPN_UNIT + "/places";
	
	public static final String NUPN_UNIT_SUBUNITS = NUPN_UNIT + "/subunits";
	
	public static final String TOOL_ATTR = "tool";
	
	public static final String VERSION_ATTR = "version";
	
	public static final String PLACES_ATTR = "places";
	
	public static final String TRANS_ATTR = "transitions";
	
	public static final String ARCS_ATTR = "arcs";
	
	public static final String UNITS_ATTR = "units";
	
	public static final String ROOT_ATTR = "root";
	
	public static final String SAFE_ATTR = "safe";

}
