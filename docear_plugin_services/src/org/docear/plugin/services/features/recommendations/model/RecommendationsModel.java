package org.docear.plugin.services.features.recommendations.model;

import java.util.Collection;
import java.util.Iterator;

import javax.swing.event.TreeModelListener;

import org.docear.plugin.services.ServiceController;
import org.freeplane.core.util.TextUtils;

public class RecommendationsModel {

	private RecommendationsModelNode rootNode;

	public RecommendationsModel(Collection<RecommendationEntry> recommendations) {
		new RecommendationsModel();
		parseRecommendations(recommendations);
	}
	
	public RecommendationsModel() {
		
	}
	
	private void parseRecommendations(Collection<RecommendationEntry> recommendations) {
		if(recommendations == null) {
			if(ServiceController.getCurrentUser().isRecommendationsEnabled()) {
				setRoot(RecommendationsModelNode.createNoRecommendationsNode(TextUtils.getText("recommendations.error.no_recommendations")));
			}
			else {
				setRoot(RecommendationsModelNode.createNoServiceNode());
			}
			return;
		}
		
		
		Iterator<RecommendationEntry> entries = recommendations.iterator();
		// small hack: first element in collection is xml-element "recommendations"
		RecommendationEntry recommendationsElement = entries.next();
		String rootTitle = recommendationsElement.getTitle();
		
		if (rootTitle != null && rootTitle.trim().length() > 0) {
			setRoot(RecommendationsModelNode.createRecommendationContainerNode(rootTitle));
		}
		else {
			//fallback to standard title
			setRoot(RecommendationsModelNode.createRecommendationContainerNode());
		}
		
		if(recommendations.isEmpty()) {
			getRootNode().insert(RecommendationsModelNode.createNoRecommendationsNode(TextUtils.getText("recommendations.error.no_recommendations")));
		} 
		else {
			while (entries.hasNext()) {
				getRootNode().insert(RecommendationsModelNode.createRecommendationEntryNode(entries.next()));
			}		
		}
	}
	
	public String getTitle() {
		String label = ServiceController.getCurrentUser().getName();
		if(label != null && label.trim().length() > 0) {
			return TextUtils.format("recommendations.map.label.forUser", label);
		}
		return TextUtils.getText("recommendations.map.label.anonymous");
	}

	public void setRoot(RecommendationsModelNode root) {
		this.rootNode = root;		
	}

	public void addTreeModelListener(TreeModelListener l) {
		
	}
	
	public void removeTreeModelListener(TreeModelListener l) {
		// TODO Auto-generated method stub
		
	}

	public RecommendationsModelNode getChild(RecommendationsModelNode parent, int index) {
		if(parent == null) {
			return null;
		}
		return parent.getChildAt(index);
	}

	public int getChildCount(RecommendationsModelNode parent) {
		if(parent == null) {
			return 0;
		}
		return parent.getChildCount();
	}

	public RecommendationsModelNode getRootNode() {
		return this.rootNode;
	}

	public boolean isLeaf(RecommendationsModelNode node) {
		if(node == null) {
			return true;
		}
		return node.isLeaf();
	}

	public int getIndexOfChild(RecommendationsModelNode parent, RecommendationsModelNode child) {
		if(parent == null) {
			return -1;
		}
		return parent.getIndex(child);
	}
}
