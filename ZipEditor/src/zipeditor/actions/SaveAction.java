package zipeditor.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import zipeditor.ZipEditor;
import zipeditor.ZipEditorPlugin;

public class SaveAction extends EditorAction {
	public SaveAction(ZipEditor editor) {
		super(ActionMessages.getString("SaveAction.0"), editor); //$NON-NLS-1$
	}

	public void run() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				fEditor.doSave(monitor);
			}
		};
		try {
			fEditor.getSite().getWorkbenchWindow().run(true, true, op);
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		}
	}
}
