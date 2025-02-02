package it.unive.golisa.cfg.expression.binary;

import it.unive.golisa.cfg.type.GoBoolType;
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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;

/**
 * A Go less than expression (e.g., x < y).
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class GoLess extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the less than expression.
	 *
	 * @param cfg      the {@link CFG} where this expression lies
	 * @param location the location where this expression is defined
	 * @param left     the left-hand side of this expression
	 * @param right    the right-hand side of this expression
	 */
	public GoLess(CFG cfg, SourceCodeLocation location, Expression left, Expression right) {
		super(cfg, location, "<", GoBoolType.INSTANCE, left, right);
	}

	@Override
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural, AnalysisState<A, H, V, T> state,
					SymbolicExpression left, SymbolicExpression right, StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		AnalysisState<A, H, V, T> result = state.bottom();
		// following the Golang specification:
		// in any comparison, the first operand must be assignable to the type
		// of the second operand, or vice versa.
		for (Type leftType : left.getRuntimeTypes())
			for (Type rightType : right.getRuntimeTypes()) {
				if (leftType.canBeAssignedTo(rightType) || rightType.canBeAssignedTo(leftType)) {
					// TODO: only, integer, floating point values, strings are
					// ordered
					// but missing lexicographical string order in LiSA
					AnalysisState<A, H,
							V, T> tmp = state
									.smallStepSemantics(
											new BinaryExpression(GoBoolType.INSTANCE,
													left, right, ComparisonLt.INSTANCE, getLocation()),
											this);
					result = result.lub(tmp);
				}
			}
		return result;
	}
}
