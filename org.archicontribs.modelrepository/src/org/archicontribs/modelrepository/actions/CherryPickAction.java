/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.ProgressMonitor;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.merge.MergeConflictHandler;
import org.eclipse.emf.common.archive.Handler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.actions.AbstractModelSelectionAction;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Cherry Pick a particular commit into the branch
 */
public class CherryPickAction extends AbstractModelAction {
	/*
    protected static final int CHERRY_STATUS_ERROR = -1;
    protected static final int CHERRY_STATUS_OK = 0;
    protected static final int CHERRY_STATUS_UP_TO_DATE = 1;
    // */
    protected static final int CHERRY_STATUS_MERGE_CANCEL = 2;
    // */
    private int fCherryPickStatus;
    
    private RevCommit fCommit;
    private BranchInfo fCurrentBranch; //, fSelectedBranch;
	
    public CherryPickAction(IWorkbenchWindow window) {
        super(window);

        // no image for the cherry pick
        // setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_SYNCED));
        setText(NLS.bind(Messages.CherryPickAction_0, ""));
        setToolTipText(NLS.bind(Messages.CherryPickAction_0, ""));
    }
    
    
    public void setCurrentBranch(BranchInfo currentBranch) {
    	fCurrentBranch = currentBranch;
    	updateText();
    }

    //public void setSelectedBranch(BranchInfo selectedBranch) {
    	//fSelectedBranch = selectedBranch;
    //}
    
    public void setCommit(RevCommit commit) {
        fCommit = commit;
        setEnabled(shouldBeEnabled());
    }
    
    // the text is updated with the current branch that we are
    private void updateText() {
    	if(fCurrentBranch != null) {
	        setText(NLS.bind(Messages.CherryPickAction_0, fCurrentBranch.getShortName()));
	        setToolTipText(NLS.bind(Messages.CherryPickAction_0, fCurrentBranch.getShortName()));
    	}
    }
    
    @Override
    public void run() {
    	// *
        // Offer to save the model if open and dirty
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
        		NLS.bind(Messages.CherryPickAction_0, fCurrentBranch.getShortName()),
                Messages.CherryPickAction_1);
		// Have to abort if no, impossible to cherry-pick a dirty workspace
        if(!response) {
            return;
        }
        try {
        	CherryPickResult result = getRepository().cherryPickCommit(fCommit);
        	
        	if(result.getStatus() == CherryPickStatus.CONFLICTING) {
        		MergeConflictHandler conflictHandler = new MergeConflictHandler(result.get, fCommit, getRepository(), fWindow.getShell());
        		try {
        			conflictHandler.init();
        			
        		}
                catch(IOException | GitAPIException ex) {
                    handler.resetToLocalState(); // Clean up

                    if(ex instanceof CanceledException) {
                        fCherryPickStatus = CHERRY_STATUS_MERGE_CANCEL;
                        return;
                    }

                    throw ex;
                }
        	}
        	
        	MessageDialog.openInformation(fWindow.getShell(),
        			NLS.bind(Messages.CherryPickAction_0, fCurrentBranch.getShortName()), 
        			result.getStatus().toString());
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(NLS.bind(Messages.CherryPickAction_0, fCurrentBranch.getShortName()), ex);
            return;
        }
		/*
        // Walk the tree and get the contents of the commit
        try(Repository repository = Git.open(getRepository().getLocalRepositoryFolder()).getRepository()) {
            repository.writeCherryPickHead(head);
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.CherryPickAction_0, ex);
            return;
        }

        // Commit changes
        try {
            getRepository().commitChanges(Messages.RestoreCommitAction_3 + " '" + fCommit.getShortMessage() + "'", false); //$NON-NLS-1$ //$NON-NLS-2$

            // Save the checksum
            getRepository().saveChecksum();
        }
        catch(GitAPIException | IOException ex) {
            displayErrorDialog(Messages.RestoreCommitAction_0, ex);
        }
        
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
        */
    }
    
    //*
    // same function as in Restore Commit at the moment
    @Override
    protected boolean shouldBeEnabled() {
        if(getRepository() == null) {
            return false;
        }
        
        boolean isHead = false;
        try {
            isHead = isCommitLocalHead();
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        
        return fCommit != null && !isHead;
    }
    
    // same function as in Restore Commit 
    protected boolean isCommitLocalHead() throws IOException {
        if(fCommit == null) {
            return false;
        }
        
        try(Repository repo = Git.open(getRepository().getLocalRepositoryFolder()).getRepository()) {
            ObjectId headID = repo.resolve(IGraficoConstants.HEAD);
            ObjectId commitID = fCommit.getId();
            return commitID.equals(headID);
        }
    }
}
