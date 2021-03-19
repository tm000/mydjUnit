package com.jcoverage.coverage.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//import jp.co.dgic.testing.common.virtualmock.asm.AsmEmptyVisitor;
import jp.co.dgic.testing.common.virtualmock.asm.AsmEmptyClassVisitor;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AsmClassCoverageTargetLineCollector extends ClassAdapter {

	private boolean isEnum = false;
	private Map lineNumberLabels = new HashMap();
	private Set finallyBlocks = new HashSet();

    public AsmClassCoverageTargetLineCollector() {
        super(Opcodes.ASM9, new AsmEmptyClassVisitor(Opcodes.ASM9));
    }

	public boolean isEnum() {
		return isEnum;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		isEnum = (access & Opcodes.ACC_ENUM) > 0;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new AsmMethodCoverageTargetLineCollector(this);
	}

	public void addLineLabel(int line, Label start) {
		lineNumberLabels.put(start.toString(), new Integer(line));
	}

	public void addFinallyBlock(Label handler) {
		finallyBlocks.add(handler.toString());
	}

	public Set getFinallyLines() {
		Set lines = new HashSet();
		String[] blocks = (String[]) finallyBlocks.toArray(new String[finallyBlocks.size()]);
		for (int i = 0; i < blocks.length; i++) {
			Integer line = (Integer) lineNumberLabels.get(blocks[i]);
			if (line != null) {
				lines.add(line);
			}
		}
		return lines;
	}
}
