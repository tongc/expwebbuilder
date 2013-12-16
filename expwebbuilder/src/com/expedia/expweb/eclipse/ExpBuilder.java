package com.expedia.expweb.eclipse;

import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.resources.ProjectScope;
import org.osgi.service.prefs.Preferences;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.prefs.BackingStoreException;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class ExpBuilder implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window = null;

	@Override
	public void run(IAction action) {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = ws.getDescription();
		boolean autoBuildConf = desc.isAutoBuilding();
		desc.setAutoBuilding(false);
		try {
			ws.setDescription(desc);
		} catch (CoreException ex) {
			ex.printStackTrace();
		}

		// https://github.com/spring-projects/eclipse-integration-gradle/blob/master/org.springsource.ide.eclipse.gradle.ui/src/org/springsource/ide/eclipse/gradle/ui/actions/RefreshAllAction.java
		try {
			RefreshAllActionCore.callOn(Arrays.asList(ws.getRoot()
					.getProjects()));
		} catch (CoreException ex) {
			ex.printStackTrace();
		}

		try {
			cleanAndBuild(ws, "platform", false);
			cleanAndBuild(ws, "dataaccess", false);
			cleanAndBuild(ws, "domain", true);
			cleanAndBuild(ws, "webdomain-api", false);
			cleanAndBuild(ws, "webdomain", false);
			cleanAndBuild(ws, "checkout.ui", true);
			cleanAndBuild(ws, "stub", false);
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		desc.setAutoBuilding(autoBuildConf);
		try {
			ws.setDescription(desc);
		} catch (CoreException ex) {
			ex.printStackTrace();
		}

		new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"bundle-name-of-other-plugin");
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