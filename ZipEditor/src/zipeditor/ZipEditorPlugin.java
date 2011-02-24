/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import zipeditor.model.Node;
import zipeditor.model.ZipModel;

/**
 * The activator class controls the plug-in life cycle
 */
public class ZipEditorPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ZipEditor"; //$NON-NLS-1$
	
	// The shared instance
	private static ZipEditorPlugin plugin;

	public final static boolean DEBUG = Boolean.valueOf(Platform.getDebugOption("ZipEditor/debug")).booleanValue(); //$NON-NLS-1$

	private Map images;
	
	private Map fModelToFileNode = new HashMap();

	/**
	 * The constructor
	 */
	public ZipEditorPlugin() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		if (images != null) {
			for (Iterator it = images.values().iterator(); it.hasNext();) {
				((Image) it.next()).dispose();
			}
			images.clear();
			images = null;
		}
		fModelToFileNode.clear();
		fModelToFileNode = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ZipEditorPlugin getDefault() {
		return plugin;
	}
	
	public void addFileMonitor(File file, Node node) {
		Map fileToNode = (Map) fModelToFileNode.get(node.getModel());
		if (fileToNode == null)
			fModelToFileNode.put(node.getModel(), fileToNode = new HashMap());
		fileToNode.put(file, new Object[] { node, new Long(System.currentTimeMillis()) });
	}
	
	public void removeFileMonitors(ZipModel model) {
		Map fileToNode = (Map) fModelToFileNode.remove(model);
		if (fileToNode != null)
			fileToNode.clear();
	}

	public void checkFilesForModification(ZipModel model) {
		Map fileToNode = (Map) fModelToFileNode.get(model);
		if (fileToNode == null)
			return;
		for (Iterator it = fileToNode.keySet().iterator(); it.hasNext(); ) {
			File file = (File) it.next();
			Object[] value = (Object[]) fileToNode.get(file);
			long creationTime = ((Long) value[1]).longValue();
			if (file.lastModified() > creationTime) {
				value[1] = new Long(file.lastModified());
				indicateModification(fileToNode, file, (Node) value[0]);
			}
		}
	}

	private void indicateModification(Map fileToNode, File file, Node node) {
		if (MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				Messages.getString("ZipEditor.1"), Messages.getFormattedString("ZipEditor.0", //$NON-NLS-1$ //$NON-NLS-2$
						new Object[] { file.getName(), node.getModel().getZipPath() != null
								? node.getModel().getZipPath().getName() : "" }))) { //$NON-NLS-1$
			node.updateContent(file);
		} else {
			node.reset();
			fileToNode.remove(file);
		}
	}

	public static IStatus log(Object message) {
		IStatus status = null;
		Object debugMessage = message;
		if (message instanceof IStatus) {
			status = (IStatus) message;
			debugMessage = ((IStatus) message).getMessage();
		} else if (message instanceof Throwable) {
			status = createErrorStatus(((Throwable) message).getMessage(), (Throwable) message);
		} else {
			status = createErrorStatus(message != null ? message.toString() : null, null);
		}
		plugin.getLog().log(status);
		if (DEBUG) {
			if (debugMessage instanceof Throwable)
				((Throwable) debugMessage).printStackTrace();
			else
				System.out.println(debugMessage);
		}
		return status;
	}
	
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static Image getImage(ImageDescriptor descriptor) {
		return doGetImage(descriptor);
	}

	public static Image getImage(String path) {
		return doGetImage(path);
	}
	
	private static Image doGetImage(Object object) {
		Map images = plugin.images;
		if (images == null)
			images = plugin.images = new HashMap();
		Image image = (Image) images.get(object);
		if (image == null) {
			ImageDescriptor descriptor = object instanceof ImageDescriptor ? (ImageDescriptor) object : getImageDescriptor((String) object);
			image = descriptor != null ? descriptor.createImage() : ImageDescriptor.getMissingImageDescriptor().createImage();
			images.put(object, image);
		}
		return image;
	}

	public static IStatus createErrorStatus(String message, Throwable exception) {
		return new Status(IStatus.ERROR, PLUGIN_ID, 0, message != null ? message : exception.toString(), exception);
	}

	private static IStatus[] createErrorStatuses(Throwable exception) {
		StringWriter sw = new StringWriter();
		if (exception != null)
			exception.printStackTrace(new PrintWriter(sw));
		StringTokenizer st = new StringTokenizer(sw.toString(), "\r\n"); //$NON-NLS-1$
		IStatus[] status = new IStatus[st.countTokens()];
		for (int i = 0; i < status.length; i++) {
			status[i] = createErrorStatus(st.nextToken(), null);
		}
		return status;
	}

	public static void showErrorDialog(Shell shell, String message, Throwable exception, boolean logError) {
		if (logError)
			log(exception);
		ErrorDialog.openError(shell, Messages.getString("ZipEditor.8"), //$NON-NLS-1$
				message,
				new MultiStatus(PLUGIN_ID, 0, createErrorStatuses(exception),
						Messages.getString("ZipEditorPlugin.0"), exception)); //$NON-NLS-1$
	}

	public static void showErrorDialog(Shell shell, String message, Throwable exception) {
		showErrorDialog(shell, message, exception, true);
	}

}
