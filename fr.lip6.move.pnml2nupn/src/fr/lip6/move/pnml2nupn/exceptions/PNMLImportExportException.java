/**
 *  Copyright 2014-2019 Université Paris Nanterre and Sorbonne Université,
 *                CNRS UMR 7606 (LIP6)
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
package fr.lip6.move.pnml2nupn.exceptions;

/**
 * General-purpose exception for the translation tool.
 * @author lom
 */
public class PNMLImportExportException extends Exception {

	
	public PNMLImportExportException(String message) {
		super(message);
	}

	private static final long serialVersionUID = -2381178550697245299L;

	public PNMLImportExportException() {
		super();
	}

	public PNMLImportExportException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PNMLImportExportException(String message, Throwable cause) {
		super(message, cause);
	}

	public PNMLImportExportException(Throwable cause) {
		super(cause);
	}
}
