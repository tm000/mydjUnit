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
package com.jcoverage.coverage;

import jp.co.dgic.eclipse.jdt.internal.coverage.util.CoverageUtil;
import jp.co.dgic.testing.common.AbstractAsmModifier;
import jp.co.dgic.testing.common.asm.AsmClassReader;
import jp.co.dgic.testing.common.asm9x.AsmClassWriter9x;
import org.objectweb.asm.ClassWriter;

import com.jcoverage.coverage.asm.AsmClassCoverageTargetLineCollector;
import com.jcoverage.coverage.asm.AsmClassInstrumenter;

public class AsmCoverageInstrumenter extends AbstractAsmModifier {

	public AsmCoverageInstrumenter() {
		super("Coverage");
	}

	protected byte[] modify(String className, AsmClassReader cr) throws Exception {

		if (!CoverageUtil.isUseCoverage())  return null;
		if (!CoverageUtil.isIncluded(className)) return null;
		if (CoverageUtil.isExcluded(className)) return null;

		AsmClassCoverageTargetLineCollector collector = new AsmClassCoverageTargetLineCollector();
		cr.accept(collector);
//		AsmClassChecker acc = new AsmClassChecker();
//		cr.accept(acc);

// version 0.8.5
//		if (acc.isInterface() || acc.isAnnotation() || acc.isEnum()) return null;

//		ClassWriter cw = new AsmClassWriter();
//		ClassWriter cw = AsmClassReader.createClassWriter();
		ClassWriter cw = ((AsmClassWriter9x)AsmClassReader.createClassWriter()).getClassWriter();
		AsmClassInstrumenter cv = new AsmClassInstrumenter(cw);
		cv.setEnum(collector.isEnum());	// version 0.8.5
		cv.setFinallyLines(collector.getFinallyLines());	// version 0.8.5
		cr.accept(cv);

		InstrumentationInternal i =
			(InstrumentationInternal) InstrumentationFactory
				.getInstance()
				.newInstrumentation(className);

		InstrumentData data = cv.getInstrumentData();
		i.setSourceLineNumbers(data.getSourceLineNumbers());
		i.setSourceFileName(data.getSourceFileName());
		i.setSourceLineNumbersByMethod(data.getMethodLineNumbers());
		i.setConditionalsByMethod(data.getMethodConditionals());
		i.setMethodNamesAndSignatures(
				data.getMethodNamesAndSignatures());

		return cw.toByteArray();
	}

}
