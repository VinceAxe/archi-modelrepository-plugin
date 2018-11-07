/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Delete local repo folder
 *
 * @author Phillip Beauvoir
 */
public class DeleteModelAction extends AbstractModelAction {

    public DeleteModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_DELETE));
        setText(Messages.DeleteModelAction_0);
        setToolTipText(Messages.DeleteModelAction_0);
    }

    public DeleteModelAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
    }

    @Override
    public void run() {
        boolean confirm = MessageDialog.openConfirm(fWindow.getShell(), Messages.DeleteModelAction_0, Messages.DeleteModelAction_2);

        if(!confirm) {
            return;
        }

        try {
            // See if the model is open and close it if it is
            IArchimateModel model = getRepository().locateModel();
            if(model != null) {
                boolean didClose = IEditorModelManager.INSTANCE.closeModel(model);
                if(!didClose) {
                    return;
                }
            }

            // Delete folder
            FileUtils.deleteFolder(getRepository().getLocalRepositoryFolder());

            // Notify
            RepositoryListenerManager.INSTANCE.fireRepositoryChangedEvent(IRepositoryListener.REPOSITORY_DELETED, getRepository());
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.DeleteModelAction_0, ex);
        }
    }
}
