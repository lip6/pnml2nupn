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
 * Bean to handle channel outputstreams.
 */
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public final class OutChannelBean {

	private FileChannel fc;
	private FileOutputStream fos;

	public OutChannelBean(FileChannel fc, FileOutputStream fos) {
		this.setFc(fc);
		this.setFos(fos);
	}

	public FileChannel getFc() {
		return fc;
	}

	public void setFc(FileChannel fc) {
		this.fc = fc;
	}

	public FileOutputStream getFos() {
		return fos;
	}

	public void setFos(FileOutputStream fos) {
		this.fos = fos;
	}

}
