package org.docear.plugin.core.ui.wizard;

import java.util.Stack;

public class WizardTraverseLog {
	Stack<Object> pageTraverseLog = new Stack<Object>();
	
	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	public void add(WizardPageDescriptor desc) {
		if(!desc.getPage().skipOnBack() && (pageTraverseLog.isEmpty() || !desc.getIdentifier().equals(pageTraverseLog.peek()))) {
			pageTraverseLog.push(desc.getIdentifier());
		}
	}
	
	public WizardPageDescriptor getPreviousPage(WizardContext context) {
		try {
			return context.getModel().getPage(pageTraverseLog.pop());
		} catch (Exception e) {
			return null;
		}
	}
	
	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
}
