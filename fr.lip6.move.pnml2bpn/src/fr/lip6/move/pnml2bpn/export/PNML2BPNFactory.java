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
package fr.lip6.move.pnml2bpn.export;

import fr.lip6.move.pnml2bpn.export.impl.PNML2BPNExporter;

/**
 * Simple Exporter factory.
 * @author lom
 *
 */
public final class PNML2BPNFactory {

	private static final class PNML2BPNFactoryHelper {
		private static volatile PNML2BPNFactory INSTANCE;
		static {
			synchronized (PNML2BPNFactoryHelper.class) {
				if (INSTANCE == null) {
					synchronized (PNML2BPNFactoryHelper.class) {
						INSTANCE = new PNML2BPNFactory();
					}
				}
			}
		}

		private PNML2BPNFactoryHelper() {
			super();
		}
	}

	
	private PNML2BPNFactory() {
		super();
	}

	
	public static PNML2BPNFactory instance() {
		return PNML2BPNFactoryHelper.INSTANCE;
	}

	public PNMLExporter createExporter() {
		return new PNML2BPNExporter();
	}

}
