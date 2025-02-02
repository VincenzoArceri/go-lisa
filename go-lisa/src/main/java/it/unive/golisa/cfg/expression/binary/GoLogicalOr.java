package it.unive.golisa.cfg.expression.binary;

import it.unive.golisa.cfg.type.GoBoolType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;

/**
 * A Go Boolean logical oe expression (e.g., x && y).
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class GoLogicalOr extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the Boolean logical or expression.
	 *
	 * @param cfg      the {@link CFG} where this expression lies
	 * @param location the location where this expression is defined
	 * @param left     the left-hand side of this expression
	 * @param right    the right-hand side of this expression
	 */
	public GoLogicalOr(CFG cfg, SourceCodeLocation location, Expression left, Expression right) {
		super(cfg, location, "||", GoBoolType.INSTANCE, left, right);
	}

	@Override
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural, AnalysisState<A, H, V, T> state,
					SymbolicExpression left, SymbolicExpression right, StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		// FIXME: need to check which state needs to be returned (left/right)
		if (!left.getDynamicType().isBooleanType() && !left.getDynamicType().isUntyped())
			return state.bottom();
		if (!right.getDynamicType().isBooleanType() && !right.getDynamicType().isUntyped())
			return state.bottom();

		if (state.satisfies(left, this) == Satisfiability.SATISFIED)
			return state;
		else if (state.satisfies(left, this) == Satisfiability.NOT_SATISFIED)
			return state
					.smallStepSemantics(new BinaryExpression(GoBoolType.INSTANCE,
							left, right, LogicalOr.INSTANCE, getLocation()), this);
		else if (state.satisfies(left, this) == Satisfiability.UNKNOWN)
			return state.lub(state
					.smallStepSemantics(new BinaryExpression(GoBoolType.INSTANCE,
							left, right, LogicalOr.INSTANCE, getLocation()), this));
		else
			return state.bottom();
	}
}