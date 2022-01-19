package it.unive.golisa.cfg.runtime.strings;

import it.unive.golisa.cfg.runtime.strings.GoIndex.IndexOf;
import it.unive.golisa.cfg.type.GoBoolType;
import it.unive.golisa.cfg.type.GoStringType;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;

public class GoIndexRune extends NativeCFG {

	public GoIndexRune(CodeLocation location, CompilationUnit stringUnit) {
		super(new CFGDescriptor(location, stringUnit, false, stringUnit.getName() + ".IndexRune", GoBoolType.INSTANCE,
				new Parameter(location, "this", GoStringType.INSTANCE),
				new Parameter(location, "other", GoStringType.INSTANCE)),
				IndexOf.class);
	}
}