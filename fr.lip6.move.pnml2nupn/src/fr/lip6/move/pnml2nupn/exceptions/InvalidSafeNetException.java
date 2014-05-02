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
package fr.lip6.move.pnml2nupn.exceptions;

/**
 * To document that the encountered Petri Net is not 1-Safe.
 * @author lom
 *
 */
public class InvalidSafeNetException extends Exception {

	private static final long serialVersionUID = 9008960891717196431L;

	public InvalidSafeNetException() {
	}

	public InvalidSafeNetException(String message) {
		super(message);
	}

	public InvalidSafeNetException(Throwable cause) {
		super(cause);
	}

	public InvalidSafeNetException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidSafeNetException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
