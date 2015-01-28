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
package fr.lip6.move.pnml2nupn.export;

import fr.lip6.move.pnml2nupn.export.impl.PNML2NUPNExporter;

/**
 * Simple Exporter factory.
 * @author lom
 *
 */
public final class PNML2NUPNFactory {

	private static final class PNML2BPNFactoryHelper {
		private static volatile PNML2NUPNFactory INSTANCE;
		static {
			synchronized (PNML2BPNFactoryHelper.class) {
				if (INSTANCE == null) {
					synchronized (PNML2BPNFactoryHelper.class) {
						INSTANCE = new PNML2NUPNFactory();
					}
				}
			}
		}

		private PNML2BPNFactoryHelper() {
			super();
		}
	}

	
	private PNML2NUPNFactory() {
		super();
	}

	
	public static PNML2NUPNFactory instance() {
		return PNML2BPNFactoryHelper.INSTANCE;
	}

	public PNMLExporter createExporter() {
		return new PNML2NUPNExporter();
	}

}
