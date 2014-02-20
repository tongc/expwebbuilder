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
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.resources.ProjectScope;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.Preferences;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.osgi.service.prefs.BackingStoreException;

public class ExpBuilderLogging extends WorkspaceJob implements
		IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window = null;
	@Inject
	private LogService log;

	public ExpBuilderLogging() {
		super("Expweb builder with popup logging");
	}

	private void openMsgBox(String msg) {
		MessageBox box = new MessageBox(window.getShell(), SWT.ICON_INFORMATION);
		box.setMessage(msg);
		box.open();
	}

	@Override
	public void run(IAction action) {
		final IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProgressMonitor monitor = null;
		String originalEclipseGradleStr = null;
		InputStream is;
		ResourceAttributes ra = null;
		boolean fileIsReadOnly = true;
		IFile file = null;
		String newEclipseGradleStr = null;
		
		IWorkspaceDescription desc = ws.getDescription();
		boolean autoBuildConf = desc.isAutoBuilding();
		desc.setAutoBuilding(false);
		try {
			ws.setDescription(desc);
			openMsgBox("auto build off");
		} catch (CoreException ex) {
			ex.printStackTrace();
		}
		
		try {
			openMsgBox("start refreshing");
			ws.getRoot().getProject("trunk").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("platform").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("dataaccess").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("domain").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("webdomain-api").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("webdomain").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("shared.ui").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("checkout.ui").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("stub").refreshLocal(IResource.DEPTH_INFINITE, monitor);
			ws.getRoot().getProject("integration.test").refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (CoreException e3) {
			openMsgBox("refresh project error: " + e3.getMessage());
			e3.printStackTrace();
		}
		
		try {
			openMsgBox("start updating eclipe.gradle");
			file = ws.getRoot().getProject("trunk").getFile(new Path("/buildtools/gradle-scripts/main-build/eclipse.gradle"));
			is = file.getContents();
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
	        String line = in.readLine();
	        while (line != null) {
	            sb.append(line);
	            sb.append('\n');
	            line = in.readLine();
	        }
	        originalEclipseGradleStr = sb.toString();
	        ra = file.getResourceAttributes();
	        fileIsReadOnly = ra.isReadOnly();
	        if(!originalEclipseGradleStr.contains("def slashedProjectRoot")) {
	        	ra.setReadOnly(false);
	        	file.setResourceAttributes(ra);
		        String strToReplace = 
		        	"                    if (subproject.name == 'integration.test') {" + System.getProperty("line.separator") + 
	                "                        classpathNode.classpathentry.findAll { it.@kind == 'src' }.each {" + System.getProperty("line.separator") + 
	                "                            classpathNode.remove it" + System.getProperty("line.separator") +
	                "                        }" + System.getProperty("line.separator") +
	                "                    }";
		        newEclipseGradleStr = originalEclipseGradleStr.replace(strToReplace, strToReplace + System.getProperty("line.separator") +
		        		"def slashedProjectRoot = \"$projectRoot\".replace('\\\\', '/')" + System.getProperty("line.separator") + 
		        		"classpathNode.classpathentry.findAll {" + System.getProperty("line.separator") + 
		        		"it.@path = it.@path.replaceAll(slashedProjectRoot, '/trunk')" + System.getProperty("line.separator") + 
		        		"it.@path = it.@path.replaceAll('/trunk/' + subproject.name + '/build/resources/test-runtime', '/' + subproject.name + '/build/resources/test-runtime')" + System.getProperty("line.separator") + 
		        		"it.@path = it.@path.replaceAll('/trunk/stub/src', '/stub/src')" + System.getProperty("line.separator") + 
		        		"}"
		        		);
		        file.setContents(new ByteArrayInputStream(newEclipseGradleStr.getBytes()), IFile.KEEP_HISTORY, monitor);
	        }
		} catch (CoreException e2) {
			openMsgBox("replacing classpath error: " + e2.getMessage());
			e2.printStackTrace();
		} catch (IOException e) {
			openMsgBox("replacing classpath error: " + e.getMessage());
			e.printStackTrace();
		}

		try {
			openMsgBox("start gradle refresh");
			RefreshAllActionCore.callOn(new ArrayList<IProject>() {
				{
					add(ws.getRoot().getProject("platform"));
					add(ws.getRoot().getProject("dataaccess"));
					add(ws.getRoot().getProject("domain"));
					add(ws.getRoot().getProject("webdomain-api"));
					add(ws.getRoot().getProject("webdomain"));
					add(ws.getRoot().getProject("shared.ui"));
					add(ws.getRoot().getProject("checkout.ui"));
					//add(ws.getRoot().getProject("api"));
					//(ws.getRoot().getProject("stub"));
					//add(ws.getRoot().getProject("integration.test"));
				}
			}).join();
		} catch (CoreException e1) {
			openMsgBox("gradle refresh error " + e1.getMessage());
			e1.printStackTrace();
		}
		catch (Exception e) {
			openMsgBox("gradle refresh error " + e.getMessage());
			e.printStackTrace();
		}

		if(newEclipseGradleStr!=null) {
			openMsgBox("start restoring eclipse.gradle");
			try {
				file = ws.getRoot().getProject("trunk").getFile(new Path("/buildtools/gradle-scripts/main-build/eclipse.gradle"));
				file.setContents(new ByteArrayInputStream(originalEclipseGradleStr.getBytes()), IFile.KEEP_HISTORY, monitor);
				ra.setReadOnly(fileIsReadOnly);
				file.setResourceAttributes(ra);
			} catch (CoreException e) {
				openMsgBox("updating eclipse.gradle error " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		try {
			openMsgBox("starting platform clean build");
			cleanAndBuild(ws, "platform", false, monitor);
			openMsgBox("starting dataaccess clean build");
			cleanAndBuild(ws, "dataaccess", false, monitor);
			openMsgBox("starting domain clean build");
			cleanAndBuild(ws, "domain", true, monitor);
			openMsgBox("starting webdomain api clean build");
			cleanAndBuild(ws, "webdomain-api", false, monitor);
			openMsgBox("starting webdomain clean build");
			cleanAndBuild(ws, "webdomain", false, monitor);
			openMsgBox("starting shared.ui clean build");
			cleanAndBuild(ws, "shared.ui", true, monitor);
			openMsgBox("starting checkout.ui clean build");
			cleanAndBuild(ws, "checkout.ui", true, monitor);
			//openMsgBox("starting api clean build");
			//(ws, "api", false, monitor);
			//openMsgBox("starting stub clean build");
			//cleanAndBuild(ws, "stub", false, monitor);
		} catch (BackingStoreException e) {
			openMsgBox("clean build error " + e.getMessage());
			e.printStackTrace();
		} catch (CoreException e) {
			openMsgBox("clean build error " + e.getMessage());
			e.printStackTrace();
		}
		desc.setAutoBuilding(autoBuildConf);
		try {
			openMsgBox("restoring auto build conf");
			ws.setDescription(desc);
		} catch (CoreException ex) {
			openMsgBox("updating eclipse.gradle error " + ex.getMessage());
			ex.printStackTrace();
		}

		openMsgBox("expweb build complete");
	}

	private void cleanAndBuild(IWorkspace ws, String projectName,
			boolean useScalaBuilder, IProgressMonitor monitor)
			throws BackingStoreException, CoreException {
		//openMsgBox("building " + projectName);
		IProject pro = ws.getRoot().getProject(projectName);
		IScopeContext projectScope = new ProjectScope(pro);
		Preferences projectNode = projectScope
				.getNode("org.scala-ide.sdt.core");
		if (projectNode != null) {
			projectNode.put("compileorder", "JavaThenScala");
			projectNode.put("scala.compiler.useProjectSettings", "true");
		}
		projectNode.flush();

		//openMsgBox("utf-8 " + projectName);
		pro.setDefaultCharset("UTF-8", monitor);
		if (useScalaBuilder) {
			ICommand[] builders = pro.getDescription().getBuildSpec();
			List<ICommand> newBuilders = new ArrayList<ICommand>();
			for (ICommand command : builders) {
				if (!command.getBuilderName().equals(
						"org.eclipse.jdt.core.javabuilder")) {
					newBuilders.add(command);
				}
			}
			if(newBuilders.size() == 0) {
				try {
					updateEclipseProjectFileToAddScalaBuilderAndNature(ws, projectName, monitor);
					ws.getRoot().getProject(projectName).refreshLocal(IResource.DEPTH_INFINITE, monitor);
				} catch (Exception e) {
					openMsgBox("updating .project error " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				IProjectDescription desc = pro.getDescription();
				desc.setBuildSpec(newBuilders.toArray(new ICommand[0]));
				pro.setDescription(desc, monitor);
			}
			IProjectDescription desc = pro.getDescription();
			desc.setBuildSpec(newBuilders.toArray(new ICommand[0]));
			pro.setDescription(desc, monitor);
		}
		//openMsgBox("clean " + projectName);
		pro.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		//openMsgBox("build " + projectName);
		pro.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
	}

	private void updateEclipseProjectFileToAddScalaBuilderAndNature(IWorkspace ws, String projectName, IProgressMonitor monitor) throws CoreException {
		IProject project = ws.getRoot().getProject(projectName);
		IProjectDescription desc = project.getDescription();
		String[] natureIds = desc.getNatureIds();
		String[] newNatureIds = new String[natureIds.length + 1];
		System.arraycopy(natureIds, 0, newNatureIds, 0, natureIds.length);
		newNatureIds[newNatureIds.length - 1] = "org.scala-ide.sdt.core.scalanature";
		desc.setNatureIds(newNatureIds);
		ICommand command = desc.newCommand();
		command.setBuilderName("org.scala-ide.sdt.core.scalabuilder");
		desc.setBuildSpec(new ICommand[]{command});
		project.setDescription(desc, monitor);
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

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		return Status.OK_STATUS;
	}
}