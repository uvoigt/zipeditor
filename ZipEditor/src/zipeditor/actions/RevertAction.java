package zipeditor.actions;

import zipeditor.ZipEditor;

public class RevertAction extends EditorAction {
	public RevertAction(ZipEditor editor) {
		super(ActionMessages.getString("RevertAction.0"), editor); //$NON-NLS-1$
	}

	public void run() {
		fEditor.doRevert();
	}
}
