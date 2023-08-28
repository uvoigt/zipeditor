/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.text.MessageFormat;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditor;

import zipeditor.Messages;
import zipeditor.PreferenceConstants;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;
import zipeditor.actions.IPostOpenProcessor;
import zipeditor.actions.OpenActionGroup;
import zipeditor.model.Node;

public class ZipSearchResultPage extends AbstractTextSearchViewPage implements IAdaptable {

	private class OccurrencesAdder implements IPostOpenProcessor {
		private Match[] fMatches;
		private String fPattern;

		public OccurrencesAdder(Match[] matches, String pattern) {
			fMatches = matches;
			fPattern = pattern;
		}

		public void postOpen(IEditorPart editor) {
			TextSelection selection = null;
			if (fMatches.length > 0)
				selection = new TextSelection(fMatches[0].getOffset(), fMatches[0].getLength());
			addAnnotations(editor, fMatches, fPattern, selection);
		}
	}

	private class SearchResultDragAdapter extends DragSourceAdapter {
		private ISelectionProvider fSelectionProvider;

		public SearchResultDragAdapter(ISelectionProvider selectionProvider) {
			fSelectionProvider = selectionProvider;
		}

		public void dragStart(DragSourceEvent event) {
			ISelection selection = fSelectionProvider.getSelection();
			Object[] objects = selection instanceof IStructuredSelection ? ((IStructuredSelection) selection).toArray() : null;
			if (objects == null || objects.length == 0) {
				event.doit = false;
				return;
			}
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
				if (!(object instanceof PlainNode) && !(object instanceof Element)) {
					event.doit = false;
					return;
				}
			}
		}

