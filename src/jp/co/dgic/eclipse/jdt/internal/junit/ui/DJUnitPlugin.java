/**
 * Copyright (C)2004 dGIC Corporation.
 *
 * This file is part of djUnit plugin.
 *
 * djUnit plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * djUnit plugin is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with djUnit plugin; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 *
 */
package jp.co.dgic.eclipse.jdt.internal.junit.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractSet;
import java.util.HashSet;

import jp.co.dgic.eclipse.jdt.internal.coverage.ui.CoverageReportView;
import jp.co.dgic.eclipse.jdt.internal.junit.launcher.DJUnitLaunchConfiguration;

import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The plug-in runtime class for the JUnit plug-in.
 */
public class DJUnitPlugin extends AbstractUIPlugin implements ILaunchListener {
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static DJUnitPlugin fgPlugin = null;

	public static final String PLUGIN_ID = "jp.co.dgic.eclipse.jdt.djunit"; //$NON-NLS-1$
	public static final String ID_EXTENSION_POINT_TESTRUN_LISTENERS = PLUGIN_ID + "." + "testRunListeners"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String ID_LAUNCH_CONFIG_TYPE = "jp.co.dgic.eclipse.jdt.djunit.launchconfig";

	public final static String TEST_SUPERCLASS_NAME = "junit.framework.TestCase"; //$NON-NLS-1$
	public final static String TEST_INTERFACE_NAME = "junit.framework.Test"; //$NON-NLS-1$

	private static URL fgIconBaseURL;

	private boolean isDJUnitTest = false;
	private DJUnitDebugEventFilter debugEventFilter = new DJUnitDebugEventFilter();

	/**
	 * Use to track new launches. We need to do this
	 * so that we only attach a TestRunner once to a launch.
	 * Once a test runner is connected it is removed from the set.
	 */
	private AbstractSet fTrackedLaunches = new HashSet(20);

	private CoverageReportView coverageView;

	private DJUnitTestRunnerClient djUnitClient;

	public DJUnitPlugin(/*IPluginDescriptor desc*/) {
		super(/*desc*/);
		fgPlugin = this;
		String pathSuffix = "icons/full/"; //$NON-NLS-1$
//		try {
//			fgIconBaseURL =
//				new URL(getDescriptor().getInstallURL(), pathSuffix);
//		} catch (MalformedURLException e) {
//			// do nothing
//		}
//		String pathSuffix= "icons/full/"; //$NON-NLS-1$
		try {
			fgIconBaseURL= new URL(Platform.getBundle(PLUGIN_ID).getEntry("/"), pathSuffix); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			// do nothing
		}

	}

