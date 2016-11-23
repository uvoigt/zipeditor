/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelDecorator;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISaveablesLifecycleListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.SaveablesProvider;

import zipeditor.model.IModelListener;
import zipeditor.model.Node;
import zipeditor.model.ZipContentDescriber;
import zipeditor.model.ZipModel;

public class ZipContentProvider extends SaveablesProvider implements ITreeContentProvider, IModelListener {
	private class StyledLabelProviderAdapter implements IStyledLabelProvider, ITableLabelProvider, IColorProvider, IFontProvider {

		private final ILabelProvider provider;

		public StyledLabelProviderAdapter(ILabelProvider provider) {
			this.provider= provider;
		}
		
		public Image getImage(Object element) {
			return provider.getImage(element);
		}

		public StyledString getStyledText(Object element) {
			if (provider instanceof IStyledLabelProvider) {
				return ((IStyledLabelProvider) provider).getStyledText(element);
			}
			if (provider instanceof DelegatingStyledCellLabelProvider) {
				return ((DelegatingStyledCellLabelProvider) provider).getStyledStringProvider().getStyledText(element);
			}
			String text= provider.getText(element);
			if (text == null)
				text= ""; //$NON-NLS-1$
			return new StyledString(text);
		}

		public void addListener(ILabelProviderListener listener) {
			provider.addListener(listener);
		}

		public void dispose() {
			provider.dispose();
		}

		public boolean isLabelProperty(Object element, String property) {
			return provider.isLabelProperty(element, property);
		}

		public void removeListener(ILabelProviderListener listener) {
			provider.removeListener(listener);
		}

		public Color getBackground(Object element) {
			if (provider instanceof IColorProvider) {
				return ((IColorProvider) provider).getBackground(element);
			}
			return null;
		}

		public Color getForeground(Object element) {
			if (provider instanceof IColorProvider) {
				return ((IColorProvider) provider).getForeground(element);
			}
			return null;
		}

		public Font getFont(Object element) {
			if (provider instanceof IFontProvider) {
				return ((IFontProvider) provider).getFont(element);
			}
			return null;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (provider instanceof ITableLabelProvider) {
				return ((ITableLabelProvider) provider).getColumnImage(element, columnIndex);
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (provider instanceof ITableLabelProvider) {
				return ((ITableLabelProvider) provider).getColumnText(element, columnIndex);
			}
			return null;
		}	
	}

	private final class ClosedFileAwareLabelProvider extends DecoratingStyledCellLabelProvider
			implements ILabelProvider {
		private final ILabelProvider orig;

		private ClosedFileAwareLabelProvider(ILabelProvider orig) {
			super(new StyledLabelProviderAdapter(orig), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(), null);
			this.orig = orig;
		}

		public Image getImage(Object element) {
			if (element instanceof IFile) {
				IFile file = (IFile) element;
				if (isForUs(file)) {
					boolean open = isOpen(file);
					Image img = open
					? ZipEditorPlugin.getImage("icons/zipicon_open.gif") : //$NON-NLS-1$
						ZipEditorPlugin.getImage("icons/zipicon_closed.gif"); //$NON-NLS-1$
					ILabelDecorator labelDecorator = getLabelDecorator();
					if (labelDecorator instanceof LabelDecorator) {
						DecorationContext context = new DecorationContext();
						return ((LabelDecorator) labelDecorator).decorateImage(img, element, context);
					} else {
						return labelDecorator.decorateImage(img, element);
					}
				}
			} else if (element instanceof Node) {
				if (viewer instanceof CommonViewer) {
					if (!((ColumnViewer) viewer).isBusy())
					((CommonViewer) viewer).refresh(((Node) element).getModel().getZipPath(), true);
				}
			}
			return orig.getImage(element);
		}

		public String getText(Object element) {
			return orig.getText(element);
		}
	}

	private class SaveableModel extends Saveable {
		private final IFile file; // TODO beim Umbennennen ist die Referenz falsch
		public SaveableModel(IFile file) {
			this.file = file;
		}

		public String getName() {
			return file.getName();
		}

