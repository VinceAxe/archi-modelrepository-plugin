/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Undo the last commit
 */
public class UndoLastCommitAction extends AbstractModelAction {
    
    public UndoLastCommitAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_UNDO_COMMIT));
        setText(Messages.UndoLastCommitAction_0);
        setToolTipText(Messages.UndoLastCommitAction_0);
    }

    @Override
    public void run() {
        // This will either return the already open model or will actually open it
        // TODO We need to load a model without opening it in the models tree. But this will need a new API in IEditorModelManager
        IArchimateModel model = IEditorModelManager.INSTANCE.openModel(GraficoUtils.getModelFileName(getLocalRepositoryFolder()));
        
        if(model == null) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.UndoLastCommitAction_0,
                    Messages.UndoLastCommitAction_1);
            return;
        }

        // Offer to save it if dirty
        if(IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // Do the Grafico Export first
        // TODO: Commented this out because we can end up in a circle of undoing last commit and then needing a commit
        // exportModelToGraficoFiles(model, getLocalRepositoryFolder());
        
        // If there are changes to commit then they'll have to be abandoned
        try {
            if(GraficoUtils.hasChangesToCommit(getLocalRepositoryFolder())) {
                MessageDialog.openError(fWindow.getShell(),
                    Messages.UndoLastCommitAction_0,
                    Messages.UndoLastCommitAction_2);
                return;
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.RevertCommitAction_3, ex);
            return;
        }

        // Is the last commit unpushed?
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            Ref online = git.getRepository().findRef("origin/master"); //$NON-NLS-1$
            Ref local = git.getRepository().findRef("HEAD"); //$NON-NLS-1$
            
            try(RevWalk revWalk = new RevWalk(git.getRepository())) {
                RevCommit onlineCommit = revWalk.parseCommit(online.getObjectId());
                RevCommit localLatestCommit = revWalk.parseCommit(local.getObjectId());
                
                // Must have been pushed
                if(onlineCommit.equals(localLatestCommit)) {
                    MessageDialog.openError(fWindow.getShell(),
                            Messages.UndoLastCommitAction_0,
                            Messages.UndoLastCommitAction_3);
                    return;
                }
            }
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.UndoLastCommitAction_0, ex);
        }
        
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            ResetCommand resetCommand = git.reset();
            resetCommand.setRef("HEAD^"); //$NON-NLS-1$
            resetCommand.setMode(ResetType.SOFT);
            resetCommand.call();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.UndoLastCommitAction_0, ex);
        }
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        return getLocalRepositoryFolder() != null;
    }
}