	public static DJUnitPlugin getDefault() {
		return fgPlugin;
	}

	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow workBenchWindow = getActiveWorkbenchWindow();
		if (workBenchWindow == null)
			return null;
		return workBenchWindow.getShell();
	}

	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		if (fgPlugin == null)
			return null;
		IWorkbench workBench = fgPlugin.getWorkbench();
		if (workBench == null)
			return null;
		return workBench.getActiveWorkbenchWindow();
	}

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
			return null;
		return activeWorkbenchWindow.getActivePage();
	}

	public static String getPluginId() {
		return PLUGIN_ID;
	}

	/*
	 * @see AbstractUIPlugin#initializeDefaultPreferences
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		super.initializeDefaultPreferences(store);
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static URL makeIconFileURL(String name)
		throws MalformedURLException {
		if (DJUnitPlugin.fgIconBaseURL == null)
			throw new MalformedURLException();
		return new URL(DJUnitPlugin.fgIconBaseURL, name);
	}

	static ImageDescriptor getImageDescriptor(String relativePath) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(relativePath));
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	public boolean isDJUnitTest() {
		return isDJUnitTest;
	}

	private void setDJUnitTest(boolean isDJUnitTest) {
		this.isDJUnitTest = isDJUnitTest;
	}

	/*
	 * @see ILaunchListener#launchRemoved(ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {
		DebugPlugin.getDefault().removeDebugEventFilter(debugEventFilter);
		fTrackedLaunches.remove(launch);
	}

	/*
	 * @see ILaunchListener#launchAdded(ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
		fTrackedLaunches.add(launch);
	}

	public void connectDJUnitClient(IJavaProject javaProject, int port) {

		if (djUnitClient == null) {
			djUnitClient = new DJUnitTestRunnerClient();
		}

		if (djUnitClient.isRunning()) {
			djUnitClient.shutDown();
		}

		djUnitClient.startListening(port);

		CoverageReportView coverageView =
			showCoverageReportViewInActivePage(findCoverageReportView());
		if (coverageView != null) {
			this.coverageView = coverageView;
			coverageView.setProject(javaProject.getProject());
			coverageView.clearCoverage();
		}
	}

	public void shutDownClient() {
		if (djUnitClient == null) {
			return;
		}
		djUnitClient.shutDown();
	}

	private CoverageReportView showCoverageReportViewInActivePage(CoverageReportView coverageView) {
		IWorkbenchPart activePart = null;
		IWorkbenchPage page = null;
		try {
			// TODO: have to force the creation of view part contents
			// otherwise the UI will not be updated
			if (coverageView != null)
				return coverageView;
			page = getActivePage();
			if (page == null)
				return null;
			activePart = page.getActivePart();
			//	show the result view if it isn't shown yet
			return (CoverageReportView) page.showView(CoverageReportView.NAME);
		} catch (PartInitException pie) {
			log(pie);
			return null;
		} finally {
			//restore focus stolen by the creation of the result view
			if (page != null && activePart != null)
				page.activate(activePart);
		}
	}

	private CoverageReportView findCoverageReportView() {
		IWorkbenchPage page = getActivePage();
		if (page == null) {
			return null;
		}
		return (CoverageReportView) page.findView(CoverageReportView.NAME);
	}

	public static CoverageReportView getCoverageReportView() {
		return getDefault().coverageView;
	}

	private boolean isLaunchedByDJUnit(ILaunch launch) {
		try {
			String id = launch.getLaunchConfiguration().getType().getIdentifier();
			if (ID_LAUNCH_CONFIG_TYPE.equals(id)) {
				return true;
			}
		} catch (CoreException e) {
			DJUnitPlugin.log(e);
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}
		return false;
	}

	/*
	 * @see ILaunchListener#launchChanged(ILaunch)
	 */
	public void launchChanged(final ILaunch launch) {
		boolean isDJUnitLaunch = isLaunchedByDJUnit(launch);
		setDJUnitTest(isDJUnitLaunch);

		if (!isDJUnitLaunch) return;

		if ("debug".equalsIgnoreCase(launch.getLaunchMode())) {
			DebugPlugin.getDefault().addDebugEventFilter(debugEventFilter);
		}

		if (!fTrackedLaunches.contains(launch))
			return;

		ILaunchConfiguration config = launch.getLaunchConfiguration();
		if (config == null) return;
		
		final IJavaProject javaProject= JUnitLaunchConfigurationConstants.getJavaProject(config);
		if (javaProject == null) return;
		
		// test whether the launch defines the JUnit attributes
		String portStr = launch.getAttribute(DJUnitLaunchConfiguration.ID_DJUNIT_CLIENT_PORT);
		if (portStr == null) return;
		

		try {
			final int port = Integer.parseInt(portStr);
			fTrackedLaunches.remove(launch);
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					connectDJUnitClient(javaProject, port);
				}
			});
		} catch (NumberFormatException e) {
			return;
		}
	}

//	/*
//	 * @see Plugin#startup()
//	 */
//	public void startup() throws CoreException {
//		super.startup();
//		ILaunchManager launchManager =
//			DebugPlugin.getDefault().getLaunchManager();
//		launchManager.addLaunchListener(this);
//	}
//
//	/*
//	 * @see Plugin#shutdown()
//	 */
//	public void shutdown() throws CoreException {
//		super.shutdown();
//		ILaunchManager launchManager =
//			DebugPlugin.getDefault().getLaunchManager();
//		launchManager.removeLaunchListener(this);
//	}
//
	/**
	 * @see AbstractUIPlugin#start(BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(this);
	}

	/**
	 * @see AbstractUIPlugin#stop(BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
			launchManager.removeLaunchListener(this);
		} finally {
			super.stop(context);
		}
	}

	public static Display getDisplay() {
		Shell shell = getActiveWorkbenchShell();
		if (shell != null) {
			return shell.getDisplay();
		}
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}
}