		public String getToolTipText() {
			return null;
		}

		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		public void doSave(IProgressMonitor monitor) throws CoreException {
			ZipModel model = getModel(file, false);
			if (model != null) {
				ZipEditorPlugin.getSpace().saveModel(model, new Path(model.getZipPath().getAbsolutePath()), monitor);
				fireSaveablesDirtyChanged(new SaveableModel[] { this} );
				refreshViewer(file);
			}
		}

		public boolean isDirty() {
			ZipModel model = getModel(file, false);
			if (model == null)
				return false;
			System.out.println("isDirty: " + model.isDirty());
			try {
				file.setSessionProperty(new QualifiedName("zipeditor", "dirty"), model.isDirty() ? Boolean.TRUE : Boolean.FALSE); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return model.isDirty();
		}

		public boolean equals(Object object) {
			if (object == this)
				return true;
			if (object instanceof SaveableModel)
				return ((SaveableModel) object).file.equals(file);
			return false;
		}

		public int hashCode() {
			return file.hashCode();
		}
		
	}

	private int fMode = PreferenceConstants.VIEW_MODE_TREE;
	private boolean fDisposeModel = true;
	private Viewer viewer;
	private IBaseLabelProvider labelProvider;
	private final Set fSaveables = new HashSet();
	private final IPartListener fPartListener = new IPartListener() {
		public void partOpened(IWorkbenchPart part) {
		}
		
		public void partDeactivated(IWorkbenchPart part) {
		}
		
		public void partClosed(IWorkbenchPart part) {
		}
		
		public void partBroughtToTop(IWorkbenchPart part) {
		}
		
		public void partActivated(IWorkbenchPart part) {
		}
	};

	public ZipContentProvider() {
//		((CommonViewer) viewer).getCommonNavigator().getSite().getPage().addPartListener(fPartListener);
		ISaveablesLifecycleListener listener = (ISaveablesLifecycleListener) PlatformUI.getWorkbench().getService(ISaveablesLifecycleListener.class);
		init(listener);
	}

	public ZipContentProvider(int mode) {
		this();
		fMode = mode;
	}

	public Object[] getChildren(final Object parentElement) {
		if (parentElement instanceof Node)
			return getNodeChildren((Node) parentElement);
		if (parentElement instanceof IFile)
			return getFileChildren((IFile) parentElement);
		if (parentElement instanceof IContainer) {
				new Job("setFileStatus") { //$NON-NLS-1$
					protected IStatus run(IProgressMonitor monitor) {
						try {
							((IContainer) parentElement).accept(new IResourceVisitor() {
								public boolean visit(IResource resource) throws CoreException {
									if (!(resource instanceof IFile))
										return true;
									IFile file = (IFile) resource;
									if (isForUs(file)) {
										ZipModel model = getModel(file, false);
										boolean open = model != null;
										resource.setSessionProperty(new QualifiedName("zipeditor", "open"), open ? Boolean.TRUE : Boolean.FALSE); //$NON-NLS-1$ //$NON-NLS-2$
										resource.setSessionProperty(new QualifiedName("zipeditor", "dirty"), open && model.isDirty() ? Boolean.TRUE : Boolean.FALSE); //$NON-NLS-1$ //$NON-NLS-2$
									}
									return false;
								}
							});
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return Status.OK_STATUS;
					}
				}.schedule();
		}
		return new Object[0];
	}

	private Object[] getNodeChildren(Node node) {
//TODO		fModels.put(null, node.getModel());
		if ((fMode & PreferenceConstants.VIEW_MODE_TREE) > 0)
			return node.getChildren();
		else {
			List result = new ArrayList();
			addChildren(result, node, 0);
			return result.toArray();
		}
	}

	private void addChildren(List list, Node node, int depth) {
		Node[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			Node child = children[i];
			addChildren(list, child, depth + 1);
			boolean foldersVisible = (fMode & PreferenceConstants.VIEW_MODE_FOLDERS_VISIBLE) > 0;
			if (foldersVisible || !child.isFolder()) {
				boolean allInOneLayer = (fMode & PreferenceConstants.VIEW_MODE_FOLDERS_ONE_LAYER) > 0;
				if (depth == 0 || allInOneLayer)
					list.add(child);
			}
		}
	}

	private Object[] getFileChildren(IFile file) {
		if (!isForUs(file))
			return new Object[0];
		ZipModel model = getModel(file, true);
		while (model.isInitializing()) {
			try {
				Thread.sleep(100);
			} catch (Exception ignore) {
			}
		}
		addSaveable(model, file);
		fireSaveablesOpened(getSaveables());
		return getNodeChildren(model.getRoot());
	}
	
	private ZipModel getModel(final IFile file, boolean create) {
		File fsFile = file.getLocation().toFile();
		ZipModel model = ZipEditorPlugin.getSpace().getModel(fsFile);
		if (model == null && create) {
			try {
				model = new ZipModel(file.getLocation().toFile(), file.getContents(), file.isReadOnly(), null);
				file.setSessionProperty(new QualifiedName("zipeditor", "open"), Boolean.TRUE); //$NON-NLS-1$ //$NON-NLS-2$
				file.setSessionProperty(new QualifiedName("zipeditor", "dirty"), Boolean.FALSE); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException e) {
				ZipEditorPlugin.log(new Status(IStatus.ERROR, ZipEditorPlugin.PLUGIN_ID,  "Cannot create Zip model", e)); //$NON-NLS-1$
				return new ZipModel(file.getLocation().toFile(), new ByteArrayInputStream(new byte[0]), true, null);
			}
			if (viewer instanceof CommonViewer) {
				((CommonViewer) viewer).getTree().getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (viewer != null)
							((CommonViewer) viewer).refresh(file, true);
					}
				});
			}
		}
		return model;
	}

