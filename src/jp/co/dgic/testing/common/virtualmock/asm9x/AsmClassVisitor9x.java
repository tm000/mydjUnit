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
package jp.co.dgic.testing.common.virtualmock.asm9x;

import jp.co.dgic.testing.common.asm9x.AsmClassWriter9x;
import jp.co.dgic.testing.common.virtualmock.InternalMockObjectManager;
import jp.co.dgic.testing.common.virtualmock.asm.AsmClassChecker;
import jp.co.dgic.testing.common.virtualmock.asm.AsmConstractorVisitor;
import jp.co.dgic.testing.common.virtualmock.asm.AsmMethodVisitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;

public class AsmClassVisitor9x extends AsmClassWriter9x {

    protected AsmClassChecker acc;
    protected String className;

    public AsmClassVisitor9x(AsmClassChecker acc) {
        this.acc = acc;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        InternalMockObjectManager.printConsole("[modify class] " + name + " " + signature);
        InternalMockObjectManager.printConsole("[class version] " + version);
        this.className = name.replace('/', '.');
        cw.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        boolean isStatic = false;
        if ((access & Opcodes.ACC_STATIC) > 0) {
            isStatic = true;
        }

        InternalMockObjectManager.printConsole("#################################################################");
        InternalMockObjectManager.printConsole("#################################################################");
        InternalMockObjectManager.printConsole("### " + access + (isStatic ? " static " : " ") + name + " " + signature);
        InternalMockObjectManager.printConsole("#################################################################");
        InternalMockObjectManager.printConsole("#################################################################");

        MethodVisitor mv = cw.visitMethod(access, name, desc, signature, exceptions);

        // is abstract or native
        if ((access & Opcodes.ACC_ABSTRACT) > 0) return mv;
        if ((access & Opcodes.ACC_NATIVE) > 0) return mv;
        if ((access & Opcodes.ACC_BRIDGE) > 0) return mv;

        int maxLocals = acc.getMaxLocals(name, desc);

        return createMethodVisitor(mv, name, desc, signature, isStatic, exceptions, maxLocals);
    }

    protected MethodVisitor createMethodVisitor(MethodVisitor mv, String name, String desc, String signature, boolean isStatic, String[] exceptions, int maxLocals) {
        if ("<init>".equalsIgnoreCase(name)) return new AsmConstractorVisitor(mv, this.className, name, desc, signature, exceptions, maxLocals, acc.getSuperClassNames());
        return new AsmMethodVisitor(mv, this.className, name, desc, signature, isStatic, exceptions, maxLocals, acc.getSuperClassNames());
    }

    public ClassWriter getClassWriter() {
        return this.cw;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return cw.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        cw.visitAttribute(attribute);
    }

    @Override
    public void visitEnd() {
        cw.visitEnd();
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return cw.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        cw.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        return cw.visitModule(name, access, version);
    }

    @Override
    public void visitNestHost(String nestHost) {
        cw.visitNestHost(nestHost);
    }

    @Override
    public void visitNestMember(String nestMember) {
        cw.visitNestMember(nestMember);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        cw.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        cw.visitPermittedSubclass(permittedSubclass);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        return cw.visitRecordComponent(name, descriptor, signature);
    }

    @Override
    public void visitSource(String source, String debug) {
        cw.visitSource(source, debug);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        return cw.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

}