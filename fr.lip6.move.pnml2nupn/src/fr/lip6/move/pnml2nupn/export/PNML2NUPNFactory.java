/**
 *  Copyright 2014-2019 Université Paris Nanterre and Sorbonne Université,
 * 							CNRS, LIP6
 *
 *  All rights reserved.   This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Project leader / Initial Contributor:
 *    Lom Messan Hillah - <lom-messan.hillah@lip6.fr>
 *
 */
package fr.lip6.move.pnml2nupn.export;

import fr.lip6.move.pnml2nupn.export.impl.PNML2NUPNExporterImpl;

/**
 * Simple Exporter factory.
 *
 */
public final class PNML2NUPNFactory {

	private static final class PNML2NUPNFactoryHelper {
		private static volatile PNML2NUPNFactory INSTANCE;
		static {
			synchronized (PNML2NUPNFactoryHelper.class) {
				if (INSTANCE == null) {
					synchronized (PNML2NUPNFactoryHelper.class) {
						INSTANCE = new PNML2NUPNFactory();
					}
				}
			}
		}

		private PNML2NUPNFactoryHelper() {
			super();
		}
	}

	
	private PNML2NUPNFactory() {
		super();
	}

	
	public static PNML2NUPNFactory instance() {
		return PNML2NUPNFactoryHelper.INSTANCE;
	}

	public PNML2NUPNExporter createExporter() {
		return new PNML2NUPNExporterImpl();
	}

}
