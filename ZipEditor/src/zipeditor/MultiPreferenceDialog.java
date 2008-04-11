package zipeditor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class MultiPreferenceDialog extends PreferenceDialog {
	private IAdaptable[] fElements;
	private MultiElementPropertyPage fPropertyPage;

	public MultiPreferenceDialog(Shell parentShell, PreferenceManager manager,
			IAdaptable[] elements) {
		super(parentShell, manager);
		fElements = elements;
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(createText());
		
		selectPropertyPage();
	}

	private void selectPropertyPage() {
        IExtension[] extensions = Platform.getExtensionRegistry().getExtensionPoint("org.eclipse.ui.propertyPages").getExtensions(); //$NON-NLS-1$
        for (int i = 0; i < extensions.length; i++) {
        	if (!extensions[i].getContributor().getName().equals(ZipEditorPlugin.PLUGIN_ID))
        		continue;
        	IConfigurationElement[] elements = extensions[i].getConfigurationElements();
        	for (int j = 0; j < elements.length; j++) {
				String objectClass = elements[j].getAttribute("objectClass"); //$NON-NLS-1$
				if (objectClass == null)
					continue;
				try {
					Class clazz = Platform.getBundle(elements[j].getContributor().getName()).loadClass(objectClass);
					if (fElements != null && fElements.length > 0 && fElements[0].getClass().isAssignableFrom(clazz)) {
						fPropertyPage = (MultiElementPropertyPage) elements[j].createExecutableExtension("class"); //$NON-NLS-1$
						fPropertyPage.setElements(fElements);
						fPropertyPage.setTitle(elements[j].getAttribute("name")); //$NON-NLS-1$
						getPreferenceManager().addToRoot(new PreferenceNode(new String(), fPropertyPage));
						break;
					}
				} catch (Exception e) {
					ZipEditorPlugin.log(e);
					continue;
				}
	        }
		}
	}

	private String createText() {
		if (fElements != null) {
			if (fElements.length == 1) {
				String s = ((IWorkbenchAdapter) fElements[0].getAdapter(IWorkbenchAdapter.class)).getLabel(fElements[0]);
				return Messages.getFormattedString("MultiPreferenceDialog.0", s); //$NON-NLS-1$
			} else {
				return Messages.getFormattedString("MultiPreferenceDialog.1", new Integer(fElements.length)); //$NON-NLS-1$
			}
		}
		return Messages.getFormattedString("MultiPreferenceDialog.0", null); //$NON-NLS-1$
	}
}
