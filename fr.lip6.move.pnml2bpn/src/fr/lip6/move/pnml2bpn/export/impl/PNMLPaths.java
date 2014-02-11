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

/**
 * A set of in-memory XPATH queries used to parsing PNML.
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
	
	public static final String PLACES_MARKING = PLACES_PATH + "/initialMarking[text > 0]";
	
	public static final String PLACES_PATH_EXCEPT_MKG = PAGES_PATH + "/child::place[not(child::initialMarking)]" 
			+ " | " + PLACES_PATH + "/initialMarking[text <= 0]";
	
	
	public static final String TRANSITIONS_PATH = PAGES_PATH + "/transition";
	
	public static final String ARCS_PATH = PAGES_PATH + "/arc";

	public static final String COUNT_PLACES_PATH = "count(" + PLACES_PATH + ")";

	public static final String COUNT_TRANSITIONS_PATH = "count(" + TRANSITIONS_PATH + ")";

}
