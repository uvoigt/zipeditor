/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;

import zipeditor.Messages;
import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;
import zipeditor.actions.OpenActionGroup;
import zipeditor.model.Node;

public class ZipSearchResultPage extends AbstractTextSearchViewPage implements IAdaptable {

	private static final String[] SHOW_IN_TARGETS = new String[] { IPageLayout.ID_RES_NAV };
	private  static final IShowInTargetList SHOW_IN_TARGET_LIST = new IShowInTargetList() {
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};

	private ZipSearchContentProvider fContentProvider;
	private OpenActionGroup fOpenActionGroup;
	private IAction fPropertiesAction;
	private final List fAnnotationModels = new ArrayList();
	private TextEditor fPreviousEditor;

	protected void clear() {
		if (fContentProvider != null)
			fContentProvider.clear();
		for (int i = 0; i < fAnnotationModels.size(); i++) {
			clearAnnotations((IAnnotationModel) fAnnotationModels.get(i));
		}
	}

	protected void configureTableViewer(TableViewer viewer) {
		fContentProvider = new ZipSearchContentProvider(PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER);
		configureViewer(viewer);
	}

	protected void configureTreeViewer(TreeViewer viewer) {
		fContentProvider = new ZipSearchContentProvider(PreferenceConstants.VIEW_MODE_TREE);
		configureViewer(viewer);
	}

	private void configureViewer(StructuredViewer viewer) {
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(new ZipSearchLabelProvider(this));
		fOpenActionGroup = new OpenActionGroup(viewer);
		if (fPropertiesAction == null) {
			fPropertiesAction = new PropertyDialogAction(getSite().getWorkbenchWindow(), getSite().getSelectionProvider());
			fPropertiesAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);		}
	}

	protected void elementsChanged(Object[] input) {
		fContentProvider.elementsChanged(input);		
	}

	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		ActionContext context= new ActionContext(getSite().getSelectionProvider().getSelection());
		context.setInput(getInput());
		fOpenActionGroup.setContext(context);
		fOpenActionGroup.fillContextMenu(mgr);

		ISelection selection = getViewer().getSelection();
		if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
			mgr.prependToGroup(IContextMenuConstants.GROUP_OPEN, new Separator());
			mgr.prependToGroup(IContextMenuConstants.GROUP_OPEN, new OpenArchiveAction(
					getSite().getPage(), findFileFromNode(((IStructuredSelection) selection).getFirstElement())));
		}
		mgr.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fPropertiesAction);
	}

	public Object getAdapter(Class adapter) {
		if (IShowInTargetList.class.equals(adapter)) {
			return SHOW_IN_TARGET_LIST;
		} else if (adapter == IShowInSource.class) {
			ISelectionProvider selectionProvider = getSite().getSelectionProvider();
			if (selectionProvider == null)
				return null;

			ISelection selection = selectionProvider.getSelection();
			if (selection instanceof IStructuredSelection) {
				final Object[] elements = ((IStructuredSelection) selection).toArray();
				final Set newSelection = new HashSet();
				for (int i = 0; i < elements.length; ) {
					Object element = elements[i];
					IFile file = findIFileFromNode(element);
					if (file != null)
						newSelection.add(file);
					break;
				}
				return new IShowInSource() {
					public ShowInContext getShowInContext() {
						return new ShowInContext(null, new StructuredSelection(new ArrayList(newSelection)));
					}
				};
			}
			return null;
		}
		return null;
	}

	private IFile findIFileFromNode(Object element) {
		File file = findFileFromNode(element);
		IFile[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());
		return workspaceFiles.length == 1 ? workspaceFiles[0] : null;
	}

	private File findFileFromNode(Object element) {
		if (element instanceof Element) {
			Element e = (Element) element;
			if (e.getChildren() != null)
				return findFileFromNode(e.getChildren().get(0));
			if (e.getNodes() != null)
				return findFileFromNode(e.getNodes().iterator().next());
		}
		File file = null;
		List parentNodes = ((Node) element).getParentNodes();
		if (parentNodes != null && !parentNodes.isEmpty())
			return findFileFromNode(parentNodes.get(0));
		else
			file = ((Node) element).getModel().getZipPath();

		if (file == null)
			file = ((Node) element).getModel().getZipPath();
		return file;
	}

	protected void showMatch(Match match, int offset, int length, boolean activate) throws PartInitException {
		final Node node = (Node) match.getElement();
		IWorkbenchPage page = getSite().getPage();
		String encoding = ((ZipSearchQuery) getInput().getQuery()).getOptions().getEncoding();

		ResultEditorInput input = new ResultEditorInput(node, encoding);
		TextEditor editor = (TextEditor) page.findEditor(input);
		if (editor != null) {
			page.bringToTop(editor);
		} else {
			try {
				boolean reuseEditor = NewSearchUI.reuseEditor();
				if (reuseEditor && fPreviousEditor != null)
					page.closeEditor(fPreviousEditor, false);
				IEditorPart part = page.openEditor(input, "org.eclipse.ui.DefaultTextEditor", false); //$NON-NLS-1$
				if (!(part instanceof TextEditor))
					return;
				editor = (TextEditor) part;
				if (reuseEditor)
					fPreviousEditor = editor;
				IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(input);
				addAnnotations(node, annotationModel);
				fAnnotationModels.add(annotationModel);
			} catch (PartInitException e) {
				ZipEditorPlugin.log(e);
				MessageDialog.openError(getSite().getShell(), Messages.getString("ZipEditor.8"), e.getMessage()); //$NON-NLS-1$
				return;
			}
		}
		editor.getSelectionProvider().setSelection(new TextSelection(offset, length));
	}

	private void addAnnotations(final Node node, IAnnotationModel annotationModel) {
		Match[] matches = getInput().getMatches(node);
		if (annotationModel instanceof IAnnotationModelExtension) {
			Map annotations = new HashMap();
			for (int i = 0; i < matches.length; i++) {
				Match m = matches[i];
				annotations.put(new Annotation("zipeditor.search.match", false, null), new Position(m.getOffset(), m.getLength())); //$NON-NLS-1$
			}
			((IAnnotationModelExtension) annotationModel).replaceAnnotations(null, annotations);
		} else {
			for (int i = 0; i < matches.length; i++) {
				Match m = matches[i];
				annotationModel.addAnnotation(new Annotation("zipeditor.search.match", false, null), new Position(m.getOffset(), m.getLength())); //$NON-NLS-1$
			}
		}
	}

	private void clearAnnotations(IAnnotationModel annotationModel) {
		if (annotationModel instanceof IAnnotationModelExtension) {
			((IAnnotationModelExtension) annotationModel).removeAllAnnotations();
		} else {
			for (Iterator it = annotationModel.getAnnotationIterator(); it.hasNext(); ) {
				annotationModel.removeAnnotation((Annotation) it.next());
			}
		}
	}

	public void dispose() {
		super.dispose();
		fContentProvider.dispose();
		fAnnotationModels.clear();
	}
}
