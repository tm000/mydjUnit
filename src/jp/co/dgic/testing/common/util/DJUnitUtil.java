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
package jp.co.dgic.testing.common.util;

import java.io.File;
import java.util.StringTokenizer;

import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

public class DJUnitUtil {

	public static final String PROJECTS_SOURCE_DIR_KEY = "jp.co.dgic.eclipse.project.source.dir";

	public static final String JUNIT_EXCLUDES_PATHS_KEY = "jp.co.dgic.eclipse.junit.excluded.paths";

	public static final String TRACE_INCLUDE_PATTERNS_KEY = "jp.co.dgic.eclipse.trace.include.patterns";

	public static final String BYTECODE_LIBRARY_KEY = "jp.co.dgic.eclipse.classloader.bytecodelibrary";

	public static final String BYTECODE_LIBRARY_ASM = "ASM";
	public static final String BYTECODE_LIBRARY_ASM3 = "ASM3";
	public static final String BYTECODE_LIBRARY_ASM2 = "ASM2";
	public static final String BYTECODE_LIBRARY_ASM15 = "ASM15";
	
	private static String[] sourceDirectries;
	private static String[] classExclusions;

	/** default excluded paths */
	private static String[] DEFAULT_EXCLUSIONS = {
			"junit.framework.", "org.objectweb.asm.",
			"junit.extensions.", "junit.runner.", "com.jcoverage.",
			"jp.co.dgic.testing.", "org.eclipse.", "org.apache.oro." };

	public static boolean isDJUnitSystemClass(String className) {
		if (className.startsWith("jp.co.dgic.testing.common")
				|| className.startsWith("jp.co.dgic.testing.framework"))
			return true;

		return false;
	}

	private static String[] getSourceDirectories() {
		if (sourceDirectries != null) {
			return sourceDirectries;
		}
		
		String directriesValue = System.getProperty(PROJECTS_SOURCE_DIR_KEY);
		if (directriesValue != null) {
			sourceDirectries = DJUnitUtil.splitValue(directriesValue);
		}
		return sourceDirectries;
	}

	public static boolean isProjectsSource(String className) {
		String[] dirs = DJUnitUtil.getSourceDirectories();
		
		if (dirs == null)
			return false;

		String pathString = null;
		File f = null;
		for (int idx = 0; idx < dirs.length; idx++) {
			pathString = dirs[idx] + "/" + toClassName(className) + ".java";
			f = new File(pathString);
			if (f.exists()) {
				return true;
			}
		}
		return false;
	}

	private static String toClassName(String className) {
		String name = className.replace('.', '/');
		if (name.indexOf('$') < 0)
			return name;
		String simpleName = getSimpleName(name);
		if (simpleName.indexOf('$') < 0)
			return name;
		int lastIndex = name.indexOf('$');
		return name.substring(0, lastIndex);
	}

	private static String getSimpleName(String className) {
		int lastIndex = className.lastIndexOf('.');
		return className.substring(lastIndex + 1);
	}

	public static String[] splitValue(String value) {
		if (value == null) {
			return null;
		}

		StringTokenizer st = new StringTokenizer(value, ";");
		String[] values = new String[st.countTokens()];
		for (int index = 0; index < values.length; index++) {
			values[index] = st.nextToken();
		}
		return values;
	}

	private static String[] getDefaultExclusions() {
		return DEFAULT_EXCLUSIONS;
	}

	private static String[] getExclusions() {
		if (classExclusions != null) {
			return classExclusions;
		}
		String paths = System.getProperty(JUNIT_EXCLUDES_PATHS_KEY);
		if (paths != null) {
			classExclusions = splitValue(paths);
		}
		return classExclusions;
	}
	
	public static boolean isDefaultExcludedPath(String className) {
		String[] defaultExcluded = getDefaultExclusions();
		for (int idx = 0; idx < defaultExcluded.length; idx++) {
			if (className.startsWith(defaultExcluded[idx]))
				return true;
		}
		return false;
	}

	public static boolean isExcluded(String className) {
		if (isDefaultExcludedPath(className)) return true;
		return isExcludedForClassloader(className);
	}

	public static boolean isExcludedForClassloader(String className) {
		String[] excluded = getExclusions();
		if (excluded == null) {
			return false;
		}
		for (int idx = 0; idx < excluded.length; idx++) {
			if (className.startsWith(removeAsterisk(excluded[idx])))
				return true;
		}
		return false;
	}

	public static boolean isInclude(String className) {
		String[] patterns = getTraceIncludPatterns();
		if (patterns == null) {
			return false;
		}

		Perl5Matcher matcher = new Perl5Matcher();
		PatternCompiler compiler = new Perl5Compiler();
		for (int index = 0; index < patterns.length; index++) {
			try {
				if (matcher.matches(className, compiler.compile(patterns[index]))) {
					return true;
				}
			} catch (Exception e) {
				// continue
			}

		}
		return false;
	}

	private static String[] getTraceIncludPatterns() {
		String excludedPatterns = System
				.getProperty(TRACE_INCLUDE_PATTERNS_KEY);

		if (excludedPatterns == null)
			return null;

		return DJUnitUtil.splitValue(excludedPatterns);
	}

	public static String getJavaProjectType(String key) {
		if (key == null)
			return key;

		int index = key.lastIndexOf('$');
		if (index < 0)
			return key;

		String className = key.substring(index + 1);
		if (!isDigit(className))
			return key.replace('$', '.');

		String type = key.substring(0, index);

		return getJavaProjectType(type);
	}

	private static boolean isDigit(String string) {
		if (string == null)
			return false;

		char[] chars = string.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (!Character.isDigit(chars[i]))
				return false;
		}

		return true;
	}

	private static String removeAsterisk(String pathString) {
		if (pathString == null) return null;
		if (!pathString.endsWith("*")) return pathString;
		return pathString.substring(0, pathString.length() - 1);
	}
	
}