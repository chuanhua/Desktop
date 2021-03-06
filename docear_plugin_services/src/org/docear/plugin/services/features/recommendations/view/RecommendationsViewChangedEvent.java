package org.docear.plugin.services.features.recommendations.view;

import java.util.EventObject;

public class RecommendationsViewChangedEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	public static final String MODEL_CHANGED_TYPE = "MODEL_CHANGED";
	private final String type;
	private final Object oldValue;
	private final Object newValue;

	public RecommendationsViewChangedEvent(Object src, String type, Object oldValue, Object newValue) {
		super(src);
		this.type = type;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public String getType() {
		return type;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public Object getNewValue() {
		return newValue;
	}

}
