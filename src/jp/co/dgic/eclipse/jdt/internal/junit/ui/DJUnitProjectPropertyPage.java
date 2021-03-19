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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import jp.co.dgic.testing.common.util.DJUnitUtil;

import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.FolderSelectionDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class DJUnitProjectPropertyPage extends PropertyPage {

	private static final String JUNIT_DEFAULT_EXCLUDED_PATHS = "";
//		"sun.*;"
//			+ "com.sun.*;"
//			+ "org.omg.*;"
//			+ "javax.*;"
//			+ "sunw.*;"
//			+ "java.*;"
//			+ "org.w3c.dom.*;"
//			+ "org.xml.sax.*;"
//			+ "net.jini.*;"
//			+ "org.apache.commons.logging.*";

	private static final String PROPERTIES_FILENAME = ".djunitplugin";
	private static final String USE_COVERAGE_KEY = "UseCoverage";
	private static final String USE_MARKER_KEY = "UseMarker";
	private static final String USE_VIRTUALMOCK_KEY = "UseVirtualMock";
	private static final String COVERAGE_WORK_DIRECTORY_KEY = "CoverageWorkingDirectory";
	private static final String COVERAGE_EXCLUDED_PATTERNS_KEY = "CoverageExcludedPatterns";
	private static final String COVERAGE_INCLUDED_PATTERNS_KEY = "CoverageIncludedPatterns";
	private static final String JUNIT_EXCLUDED_PATHS_KEY = "JUnitExcludedPaths";
	private static final String JUNIT_EXCLUDED_USE_DEFAULT_KEY = "JUnitExcludedUseDefault";
	private static final String VIRTUALMOCK_IGNORE_LIBRARY_KEY = "VirtualMockIgnoreLibrary";
	private static final String VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY = "VirtualMockNotIgnorePatterns";
	private static final String USE_NOVERIFY_KEY = "UseNoverify";
	private static final String BYTECODE_LIBRARY_KEY = "BytecodeLibrary";
	
	private static final String BYTECODE_LIBRARY_TEXT_ASM90 = "ASM9.x";
	private static final String BYTECODE_LIBRARY_TEXT_ASM30 = "ASM3.x";
	private static final String BYTECODE_LIBRARY_TEXT_ASM20 = "ASM2.x";
	private static final String BYTECODE_LIBRARY_TEXT_ASM15 = "ASM1.5.x";

	private static final String COVERAGE_REPORT_CHARSET_KEY = "CoverageReportCharset";

	private TabFolder folder;
	private Button useCoverageButton;
	private Button useMarkerButton;
	private Button useVirtualMockButton;
	private Button ignoreLibraryButton;
	private Button useNoverifyButton;
	private Button useAsm;
	private Button useAsm3;
	private Button useAsm2;
	private Button useAsm15;
//	private Button useBcel;		// remove(version 0.8.4)
	private Text workDirectoryName;
//	private ExcludePropertyPanel junitExcludedProperty;
	private ExcludePropertyPanel coverageExcludedProperty;
	private ExcludePropertyPanel coverageIncludedProperty;
	private ExcludePropertyPanel virtualmockNotIgnoreProperty;
	
	private Text reportCharset;	// version 0.8.5

	protected Control createContents(Composite parent) {

		// folder
		folder = new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		// coverage
		TabItem coverageTab = new TabItem(folder, SWT.NONE);
		coverageTab.setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.label.coveragetab"));
		coverageTab.setControl(createCoveragePanel(folder));

		// Virtual Mock Objects
		TabItem virtualMockTab = new TabItem(folder, SWT.NONE);
		virtualMockTab.setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.label.virtualmocktab"));
		virtualMockTab.setControl(createVirtualMockPanel(folder));

		// JUnit
		TabItem junitTab = new TabItem(folder, SWT.NONE);
		junitTab.setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.label.junittab"));
		junitTab.setControl(createJunitPanel(folder));

		return null;
	}

	private Composite createCoveragePanel(Composite parent) {

		Composite c = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.numColumns = 2;
		c.setLayout(gl);

		useCoverageButton = new Button(c, SWT.CHECK);
		useCoverageButton.setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.label.usecoverage"));
		useCoverageButton.setSelection(readUseCoverage());

		new Label(c, SWT.NONE); // blank

		useMarkerButton = new Button(c, SWT.CHECK);
		useMarkerButton.setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.label.usemarker"));
		useMarkerButton.setSelection(readUseMarker());

		new Label(c, SWT.NONE); // blank
		
		// version 0.8.5
		new Label(c, SWT.NONE).setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.label.reportencoding"));
		new Label(c, SWT.NONE); // blank
		reportCharset = new Text(c, SWT.SINGLE | SWT.BORDER);
		GridData egd = new GridData();
		egd.widthHint = 300;
		reportCharset.setLayoutData(egd);
		reportCharset.setText(readCoverageReportCharset());
		new Label(c, SWT.NONE); // blank

		new Label(c, SWT.NONE).setText(
			DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.label.workingdirectory"));

		new Label(c, SWT.NONE); // blank

		workDirectoryName = new Text(c, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		workDirectoryName.setLayoutData(gd);
		String workingDirName = readWorkingDirectory();
		workDirectoryName.setText(workingDirName == null ? "" : workingDirName);

		Button workDirectorySelectionButton = new Button(c, SWT.NONE);
		workDirectorySelectionButton.setText(
			DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.label.choicefolder"));
		workDirectorySelectionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openFolderSelectionDialog();
			}
		});

		// included patterns
		coverageIncludedProperty = new ExcludePropertyPanel(c, 300, 120);
		coverageIncludedProperty.setText(
			DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.label.includedpatterns"));
		coverageIncludedProperty.setValue(readCoverageIncludedPatterns());

		new Label(c, SWT.NONE); // blank

		// excluded patterns
		coverageExcludedProperty = new ExcludePropertyPanel(c, 300, 120);
		coverageExcludedProperty.setText(
			DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.label.excludedpatterns"));
		coverageExcludedProperty.setValue(readCoverageExcludedPatterns());

		return c;

	}

	private Composite createVirtualMockPanel(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.numColumns = 1;
		c.setLayout(gl);

		useVirtualMockButton = new Button(c, SWT.CHECK);
		useVirtualMockButton.setText(
			DJUnitMessages.getString("DJUnitProjectPropertyPage.virtualmock.label.usevirtualmock"));
		useVirtualMockButton.setSelection(readUseVirtualMock());

		new Label(c, SWT.NONE); // blank

		ignoreLibraryButton = new Button(c, SWT.CHECK);
		ignoreLibraryButton.setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.virtualmock.label.ignorelibrary"));
		ignoreLibraryButton.setSelection(readIgnoreLibrary());
		ignoreLibraryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected = ((Button) e.getSource()).getSelection();
				virtualmockNotIgnoreProperty.setEnabled(isSelected);
			}
		});

		// not ignore patterns
		virtualmockNotIgnoreProperty = new ExcludePropertyPanel(c, false);
		virtualmockNotIgnoreProperty.setText(
			DJUnitMessages.getString("DJUnitProjectPropertyPage.virtualmock.label.notignorepatterns"));
		virtualmockNotIgnoreProperty.setValue(readNotIgnorePatterns());
		virtualmockNotIgnoreProperty.setEnabled(ignoreLibraryButton.getSelection());

		return c;
	}

	private Composite createJunitPanel(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.numColumns = 1;
		c.setLayout(gl);

		Group libGroup = new Group(c, SWT.NONE);
		GridLayout groupLayout = new GridLayout();
		groupLayout.numColumns = 2;
		libGroup.setLayout(groupLayout);
		libGroup.setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.classloader.label.bytecodelibrary"));

		useAsm = new Button(libGroup, SWT.RADIO);
		useAsm.setText(BYTECODE_LIBRARY_TEXT_ASM90);
        useAsm3 = new Button(libGroup, SWT.RADIO);
        useAsm3.setText(BYTECODE_LIBRARY_TEXT_ASM30);
		useAsm2 = new Button(libGroup, SWT.RADIO);
		useAsm2.setText(BYTECODE_LIBRARY_TEXT_ASM20);
		useAsm15 = new Button(libGroup, SWT.RADIO);
		useAsm15.setText(BYTECODE_LIBRARY_TEXT_ASM15);

		// remove (version 0.8.4)
//		useBcel = new Button(libGroup, SWT.RADIO);
//		useBcel.setText(DJUnitUtil.BYTECODE_LIBRARY_BCEL);

		String library = readBytecodeLibrary();
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM3.equals(library)) {
            useAsm3.setSelection(true);
        } else if (DJUnitUtil.BYTECODE_LIBRARY_ASM2.equals(library)) {
			useAsm2.setSelection(true);
		} else if (DJUnitUtil.BYTECODE_LIBRARY_ASM15.equals(library)) {
			useAsm15.setSelection(true);
		} else {
			useAsm.setSelection(true);
		}

		useNoverifyButton = new Button(c, SWT.CHECK);
		useNoverifyButton.setText(DJUnitMessages.getString("DJUnitProjectPropertyPage.virtualmock.label.noverify"));
		useNoverifyButton.setSelection(readUseNoverify());

