package it.unive.golisa.cfg.statement.assignment;

import it.unive.golisa.cfg.type.untyped.GoUntypedFloat;
import it.unive.golisa.cfg.type.untyped.GoUntypedInt;
import it.unive.golisa.golang.util.GoLangUtils;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * Go variable declaration class (e.g., var x int = 5).
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class GoVariableDeclaration extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	private final Type type;

	/**
	 * Builds a Go variable declaration with initialization, assigning
	 * {@code expression} to {@code target}, happening at the given location in
	 * the program.
	 * 
	 * @param cfg        the cfg that this declaration belongs to
	 * @param location   the location where this statement is defined within the
	 *                       source file
	 * @param type       the type of this declaration
	 * @param var        the declared variable
	 * @param expression the expression to assign to {@code var}
	 */
	public GoVariableDeclaration(CFG cfg, SourceCodeLocation location, Type type, VariableRef var,
			Expression expression) {
		super(cfg, location, ":=", var, expression);
		this.type = type;
	}

	@Override
	public String toString() {
		return "var " + getLeft() + " " + type + " = " + getRight();
	}

	/**
	 * Yields the type of the declaration.
	 * 
	 * @return the type of the declaration
	 */
	public Type getDeclaredType() {
		return type;
	}

	@Override
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural, AnalysisState<A, H, V, T> state,
					SymbolicExpression left, SymbolicExpression right, StatementStore<A, H, V, T> expressions)

					throws SemanticException {
		// e.g., _ = f(), we just return right state
		if (GoLangUtils.refersToBlankIdentifier(getLeft()))
			return state;

		ExternalSet<Type> idType = Caches.types().mkSingletonSet(type);
		Variable id = new Variable(type, ((VariableRef) getLeft()).getName(), getLeft().getLocation());

		AnalysisState<A, H, V, T> result = state.bottom();
		for (Type rightType : right.getRuntimeTypes()) {
			AnalysisState<A, H, V, T> tmp = state.bottom();
			if (rightType instanceof GoUntypedInt || rightType instanceof GoUntypedFloat) {
				Constant typeCast = new Constant(new TypeTokenType(idType), type, getRight().getLocation());
				tmp = state.assign(id, new BinaryExpression(type, right, typeCast, TypeConv.INSTANCE,
						getRight().getLocation()), this);
			} else
				tmp = state.assign(id, right, this);

			result = result.lub(tmp);
		}

		if (!getRight().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getRight().getMetaVariables());
		if (!getLeft().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getLeft().getMetaVariables());

		return result;
	}
}
