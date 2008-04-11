/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.PropertyDialogAction;

import zipeditor.MultiPreferenceDialog;

public class MultiPropertyDialogAction extends PropertyDialogAction {
	private IShellProvider fShellProvider;

	public MultiPropertyDialogAction(IShellProvider shell, ISelectionProvider provider) {
		super(shell, provider);
		fShellProvider = shell;
	}

	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(!selection.isEmpty());
	}

	public void run() {
		List elements = getStructuredSelection().toList();
		Window dialog = new MultiPreferenceDialog(fShellProvider.getShell(),
				new PreferenceManager(), (IAdaptable[]) elements
						.toArray(new IAdaptable[elements.size()]));
		dialog.open();
	}
}