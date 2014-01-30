/**
 *  Copyright 2014 Universite Paris Ouest Nanterre & Sorbonne Universites, Univ. Paris 06 - CNRS UMR 7606 (LIP6/MoVe)
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
package fr.lip6.move.pnml2bpn.export;

import fr.lip6.move.pnml2bpn.export.impl.PNML2BPNExporter;

/**
 * Simple Exporter factory.
 * @author lom
 *
 */
public final class PNML2BPNFactory {

	public PNML2BPNFactory() {
		super();
	}


	public PNMLExporter createExporter() {
		return new PNML2BPNExporter();
	}

}
