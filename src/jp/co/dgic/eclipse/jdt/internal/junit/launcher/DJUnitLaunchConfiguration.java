/**
 * Copyright (C)2004 dGIC Corporation.
 * 
 * This file is part of djUnit plugin.
 * 
 * djUnit plugin is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * djUnit plugin is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * djUnit plugin; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 */
package jp.co.dgic.eclipse.jdt.internal.junit.launcher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jp.co.dgic.eclipse.jdt.internal.coverage.util.CoveragePluginUtil;
import jp.co.dgic.eclipse.jdt.internal.coverage.util.CoverageUtil;
import jp.co.dgic.eclipse.jdt.internal.junit.ui.DJUnitMessages;
import jp.co.dgic.eclipse.jdt.internal.junit.ui.DJUnitPlugin;
import jp.co.dgic.eclipse.jdt.internal.junit.ui.DJUnitProjectPropertyPage;
import jp.co.dgic.testing.common.util.DJUnitUtil;
import jp.co.dgic.testing.common.virtualmock.VirtualMockUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;

// TODO This class is different from Eclipse2.1.x version.

public class DJUnitLaunchConfiguration extends JUnitLaunchConfigurationDelegate {

	public static final String ID_JUNIT_APPLICATION = "jp.co.dgic.eclipse.jdt.djunit.launchconfig"; //$NON-NLS-1$

	public static final String ID_DJUNIT_CLIENT_PORT = "jp.co.dgic.eclipse.jdt.djunit.client.port";

	private static final String LIB_DIR = "lib/";
	private int clientPort = -1;
	private static final String DJUNIT_CLASS_LOADER = "jp.co.dgic.testing.common.DJUnitEclipseClassLoader";
	private static final String TEST_RUNNER_CLASS_NAME = "jp.co.dgic.eclipse.jdt.internal.junit.runner.DJUnitRunner";

	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor pm) throws CoreException {

		clientPort = SocketUtil.findFreePort();
		launch.setAttribute(ID_DJUNIT_CLIENT_PORT, String.valueOf(clientPort));

		super.launch(configuration, mode, launch, pm);
	}

	public String verifyMainTypeName(ILaunchConfiguration configuration)
			throws CoreException {
		return TEST_RUNNER_CLASS_NAME;
	}

	protected void collectExecutionArguments(
			ILaunchConfiguration configuration, List vmArguments,
			List programArguments) throws CoreException {

		super.collectExecutionArguments(configuration, vmArguments, programArguments);

		programArguments.add("-djunitport");
		programArguments.add(Integer.toString(clientPort));
	}

//	protected VMRunnerConfiguration createVMRunner(
//			ILaunchConfiguration configuration, TestSearchResult testTypes,
//			int port, String runMode) throws CoreException {
//		String[] classPath = createClassPath(configuration, testTypes
//				.getTestKind());
//		VMRunnerConfiguration vmConfig = new VMRunnerConfiguration(
//				TEST_RUNNER_CLASS_NAME, classPath); //$NON-NLS-1$
//		Vector argv = getVMArgs(configuration, testTypes, port, runMode);
//		String[] args = new String[argv.size()];
//		argv.copyInto(args);
//		vmConfig.setProgramArguments(args);
//		return vmConfig;
//	}
//
//	public Vector getVMArgs(ILaunchConfiguration configuration,
//			TestSearchResult result, int port, String runMode)
//			throws CoreException {
//		Vector argv = super.getVMArgs(configuration, result, port, runMode);
//		argv.add("-djunitport");
//		argv.add(Integer.toString(clientPort));
//		return argv;
//	}

	public String[] getClasspath(ILaunchConfiguration configuration)
			throws CoreException {
		String[] cp = super.getClasspath(configuration);
		return addDJUnitClassPath(configuration, cp);
	}

