package com.expedia.expweb.eclipse

import org.eclipse.ui.IWorkbenchWindowActionDelegate
import org.eclipse.jface.action.IAction
import org.eclipse.ui.IWorkbenchWindow
import org.eclipse.jface.viewers.ISelection
import org.eclipse.core.resources.IWorkspace
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.resources.ProjectScope
import org.osgi.service.prefs.Preferences
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.resources.IWorkspaceDescription
import org.eclipse.core.runtime.CoreException
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore
import java.util.Arrays
import java.util.ArrayList
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import org.osgi.service.prefs.BackingStoreException
import org.eclipse.ui.preferences.ScopedPreferenceStore
import org.eclipse.core.runtime.preferences.InstanceScope

class ExpBuilder extends IWorkbenchWindowActionDelegate {
  var window: Option[IWorkbenchWindow] = None

  @Override
  def run(action: IAction) {
    val ws: IWorkspace = ResourcesPlugin.getWorkspace()
    val desc: IWorkspaceDescription = ws.getDescription()
    val autoBuildConf = desc.isAutoBuilding()
    desc.setAutoBuilding(false)
    try {
      ws.setDescription(desc)
    } catch {
      case ex: CoreException => {
        ex.printStackTrace()
      }
    }

    // https://github.com/spring-projects/eclipse-integration-gradle/blob/master/org.springsource.ide.eclipse.gradle.ui/src/org/springsource/ide/eclipse/gradle/ui/actions/RefreshAllAction.java
    try {
      RefreshAllActionCore.callOn(ListBuffer(ws.getRoot().getProjects().toList: _*));
    } catch {
      case ex: CoreException => {
        ex.printStackTrace()
      }
    }

    try {
      cleanAndBuild(ws, "platform", false)
      cleanAndBuild(ws, "dataaccess", false)
      cleanAndBuild(ws, "domain", true)
      cleanAndBuild(ws, "webdomain-api", false)
      cleanAndBuild(ws, "webdomain", false)
      cleanAndBuild(ws, "checkout.ui", true)
      cleanAndBuild(ws, "stub", false)
    } catch {
      case ex: BackingStoreException => {
        ex.printStackTrace()
      }
      case ex1: CoreException => {
        ex1.printStackTrace()
      }
    }

    desc.setAutoBuilding(autoBuildConf);
    try {
      ws.setDescription(desc)
    } catch {
      case ex: CoreException => {
        ex.printStackTrace()
      }
    }

    new ScopedPreferenceStore(InstanceScope.INSTANCE,
      "bundle-name-of-other-plugin")
  }

  def cleanAndBuild(ws: IWorkspace, projectName: String, useScalaBuilder: Boolean) {
    val pro: IProject = ws.getRoot().getProject(projectName);
    val projectScope: IScopeContext = new ProjectScope(pro);
    val projectNode: Preferences = projectScope.getNode("org.scala-ide.sdt.core");
    if (projectNode != null) {
      projectNode.put("compileorder", "JavaThenScala");
      projectNode.put("scala.compiler.useProjectSettings", "true");
      //value = projectNode.getBoolean("MyPreference", "true");
      //do something with the value.
    }
    projectNode.flush();

    pro.setDefaultCharset("UTF-8", null);
    if(useScalaBuilder) {
    	val builders = pro.getDescription().getBuildSpec()
    	val newBuilders = builders.filterNot(_.getBuilderName() == "org.eclipse.jdt.core.javabuilder")
    	val desc = pro.getDescription()
    	desc.setBuildSpec(newBuilders)
    	pro.setDescription(desc, null)
    }
    val builderssetornot = pro.getDescription().getBuildSpec()
    pro.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
    pro.build(IncrementalProjectBuilder.FULL_BUILD, null);
  }

  @Override
  def selectionChanged(action: IAction, selection: ISelection) {
  }

  @Override
  def dispose() {
  }

  @Override
  def init(window: IWorkbenchWindow) {
    this.window = Option(window);
  }
}