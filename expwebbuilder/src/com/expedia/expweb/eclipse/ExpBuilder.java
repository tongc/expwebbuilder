package com.expedia.expweb.eclipse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.resources.ProjectScope;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.Preferences;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.osgi.service.prefs.BackingStoreException;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class ExpBuilder implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window = null;
	@Inject
	private LogService log;

	public ExpBuilder() {

	}
	
	private void openMsgBox(String msg) {
		MessageBox box = new MessageBox(window.getShell(),
				SWT.ICON_INFORMATION);
		InputDialog dialog = new InputDialog(window.getShell(),
				"Lets try!", msg, "", null);
		dialog.open();
	}

	@Override
	public void run(IAction action) {
		//Job job = new Job("Test Job") {
		//	@Override
		//	protected IStatus run(IProgressMonitor monitor) {
		//		// Set total number of work units
		//		monitor.beginTask("start task", 100);

				final IWorkspace ws = ResourcesPlugin.getWorkspace();
				IWorkspaceDescription desc = ws.getDescription();
				boolean autoBuildConf = desc.isAutoBuilding();
				desc.setAutoBuilding(false);
				try {
					ws.setDescription(desc);
					//openMsgBox("desc set");
				} catch (CoreException ex) {
					ex.printStackTrace();
				}

				// log.log(LogService.LOG_WARNING, "starting gradle refresh");
				// https://github.com/spring-projects/eclipse-integration-gradle/blob/master/org.springsource.ide.eclipse.gradle.ui/src/org/springsource/ide/eclipse/gradle/ui/actions/RefreshAllAction.java
					openMsgBox("Starting gradle");
					try {
						RefreshAllActionCore.callOn(new ArrayList<IProject>(){{
							//openMsgBox("gradle start ");
							add(ws.getRoot().getProject("platform"));
							add(ws.getRoot().getProject("dataaccess"));
							add(ws.getRoot().getProject("domain"));
							add(ws.getRoot().getProject("webdomain-api"));
							add(ws.getRoot().getProject("webdomain"));
							add(ws.getRoot().getProject("shared.ui"));
							add(ws.getRoot().getProject("checkout.ui"));
							add(ws.getRoot().getProject("stub"));
							add(ws.getRoot().getProject("integration.test"));
							//openMsgBox("gradle stop ");
						}}).join();
					} catch (CoreException e1) {
						openMsgBox("clean build error " + e1.getMessage());
						e1.printStackTrace();
					}
					//RefreshAllActionCore.callOn(Arrays.asList(ws.getRoot().getProjects()));
				// log.log(LogService.LOG_WARNING, "completed gradle refresh");
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				try {
					//openMsgBox("starting platform clean build");
					cleanAndBuild(ws, "platform", false);
					//openMsgBox("starting dataaccess clean build");
					cleanAndBuild(ws, "dataaccess", false);
					//openMsgBox("starting domain clean build");
					cleanAndBuild(ws, "domain", true);
					//openMsgBox("starting webdomain api clean build");
					cleanAndBuild(ws, "webdomain-api", false);
					//openMsgBox("starting webdomain clean build");
					cleanAndBuild(ws, "webdomain", false);
					//openMsgBox("starting shared.ui clean build");
					cleanAndBuild(ws, "shared.ui", false);
					//openMsgBox("starting checkout.ui clean build");
					cleanAndBuild(ws, "checkout.ui", true);
					//openMsgBox("starting stub clean build");
					cleanAndBuild(ws, "stub", false);
				} catch (BackingStoreException e) {
					openMsgBox("clean build error " + e.getMessage());
					e.printStackTrace();
				} catch (CoreException e) {
					openMsgBox("clean build error " + e.getMessage());
					e.printStackTrace();
				}
				desc.setAutoBuilding(autoBuildConf);
				try {
					//openMsgBox("set desc back");
					ws.setDescription(desc);
				} catch (CoreException ex) {
					ex.printStackTrace();
				}
				
				openMsgBox("expweb build complete");
		//		return Status.OK_STATUS;
		//	}
		//};
		//job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		//job.schedule();
	}

	private void cleanAndBuild(IWorkspace ws, String projectName,
			boolean useScalaBuilder) throws BackingStoreException,
			CoreException {
		IProject pro = ws.getRoot().getProject(projectName);
		IScopeContext projectScope = new ProjectScope(pro);
		Preferences projectNode = projectScope
				.getNode("org.scala-ide.sdt.core");
		if (projectNode != null) {
			projectNode.put("compileorder", "JavaThenScala");
			projectNode.put("scala.compiler.useProjectSettings", "true");
			// value = projectNode.getBoolean("MyPreference", "true");
			// do something with the value.
		}
		projectNode.flush();

		pro.setDefaultCharset("UTF-8", null);
		if (useScalaBuilder) {
			ICommand[] builders = pro.getDescription().getBuildSpec();
			List<ICommand> newBuilders = new ArrayList<ICommand>();
			for (ICommand command : builders) {
				if (!command.getBuilderName().equals(
						"org.eclipse.jdt.core.javabuilder")) {
					newBuilders.add(command);
				}
			}
			IProjectDescription desc = pro.getDescription();
			desc.setBuildSpec(newBuilders.toArray(new ICommand[0]));
			pro.setDescription(desc, null);
		}
		pro.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
		pro.build(IncrementalProjectBuilder.FULL_BUILD, null);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}