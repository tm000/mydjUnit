package jp.co.dgic.testing.common.asm;

import java.io.IOException;

import jp.co.dgic.testing.common.asm2x.AsmClassReader2x;
import jp.co.dgic.testing.common.asm2x.AsmClassWriter2x;
import jp.co.dgic.testing.common.asm9x.AsmClassReader9x;
import jp.co.dgic.testing.common.asm9x.AsmClassWriter9x;
import jp.co.dgic.testing.common.virtualmock.asm9x.AsmClassVisitor9x;
import jp.co.dgic.testing.common.asm3x.AsmClassReader3x;
import jp.co.dgic.testing.common.asm3x.AsmClassWriter3x;
import jp.co.dgic.testing.common.util.DJUnitUtil;
import jp.co.dgic.testing.common.virtualmock.asm.AsmClassChecker;
import jp.co.dgic.testing.common.virtualmock.asm2x.AsmClassVisitor2x;
import jp.co.dgic.testing.common.virtualmock.asm3x.AsmClassVisitor3x;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public abstract class AsmClassReader extends ClassReader {
	
	public static AsmClassReader createAsmClassReader(String name) throws IOException {
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM2.equals(getAsmType())) {
		return new AsmClassReader2x(name);
		}
//		return new AsmClassReader3x(name);
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM3.equals(getAsmType())) {
            return new AsmClassReader3x(name);
        }
        return new AsmClassReader9x(name);
    }

	public static AsmClassReader createAsmClassReader(byte[] bytecodes) throws IOException {
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM2.equals(getAsmType())) {
			return new AsmClassReader2x(bytecodes);
		}
//		return new AsmClassReader3x(bytecodes);
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM3.equals(getAsmType())) {
            return new AsmClassReader3x(bytecodes);
        }
        return new AsmClassReader9x(bytecodes);
	}
	
	public static ClassVisitor createClassWriter() {
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM2.equals(getAsmType())) {
			return new AsmClassWriter2x();
		}
//		return new AsmClassWriter3x();
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM3.equals(getAsmType())) {
            return new AsmClassWriter3x();
        }
        return new AsmClassWriter9x();
	}

	public static ClassVisitor createClassVisitor(AsmClassChecker acc) {
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM2.equals(getAsmType())) {
			return new AsmClassVisitor2x(acc);
		}
//		return new AsmClassVisitor3x(acc);
		if (DJUnitUtil.BYTECODE_LIBRARY_ASM3.equals(getAsmType())) {
            return new AsmClassVisitor3x(acc);
        }
        return new AsmClassVisitor9x(acc);
	}
	private static String getAsmType() {
		return System.getProperty(DJUnitUtil.BYTECODE_LIBRARY_KEY);
	}

	public AsmClassReader(String name) throws IOException {
		super(name);
	}

	public AsmClassReader(byte[] bytecodes) {
		super(bytecodes);
	}

	public abstract void accept(ClassVisitor cv);
	
}
