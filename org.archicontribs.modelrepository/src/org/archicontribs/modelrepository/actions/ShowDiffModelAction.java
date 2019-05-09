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
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Show Diff Model Action
 * 
 * 1. Offer to save the model
 * 
 * @author Vincent Bellec
 */
public class ShowDiffModelAction extends AbstractModelAction {
    
    public ShowDiffModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_COMMIT));
        setText(Messages.ShowDiffAction_0);
        setToolTipText(Messages.ShowDiffAction_0);
    }

    public ShowDiffModelAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
    }

    @Override
    public void run() {
        IArchiRepository repo = getRepository();
        
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = repo.locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // If there are no changes at all, skip the diff call
        try {
			if(!repo.hasLocalChanges() && !repo.hasChangesToCommit())
			{
                MessageDialog.openInformation(fWindow.getShell()
                		, Messages.ShowDiffAction_0
                		, "No changes, nothing to diff");
				return;
			}
		} catch (IOException | GitAPIException ex ) {
			displayErrorDialog(Messages.ShowDiffAction_0, ex);
		}
        
        // Call the Diff
        repo.getDiff();
    }
}
