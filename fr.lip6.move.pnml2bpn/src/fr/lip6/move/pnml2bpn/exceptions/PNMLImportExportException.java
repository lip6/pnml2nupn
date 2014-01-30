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
package fr.lip6.move.pnml2bpn.exceptions;

/**
 * General-purpose exception for the translation tool.
 * @author lom
 *
 */
public class PNMLImportExportException extends Exception {

	
	public PNMLImportExportException(String message) {
		super(message);
	}

	private static final long serialVersionUID = -2381178550697245299L;

	public PNMLImportExportException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PNMLImportExportException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	public PNMLImportExportException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public PNMLImportExportException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
