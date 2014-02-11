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
package fr.lip6.move.pnml2bpn.exceptions;

/**
 * Documents every invalid net cause, except for not being 1-Safe.
 * @author lom
 *
 */
public class InvalidNetException extends Exception {

	private static final long serialVersionUID = 7768156446274359970L;

	public InvalidNetException() {
		super();
	}

	public InvalidNetException(String message) {
		super(message);
	}

	public InvalidNetException(Throwable cause) {
		super(cause);
	}

	public InvalidNetException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidNetException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
