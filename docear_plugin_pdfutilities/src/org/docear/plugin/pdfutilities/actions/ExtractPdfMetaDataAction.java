package org.docear.plugin.pdfutilities.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;

import org.docear.pdf.PdfDataExtractor;
import org.docear.plugin.pdfutilities.util.MonitoringUtils;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.URIUtils;



public class ExtractPdfMetaDataAction extends AFreeplaneAction {

	public static final String KEY = "ExtractPdfMetaDataAction";

	public ExtractPdfMetaDataAction() {
		super(KEY);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unchecked")
	public void actionPerformed(final ActionEvent evt) {
		final NodeModel targetNode = Controller.getCurrentController().getSelection().getSelected();
		
		if(MonitoringUtils.isPdfLinkedNode(targetNode)) {
			URI uri = URIUtils.getAbsoluteURI(targetNode);
			
			try {
				System.out.println(extractTitle(uri));
				System.out.println(getUniqueHashCode(uri));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static String getUniqueHashCode(URI uri) throws IOException {
		PdfDataExtractor extractor = new PdfDataExtractor(uri);
		try {
			return extractor.getUniqueHashCode();
		}
		finally {
			extractor.close();
			extractor = null;
		}
	}
	
	public static String extractTitle(URI uri) throws IOException {
		PdfDataExtractor extractor = new PdfDataExtractor(uri);
		try {
			return extractor.extractTitle();
		}
		finally {
			extractor.close();
			extractor = null;
		}
	}

}