//		junitExcludedProperty = new ExcludePropertyPanel(c, true);
//		junitExcludedProperty.setText(
//			DJUnitMessages.getString("DJUnitProjectPropertyPage.virtualmock.label.excludedpaths"));
//
//		junitExcludedProperty.setUseDefault(readJUnitExcludedUseDefault());
//		junitExcludedProperty.setValue(readJUnitExcludedPaths());
//		junitExcludedProperty.setDefaultValues(JUNIT_DEFAULT_EXCLUDED_PATHS);

		return c;

	}

	protected void saveProjectSettings() {

		FileOutputStream out = null;
		try {
			Properties p = getProperties(getJavaProject().getProject());

			p.put(USE_COVERAGE_KEY, new Boolean(useCoverageButton.getSelection()).toString());
			p.put(USE_MARKER_KEY, new Boolean(useMarkerButton.getSelection()).toString());
			p.put(COVERAGE_WORK_DIRECTORY_KEY, workDirectoryName.getText());
			p.put(COVERAGE_EXCLUDED_PATTERNS_KEY, coverageExcludedProperty.getValue());
			p.put(COVERAGE_INCLUDED_PATTERNS_KEY, coverageIncludedProperty.getValue());

			p.put(USE_VIRTUALMOCK_KEY, new Boolean(useVirtualMockButton.getSelection()).toString());
			p.put(VIRTUALMOCK_IGNORE_LIBRARY_KEY, new Boolean(ignoreLibraryButton.getSelection()).toString());
			p.put(VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY, virtualmockNotIgnoreProperty.getValue());
			p.put(USE_NOVERIFY_KEY, new Boolean(useNoverifyButton.getSelection()).toString());

//			String library = useBcel.getSelection() ? DJUnitUtil.BYTECODE_LIBRARY_BCEL : DJUnitUtil.BYTECODE_LIBRARY_ASM;
			String library = DJUnitUtil.BYTECODE_LIBRARY_ASM;
			if (useAsm3.getSelection()) {
                library = DJUnitUtil.BYTECODE_LIBRARY_ASM3;
            } else if (useAsm2.getSelection()) {
				library = DJUnitUtil.BYTECODE_LIBRARY_ASM2;
			} else if (useAsm15.getSelection()) {
				library = DJUnitUtil.BYTECODE_LIBRARY_ASM15;
			}
			p.put(BYTECODE_LIBRARY_KEY, library);

//			boolean useDefault = junitExcludedProperty.getUseDefault();
//			p.put(JUNIT_EXCLUDED_USE_DEFAULT_KEY, new Boolean(useDefault).toString());
//			if (!useDefault) {
//				p.put(JUNIT_EXCLUDED_PATHS_KEY, junitExcludedProperty.getValue());
//			}

			p.put(COVERAGE_WORK_DIRECTORY_KEY, workDirectoryName.getText());
			
			p.put(COVERAGE_REPORT_CHARSET_KEY, reportCharset.getText());

			out = new FileOutputStream(getPropertyFile(getJavaProject().getProject()));
			p.store(out, "djUnit Plugin Settings");
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				DJUnitPlugin.log(e);
			}
		}
	}

	private String readWorkingDirectory() {
		String directoryName = null;
		try {
			directoryName = readWorkingDirectory(getJavaProject().getProject());
		} catch (Throwable t) {
		}
		return directoryName;
	}

	private boolean readUseCoverage() {
		boolean useCoverage = true;
		try {
			useCoverage = readUseCoverage(getJavaProject().getProject());
		} catch (Throwable t) {
		}
		return useCoverage;
	}

	private boolean readUseMarker() {
		boolean useMarker = true;
		try {
			useMarker = readUseMarker(getJavaProject().getProject());
		} catch (Throwable t) {
		}
		return useMarker;
	}

	private boolean readUseVirtualMock() {
		boolean useVmo = false;
		try {
			useVmo = readUseVirtualMock(getJavaProject().getProject());
		} catch (Throwable t) {
		}
		return useVmo;
	}

	private boolean readIgnoreLibrary() {
		boolean isIgnoreLibrary = false;
		try {
			isIgnoreLibrary = readIgnoreLibrary(getJavaProject().getProject());
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}
		return isIgnoreLibrary;
	}

	private boolean readUseNoverify() {
		boolean useNoverify = false;
		try {
			useNoverify = readUseNoverify(getJavaProject().getProject());
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}
		return useNoverify;
	}

	private String readBytecodeLibrary() {
		String library = DJUnitUtil.BYTECODE_LIBRARY_ASM;
		try {
			library = readBytecodeLibrary(getJavaProject().getProject());
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}
		return library;
	}

	private String readNotIgnorePatterns() {
		String patterns = null;
		try {
			patterns = getProperties(getJavaProject().getProject()).getProperty(VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY);
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}
		return patterns;
	}

	private String readCoverageExcludedPatterns() {
		String patterns = null;
		try {
			patterns = getProperties(getJavaProject().getProject()).getProperty(COVERAGE_EXCLUDED_PATTERNS_KEY);
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}
		return patterns;
	}

	private String readCoverageIncludedPatterns() {
		String patterns = null;
		try {
			patterns = getProperties(getJavaProject().getProject()).getProperty(COVERAGE_INCLUDED_PATTERNS_KEY);
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}
		return patterns;
	}
	
	private String readCoverageReportCharset() {
		String charset = "";
		try {
			charset = readCoverageReportCharset(getJavaProject().getProject());
		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}
		return charset;
	}

