package it.unive.golisa.cfg.expression;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;

public class GoCollectionAccess extends BinaryExpression {

	public GoCollectionAccess(CFG cfg, SourceCodeLocation location, Expression container, Expression child) {
		super(cfg, location, container + "::" + child, container, child);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V> state,
					SymbolicExpression left,
					SymbolicExpression right) throws SemanticException {
		AnalysisState<A, H, V> result = state.bottom();

		AnalysisState<A, H, V> rec = state.smallStepSemantics(left, this);
		for (SymbolicExpression expr : rec.getComputedExpressions()) {
			AnalysisState<A, H,
					V> tmp = rec.smallStepSemantics(
							new AccessChild(getRuntimeTypes(),
									new HeapDereference(getRuntimeTypes(), expr, getLocation()), right, getLocation()),
							this);
			result = result.lub(tmp);
		}

		return result;
	}
}