//	public String[] createClassPath(ILaunchConfiguration configuration,
//			ITestKind kind) throws CoreException {
//		String[] cp = super.createClassPath(configuration, kind);
//		return addDJUnitClassPath(cp);
//	}

	private String[] addDJUnitClassPath(ILaunchConfiguration configuration, String[] classPath)
			throws CoreException {
		URL url = Platform.getBundle(DJUnitPlugin.PLUGIN_ID).getEntry("/");
		String[] newClassPath = null;
		try {

			List junitEntries = new ArrayList();

			if (Platform.inDevelopmentMode()) {
				// we first try the bin output folder
				try {
					junitEntries.add(FileLocator
							.toFileURL(new URL(url, "bin")).getFile()); //$NON-NLS-1$
				} catch (IOException e1) {
					try {
						junitEntries.add(FileLocator.toFileURL(
								new URL(url, "djunit.jar")).getFile()); //$NON-NLS-1$
					} catch (IOException e2) {
						// fall through
					}
				}
			} else {
				junitEntries.add(FileLocator.toFileURL(
						new URL(url, "djunit.jar")).getFile());
			}
			addRequiredJars(configuration, junitEntries, url);

			newClassPath = new String[classPath.length + junitEntries.size()];
			Object[] jea = junitEntries.toArray();
			System.arraycopy(classPath, 0, newClassPath, 0, classPath.length);
			System.arraycopy(jea, 0, newClassPath, classPath.length, jea.length);

		} catch (IOException e) {
			JUnitPlugin.log(e); // TODO abort run and inform user
		}
		return newClassPath;
	}

	private void addRequiredJars(ILaunchConfiguration configuration, List junitEntries, URL djunitURL)
			throws IOException, CoreException {

		IJavaProject javaProject = getJavaProject(configuration);
		if ((javaProject == null) || !javaProject.exists()) {
			abort(
					"Invalid project specified."/*JUnitMessages.JUnitLaunchConfigurationDelegate_error_invalidproject*/,
                    null,
					IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT); //$NON-NLS-1$
		}

		junitEntries.add(FileLocator.toFileURL(
				new URL(djunitURL, LIB_DIR + "jcoverage-djunit-1.0.5.jar"))
				.getFile());
		// remove (version 0.8.4)
//		junitEntries.add(FileLocator.toFileURL(
//				new URL(djunitURL, LIB_DIR + "bcel-r643711.jar")).getFile());
		
		// version 0.8.4
		String asmType = DJUnitProjectPropertyPage.readBytecodeLibrary(javaProject.getProject());
		URL asmLibURL = null;
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM3.equals(asmType)) {
            asmLibURL = new URL(djunitURL, LIB_DIR + "asm-3.1.jar");
        } else if (DJUnitUtil.BYTECODE_LIBRARY_ASM2.equals(asmType)) {
			asmLibURL = new URL(djunitURL, LIB_DIR + "asm-2.2.1.jar");
		} else if (DJUnitUtil.BYTECODE_LIBRARY_ASM15.equals(asmType)) {
			asmLibURL = new URL(djunitURL, LIB_DIR + "asm-1.5.3.jar");
		} else {
			asmLibURL = new URL(djunitURL, LIB_DIR + "asm-9.1.jar");
		}
		junitEntries.add(FileLocator.toFileURL(asmLibURL).getFile());
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM15.equals(asmType)) {
			junitEntries.add(FileLocator.toFileURL(
					new URL(djunitURL, LIB_DIR + "asm-attrs-1.5.3.jar"))
					.getFile());
		}

		junitEntries.add(FileLocator.toFileURL(
				new URL(djunitURL, LIB_DIR + "jakarta-oro-2.0.7.jar"))
				.getFile());
	}

	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {

		IJavaProject javaProject = getJavaProject(configuration);
		if ((javaProject == null) || !javaProject.exists()) {
			abort(
					"Invalid project specified."/*JUnitMessages.JUnitLaunchConfigurationDelegate_error_invalidproject*/,
                    null,
					IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT); //$NON-NLS-1$
		}
		// IJavaLaunchConfigurationConstants.ERR_WORKING_DIRECTORY_DOES_NOT_EXIST
		// DJUnit.message.error.workdirectory.notexist
		if (!existsCoverageWorkingDirectory(javaProject)) {
			String workingDirectoryName = DJUnitProjectPropertyPage
					.readWorkingDirectory(javaProject.getProject());
			abort(
					DJUnitMessages
							.getFormattedString(
									"DJUnit.message.error.workdirectory.notexist", workingDirectoryName), null, IJavaLaunchConfigurationConstants.ERR_WORKING_DIRECTORY_DOES_NOT_EXIST); //$NON-NLS-1$
		}

		StringBuffer sb = new StringBuffer();
		sb.append(getCoverageWorkingDirectory(javaProject));
		sb.append(" ");
		sb.append(getSourceDirectories(javaProject));
		sb.append(" ");
		// sb.append(getJUnitExcludedPaths(javaProject));
		// sb.append(" ");
		sb.append(getCoverageIncludedPatterns(javaProject));
		sb.append(" ");
		sb.append(getCoverageExcludedPatterns(javaProject));
		sb.append(" ");
		sb.append(getUseCoverage(javaProject));
		sb.append(" ");
		sb.append(getUseVirtualMock(javaProject));
		sb.append(" ");
		sb.append(getIgnoreLibrary(javaProject));
		sb.append(" ");
		sb.append(getNotIgnorePatterns(javaProject));
		sb.append(" ");
		sb.append(getUseNoverify(javaProject));
		sb.append(" ");
		sb.append(getBytecodeLibrary(javaProject));
		sb.append(" ");
		sb.append(createClassLoaderOption());
		sb.append(" ");
		sb.append(super.getVMArguments(configuration));

		return sb.toString();
	}

	private String getCoverageWorkingDirectory(IJavaProject javaProject) {
		String workingDirectoryName = DJUnitProjectPropertyPage
				.readWorkingDirectory(javaProject.getProject());

		String value = javaProject.getProject().getLocation()
				+ workingDirectoryName;
		return createSystemPropertyOption(
				CoveragePluginUtil.COVERAGE_WORK_DIR_KEY, value);
	}

	private String getSourceDirectories(IJavaProject javaProject)
			throws CoreException {

		Set dirs = new HashSet();
		Set checkedProjects = new HashSet(); // for checking cycle reference
		findSourceDirs(javaProject, dirs, checkedProjects);

		StringBuffer sb = new StringBuffer();

		for (Iterator it = dirs.iterator(); it.hasNext();) {
			sb.append(it.next());
			sb.append(";");
		}

		return createSystemPropertyOption(DJUnitUtil.PROJECTS_SOURCE_DIR_KEY,
				sb.toString());
	}

	private void findSourceDirs(IJavaProject javaProject, Set sourceDirs,
			Set checkedProjects) throws CoreException {

		// check cycle reference
		if (checkedProjects.contains(javaProject.getProject().getName()))
			return;
		checkedProjects.add(javaProject.getProject().getName());

		findSourceDirFrom(javaProject, sourceDirs);
		try {
			String[] names = javaProject.getRequiredProjectNames();
			for (int i = 0; i < names.length; i++) {
				IJavaProject requiredProject = javaProject.getJavaModel()
						.getJavaProject(names[i]);
				findSourceDirs(requiredProject, sourceDirs, checkedProjects);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void findSourceDirFrom(IJavaProject javaProject, Set sourceDirs)
			throws CoreException {
		IPackageFragmentRoot[] roots;
		try {
			roots = javaProject.getAllPackageFragmentRoots();
			for (int idx = 0; idx < roots.length; idx++) {
				int kind = roots[idx].getKind();
				if (kind != IPackageFragmentRoot.K_SOURCE)
					continue;
				sourceDirs.add(roots[idx].getResource().getLocation());
			}
		} catch (JavaModelException e) {
			throw new CoreException(e.getStatus());
		}
	}

	private String getCoverageExcludedPatterns(IJavaProject javaProject) {
		String excludedPatterns = DJUnitProjectPropertyPage
				.readCoverageExcludedPatterns(javaProject.getProject());
		if (excludedPatterns == null || "".equals(excludedPatterns)) {
			return "";
		}
		return createSystemPropertyOption(
				CoverageUtil.COVERAGE_EXCLUDES_PATTERNS_KEY, excludedPatterns);
	}

	private String getCoverageIncludedPatterns(IJavaProject javaProject) {
		String includedPatterns = DJUnitProjectPropertyPage
				.readCoverageIncludedPatterns(javaProject.getProject());
		if (includedPatterns == null || "".equals(includedPatterns)) {
			return "";
		}
		return createSystemPropertyOption(
				CoverageUtil.COVERAGE_INCLUDES_PATTERNS_KEY, includedPatterns);
	}

	private String getUseCoverage(IJavaProject javaProject) {
		boolean isUseCoverage = DJUnitProjectPropertyPage
				.readUseCoverage(javaProject.getProject());

		return createSystemPropertyOption(
				CoverageUtil.COVERAGE_USE_COVERAGE_KEY, isUseCoverage);
	}

	private String getUseVirtualMock(IJavaProject javaProject) {
		boolean isUseVirtualMock = DJUnitProjectPropertyPage
				.readUseVirtualMock(javaProject.getProject());

		return createSystemPropertyOption(
				VirtualMockUtil.VIRTUALMOCK_USE_VIRTUALMOCK_KEY,
				isUseVirtualMock);
	}

	private String getIgnoreLibrary(IJavaProject javaProject) {
		boolean isIgnoreLibrary = DJUnitProjectPropertyPage
				.readIgnoreLibrary(javaProject.getProject());

		return createSystemPropertyOption(
				VirtualMockUtil.VIRTUALMOCK_IGNORE_LIBRARY_KEY, isIgnoreLibrary);
	}

	private String getNotIgnorePatterns(IJavaProject javaProject) {
		String notIgnorePatterns = DJUnitProjectPropertyPage
				.readNotIgnorePatterns(javaProject.getProject());
		if (notIgnorePatterns == null || "".equals(notIgnorePatterns)) {
			return "";
		}
		return createSystemPropertyOption(
				VirtualMockUtil.VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY,
				notIgnorePatterns);
	}

	private String getUseNoverify(IJavaProject javaProject) {
		boolean isUseNoverify = DJUnitProjectPropertyPage
				.readUseNoverify(javaProject.getProject());

		return isUseNoverify ? "-noverify" : "";
	}

	private String getBytecodeLibrary(IJavaProject javaProject) {
		String library = DJUnitProjectPropertyPage
				.readBytecodeLibrary(javaProject.getProject());

		return createSystemPropertyOption(DJUnitUtil.BYTECODE_LIBRARY_KEY,
				library);
	}

	private String createClassLoaderOption() {
		return createSystemPropertyOption("java.system.class.loader",
				DJUNIT_CLASS_LOADER);
	}

	private String createSystemPropertyOption(String key, String value) {
		return "-D" + key + "=\"" + value + "\"";
	}

	private String createSystemPropertyOption(String key, boolean value) {
		return "-D" + key + "=" + value;
	}

	private boolean existsCoverageWorkingDirectory(IJavaProject javaProject) {
		String workingDirectory = DJUnitProjectPropertyPage
				.readWorkingDirectory(javaProject.getProject());
		if (workingDirectory == null || "".equals(workingDirectory)) {
			return true;
		}

		String workDirectoryPath = javaProject.getProject().getLocation()
				.append(workingDirectory).toString();
		File dir = new File(workDirectoryPath);
		if (dir.exists() && dir.isDirectory()) {
			return true;
		}
		return false;
	}

}