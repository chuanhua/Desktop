package org.docear.plugin.pdfutilities.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.docear.plugin.pdfutilities.features.AnnotationModel;
import org.docear.plugin.pdfutilities.features.IAnnotation.AnnotationType;
import org.docear.plugin.pdfutilities.pdf.PdfAnnotationImporter;
import org.docear.plugin.pdfutilities.util.MonitoringUtils;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.URIUtils;

import de.intarsys.pdf.cos.COSRuntimeException;
import de.intarsys.pdf.parser.COSLoadException;

@EnabledAction(checkOnNodeChange=true)
public class ImportAllAnnotationsAction extends ImportAnnotationsAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;	

	@SuppressWarnings("serial")
	public ImportAllAnnotationsAction(String key) {
		super(key);
		this.setEnableType(new ArrayList<AnnotationType>(){{ add(AnnotationType.PDF_FILE); }});
	}

	public void actionPerformed(ActionEvent event) {
		NodeModel selected = Controller.getCurrentController().getSelection().getSelected();
		if(selected == null){
			return;
		}
		
		else{
			URI uri = URIUtils.getAbsoluteURI(selected);
            try {
            	PdfAnnotationImporter importer = new PdfAnnotationImporter();            	
				List<AnnotationModel> annotations = importer.importAnnotations(uri); 
				//System.gc();
				MonitoringUtils.insertChildNodesFrom(annotations, selected.isLeft(), selected);
			} catch (IOException e) {
				LogUtils.severe("ImportAllAnnotationsAction IOException at URI("+uri+"): ", e); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (COSLoadException e) {
				LogUtils.severe("ImportAllAnnotationsAction COSLoadException at URI("+uri+"): ", e); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (COSRuntimeException e) {
				LogUtils.severe("ImportAllAnnotationsAction COSRuntimeException at URI("+uri+"): ", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
}