		public void dragSetData(DragSourceEvent event) {
			ISelection selection = fSelectionProvider.getSelection();
			Object[] objects = selection instanceof IStructuredSelection ? ((IStructuredSelection) selection).toArray() : null;
			String[] paths = new String[objects.length];
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
				if (object instanceof PlainNode) {
					paths[i] = ((PlainNode) object).getModel().getZipPath().getAbsolutePath();
				} else if (object instanceof Element) {
					IFile file = findIFileFromNode(object);
					paths[i] = file != null ? file.getLocation().toFile().getAbsolutePath() : ((Element) object).getPath();
				}
			}
			event.data = paths;
		}

	}

	private static final String[] SHOW_IN_TARGETS = new String[] { IPageLayout.ID_PROJECT_EXPLORER };
	private  static final IShowInTargetList SHOW_IN_TARGET_LIST = new IShowInTargetList() {
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};

	private ZipSearchContentProvider fContentProvider;
	private OpenActionGroup fOpenActionGroup;
	private IAction fPropertiesAction;
	private final List fAnnotationModels = new ArrayList();
	private IEditorPart fPreviousEditor;

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
		addDragSupport(viewer);
		fOpenActionGroup = new OpenActionGroup(viewer);
		if (fPropertiesAction == null) {
			fPropertiesAction = new PropertyDialogAction(getSite().getWorkbenchWindow(), getSite().getSelectionProvider());
			fPropertiesAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);
		}
	}

	private void addDragSupport(StructuredViewer viewer) {
		Transfer[] transfers = new Transfer[] { FileTransfer.getInstance(), ResourceTransfer.getInstance() };
		viewer.addDragSupport(DND.DROP_COPY | DND.DROP_LINK, transfers, new SearchResultDragAdapter(viewer));
	}

	protected void elementsChanged(Object[] input) {
		fContentProvider.elementsChanged(input);		
	}

	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		ISelection selection = getViewer().getSelection();
		ActionContext context = new ActionContext(selection);
		IStructuredSelection structuredSelection = selection instanceof IStructuredSelection ? (IStructuredSelection) selection : null;
		if (structuredSelection != null && Utils.allNodesAreFileNodes(structuredSelection)) {
			Node[] selectedNodes = Utils.getSelectedNodes(structuredSelection);
			String pattern = ((ZipSearchQuery) getInput().getQuery()).getOptions().getPattern();
			for (int i = 0; i < selectedNodes.length; i++) {
				Node node = selectedNodes[i];
				node.setProperty("postOpen", new OccurrencesAdder(getInput().getMatches(node), pattern)); //$NON-NLS-1$
			}
		}
		fOpenActionGroup.setContext(context);
		fOpenActionGroup.fillContextMenu(mgr);

		if (structuredSelection != null && structuredSelection.size() == 1) {
			mgr.prependToGroup(IContextMenuConstants.GROUP_OPEN, new Separator());
			Object[] fileAndNode = findFileFromNode(structuredSelection.getFirstElement());
			if (!(fileAndNode[1] instanceof PlainNode)) {
				mgr.prependToGroup(IContextMenuConstants.GROUP_OPEN, new OpenArchiveAction(
						getSite().getPage(), (File) fileAndNode[0], (Node) fileAndNode[1]));
			}
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
		Object[] fileAndNode = findFileFromNode(element);
		if (fileAndNode[0] instanceof File) {
			IFile[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(((File) fileAndNode[0]).toURI());
			if (workspaceFiles.length == 1)
				return workspaceFiles[0];
		}
		return null;
	}

	/*
	 * returns a file at 0 and a node at 1 
	 */
	private Object[] findFileFromNode(Object element) {
		if (element instanceof Element) {
			Object[] children = fContentProvider.getChildren(element);
			if (children.length > 0)
				return findFileFromNode(children[0]);
		}
		File file = null;
		Node node = null;
		if (element instanceof Node) {
			List parentNodes = ((Node) element).getParentNodes();
			if (parentNodes != null && !parentNodes.isEmpty()) {
				return findFileFromNode(parentNodes.get(0));
			}
			node = (Node) element;
			file = node.getModel().getZipPath();
		}
		return new Object[] {file, node};
	}

	protected void showMatch(Match match, int offset, int length, boolean activate) throws PartInitException {
		final Node node = (Node) match.getElement();
		IWorkbenchPage page = getSite().getPage();
		String encoding = ((ZipSearchQuery) getInput().getQuery()).getOptions().getEncoding();

		ResultEditorInput input = new ResultEditorInput(node, encoding);
		IEditorPart editor = page.findEditor(input);
		if (editor != null) {
			page.bringToTop(editor);
		} else {
			try {
				boolean reuseEditor = NewSearchUI.reuseEditor();
				if (reuseEditor && fPreviousEditor != null)
					page.closeEditor(fPreviousEditor, false);
				editor = page.openEditor(input, "org.eclipse.ui.DefaultTextEditor", false); //$NON-NLS-1$
				if (reuseEditor)
					fPreviousEditor = editor;
				addAnnotations(editor, getInput().getMatches(node), ((ZipSearchQuery) getInput().getQuery()).
						getOptions().getPattern(), null);
			} catch (PartInitException e) {
				ZipEditorPlugin.log(e);
				MessageDialog.openError(getSite().getShell(), Messages.getString("ZipEditor.8"), e.getMessage()); //$NON-NLS-1$
				return;
			}
		}
		if (editor instanceof ITextEditor)
			((ITextEditor) editor).getSelectionProvider().setSelection(new TextSelection(offset, length));
	}

	private void addAnnotations(IEditorPart editor, Match[] matches, String pattern, TextSelection textSelection) {
		ITextEditor textEditor = findTextEditor(editor);
		if (textEditor == null)
			return;

		IAnnotationModel annotationModel = textEditor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		String text = MessageFormat.format(SearchMessages.getString("ZipSearchResultPage.1"), new Object[] { pattern }); //$NON-NLS-1$
		if (annotationModel instanceof IAnnotationModelExtension) {
			Map annotations = new HashMap();
			for (int i = 0; i < matches.length; i++) {
				Match m = matches[i];
				// ignore matches of zero length, this should occur if ZipMatch.fOnNodeName == true
				if (m.getLength() > 0)
					annotations.put(new Annotation("zipeditor.search.match", false, text), new Position(m.getOffset(), m.getLength())); //$NON-NLS-1$
			}
			((IAnnotationModelExtension) annotationModel).replaceAnnotations(null, annotations);
		} else {
			for (int i = 0; i < matches.length; i++) {
				Match m = matches[i];
				if (m.getLength() > 0)
					annotationModel.addAnnotation(new Annotation("zipeditor.search.match", false, text), new Position(m.getOffset(), m.getLength())); //$NON-NLS-1$
			}
		}
		fAnnotationModels.add(annotationModel);
		if (textSelection != null)
			textEditor.getSelectionProvider().setSelection(textSelection);
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

	private ITextEditor findTextEditor(IEditorPart editor) {
		if (editor instanceof ITextEditor)
			return (ITextEditor) editor;
		if (editor instanceof MultiPageEditorPart) {
			MultiPageEditorPart multiPageEditor = (MultiPageEditorPart) editor;
			IEditorPart[] editors = multiPageEditor.findEditors(editor.getEditorInput());
			for (int i = 0; i < editors.length; i++) {
				if (editors[i] instanceof ITextEditor)
					return (ITextEditor) editors[i];
			}
			Object adapted = multiPageEditor.getAdapter(IEditorPart.class);
			if (adapted instanceof ITextEditor)
				return (ITextEditor) adapted;
			Object selectedPage = multiPageEditor.getSelectedPage();
			if (selectedPage instanceof ITextEditor)
				return (ITextEditor) selectedPage;
		}
		return null;
	}

	public void dispose() {
		super.dispose();
		fContentProvider.dispose();
		fAnnotationModels.clear();
	}
}
