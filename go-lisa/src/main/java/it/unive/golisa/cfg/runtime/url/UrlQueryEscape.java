package it.unive.golisa.cfg.runtime.url;

import it.unive.golisa.cfg.type.GoStringType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;

public class UrlQueryEscape extends NativeCFG {

	public UrlQueryEscape(CodeLocation location, CompilationUnit urlUnit) {
		super(new CFGDescriptor(location, urlUnit, false, "QueryEscape", GoStringType.INSTANCE,
				new Parameter(location, "this", GoStringType.INSTANCE)),
				QueryEscape.class);
	}

	public static class QueryEscape extends UnaryExpression implements PluggableStatement {

		private Statement original;

		@Override
		public void setOriginatingStatement(Statement st) {
			original = st;
		}

		public static QueryEscape build(CFG cfg, CodeLocation location, Expression... params) {
			return new QueryEscape(cfg, location, params[0]);
		}

		public QueryEscape(CFG cfg, CodeLocation location, Expression exp1) {
			super(cfg, location, "QueryEscape", GoStringType.INSTANCE, exp1);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> unarySemantics(
						InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V> state,
						SymbolicExpression expr, StatementStore<A, H, V> expressions)
						throws SemanticException {
			// TODO to implement query escape method from url package
			return state.smallStepSemantics(new PushAny(getRuntimeTypes(), getLocation()), original);
		}
	}
}
