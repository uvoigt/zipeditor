/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.actions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

import zipeditor.PreferenceConstants;
import zipeditor.PreferenceInitializer;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.Node;

public class FileOpener {
	private final static int MAX_RECENTLY_USED_FILTERS = 5;

	public static class Editor {
		private String path;
		private String label;
		private IEditorDescriptor descriptor;
		protected Editor(IEditorDescriptor descriptor) {
			this.descriptor = descriptor;
			label = descriptor.getLabel();
		}

		public Editor(String string) {
			int _1st = string.indexOf(255);
			if (_1st != -1) {
				label = string.substring(0, _1st);
				path = string.substring(_1st + 1);
			} else {
				_1st = string.indexOf(254);
				if (_1st != -1)
					label = string.substring(0, _1st);
				descriptor = PlatformUI.getWorkbench().getEditorRegistry().findEditor(string.substring(_1st + 1));
			}
		}
		
		protected Editor(String label, String path) {
			this.label = label;
			this.path = path;
		}
		
		public String getLabel() {
			return label;
		}
		
		public String getPath() {
			return path;
		}
		
		public IEditorDescriptor getDescriptor() {
			return descriptor;
		}
		
		public void setLabel(String label) {
			this.label = label;
		}
		
		public void setPath(String path) {
			this.path = path;
		}
		
		public boolean equals(Object obj) {
			return obj != null && obj.toString().equals(toString());
		}

		public String toString() {
			return label + (descriptor != null ? (char) 254 + descriptor.getId()
					: (char) 255 + path);
		}
	};

	private final IWorkbenchPage fPage;
	private final Node fNode;
	
	public FileOpener(IWorkbenchPage page, Node node) {
		fPage = page;
		fNode = node;
	}

	public void openFromOther(IFileStore file, Editor editor) {
		if (editor.getDescriptor() == null) {
			try {
				processCommand(file, editor);
				addToRecentlyUsedExecutables(editor);
				ZipEditorPlugin.getDefault().addFileMonitor(new File(file.toURI()), fNode);
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
				ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						ActionMessages.getString("FileOpener.0"), ActionMessages.getString("FileOpener.1"), //$NON-NLS-1$ //$NON-NLS-2$
						ZipEditorPlugin.createErrorStatus(e.getClass().getName(), e));
			}
		} else {
			if (!file.fetchInfo().isDirectory() && file.fetchInfo().exists()) {
				IEditorInput input = Utils.createEditorInput(file);
				String editorId = editor.getDescriptor().getId();
				try {
					fPage.openEditor(input, editorId);
					ZipEditorPlugin.getDefault().addFileMonitor(new File(file.toURI()), fNode);
					addToRecentlyUsedExecutables(editor);
				} catch (PartInitException e) {
					ZipEditorPlugin.log(e);
				}
			}
		}
	}

	private void processCommand(IFileStore file, Editor editor) throws Exception {
		String commandString = editor.getPath();
		commandString = replaceAll(commandString, "$p", file.toString()); //$NON-NLS-1$
		commandString = replaceAll(commandString, "$f", file.getName()); //$NON-NLS-1$
		commandString = replaceAll(commandString, "$z", fNode.getModel().getZipPath().getAbsolutePath()); //$NON-NLS-1$
		int dotIndex = file.getName().lastIndexOf('.');
		commandString = replaceAll(commandString, "$n", file.getName().substring(0, dotIndex != -1 ? dotIndex : file.getName().length())); //$NON-NLS-1$
		if (commandString.equals(editor.getPath()))
			commandString += ' ' + file.toString();
		String outCommand = null;
		int pipeIndex = commandString.indexOf('|');
		if (pipeIndex != -1) {
			outCommand = commandString.substring(pipeIndex + 1);
			commandString = commandString.substring(0, pipeIndex);
		}
        int extIndex = commandString.indexOf("$x"); //$NON-NLS-1$
        String ext = null;
		if (extIndex != -1) {
			int endIndex = (ext = commandString.substring(extIndex + 2).trim()).indexOf(' ');
			ext = ext.substring(0, endIndex == -1 ? ext.length() : endIndex);
			commandString = commandString.substring(0, extIndex) + commandString.substring(commandString.indexOf(ext) + ext.length());
		}
		Process process = Runtime.getRuntime().exec(commandString);
		if (outCommand == null)
			return;
		final InputStream stdIn = process.getInputStream();
		final InputStream stdErr = process.getErrorStream();
		int defaultEditorIndex = outCommand.indexOf("$i"); //$NON-NLS-1$
		int editorIdIndex = outCommand.indexOf("$e"); //$NON-NLS-1$
		if (defaultEditorIndex != -1 || editorIdIndex != -1) {
			String editorId = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
			if (editorIdIndex != -1) {
				editorId = outCommand.substring(editorIdIndex + 2).trim();
				if (editorId.indexOf(' ') != -1)
					editorId = editorId.substring(0, editorId.indexOf(' ')).trim();
			}
			final File tmpFile = File.createTempFile("internal", ext, fNode.getModel().getTempDir()); //$NON-NLS-1$
			final FileOutputStream out = new FileOutputStream(tmpFile);
			Thread.sleep(100);
			new Thread(new Runnable() {
				public void run() {
					try {
						Utils.readAndWrite(stdErr, out, false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, "ErrReader").start(); //$NON-NLS-1$
			Thread.sleep(100);
			new Thread(new Runnable() {
				public void run() {
					try {
						Utils.readAndWrite(stdIn, out, false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, "InReader").start(); //$NON-NLS-1$
			process.waitFor();
			Thread.sleep(100);
			out.flush();
			if (out.getChannel().isOpen())
				out.close();
			fPage.openEditor(Utils.createEditorInput(Utils.getFileStore(tmpFile)), editorId);
		} else {
			Process outCommandProcess = Runtime.getRuntime().exec(outCommand);
			BufferedOutputStream out = new BufferedOutputStream(outCommandProcess.getOutputStream());
			Utils.readAndWrite(stdIn, out, true);
		}
	}
	
	private String replaceAll(String string, String what, String replacement) {
		int index = string.indexOf(what);
		if (index == -1)
			return string;
		return string.substring(0, index) + replacement + replaceAll(string.substring(index + what.length()), what, replacement);
	}

	private void addToRecentlyUsedExecutables(Editor editor) {
		IPreferenceStore store = ZipEditorPlugin.getDefault().getPreferenceStore();
		Editor[] recentlyUsedEditors = (Editor[]) PreferenceInitializer.split(store.getString(PreferenceConstants.RECENTLY_USED_EDITORS), PreferenceConstants.RECENTLY_USED_SEPARATOR, Editor.class);
		List newEditors = new ArrayList(Arrays.asList(recentlyUsedEditors));
		for (int i = 0; i < recentlyUsedEditors.length; i++) {
			if (!editor.getLabel().equals(recentlyUsedEditors[i].getLabel()))
				continue;
			newEditors.remove(i);
		}
		newEditors.add(0, editor.toString());
		if (newEditors.size() > MAX_RECENTLY_USED_FILTERS)
			newEditors = newEditors.subList(0, MAX_RECENTLY_USED_FILTERS);
		store.setValue(PreferenceConstants.RECENTLY_USED_EDITORS, PreferenceInitializer.join(newEditors.toArray(), PreferenceConstants.RECENTLY_USED_SEPARATOR));
	}

}
