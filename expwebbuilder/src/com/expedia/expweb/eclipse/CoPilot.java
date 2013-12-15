package com.expedia.expweb.eclipse;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.inject.Inject;



//import org.eclipse.core.commands.Command;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.log.Logger;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class CoPilot implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	
	@Inject Logger logger; 

	/**
	 * The constructor.
	 */
	public CoPilot() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(IAction action) {
		MessageBox box = new MessageBox(window.getShell(), SWT.ICON_INFORMATION);
		InputDialog dialog = new InputDialog(window.getShell(), "Lets try!",
				"Please enter your message", "", null);
		if (dialog.open() == IStatus.OK) {
			String value = dialog.getValue();
		}

		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = ws.getDescription();
		desc.setAutoBuilding(false);
		try {
			ws.setDescription(desc);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// action parameter in run is not required
		// https://github.com/spring-projects/eclipse-integration-gradle/blob/master/org.springsource.ide.eclipse.gradle.ui/src/org/springsource/ide/eclipse/gradle/ui/actions/RefreshAllAction.java
		// new RefreshAllAction().run(action);
		try {
			RefreshAllActionCore.callOn(Arrays.asList(ws.getRoot()
					.getProjects()));
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			cleanAndBuild(ws, "platform");
			cleanAndBuild(ws, "dataaccess");
			cleanAndBuild(ws, "domain");
		} catch (BackingStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch(CoreException e1) {
			e1.printStackTrace();
		}
		
		desc.setAutoBuilding(false);
		try {
			ws.setDescription(desc);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"bundle-name-of-other-plugin");
	}
	
	private void cleanAndBuild(IWorkspace ws, String projectName) throws BackingStoreException, CoreException {
			IProject pro = ws.getRoot().getProject(projectName);				
				IScopeContext projectScope = new ProjectScope(pro);
				Preferences projectNode = projectScope.getNode("org.scala-ide.sdt.core");
				if (projectNode != null) {
					projectNode.put("compileorder", "JavaThenScala");
					projectNode.put("scala.compiler.useProjectSettings", "true");
					//value = projectNode.getBoolean("MyPreference", "true");
					//do something with the value.
				}
				projectNode.flush();

				pro.setDefaultCharset("UTF-8", null);
				for(IBuildConfiguration ibc:pro.getBuildConfigs()) {
					logger.log(LogService.LOG_ERROR, ibc.getName());
				}
				pro.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
				pro.build(IncrementalProjectBuilder.FULL_BUILD, null);
	}
	
	public class MyToolControl implements IProgressMonitor {
		  private ProgressBar progressBar;

		  @PostConstruct
		  public void createControls(Composite parent) {
		    progressBar = new ProgressBar(parent, SWT.SMOOTH);
		    progressBar.setBounds(100, 10, 200, 20);
		  }

		  @Override
		  public void worked(final int work) {
		        System.out.println("Worked");
		        progressBar.setSelection(progressBar.getSelection() + work);
		  }

		  @Override
		  public void subTask(String name) {

		  }

		  @Override
		  public void setTaskName(String name) {

		  }

		  @Override
		  public void setCanceled(boolean value) {

		  }

		  @Override
		  public boolean isCanceled() {
		    return false;
		  }

		  @Override
		  public void internalWorked(double work) {
		  }

		  @Override
		  public void done() {
		    System.out.println("Done");

		  }

		  @Override
		  public void beginTask(String name, int totalWork) {
		        progressBar.setMaximum(totalWork);
		        progressBar.setToolTipText(name);
		    System.out.println("Starting");
		  }
		} 

	/**
	 * s Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	@Override
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}
