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
 * jcoverage; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 */
package jp.co.dgic.testing.common;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

import jp.co.dgic.testing.common.util.DJUnitUtil;

public class DJUnitClassLoader extends URLClassLoader {

	private static final String ASM_CLASS_MODIFIER_CLASS_NAME = "jp.co.dgic.testing.common.AsmClassModifier";
	private static final String ASM_15x_CLASS_MODIFIER_CLASS_NAME = "jp.co.dgic.testing.common.Asm15xClassModifier";
	
	private static final String CLASS_MODIFIER_MTHOD_NAME = "getModifiedClass";

	/** Class Modifier */
	protected Object classModifier;

	public DJUnitClassLoader(ClassLoader parent) {
//		super(((URLClassLoader) parent).getURLs(), parent.getParent());
        // java9以上ではjava.lang.ClassCastException: class jdk.internal.loader.ClassLoaders$AppClassLoader cannot be cast to class java.net.URLClassLoader (jdk.internal.loader.ClassLoaders$AppClassLoader and java.net.URLClassLoader are in module java.base of loader 'bootstrap')が発生するので書き換える
        super(Stream.of(System.getProperty("java.class.path",".").split(System.getProperty("path.separator"))).map(p -> {
            try {
                return new File(p).toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        }).toArray(URL[]::new), parent.getParent());
        
		createClassModifier();
	}

	protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return super.loadClass(name, resolve);
	}

	protected Class findClass(String name) throws ClassNotFoundException {

		if (classModifier == null || !DJUnitUtil.isProjectsSource(name)) {
			return super.findClass(name);
		}

		byte[] data = getModifiedClass(name);
		if (data == null) {
			return super.findClass(name);
		}

		return defineClass(name, data);
	}

	protected Class defineClass(String name, byte[] data) {

		int index = name.lastIndexOf('.');
		String packageName = name;
		if (index != -1) {
			packageName = name.substring(0, index);
		}

		Package p = getPackage(packageName);
		if (p == null) {
			definePackage(packageName, null, null, null, null, null, null, null);
		}

		return defineClass(name, data, 0, data.length);
	}

	public void createClassModifier() {

		String library = System.getProperty(DJUnitUtil.BYTECODE_LIBRARY_KEY);
		String modifierClassName = ASM_CLASS_MODIFIER_CLASS_NAME;
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM15.equalsIgnoreCase(library)) {
			modifierClassName = ASM_15x_CLASS_MODIFIER_CLASS_NAME;
		}

		Class cls = null;
		try {
			cls = loadClass(modifierClassName);
			classModifier = cls.newInstance();
		} catch (Exception e) {
			throw new Error("djUnit initialize error. [" + modifierClassName + "]", e);
		}
	}

	protected byte[] getModifiedClass(String name) throws ClassNotFoundException {
		try {
			Method modifyMethod = classModifier.getClass().getMethod(CLASS_MODIFIER_MTHOD_NAME, new Class[] { String.class });
				return (byte[]) modifyMethod.invoke(classModifier, new Object[] { name });
		} catch (InvocationTargetException e) {
			throw new ClassNotFoundException(name, e.getCause());
		} catch (Exception e) {
			throw new Error("djUnit class load error (Class : " + name + ")", e);
		}
	}

}