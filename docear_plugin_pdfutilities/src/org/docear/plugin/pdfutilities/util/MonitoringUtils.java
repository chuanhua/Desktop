package org.docear.plugin.pdfutilities.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.docear.plugin.core.CoreConfiguration;
import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.util.NodeUtilities;
import org.docear.plugin.core.workspace.AVirtualDirectory;
import org.docear.plugin.core.workspace.model.DocearWorkspaceProject;
import org.docear.plugin.pdfutilities.PdfUtilitiesController;
import org.docear.plugin.pdfutilities.features.AnnotationID;
import org.docear.plugin.pdfutilities.features.AnnotationModel;
import org.docear.plugin.pdfutilities.features.AnnotationNodeModel;
import org.docear.plugin.pdfutilities.features.DocearNodeMonitoringExtension.DocearExtensionKey;
import org.docear.plugin.pdfutilities.features.DocearNodeMonitoringExtensionController;
import org.docear.plugin.pdfutilities.features.IAnnotation;
import org.docear.plugin.pdfutilities.features.IAnnotation.AnnotationType;
import org.docear.plugin.pdfutilities.map.AnnotationController;
import org.docear.plugin.pdfutilities.pdf.PdfFileFilter;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.url.UrlManager;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.features.WorkspaceMapModelExtension;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;

public abstract class MonitoringUtils {
	
	public static boolean isMonitoringNode(NodeModel node) {
		NodeAttributeTableModel attributeModel = (NodeAttributeTableModel) node.getExtension(NodeAttributeTableModel.class);
		return (attributeModel != null && attributeModel.getAttributeKeyList().contains(PdfUtilitiesController.MON_INCOMING_FOLDER));
	}

	public static File getPdfDirFromMonitoringNode(NodeModel node) {
		if(!isMonitoringNode(node)) return null;
		NodeAttributeTableModel attributeModel = (NodeAttributeTableModel) node.getExtension(NodeAttributeTableModel.class);
		if(attributeModel == null || !attributeModel.getAttributeKeyList().contains(PdfUtilitiesController.MON_INCOMING_FOLDER)){
			return null;
		}
		
		Object value  = attributeModel.getValue(attributeModel.getAttributePosition(PdfUtilitiesController.MON_INCOMING_FOLDER));
		if (value instanceof String) {
			try {
				value = new URI((String) value);
			}
			catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		WorkspaceMapModelExtension ext = WorkspaceController.getMapModelExtension(node.getMap());
		if(ext == null || ext.getProject() == null) {
			return null;
		}
		if(value.toString().equals(CoreConfiguration.DOCUMENT_REPOSITORY_PATH)){
			return ((DocearWorkspaceProject)ext.getProject()).getProjectLiteratureRepository();
			
		}
		else{			
			try {
				return URIUtils.getAbsoluteFile(UrlManager.getController().getAbsoluteUri(node.getMap(), (URI)value));
			} catch (MalformedURLException e) {
				return null;
			}
		}
	}
	
	public static List<URI> getMindmapDirFromMonitoringNode(NodeModel node) {
		List<URI> result = new ArrayList<URI>();
		if(!isMonitoringNode(node)) return result;
		NodeAttributeTableModel attributeModel = (NodeAttributeTableModel) node.getExtension(NodeAttributeTableModel.class);
		if(attributeModel == null || !attributeModel.getAttributeKeyList().contains(PdfUtilitiesController.MON_MINDMAP_FOLDER)){
			return result;
		}
		
		Object value = attributeModel.getValue(attributeModel.getAttributePosition(PdfUtilitiesController.MON_MINDMAP_FOLDER));
		
		if(value.toString().equals(CoreConfiguration.LIBRARY_PATH)){
			AWorkspaceProject project = WorkspaceController.getProject(node.getMap());
			if(project == null || !(project instanceof DocearWorkspaceProject)) {
				//WORKSPACE - DOCEAR info: better with an exception? 
				return result;
			}
			return ((DocearWorkspaceProject) project).getLibraryMaps();
		}
		else{			
			result.add(URIUtils.getAbsoluteURI((URI)value));
			return result;
		}		
	}

	public static void removeMonitoringEntries(NodeModel selected) {
		NodeAttributeTableModel attributeModel = (NodeAttributeTableModel) selected.getExtension(NodeAttributeTableModel.class);
		if(attributeModel == null) return;
		
		if(attributeModel.getAttributeKeyList().contains(PdfUtilitiesController.MON_INCOMING_FOLDER)){
			AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributeModel, attributeModel.getAttributePosition(PdfUtilitiesController.MON_INCOMING_FOLDER));			
		}
		
		if(attributeModel.getAttributeKeyList().contains(PdfUtilitiesController.MON_MINDMAP_FOLDER)){
			AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributeModel, attributeModel.getAttributePosition(PdfUtilitiesController.MON_MINDMAP_FOLDER));			
		}
		
