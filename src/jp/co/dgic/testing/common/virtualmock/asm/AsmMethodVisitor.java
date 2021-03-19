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

import org.objectweb.asm.MethodVisitor;

public class AsmMethodVisitor extends AbstractAsmMethodVisitor {

	public AsmMethodVisitor(MethodVisitor methodVisitor, String className,
			String methodName, String desc, String signature, boolean isStatic, String[] exceptions, int maxLocals, String[] superClassNames) {
		super(methodVisitor, className, methodName, desc, signature, isStatic, exceptions, maxLocals, superClassNames);
	}

	public void visitCode() {

		super.visitCode();

		// crate arguments array
		createCreateArgsArray(_isStatic, _types, 0);

		// call MockObjecManager.indicateCalledAndGetReturnValue
		mv.visitLdcInsn(makeKey(_className, _methodName));
		mv.visitVarInsn(ALOAD, _maxLocals);
		mv.visitMethodInsn(INVOKESTATIC, MANAGER_CLASS_NAME, "indicateCalledAndGetReturnValue", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
		mv.visitVarInsn(ASTORE, _maxLocals);

		// if (Mock value != null) return mock value
		createReturnValueProcess();
	}

}
