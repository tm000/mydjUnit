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
package com.jcoverage.coverage.asm;

import java.util.Set;

import jp.co.dgic.testing.common.virtualmock.InternalMockObjectManager;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.jcoverage.coverage.InstrumentData;

public class AsmClassInstrumenter extends ClassAdapter {

	private InstrumentData instrumentData;
	private String className;

	// version 0.8.5
	private static final String ENUM_DEFAULT_METHOD_VALUES = "values";
	private static final String ENUM_DEFAULT_METHOD_VALUEOF = "valueOf";
	private boolean isEnum = false;
	private Set finallyLines;

	public boolean isEnum() {
		return isEnum;
	}

	public void setEnum(boolean isEnum) {
		this.isEnum = isEnum;
	}

	public void setFinallyLines(Set lines) {
		this.finallyLines = lines;
	}

	public String getClassName() {
		return this.className;
	}

	public AsmClassInstrumenter(ClassVisitor cv) {
		super(Opcodes.ASM9, cv);
	}

	public InstrumentData getInstrumentData() {
		return instrumentData;
	}

	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {

		this.className = name.replace('/', '.');
		this.instrumentData = new InstrumentData(className);

		super.visit(version, access, name, signature, superName, interfaces);
	}

	public void visitSource(String source, String debug) {
		super.visitSource(source, debug);
		instrumentData.setSourceFileName(source);
	}

	public MethodVisitor visitMethod(int access, String name,
			String desc, String signature, String[] exceptions) {

		if ((access & Opcodes.ACC_BRIDGE) == Opcodes.ACC_BRIDGE) {
			InternalMockObjectManager.printConsole("#################################################");
			InternalMockObjectManager.printConsole("#### found Bridge method [" + name + "]");
			InternalMockObjectManager.printConsole("#################################################");
			
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
		
		// version 0.8.5
		// for enum
		if (isEnum() && isEnumDefaultMethod(access, name, desc)) {
			InternalMockObjectManager.printConsole("#################################################");
			InternalMockObjectManager.printConsole("#### found enum default static method [" + name + "], DESC [" + desc + "]");
			InternalMockObjectManager.printConsole("#################################################");

			return super.visitMethod(access, name, desc, signature, exceptions);
		}

		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		instrumentData.addMethodNamesAndSignatures(name, desc);

		AsmMethodInstrumenter methodInstrumenter = new AsmMethodInstrumenter(mv, className, name, desc, instrumentData);
		methodInstrumenter.setFinallyLines(finallyLines);
		return methodInstrumenter;
	}
	
	// version 0.8.5
	private boolean isEnumDefaultMethod(int access,  String name, String desc) {
		if ("<init>".equals(name)) return true;
		if ("<clinit>".equals(name)) return true;
		if ((access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) return false;
		if (!isEnumDefaultMethodName(name)) return false;
		if (!isEnumDefaultMethodSignature(name, desc)) return false;
		return true;
	}
	
	// version 0.8.5
	private boolean isEnumDefaultMethodName(String methodName) {
		if (ENUM_DEFAULT_METHOD_VALUES.equals(methodName)) return true;
		if (ENUM_DEFAULT_METHOD_VALUEOF.equals(methodName)) return true;
		return false;
	}
	
	// version 0.8.5
	private boolean isEnumDefaultMethodSignature(String methodName, String desc) {
		if (desc == null) return false;
		if (ENUM_DEFAULT_METHOD_VALUES.equals(methodName)) {
			if (desc.startsWith("()")) return true; 
		}
		if (ENUM_DEFAULT_METHOD_VALUEOF.equals(methodName)) {
			if (desc.startsWith("(Ljava/lang/String;)")) return true; 
		}
		return false;
	}
}