//	private String readJUnitExcludedPaths() {
//		String packages = null;
//		try {
//			if (readJUnitExcludedUseDefault(getJavaProject().getProject())) {
//				return getJUnitDefaultExcludedPaths();
//			}
//			packages = getProperties(getJavaProject().getProject()).getProperty(JUNIT_EXCLUDED_PATHS_KEY);
//		} catch (Throwable t) {
//			DJUnitPlugin.log(t);
//		}
//		return packages;
//	}

//	private boolean readJUnitExcludedUseDefault() {
//		boolean userDefault = true;
//		try {
//			userDefault = readJUnitExcludedUseDefault(getJavaProject().getProject());
//		} catch (Throwable t) {
//			DJUnitPlugin.log(t);
//		}
//		return userDefault;
//	}

	public static String readWorkingDirectory(IProject project) {
		String directoeyName = getProperties(project).getProperty(COVERAGE_WORK_DIRECTORY_KEY);
		return directoeyName == null ? "" : directoeyName;
	}

	public static boolean readUseCoverage(IProject project) {
		String useCoverage = getProperties(project).getProperty(USE_COVERAGE_KEY);
		if ("false".equalsIgnoreCase(useCoverage)) {
			return false;
		}
		return true;
	}

	public static boolean readUseMarker(IProject project) {
		String useMarker = getProperties(project).getProperty(USE_MARKER_KEY);
		if ("false".equalsIgnoreCase(useMarker)) {
			return false;
		}
		return true;
	}

	public static boolean readUseVirtualMock(IProject project) {
		String useVirtualMock = getProperties(project).getProperty(USE_VIRTUALMOCK_KEY);
		if ("true".equalsIgnoreCase(useVirtualMock)) {
			return true;
		}
		return false;
	}

	public static boolean readIgnoreLibrary(IProject project) {
		String ignoreLibrary = getProperties(project).getProperty(VIRTUALMOCK_IGNORE_LIBRARY_KEY);
		if ("true".equalsIgnoreCase(ignoreLibrary)) {
			return true;
		}
		return false;
	}

	public static String readNotIgnorePatterns(IProject project) {
		String patterns = getProperties(project).getProperty(VIRTUALMOCK_NOTIGNORE_PATTERNS_KEY);
		return patterns == null ? "" : removeInvalidValues(patterns);
	}

	public static boolean readUseNoverify(IProject project) {
		String iseNoverify = getProperties(project).getProperty(USE_NOVERIFY_KEY);
		if ("true".equalsIgnoreCase(iseNoverify)) {
			return true;
		}
		return false;
	}

	public static String readBytecodeLibrary(IProject project) {
		String bytecodeLibrary = getProperties(project).getProperty(BYTECODE_LIBRARY_KEY);
		if (bytecodeLibrary == null || "".equals(bytecodeLibrary)) {
			return DJUnitUtil.BYTECODE_LIBRARY_ASM;
		}
		return bytecodeLibrary;
	}

	public static String readCoverageExcludedPatterns(IProject project) {
		String patterns = getProperties(project).getProperty(COVERAGE_EXCLUDED_PATTERNS_KEY);
		return patterns == null ? "" : removeInvalidValues(patterns);
	}

	public static String readCoverageIncludedPatterns(IProject project) {
		String patterns = getProperties(project).getProperty(COVERAGE_INCLUDED_PATTERNS_KEY);
		return patterns == null ? "" : removeInvalidValues(patterns);
	}

	public static String readCoverageReportCharset(IProject project) {
		String charset = getProperties(project).getProperty(COVERAGE_REPORT_CHARSET_KEY);
		return charset == null ? "" : charset;
	}

