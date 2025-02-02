package it.unive.golisa.cfg.expression.ternary;

import it.unive.golisa.cfg.type.GoStringType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.type.Type;

/**
 * A Go slice expression (e.g., s[1:5]).
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class GoSimpleSlice extends it.unive.lisa.program.cfg.statement.TernaryExpression {
	/**
	 * Builds a Go slice expression.
	 * 
	 * @param cfg      the {@link CFG} where this expression lies
	 * @param location the location where this expression is defined
	 * @param left     the left expression
	 * @param middle   the middle expression
	 * @param right    the right expression
	 */
	public GoSimpleSlice(CFG cfg, SourceCodeLocation location, Expression left, Expression middle, Expression right) {
		super(cfg, location, "slice", left, middle, right);
	}

	@Override
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> ternarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural, AnalysisState<A, H, V, T> state,
					SymbolicExpression left, SymbolicExpression middle, SymbolicExpression right,
					StatementStore<A, H, V, T> expressions) throws SemanticException {
		AnalysisState<A, H, V, T> result = state.bottom();
		for (Type leftType : left.getRuntimeTypes())
			for (Type middleType : middle.getRuntimeTypes())
				for (Type rightType : right.getRuntimeTypes())
					if ((leftType.isStringType() || leftType.isUntyped())
							&& (middleType.isNumericType() || middleType.isUntyped())
							&& (rightType.isNumericType() || rightType.isUntyped())) {
						AnalysisState<A, H,
								V, T> tmp = state.smallStepSemantics(
										new TernaryExpression(GoStringType.INSTANCE,
												left, middle, right, StringSubstring.INSTANCE, getLocation()),
										this);
						result = result.lub(tmp);
					}
		return result;
	}
}
