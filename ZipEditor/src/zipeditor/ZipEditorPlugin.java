/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ZipEditorPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ZipEditor"; //$NON-NLS-1$
	
	// The shared instance
	private static ZipEditorPlugin plugin;

	private Map images;
	
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
	
	public static void log(Object message) {
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
		debug(debugMessage);
	}
	
	public static void debug(Object message) {
		if (plugin.isDebugging()) {
			if (message instanceof Throwable)
				((Throwable) message).printStackTrace();
			else
				System.out.println(message);
		}
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
		return new Status(IStatus.ERROR, ZipEditorPlugin.PLUGIN_ID, 0, message != null ? message : exception.toString(), exception);
	}

}