//	public static String readJUnitExcludedPaths(IProject project) {
//		if (readJUnitExcludedUseDefault(project)) {
//			return getJUnitDefaultExcludedPaths();
//		}
//		String packages = getProperties(project).getProperty(JUNIT_EXCLUDED_PATHS_KEY);
//		return packages == null ? "" : removeInvalidValues(packages);
//	}
//
//	public static boolean readJUnitExcludedUseDefault(IProject project) {
//		String useDefault = getProperties(project).getProperty(JUNIT_EXCLUDED_USE_DEFAULT_KEY);
//		if ("false".equalsIgnoreCase(useDefault)) {
//			return false;
//		}
//		return true;
//	}
//
	private static String removeInvalidValues(String value) {

		String[] values = DJUnitUtil.splitValue(value);
		if (values == null) {
			return "";
		}

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			if (values[i].startsWith(ExcludePropertyPanel.INVALID_MARK)) {
				continue;
			}
			sb.append(values[i]);
			sb.append(ExcludePropertyPanel.PATH_DELIMITOR);
		}

		return sb.toString();
	}

	private String addProjectPathString(String directoryPathString) throws CoreException {
		return getJavaProject().getProject().getName() + directoryPathString;
	}

	private String removeProjectPathString(String directoryPathString) throws CoreException {
		String projectPath = getJavaProject().getProject().getLocation().toString();
		if (directoryPathString.length() <= projectPath.length()) {
			return directoryPathString;
		}
		return directoryPathString.substring(projectPath.length());
	}

	private static Properties getProperties(IProject project) {

		Properties p = new Properties();
		File f = null;
		FileInputStream in = null;
		try {
			f = getPropertyFile(project);
			if (!f.exists()) {
				f.createNewFile();
			}

			in = new FileInputStream(f);
			p.load(in);

		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				DJUnitPlugin.log(e);
			}
		}

		return p;
	}

	private static File getPropertyFile(IProject project) throws CoreException {
		return project.getLocation().append(PROPERTIES_FILENAME).toFile();
	}

	/* helper methods */
	protected IJavaProject getJavaProject() throws CoreException {
		IProject project = (IProject) (this.getElement().getAdapter(IProject.class));
		return (IJavaProject) (project.getNature(JavaCore.NATURE_ID));
	}

	protected void performApply() {
		saveProjectSettings();
	}

	protected void performDefaults() {

		// Coverage
		useCoverageButton.setSelection(true);
		useMarkerButton.setSelection(true);
		workDirectoryName.setText("");
		coverageIncludedProperty.clear();
		coverageExcludedProperty.clear();

		// Virtual Mock Objects
		useVirtualMockButton.setSelection(false);

//		// JUnit
//		junitExcludedProperty.setUseDefault(true);
		useNoverifyButton.setSelection(false);
		useAsm.setSelection(true);
		useAsm2.setSelection(false);
		useAsm15.setSelection(false);
//		useBcel.setSelection(false); // remove (version 0.8.4)
		
		reportCharset.setText("");	// version 0.8.5

	}

	protected static String getJUnitDefaultExcludedPaths() {
		return JUNIT_DEFAULT_EXCLUDED_PATHS;

	}

	public boolean performOk() {
		saveProjectSettings();
		return true;
	}

	private void openFolderSelectionDialog() {

		try {
			// root
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

			// init selection
			IResource initSelection = null;
			if (readWorkingDirectory(getJavaProject().getProject()) != null) {
				initSelection =
					workspaceRoot.findMember(addProjectPathString(readWorkingDirectory(getJavaProject().getProject())));
			}

			// filter
			Class[] acceptedClasses = new Class[] { IProject.class, IFolder.class };
			ISelectionStatusValidator validator = new TypedElementSelectionValidator(acceptedClasses, false);
			IProject[] allProjects = workspaceRoot.getProjects();
			ArrayList rejectedElements = new ArrayList(allProjects.length);
			IProject currProject = getJavaProject().getProject();
			for (int i = 0; i < allProjects.length; i++) {
				if (!allProjects[i].equals(currProject)) {
					rejectedElements.add(allProjects[i]);
				}
			}
			ViewerFilter filter = new TypedViewerFilter(acceptedClasses, rejectedElements.toArray());

			// providers
			ILabelProvider lp = new WorkbenchLabelProvider();
			ITreeContentProvider cp = new WorkbenchContentProvider();

			FolderSelectionDialog dialog = new FolderSelectionDialog(getShell(), lp, cp);

			dialog.setTitle(DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.label.workingdirectory"));
			dialog.setMessage(DJUnitMessages.getString("DJUnitProjectPropertyPage.coverage.message.choicefolder"));

			dialog.setInput(workspaceRoot);
			dialog.setValidator(validator);
			dialog.addFilter(filter);
			dialog.setInitialSelection(initSelection);
			dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

			if (dialog.open() == FolderSelectionDialog.OK) {
				workDirectoryName.setText(getFolderName(dialog.getFirstResult()));
			}

		} catch (Throwable t) {
			DJUnitPlugin.log(t);
		}

	}

	private String getFolderName(Object result) throws CoreException {
		if (result instanceof Folder) {
			Folder folder = (Folder) result;
			String folderName = removeProjectPathString(folder.getLocation().toString());
			return folderName;
		}
		return "";
	}

}
