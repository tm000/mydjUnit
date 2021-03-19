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
package jp.co.dgic.testing.common.virtualmock.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.Opcodes;

public class AsmMethodChecker extends MethodAdapter {

	private static final String JUNIT4_TEST_ANNOTATION = "org/junit/";
	
	private AsmClassChecker acc;
	private String name;
	private String desc;

	public AsmMethodChecker(AsmClassChecker acc, String name, String desc) {
		super(Opcodes.ASM9, new AsmEmptyMethodVisitor(Opcodes.ASM9));
		this.acc = acc;
		this.name = name;
		this.desc = desc;
	}

	public void visitMaxs(int maxStack, int maxLocals) {
		acc.putMaxLocals(this.name, this.desc, maxLocals);
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc != null && desc.indexOf(JUNIT4_TEST_ANNOTATION) >= 0) {
			acc.setJUnit4TestAnnotation(true);
		}
		return super.visitAnnotation(desc, visible);
	}
}
