/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.widgets.Display;

public class DeferredMenuManager extends MenuManager {
	public static abstract class MenuJob extends Job {
		private DeferredMenuManager fMenuManager;
		protected Object fFamily;
		protected Object fProperty;
		public MenuJob(Object family, Object propertyValue) {
			super(ActionMessages.getString("DeferredMenuManager.0")); //$NON-NLS-1$
			Assert.isNotNull(family);
			fFamily = family;
			fProperty = propertyValue;
		}
		
		protected abstract IStatus addToMenu(IProgressMonitor monitor, IMenuManager menu);
		
		protected IStatus run(IProgressMonitor monitor) {
			Assert.isNotNull(fMenuManager);
			addToMenu(monitor, fMenuManager);
			fMenuManager.finish();
			return Status.OK_STATUS;
		}
		
		public boolean belongsTo(Object family) {
			return fFamily.equals(family);
		}
	};
	
	private final static QualifiedName ID_PROPERTY_NAME = new QualifiedName(DeferredMenuManager.class.getName(), "type"); //$NON-NLS-1$
	private final static QualifiedName MENU_PROPERTY_NAME = new QualifiedName(DeferredMenuManager.class.getName(), "menu"); //$NON-NLS-1$
	private static int useCount;

	private Action fPendingAction = new Action(ActionMessages.getString("DeferredMenuManager.1")) {}; //$NON-NLS-1$
	private int fCreateCount;

	public DeferredMenuManager(IContributionManager parent, String text, String id) {
		super(text, id);
		fCreateCount = useCount;
		setParent(parent);
		fPendingAction.setId(getPendingId());
		super.add(fPendingAction);
	}
	
	public void add(IAction action) {
		removePendingAction();
		super.add(action);
	}
	
	private void removePendingAction() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				remove(getPendingId());
			}
		});
	}
	
	public String getPendingId() {
		return fPendingAction.getClass().getName() + fPendingAction.hashCode();
	}

	public synchronized void finish() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IContributionItem[] items = getItems();
				if (items.length == 1 && getPendingId().equals(items[0].getId())) {
					dispose();
					removeAll();
					getParent().remove(DeferredMenuManager.this);
				} else {
					remove(getPendingId());
					update(false);
				}
			}
		});
	}

	public static void addToMenu(IMenuManager parentMenu, String parentId, String menuText, String menuId, MenuJob job) {
		Assert.isNotNull(parentMenu);
		Assert.isNotNull(job);
		DeferredMenuManager menuFromRunningJob = getMenuFromRunningJob(job.fFamily, job.fProperty); 
		DeferredMenuManager subMenu = menuFromRunningJob != null ?
				menuFromRunningJob : new DeferredMenuManager(parentMenu, menuText, menuId);
		if (menuFromRunningJob == null) {
			job.setSystem(true);
			job.setProperty(ID_PROPERTY_NAME, job.fProperty);
			job.setProperty(MENU_PROPERTY_NAME, subMenu);
			job.fMenuManager = subMenu;
			job.schedule();
		} else {
			useCount++;
		}
		if (parentId != null && parentMenu.find(parentId) != null)
			parentMenu.appendToGroup(parentId, subMenu);
		else
			parentMenu.add(subMenu);
	}
	
	public boolean isVisible() {
		if (useCount == fCreateCount)
			return super.isVisible();
		// this adds the pending action again because this item
		// has been removed in the update implementation of the super class
		if (getItems().length == 0)
			super.add(fPendingAction);
		return true;
	}
	
	public static boolean isRunning(Object jobFamily, Object propertyValue) {
		return getMenuFromRunningJob(jobFamily, propertyValue) != null;
	}

	private static DeferredMenuManager getMenuFromRunningJob(Object jobFamily, Object propertyValue) {
		Job[] jobs = Platform.getJobManager().find(jobFamily);
		DeferredMenuManager menuFromRunningJob = null;
		for (int i = 0; i < jobs.length; i++) {
			Object property = jobs[i].getProperty(ID_PROPERTY_NAME);
			if (property == propertyValue || property != null && property.equals(propertyValue)) {
				menuFromRunningJob = (DeferredMenuManager) jobs[i].getProperty(MENU_PROPERTY_NAME);
			}
		}
		return menuFromRunningJob;
	}

}