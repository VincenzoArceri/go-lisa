package it.unive.golisa.cfg.expression.binary;

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

/**
 * A Go right shift function call (e.g., e1 >> e2).
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class GoRightShift extends BinaryExpression {

	/**
	 * Builds a Go right shift expression at a given location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this expression happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param exp1       left-hand side operand
	 * @param exp2       right-hand side operand
	 */
	public GoRightShift(CFG cfg, SourceCodeLocation location, Expression exp1, Expression exp2) {
		super(cfg, location, ">>", exp1, exp2);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V> state,
					SymbolicExpression left,
					SymbolicExpression right) throws SemanticException {
		// TODO Auto-generated method stub
		return null;
	}
}
