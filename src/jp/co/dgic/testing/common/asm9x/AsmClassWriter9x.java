package jp.co.dgic.testing.common.asm9x;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class AsmClassWriter9x extends ClassVisitor {
    protected ClassWriter cw;

    public AsmClassWriter9x() {
        super(Opcodes.ASM9);
        this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    }

    public ClassWriter getClassWriter() {
        return this.cw;
    }
}