		if(attributeModel.getAttributeKeyList().contains(PdfUtilitiesController.MON_AUTO)){
			AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributeModel, attributeModel.getAttributePosition(PdfUtilitiesController.MON_AUTO));			
		}
		
		if(attributeModel.getAttributeKeyList().contains(PdfUtilitiesController.MON_SUBDIRS)){
			AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributeModel, attributeModel.getAttributePosition(PdfUtilitiesController.MON_SUBDIRS));			
		}
		
		if(attributeModel.getAttributeKeyList().contains(PdfUtilitiesController.MON_FLATTEN_DIRS)){
			AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributeModel, attributeModel.getAttributePosition(PdfUtilitiesController.MON_FLATTEN_DIRS));			
		}
	}
	
	public static boolean isAutoMonitorNode(NodeModel node) {
		if(NodeUtilities.getAttributeValue(node, PdfUtilitiesController.MON_AUTO) == null) return false;
		Integer value = NodeUtilities.getAttributeIntValue(node, PdfUtilitiesController.MON_AUTO);	
		
		switch(value){
			
			case 0:
				return false;				
				
			case 1:
				return true;				
				
			case 2:
				return DocearController.getPropertiesController().getBooleanProperty("docear_auto_monitoring"); //$NON-NLS-1$
				
			default:
				return false;
		}
	}
	
	public static Stack<File> getFolderStructureStack(NodeModel monitoringNode, URI pdfFile){
		Stack<File> folderStack = new Stack<File>();		
		File pdfDirFile = getPdfDirFromMonitoringNode(monitoringNode);
		if(pdfDirFile == null || !pdfDirFile.exists() || !pdfDirFile.isDirectory()){
			return folderStack;
		}
		File parent = URIUtils.getAbsoluteFile(pdfFile).getParentFile();
		while(parent != null && !isParent(pdfDirFile, parent)){
			folderStack.push(parent);
			parent = parent.getParentFile();
			if(parent == null){
				folderStack.clear();
			}
		}
		return folderStack;
	}
	
	public static boolean isParent(File parent, File f) {
		File file = new File(f.toURI().normalize());
		if(parent instanceof AVirtualDirectory) {
			for(File fi : parent.listFiles()) {
				File parentFile = new File(fi.toURI().normalize());
				if(file.equals(parentFile)) {
					return true;
				}
			}
		}
		else {
			return file.equals(parent);
		}
		return false;
	}
	
	public static boolean isPdfLinkedNode(NodeModel node){
		URI link = NodeUtilities.getURI(node);		
        return new PdfFileFilter().accept(link);
    }
	

	public static List<NodeModel> insertNewChildNodesFrom(URI pdfUri, Collection<AnnotationModel> annotations, boolean isLeft, boolean flattenSubfolder, NodeModel target){
		File pdfFile = URIUtils.getAbsoluteFile(pdfUri);
		AnnotationModel root = new AnnotationModel(0, AnnotationType.PDF_FILE);
		root.setSource(pdfFile.toURI());
		root.setTitle(pdfFile.getName());
		root.getChildren().addAll(annotations);
		Collection<AnnotationModel> newList = new ArrayList<AnnotationModel>();
		newList.add(root);
		if(!flattenSubfolder){		
			Stack<File> folderStack = getFolderStructureStack(target, pdfUri);
			target = createFolderStructurePath(target, folderStack);
		}
		return insertNewChildNodesFrom(newList, isLeft, target, target);
	}
	
	public static NodeModel createFolderStructurePath(NodeModel target, Stack<File> pathStack) {
		if (pathStack.isEmpty()) {
			return target;
		}
		File parent = pathStack.pop();
		NodeModel pathNode = null;
		for (NodeModel child : target.getChildren()) {
			if (child.getText().equals(parent.getName()) && DocearNodeMonitoringExtensionController.containsKey(child, DocearExtensionKey.MONITOR_PATH)) {
				pathNode = child;
				break;
			}
		}
		if (pathNode != null) {
			return createFolderStructurePath(pathNode, pathStack);
		}
		else {
			pathNode = ((MMapController) Controller.getCurrentModeController().getMapController()).newNode(parent.getName(), target.getMap());
			DocearNodeMonitoringExtensionController.setEntry(pathNode, DocearExtensionKey.MONITOR_PATH, null);
			NodeUtilities.setLinkFrom(LinkController.normalizeURI(parent.toURI()), pathNode);
			NodeUtilities.insertChildNodeFrom(pathNode, target.isLeft(), target);
			return createFolderStructurePath(pathNode, pathStack);
		}
	}
	public static Map<AnnotationID, Collection<AnnotationNodeModel>> getOldAnnotationsFromMaps(Collection<URI> mindmaps){
		Map<AnnotationID, Collection<AnnotationNodeModel>> result = new HashMap<AnnotationID, Collection<AnnotationNodeModel>>();
		for(MapModel map : NodeUtilities.getMapsFromUris(mindmaps)){
			
			Map<AnnotationID, Collection<AnnotationNodeModel>> temp = getOldAnnotationsFrom(map.getRootNode());
			for(AnnotationID id : temp.keySet()){
				if(!result.containsKey(id)){
					result.put(id, new ArrayList<AnnotationNodeModel>());				
				}
				result.get(id).addAll(temp.get(id));
			}
		} 
		return result;
	}
	
	public static Map<AnnotationID, Collection<AnnotationNodeModel>> getOldAnnotationsFromCurrentMap(){
		return getOldAnnotationsFrom(((MMapController) Controller.getCurrentModeController().getMapController()).getRootNode());
	}
	
	private static Map<AnnotationID, Collection<AnnotationNodeModel>> getOldAnnotationsFrom(NodeModel parent){
		Map<AnnotationID, Collection<AnnotationNodeModel>> result = new HashMap<AnnotationID, Collection<AnnotationNodeModel>>();
		try {
			Thread.sleep(1L);
			if(Thread.currentThread().isInterrupted()) return result;				
		} catch (InterruptedException e) {			
		}
		if(isPdfLinkedNode(parent)){
			URI uri = URIUtils.getAbsoluteURI(NodeUtilities.getURI(parent));
			AnnotationNodeModel oldAnnotation = AnnotationController.getAnnotationNodeModel(parent);
			if(uri != null && oldAnnotation != null){				
				result.put(oldAnnotation.getAnnotationID(), new ArrayList<AnnotationNodeModel>());				
				result.get(oldAnnotation.getAnnotationID()).add(oldAnnotation);
			}		 
		}
		
		for(NodeModel child : parent.getChildren()){
			Map<AnnotationID, Collection<AnnotationNodeModel>> children = getOldAnnotationsFrom(child);
			for(AnnotationID id : children.keySet()){
				if(!result.containsKey(id)){
					result.put(id, new ArrayList<AnnotationNodeModel>());				
				}
				result.get(id).addAll(children.get(id));
			}
		}
		
		return result;
	}
	
	public static Map<AnnotationID, Collection<AnnotationNodeModel>> getOldAnnotationsFromMap(URI mindmap){
		MapModel map = NodeUtilities.getMapFromUri(mindmap);
		if(map != null){
			return getOldAnnotationsFrom(map.getRootNode());
		}
		return new HashMap<AnnotationID, Collection<AnnotationNodeModel>>();
	}
	
	public static NodeModel insertChildNodesFromPdf(URI pdfFile, List<AnnotationModel> annotations, boolean isLeft, NodeModel target){
		NodeModel node = insertChildNodeFrom(pdfFile, isLeft, target, AnnotationType.PDF_FILE);
		insertChildNodesFrom(annotations, isLeft, node);
		return node;
	}
	
	public static List<NodeModel> insertChildNodesFrom(List<AnnotationModel> annotations, boolean isLeft, NodeModel target){
		List<NodeModel> nodes = new ArrayList<NodeModel>();
		
		for(AnnotationModel annotation : annotations){
			NodeModel node = insertChildNodeFrom(annotation.getSource(), annotation, isLeft, target);
			insertChildNodesFrom(annotation.getChildren(), isLeft, node);
			nodes.add(node);
		}
		
		return nodes;
	}
	
	public static NodeModel insertChildNodeFrom(URI uri, boolean isLeft, NodeModel target, AnnotationType type){
		if(uri == null) {
			return null;
		}
		File file = URIUtils.getAbsoluteFile(uri);
		final NodeModel node = ((MMapController) Controller.getCurrentModeController().getMapController()).newNode(file.getName(), target.getMap());
		
		
		if(type != null){
			AnnotationModel model;//new AnnotationID(file, -1), type);
			
			if(type == AnnotationType.PDF_FILE){
				model = new AnnotationModel(0);
				model.setSource(file.toURI());
			}
			else {
				model = new AnnotationModel(-1);
				model.setSource(uri);
			}
			model.setAnnotationType(type);
			
			AnnotationController.setModel(node, model);
		}
		NodeUtilities.setLinkFrom(uri, node);
		return NodeUtilities.insertChildNodeFrom(node, isLeft, target);
	}
	
	public static NodeModel insertChildNodeFrom(URI file, IAnnotation annotation, boolean isLeft, NodeModel target){	
		if(annotation.getTitle() != null && annotation.getTitle().length() > 1 && annotation.getTitle().charAt(0) == '='){
			annotation.setTitle(" " + annotation.getTitle()); //$NON-NLS-1$
		}
		final NodeModel node = ((MMapController) Controller.getCurrentModeController().getMapController()).newNode(annotation.getTitle(), target.getMap());
		AnnotationController.setModel(node, annotation);
		NodeUtilities.setLinkFrom(file, node);
				
		return NodeUtilities.insertChildNodeFrom(node, isLeft, target);
	}
	
	public static List<NodeModel> insertNewChildNodesFrom(Collection<AnnotationModel> annotations, boolean isLeft, NodeModel target, NodeModel rootTarget) {
		List<NodeModel> nodes = new ArrayList<NodeModel>();
		
		for(AnnotationModel annotation : annotations){			
			if(annotation.isNew() || annotation.hasNewChildren()){
				NodeModel equalChild = targetHasEqualChild(rootTarget, annotation);
				
				if(equalChild == null){
					NodeModel node = insertChildNodeFrom(annotation.getSource(), annotation, isLeft, target);
					insertNewChildNodesFrom(annotation.getChildren(), isLeft, node, rootTarget);
					nodes.add(node);
				}
				else{
					insertNewChildNodesFrom(annotation.getChildren(), isLeft, equalChild, rootTarget);
					nodes.add(equalChild);
				}
				
			}		
		}
		
		return nodes;
	}
	
	public static NodeModel targetHasEqualChild(NodeModel target, IAnnotation annotation){
		if(annotation == null)	return null;
		
		for(NodeModel child : target.getChildren()){
			IAnnotation oldAnnotation = AnnotationController.getAnnotationNodeModel(child);
			NodeModel equalChild = targetHasEqualChild(child, annotation);
			if(equalChild != null) {
				return equalChild;
			}
			if(oldAnnotation == null || oldAnnotation.getAnnotationType() != annotation.getAnnotationType()){
				continue;
			}
			if(annotation.getAnnotationType().equals(AnnotationType.PDF_FILE)){
				if(annotation.getSource().equals(URIUtils.getAbsoluteURI(child))){
					return child;
				}
			}			
			if(oldAnnotation != null && oldAnnotation.getAnnotationID().equals(annotation.getAnnotationID())){
				return child;
			}
		}
		return null;
	}


}
