package it.unive.golisa.cfg.runtime.crypto.rand.function;

import it.unive.golisa.cfg.runtime.io.type.Reader;
import it.unive.golisa.cfg.type.composite.GoErrorType;
import it.unive.golisa.cfg.type.composite.GoTupleType;
import it.unive.golisa.cfg.type.numeric.signed.GoIntType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * func Prime(rand io.Reader, bits int) (p *big.Int, err error).
 * 
 * @author <a href="mailto:luca.olivieri@univr.it">Luca Olivieri</a>
 */
public class Prime extends NativeCFG {

	/**
	 * Builds the native cfg.
	 * 
	 * @param location the location where this native cfg is defined
	 * @param randUnit the unit to which this native cfg belongs to
	 */
	public Prime(CodeLocation location, CompilationUnit randUnit) {
		super(new CFGDescriptor(location, randUnit, false, "Read",
				GoTupleType.getTupleTypeOf(location, it.unive.golisa.cfg.runtime.math.big.type.Int.INSTANCE,
						GoErrorType.INSTANCE),
				new Parameter(location, "rand", Reader.INSTANCE),
				new Parameter(location, "bits", GoIntType.INSTANCE)),
				PrimeImpl.class);
	}

	/**
	 * The Prime implementation.
	 * 
	 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
	 */
	public static class PrimeImpl extends BinaryExpression
			implements PluggableStatement {

		private Statement original;

		@Override
		public void setOriginatingStatement(Statement st) {
			original = st;
		}

		/**
		 * Builds the pluggable statement.
		 * 
		 * @param cfg      the {@link CFG} where this pluggable statement lies
		 * @param location the location where this pluggable statement is
		 *                     defined
		 * @param params   the parameters
		 * 
		 * @return the pluggable statement
		 */
		public static PrimeImpl build(CFG cfg, CodeLocation location, Expression... params) {
			return new PrimeImpl(cfg, location, params[0], params[1]);
		}

		/**
		 * Builds the pluggable statement.
		 * 
		 * @param cfg      the {@link CFG} where this pluggable statement lies
		 * @param location the location where this pluggable statement is
		 *                     defined
		 * @param left     the left-hand side of this expression
		 * @param right    the right-hand side of this expression
		 */
		public PrimeImpl(CFG cfg, CodeLocation location, Expression left, Expression right) {
			super(cfg, location, "PrimeImpl", GoTupleType.getTupleTypeOf(location,
					it.unive.golisa.cfg.runtime.math.big.type.Int.INSTANCE, GoErrorType.INSTANCE), left, right);
		}

		@Override
		protected <A extends AbstractState<A, H, V, T>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>,
				T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
						InterproceduralAnalysis<A, H, V, T> interprocedural, AnalysisState<A, H, V, T> state,
						SymbolicExpression left, SymbolicExpression right, StatementStore<A, H, V, T> expressions)
						throws SemanticException {
			return state.top();
		}
	}
}
