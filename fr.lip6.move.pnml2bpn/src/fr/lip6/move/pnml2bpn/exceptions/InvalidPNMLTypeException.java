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
 * To document unexpected Petri net types read from PNML Documents.
 * @author lom
 *
 */
public class InvalidPNMLTypeException extends Exception {

	private static final long serialVersionUID = 4240100937495001696L;

	public InvalidPNMLTypeException() {
	}

	public InvalidPNMLTypeException(String message) {
		super(message);

	}

	public InvalidPNMLTypeException(Throwable cause) {
		super(cause);
	}

	public InvalidPNMLTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidPNMLTypeException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
