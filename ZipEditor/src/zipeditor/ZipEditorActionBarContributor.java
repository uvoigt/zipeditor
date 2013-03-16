/*
 * (c) Copyright 2002, 2010 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.part.EditorActionBarContributor;

import zipeditor.actions.ReverseSelectionAction;
import zipeditor.actions.SelectPatternAction;

public class ZipEditorActionBarContributor extends EditorActionBarContributor {
	private class ErrorStatus extends ControlContribution {
		private ErrorStatus() {
			super("zipeditor.ErrorStatusContribution"); //$NON-NLS-1$
		}

		protected Control createControl(Composite parent) {
			Composite control = new Composite(parent, SWT.NONE);
			StatusLineLayoutData data = new StatusLineLayoutData();
			data.widthHint = Integer.MAX_VALUE;
			control.setLayoutData(data);
			GridLayout layout = new GridLayout(3, false);
			layout.marginHeight = layout.marginWidth = 0;
			layout.verticalSpacing = layout.horizontalSpacing = 0;
			control.setLayout(layout);
			errorLabel = new Label(control, SWT.LEFT);
			errorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			errorLabel.setBackground(parent.getBackground());
			errorLabel.addMouseListener(new MouseAdapter() {
				public void mouseDoubleClick(MouseEvent e) {
					showErrorDetails();
				}
			});
			errorLabel.setForeground(JFaceColors.getErrorText(parent.getDisplay()));
			ToolBar bar = new ToolBar(control, SWT.HORIZONTAL | SWT.FLAT);
			ToolItem delete = new ToolItem(bar, SWT.FLAT | SWT.PUSH);
			delete.setImage(ZipEditorPlugin.getImage(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
					ISharedImages.IMG_TOOL_DELETE)));
			delete.setToolTipText(Messages.getString("ZipEditorActionBarContributor.0")); //$NON-NLS-1$
			delete.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					clearErrors();
				}
			});
			errorStatusControl = control;
			showCurrentError();
			return control;
		}
	}

	private Composite errorStatusControl;
	private Label errorLabel;
	private Map errors = new HashMap();
	private IEditorPart activeEditor;
	private RetargetAction selectPattern;
	private RetargetAction reverseSelection;

	public void contributeToStatusLine(IStatusLineManager statusLineManager) {
		statusLineManager.add(new ErrorStatus());
	}

	public void contributeToMenu(IMenuManager menuManager) {
		initActions();
		IMenuManager menu = menuManager.findMenuUsingPath("edit"); //$NON-NLS-1$
		if (menu != null) {
			menu.insertAfter(ActionFactory.SELECT_ALL.getId(), selectPattern);
			menu.insertAfter(SelectPatternAction.ID, reverseSelection);
		}
	}
	
	private void initActions() {
		if (selectPattern == null) {
			selectPattern = new RetargetAction(SelectPatternAction.ID, Messages.getString("ZipEditorActionBarContributor.1")); //$NON-NLS-1$
			selectPattern.setActionDefinitionId(SelectPatternAction.ID);
			getPage().addPartListener(selectPattern);
		}
		if (reverseSelection == null) {
			reverseSelection = new RetargetAction(ReverseSelectionAction.ID, Messages.getString("ZipEditorActionBarContributor.2")); //$NON-NLS-1$
			reverseSelection.setActionDefinitionId(ReverseSelectionAction.ID);
			getPage().addPartListener(reverseSelection);
		}
	}

	private void showErrorDetails() {
		if (getErrors().size() == 0)
			return;
		IStatus status = (IStatus) getErrors().remove(0);
		ZipEditorPlugin.showErrorDialog(getPage().getWorkbenchWindow().getShell(),
				status.getMessage(), status.getException(), false);
		showCurrentError();
	}
	
	private void clearErrors() {
		getErrors().clear();
		errorStatusControl.setVisible(false);
	}
	
	private boolean hasErrors() {
		return getErrors().size() > 0;
	}

	private void showCurrentError() {
		if (errorStatusControl == null || errorStatusControl.isDisposed())
			return;
		boolean hasErrors = hasErrors();
		errorStatusControl.setVisible(hasErrors);
		if (hasErrors) {
			List errorList = getErrors();
			errorLabel.setText("[" + errorList.size() + "] " //$NON-NLS-1$ //$NON-NLS-2$
					+ ((IStatus) errorList.get(0)).getMessage());
		}
	}
	
	private List getErrors() {
		List errorList = (List) errors.get(activeEditor);
		if (errorList == null)
			errors.put(activeEditor, errorList = new ArrayList());
		return errorList;
	}
	
	public void setActiveEditor(IEditorPart targetEditor) {
		activeEditor = targetEditor;
		showCurrentError();
	}

	public void dispose() {
		activeEditor = null;
		errors.clear();
		getPage().removePartListener(selectPattern);
	}

	public void reportError(IEditorPart editor, IStatus message) {
		activeEditor = editor;
		getErrors().add(message);
		getPage().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				showCurrentError();
			}
		});
	}
}