	private void setupCommonViewer(final CommonViewer viewer) {
		ILabelProvider orig = (ILabelProvider) viewer.getLabelProvider();
		if (orig == labelProvider)
			return;
		labelProvider = new ClosedFileAwareLabelProvider(orig);
//		viewer.getControl().getDisplay().asyncExec(new Runnable() {
//			public void run() {
				viewer.setLabelProvider(labelProvider);
//			}
//		});
		viewer.addOpenListener(new IOpenListener() {
			public void open(final OpenEvent event) {
				viewer.getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						IStructuredSelection selection = (IStructuredSelection) event.getSelection();
						for (Iterator it = selection.iterator(); it.hasNext(); ) {
							Object next = it.next();
							viewer.refresh(next, true);
							if (next instanceof IFile) {
								IFile file = (IFile) next;
								SaveableModel saveableModel = findModel(file);
								if (saveableModel == null) {
									ZipModel model = ZipEditorPlugin.getSpace().getModel(file.getLocation().toFile());
									addSaveable(model, file);
								}
							}
						}
					}
				});
			}
		});
//		viewer.getCommonNavigator().getNavigatorContentService().getDnDService().bindDragAssistant(
//				new ZipDragAssistant());
	}

	public Object getParent(Object element) {
		return element instanceof Node ? ((Node) element).getParent() : null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IFile)
			return isForUs((IFile) element);
		return getChildren(element).length > 0;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		disposeModels();
		if (viewer instanceof CommonViewer) {
			((CommonViewer) viewer).getCommonNavigator().getSite().getPage().removePartListener(fPartListener);
		}
		viewer = null;
	}

	private void disposeModels() {
		if (fDisposeModel && viewer instanceof CommonViewer)
			ZipEditorPlugin.getSpace().disposeModels();
	}

	public boolean isOpen(IFile file) {
		return ZipEditorPlugin.getSpace().getModel(file.getLocation().toFile()) != null;
	}
	
	public void openModelFor(IFile file) throws CoreException {
		getModel(file, true);
	}

	public void closeModelFor(IFile file) {
		ZipModel model = ZipEditorPlugin.getSpace().getModel(file.getLocation().toFile());
		if (model == null)
			return;
		// this is unusable as of Eclipse 3.7.2
//		fireSaveablesClosing(getSaveables(), false);
		if (model.isDirty()) {
			final SaveableModel saveableModel = findModel(file);
			if (MessageDialog.openQuestion(viewer.getControl().getShell(),
					Messages.getString("ZipEditor.4"), //$NON-NLS-1$
					"The file " + saveableModel.getName() + " has been changed. Should it be saved?")) {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						try {
							saveableModel.doSave(monitor);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, op);
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		try {
			file.setSessionProperty(new QualifiedName("zipeditor", "open"), Boolean.FALSE); //$NON-NLS-1$ //$NON-NLS-2$
			file.setSessionProperty(new QualifiedName("zipeditor", "dirty"), Boolean.FALSE); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (CoreException e) {
			e.printStackTrace();
		}
		ZipEditorPlugin.getSpace().disposeModel(model);
		Saveable[] saveables = getSaveables();
		SaveableModel saveableModel = findModel(file);
		fSaveables.remove(saveableModel);
		fireSaveablesClosed(saveables);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null) {
			disposeModels();
		} else {
			this.viewer = viewer;
			if (viewer instanceof CommonViewer)
				setupCommonViewer((CommonViewer) viewer);
		}
	}

	public boolean isForUs(IFile file) {
		try {
			IContentDescription contentDescription = file.getContentDescription();
			if (contentDescription == null)
				return false;
			if (contentDescription.getContentType() == null)
				return false;
			String contentTypeId = contentDescription.getContentType().getId();
			return ZipContentDescriber.GZ_FILE.equals(contentTypeId)
					|| ZipContentDescriber.TAR_FILE.equals(contentTypeId)
					|| ZipContentDescriber.TGZ_FILE.equals(contentTypeId)
					|| ZipContentDescriber.ZIP_FILE.equals(contentTypeId);
		} catch (CoreException e) {
			ZipEditorPlugin.log(e);
			return false;
		}
	}

	public void disposeModel(boolean enable) {
		fDisposeModel = enable;
	}
	private void addSaveable(ZipModel model, IFile file) {
		fSaveables.add(new SaveableModel(file));
		model.addModelListener(this);
	}

	private void removeSaveable(SaveableModel saveableModel) {
		ZipModel model = getModel(saveableModel.file, false);
		if (model != null)
			model.removeModelListener(this);
		fSaveables.remove(saveableModel);
	}

	public Object[] getElements(Saveable saveable) {
		if (saveable instanceof SaveableModel)
			return new Object[] { ((SaveableModel) saveable).file };
		return new Object[0];
	}

	public Saveable getSaveable(Object element) {
		if (element instanceof IFile)
			return findModel((IFile) element);
		return null;
	}

	public Saveable[] getSaveables() {
		return (Saveable[]) fSaveables.toArray(new Saveable[fSaveables.size()]);
	}

	private SaveableModel findModel(IFile file) {
		for (Iterator it = fSaveables.iterator(); it.hasNext();) {
			SaveableModel saveable = (SaveableModel) it.next();
			if (saveable.file.equals(file))
				return saveable;
		}
		return null;
	}

	private SaveableModel findModel(ZipModel model) {
		for (Iterator it = fSaveables.iterator(); it.hasNext();) {
			SaveableModel saveable = (SaveableModel) it.next();
			if (saveable.file.getLocation().toFile().equals(model.getZipPath()))
				return saveable;
		}
		return null;
	}

	public void modelChanged(ModelChangeEvent event) {
		ZipModel model = event.getModelRoot().getModel();
		final SaveableModel saveableModel = findModel(model);
		viewer.getControl().getDisplay().syncExec(new Runnable() {
			public void run() {
				fireSaveablesDirtyChanged(new SaveableModel[] { saveableModel });
			}
		});
		final Node node = event.getNode();
		if (node != null) {
			refreshViewer(node);
			IFile file = Utils.getFile(node.getModel().getZipPath());
			if (file != null)
				refreshViewer(file);
		}

		if (event.isDispose()) {
			removeSaveable(saveableModel);
//			((CommonViewer) viewer).getCommonNavigator().
		}
	}

	private void refreshViewer(final Object node) {
		if (viewer instanceof CommonViewer) {
			viewer.getControl().getDisplay().syncExec(new Runnable() {
				public void run() {
					((CommonViewer) viewer).refresh(node);
				}
			});
		}
	}
}
