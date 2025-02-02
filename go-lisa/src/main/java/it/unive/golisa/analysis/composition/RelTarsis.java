package it.unive.golisa.analysis.composition;

import it.unive.golisa.analysis.StringConstantPropagation;
import it.unive.golisa.analysis.rsubs.RelationalSubstringDomain;
import it.unive.golisa.analysis.tarsis.Tarsis;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.PairRepresentation;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * The reduced product between Tarsis, string constant propagation and RSub.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class RelTarsis extends BaseLattice<RelTarsis> implements ValueDomain<RelTarsis> {

	private final ValueEnvironment<Tarsis> tarsis;
	private final ValueEnvironment<StringConstantPropagation> constant;
	private final RelationalSubstringDomain rsubs;

	/**
	 * Builds the top abstract value.
	 */
	public RelTarsis() {
		this(new ValueEnvironment<Tarsis>(new Tarsis()), new RelationalSubstringDomain(),
				new ValueEnvironment<StringConstantPropagation>(new StringConstantPropagation()));
	}

	private RelTarsis(ValueEnvironment<Tarsis> tarsis, RelationalSubstringDomain rsubs,
			ValueEnvironment<StringConstantPropagation> constant) {
		this.tarsis = tarsis;
		this.rsubs = rsubs;
		this.constant = constant;
	}

	@Override
	public RelTarsis assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
		RelationalSubstringDomain rsubsAssign = rsubs.assign(id, expression, pp);
		ValueEnvironment<StringConstantPropagation> csAssign = constant.assign(id, expression, pp);
		return new RelTarsis(tarsis.assign(id, expression, pp), rsubsAssign.propagateConstants(csAssign), csAssign);
	}

	@Override
	public RelTarsis smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return new RelTarsis(tarsis.smallStepSemantics(expression, pp), rsubs.smallStepSemantics(expression, pp),
				constant.smallStepSemantics(expression, pp));
	}

	@Override
	public RelTarsis assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return new RelTarsis(tarsis.assume(expression, pp), rsubs.assume(expression, pp),
				constant.assume(expression, pp));
	}

	@Override
	public RelTarsis forgetIdentifier(Identifier id) throws SemanticException {
		return new RelTarsis(tarsis.forgetIdentifier(id), rsubs.forgetIdentifier(id), constant.forgetIdentifier(id));
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		if (tarsis.satisfies(expression, pp) == Satisfiability.SATISFIED
				|| rsubs.satisfies(expression, pp) == Satisfiability.SATISFIED
				|| constant.satisfies(expression, pp) == Satisfiability.SATISFIED)
			return Satisfiability.SATISFIED;

		if (tarsis.satisfies(expression, pp) == Satisfiability.NOT_SATISFIED
				|| rsubs.satisfies(expression, pp) == Satisfiability.NOT_SATISFIED
				|| constant.satisfies(expression, pp) == Satisfiability.NOT_SATISFIED)
			return Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public DomainRepresentation representation() {
		if (isTop())
			return Lattice.TOP_REPR;
		if (isBottom())
			return Lattice.BOTTOM_REPR;

		return new PairRepresentation(new PairRepresentation(tarsis.representation(), rsubs.representation()),
				constant.representation());
	}

	@Override
	public boolean isTop() {
		return tarsis.isTop() && rsubs.isTop() && constant.isTop();
	}

	@Override
	public boolean isBottom() {
		return tarsis.isBottom() && rsubs.isBottom() && constant.isTop();
	}

	@Override
	public RelTarsis top() {
		return new RelTarsis(new ValueEnvironment<Tarsis>(new Tarsis()), new RelationalSubstringDomain(),
				new ValueEnvironment<StringConstantPropagation>(new StringConstantPropagation()));
	}

	@Override
	public RelTarsis bottom() {
		return new RelTarsis(tarsis.bottom(), rsubs.bottom(), constant.bottom());
	}

	@Override
	protected RelTarsis lubAux(RelTarsis other) throws SemanticException {
		return new RelTarsis(tarsis.lub(other.tarsis), rsubs.lub(other.rsubs), constant.lub(other.constant));
	}

	@Override
	protected RelTarsis wideningAux(RelTarsis other) throws SemanticException {
		return new RelTarsis(tarsis.widening(other.tarsis), rsubs.widening(other.rsubs),
				constant.widening(other.constant));
	}

	@Override
	protected boolean lessOrEqualAux(RelTarsis other) throws SemanticException {
		return tarsis.lessOrEqual(other.tarsis) && rsubs.lessOrEqual(other.rsubs)
				&& constant.lessOrEqual(other.constant);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constant == null) ? 0 : constant.hashCode());
		result = prime * result + ((rsubs == null) ? 0 : rsubs.hashCode());
		result = prime * result + ((tarsis == null) ? 0 : tarsis.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelTarsis other = (RelTarsis) obj;
		if (constant == null) {
			if (other.constant != null)
				return false;
		} else if (!constant.equals(other.constant))
			return false;
		if (rsubs == null) {
			if (other.rsubs != null)
				return false;
		} else if (!rsubs.equals(other.rsubs))
			return false;
		if (tarsis == null) {
			if (other.tarsis != null)
				return false;
		} else if (!tarsis.equals(other.tarsis))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public RelTarsis pushScope(ScopeToken token) throws SemanticException {
		return new RelTarsis(tarsis.pushScope(token), rsubs.pushScope(token), constant.pushScope(token));
	}

	@Override
	public RelTarsis popScope(ScopeToken token) throws SemanticException {
		return new RelTarsis(tarsis.popScope(token), rsubs.popScope(token), constant.popScope(token));
	}
}