package com.jcoverage.coverage.asm;

//import jp.co.dgic.testing.common.virtualmock.asm.AsmEmptyVisitor;
import jp.co.dgic.testing.common.virtualmock.asm.AsmEmptyMethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.Opcodes;

public class AsmMethodCoverageTargetLineCollector extends MethodAdapter {

	private AsmClassCoverageTargetLineCollector classLineCollector;

    public AsmMethodCoverageTargetLineCollector(AsmClassCoverageTargetLineCollector classLineCollector) {
        super(Opcodes.ASM9, new AsmEmptyMethodVisitor(Opcodes.ASM9));
        this.classLineCollector = classLineCollector;
    }

	public void visitLineNumber(final int line, final Label start) {
		classLineCollector.addLineLabel(line, start);
	}

	public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
		if (type == null) {
			classLineCollector.addFinallyBlock(handler);
		}
	}
}
