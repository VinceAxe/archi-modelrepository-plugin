/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticater;
import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.dialogs.CloneInputDialog;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Clone a model
 *
 * 1. Get user credentials
 * 2. Check Proxy
 * 3. Clone from Remote
 * 4. If Grafico files exist load the model from the Grafico files and save it as temp file
 * 5. If Grafico files do not exist create a new temp model and save it
 * 6. Store user credentials if prefs agree
 */
public class CloneModelAction extends AbstractModelAction {

    public CloneModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CLONE));
        setText(Messages.CloneModelAction_0);
        setToolTipText(Messages.CloneModelAction_0);
    }

    @Override
    public void run() {
        CloneInputDialog dialog = new CloneInputDialog(fWindow.getShell());
        if(dialog.open() != Window.OK) {
            return;
        }

        final String repoURL = dialog.getURL();
        final String userName = dialog.getUsername();
        final String userPassword = dialog.getPassword();

        if(!StringUtils.isSet(repoURL) && !StringUtils.isSet(userName) && !StringUtils.isSet(userPassword)) {
            return;
        }

        File localRepoFolder = new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                GraficoUtils.getLocalGitFolderName(repoURL));

        // Folder is not empty
        if(localRepoFolder.exists() && localRepoFolder.isDirectory() && localRepoFolder.list().length > 0) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.CloneModelAction_0,
                    Messages.CloneModelAction_2 + " " + localRepoFolder.getAbsolutePath()); //$NON-NLS-1$

            return;
        }

        setRepository(new ArchiRepository(localRepoFolder));

        try {
            // Proxy check
            ProxyAuthenticater.update(repoURL);

            // Clone
            Exception[] exception = new Exception[1];
            IProgressService ps = PlatformUI.getWorkbench().getProgressService();
            ps.busyCursorWhile(new IRunnableWithProgress() {
                @Override
				public void run(IProgressMonitor pm) {
                    try {
                        getRepository().cloneModel(repoURL, userName, userPassword, new ProgressMonitorWrapper(pm));
                    }
                    catch(GitAPIException | IOException ex) {
                        exception[0] = ex;
                    }
                }
            });

            if(exception[0] != null) {
                throw exception[0];
            }

            // Load it from the Grafico files if we can
            IArchimateModel graficoModel = new GraficoModelLoader(getRepository()).loadModel();

            // We couldn't load it from Grafico so create a new blank model
            if(graficoModel == null) {
                // New one. This will open in the tree
                IArchimateModel model = IEditorModelManager.INSTANCE.createNewModel();
                model.setFile(getRepository().getTempModelFile());

                // And Save it
                IEditorModelManager.INSTANCE.saveModel(model);

                // Export to Grafico
                getRepository().exportModelToGraficoFiles();

                // And do a first commit
                getRepository().commitChanges(Messages.CloneModelAction_3, false);

                // Save the checksum
                getRepository().saveChecksum();
            }

            // Store repo credentials if option is set
            if(ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_STORE_REPO_CREDENTIALS)) {
                SimpleCredentialsStorage scs = new SimpleCredentialsStorage(new File(getRepository().getLocalGitFolder(), IGraficoConstants.REPO_CREDENTIALS_FILE));
                scs.store(userName, userPassword);
            }

            // Notify listeners
            notifyChangeListeners(IRepositoryListener.REPOSITORY_ADDED);
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.CloneModelAction_0, ex);
        }
    }

    @Override
    protected boolean shouldBeEnabled() {
        return true;
    }
}
