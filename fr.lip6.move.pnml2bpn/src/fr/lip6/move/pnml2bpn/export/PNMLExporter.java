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

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;

import fr.lip6.move.pnml2bpn.exceptions.InvalidPNMLTypeException;
import fr.lip6.move.pnml2bpn.exceptions.PNMLImportExportException;

/**
 * <p>
 * Interface to implement for exporting PNML into other formats.
 * </p>
 * <p>
 * It is offered as a convenience. The Event Bus method could alternatively be
 * used.
 * </p>
 * 
 * @author lom
 * 
 */
public interface PNMLExporter {
	void exportPNML(URI inFile, URI outFile, Logger journal)
			throws PNMLImportExportException, InterruptedException, IOException;

	void exportPNML(File inFile, File outFile, Logger journal)
			throws PNMLImportExportException, InterruptedException, IOException;

	void exportPNML(String inFile, String outFile, Logger journal)
			throws PNMLImportExportException, InterruptedException, IOException;

	void hasUnsafeArcs(String inFile, String outFile, Logger journal)
			throws InvalidPNMLTypeException, IOException,
			PNMLImportExportException;
}
