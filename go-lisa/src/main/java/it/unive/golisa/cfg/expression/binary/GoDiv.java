package it.unive.golisa.cfg.expression.binary;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.BinaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * A Go numerical division function call (e1 / e2).
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class GoDiv extends BinaryNativeCall implements GoBinaryNumericalOperation {
	
	/**
	 * Builds a Go division expression. 
	 * The location where this expression appears is unknown 
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param cfg	the cfg that this expression belongs to
	 * @param exp1	left-hand side operand
	 * @param exp2 	right-hand side operand 
	 */
	public GoDiv(CFG cfg, SourceCodeLocation location, Expression exp1, Expression exp2) {
		super(cfg, location, "/", exp1, exp2);
	}

	@Override
	protected <A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
			AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V> leftState,
			SymbolicExpression leftExp, AnalysisState<A, H, V> rightState, SymbolicExpression rightExp)
			throws SemanticException {
		
		ExternalSet<Type> types;

		AnalysisState<A, H, V> result = entryState.bottom();
		for (Type leftType : leftExp.getTypes())
			for (Type rightType : rightExp.getTypes()) 
				if (leftType.isNumericType() && rightType.isNumericType()) {
					types = resultType(leftExp, rightExp);
					result = result.lub(rightState.smallStepSemantics(new BinaryExpression(types, leftExp, rightExp, BinaryOperator.NUMERIC_NON_OVERFLOWING_DIV, getLocation()), this));
				} 

		return result;
	}
}
