/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.IOException;
import java.util.List;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.ui.UIUtils;
import com.archimatetool.editor.ui.components.ExtendedTitleAreaDialog;
import com.archimatetool.editor.utils.StringUtils;

/**
 * Show Diff Dialog
 * 
 * @author Vincent Bellec
 */
public class ShowDiffDialog extends ExtendedTitleAreaDialog {
    
    private static String DIALOG_ID = "ShowDiffDialog"; //$NON-NLS-1$
    
    
    private List<DiffEntry> fDiffEntry;
    
    public ShowDiffDialog(Shell parentShell, List<DiffEntry> diffs) {
        super(parentShell, DIALOG_ID);
        setTitle(Messages.ShowDiffDialog_0);
        fDiffEntry = diffs;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.ShowDiffDialog_0);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(Messages.CommitDialog_1, IMessageProvider.INFORMATION);
        setTitleImage(IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.BANNER_COMMIT));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);
        
        // Repo and branch
        String shortBranchName = "unknown"; //$NON-NLS-1$

        Label label = new Label(container, SWT.NONE);
        label.setText(Messages.CommitDialog_6);
        
        label = new Label(container, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        label.setText(" [" + shortBranchName + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        
        // User name & email
        String userName = ""; //$NON-NLS-1$
        String userEmail = ""; //$NON-NLS-1$
        

        label = new Label(container, SWT.NONE);
        label.setText("CommitDialog_2");
        
        
        label = new Label(container, SWT.NONE);
        label.setText("CommitDialog_3");
        
        // Single text control so strip CRLFs
        label = new Label(container, SWT.NONE);
        label.setText("CommitDialog_4");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);
      
        
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        
        
        return area;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
    
    @Override
    protected Point getDefaultDialogSize() {
        return new Point(600, 450);
    }

}