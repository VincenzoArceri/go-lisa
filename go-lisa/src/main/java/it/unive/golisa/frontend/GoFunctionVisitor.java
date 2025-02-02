package it.unive.golisa.frontend;

import it.unive.golisa.antlr.GoParser.ExpressionContext;
import it.unive.golisa.antlr.GoParser.FunctionDeclContext;
import it.unive.golisa.antlr.GoParser.FunctionLitContext;
import it.unive.golisa.antlr.GoParser.FunctionTypeContext;
import it.unive.golisa.antlr.GoParser.ParametersContext;
import it.unive.golisa.antlr.GoParser.SignatureContext;
import it.unive.golisa.antlr.GoParserBaseVisitor;
import it.unive.golisa.cfg.VariableScopingCFG;
import it.unive.golisa.cfg.expression.unknown.GoUnknown;
import it.unive.golisa.cfg.statement.assignment.GoShortVariableDeclaration;
import it.unive.golisa.cfg.type.GoType;
import it.unive.golisa.cfg.type.composite.GoFunctionType;
import it.unive.golisa.cfg.type.composite.GoTupleType;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

/**
 * An {@link GoParserBaseVisitor} that will parse the code of an Go function.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
class GoFunctionVisitor extends GoCodeMemberVisitor {

	/**
	 * Builds a function visitor (side-effects on packageUnit).
	 * 
	 * @param funcDecl    the function declaration context to visit
	 * @param packageUnit the current unit
	 * @param file        the file path
	 * @param program     the current program
	 * @param constants   the constant mapping
	 */
	protected GoFunctionVisitor(FunctionDeclContext funcDecl, CompilationUnit packageUnit, String file, Program program,
			Map<String, ExpressionContext> constants) {
		super(packageUnit, file, program, constants);
		this.currentUnit = packageUnit;

		// side effects on entrypoints and matrix will affect the cfg
		cfg = new VariableScopingCFG(buildCFGDescriptor(funcDecl, packageUnit), entrypoints, new AdjacencyMatrix<>());
		initializeVisibleIds();

		packageUnit.addCFG(cfg);
	}

	/**
	 * Builds a function visitor (side-effects on packageUnit).
	 * 
	 * @param funcLit     the function literal context to visit
	 * @param packageUnit the current unit
	 * @param file        the file path
	 * @param program     the current program
	 * @param constants   the constant mapping
	 */
	protected GoFunctionVisitor(FunctionLitContext funcLit, CompilationUnit packageUnit, String file, Program program,
			Map<String, ExpressionContext> constants) {
		super(packageUnit, file, program, constants);
		this.currentUnit = packageUnit;

		// side effects on entrypoints and matrix will affect the cfg
		cfg = new VariableScopingCFG(buildCFGDescriptor(funcLit), entrypoints, new AdjacencyMatrix<>());
		initializeVisibleIds();

		packageUnit.addCFG(cfg);
	}

	/**
	 * Builds the function visitor.
	 * 
	 * @param unit      the current unit
	 * @param file      the current file path
	 * @param program   the current program
	 * @param constants the constant mapping
	 */
	public GoFunctionVisitor(CompilationUnit unit, String file, Program program,
			Map<String, ExpressionContext> constants) {
		super(unit, file, program, constants);
	}

	@Override
	public Pair<Statement, Statement> visitFunctionDecl(FunctionDeclContext ctx) {

		Statement entryNode = null;
		Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>,
				Statement> body = visitMethodBlock(ctx.block());

		for (Entry<Statement, String> gotoStmt : gotos.entrySet())
			// we must call cfg.addEdge, and not addEdge
			cfg.addEdge(new SequentialEdge(gotoStmt.getKey(), labeledStmt.get(gotoStmt.getValue())));

		// The function named "main" is the entry point of the program
		if (cfg.getDescriptor().getName().equals("main"))
			program.addEntryPoint(cfg);

		Type returnType = cfg.getDescriptor().getReturnType();

		AdjacencyMatrix<Statement, Edge, CFG> matrix = cfg.getAdjacencyMatrix();
		if (!(returnType instanceof GoTupleType))
			entryNode = body.getLeft();
		else {
			GoTupleType tuple = (GoTupleType) returnType;
			if (tuple.isNamedValues()) {
				Statement lastStmt = null;

				for (Parameter par : tuple) {
					VariableRef var = new VariableRef(cfg, par.getLocation(), par.getName());
					Type parType = par.getStaticType();
					Expression decl;
					if (parType instanceof GoType)
						decl = new GoShortVariableDeclaration(cfg, par.getLocation(), var,
								((GoType) parType).defaultValue(cfg, (SourceCodeLocation) par.getLocation()));
					else
						decl = new GoUnknown(cfg, (SourceCodeLocation) par.getLocation());

					cfg.addNode(decl);

					if (lastStmt != null)
						addEdge(new SequentialEdge(lastStmt, decl), matrix);
					else
						entryNode = decl;
					lastStmt = decl;
				}

				addEdge(new SequentialEdge(lastStmt, body.getLeft()), matrix);
				cfg.getEntrypoints().add(entryNode);

			} else
				entryNode = body.getLeft();
		}

		cfg.getEntrypoints().add(entryNode);

		// If the function body does not have exit points
		// a return statement is added
		if (cfg.getAllExitpoints().isEmpty()) {
			Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());
			if (cfg.getNodesCount() == 0) {
				// empty method, so the ret is also the entrypoint
				matrix.addNode(ret);
				entrypoints.add(ret);
			} else {
				// every non-throwing instruction that does not have a follower
				// is ending the method
				Collection<Statement> preExits = new LinkedList<>();
				for (Statement st : matrix.getNodes())
					if (!st.stopsExecution() && matrix.followersOf(st).isEmpty())
						preExits.add(st);
				matrix.addNode(ret);
				for (Statement st : preExits)
					matrix.addEdge(new SequentialEdge(st, ret));

				for (VariableTableEntry entry : cfg.getDescriptor().getVariables())
					if (preExits.contains(entry.getScopeEnd()))
						entry.setScopeEnd(ret);
			}
		}

		for (Statement st : matrix.getExits())
			if (st instanceof NoOp && !matrix.getIngoingEdges(st).isEmpty()) {
				Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());
				if (!st.stopsExecution() && matrix.followersOf(st).isEmpty())
					matrix.addNode(ret);
				matrix.addEdge(new SequentialEdge(st, ret));
			}

		cfg.simplify();
		return Pair.of(entryNode, body.getRight());
	}

	/**
	 * Builds a {@link CFG} for an anonymous function.
	 * 
	 * @param ctx the function literal context to visit
	 * 
	 * @return a {@link CFG} for an anonymous function
	 */
	protected CFG buildAnonymousCFG(FunctionLitContext ctx) {
		Statement entryNode = null;
		Triple<Statement, AdjacencyMatrix<Statement, Edge, CFG>,
				Statement> body = visitMethodBlock(ctx.block());

		for (Entry<Statement, String> gotoStmt : gotos.entrySet())
			// we must call cfg.addEdge, and not addEdge
			cfg.addEdge(new SequentialEdge(gotoStmt.getKey(), labeledStmt.get(gotoStmt.getValue())));

		Type returnType = cfg.getDescriptor().getReturnType();

		AdjacencyMatrix<Statement, Edge, CFG> matrix = cfg.getAdjacencyMatrix();
		if (!(returnType instanceof GoTupleType))
			entryNode = body.getLeft();
		else {
			GoTupleType tuple = (GoTupleType) returnType;
			if (tuple.isNamedValues()) {
				Statement lastStmt = null;

				for (Parameter par : tuple) {
					VariableRef var = new VariableRef(cfg, par.getLocation(), par.getName());
					GoType parType = (GoType) par.getStaticType();
					GoShortVariableDeclaration decl = new GoShortVariableDeclaration(cfg, par.getLocation(), var,
							parType.defaultValue(cfg, (SourceCodeLocation) par.getLocation()));

					cfg.addNode(decl);

					if (lastStmt != null)
						addEdge(new SequentialEdge(lastStmt, decl), matrix);
					else
						entryNode = decl;
					lastStmt = decl;
				}

				addEdge(new SequentialEdge(lastStmt, body.getLeft()), matrix);
				cfg.getEntrypoints().add(entryNode);

			} else
				entryNode = body.getLeft();
		}

		cfg.getEntrypoints().add(entryNode);

		// If the function body does not have exit points
		// a return statement is added
		if (cfg.getAllExitpoints().isEmpty()) {
			Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());
			if (cfg.getNodesCount() == 0) {
				// empty method, so the ret is also the entrypoint
				matrix.addNode(ret);
				entrypoints.add(ret);
			} else {
				// every non-throwing instruction that does not have a follower
				// is ending the method
				Collection<Statement> preExits = new LinkedList<>();
				for (Statement st : matrix.getNodes())
					if (!st.stopsExecution() && matrix.followersOf(st).isEmpty())
						preExits.add(st);
				matrix.addNode(ret);
				for (Statement st : preExits)
					matrix.addEdge(new SequentialEdge(st, ret));

				for (VariableTableEntry entry : cfg.getDescriptor().getVariables())
					if (preExits.contains(entry.getScopeEnd()))
						entry.setScopeEnd(ret);
			}
		}

		for (Statement st : matrix.getExits())
			if (st instanceof NoOp && !matrix.getIngoingEdges(st).isEmpty()) {
				Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());
				if (!st.stopsExecution() && matrix.followersOf(st).isEmpty())
					matrix.addNode(ret);
				matrix.addEdge(new SequentialEdge(st, ret));
			}

		cfg.simplify();
		return cfg;
	}

	private CFGDescriptor buildCFGDescriptor(FunctionDeclContext funcDecl, Unit unit) {
		String funcName = funcDecl.IDENTIFIER().getText();
		SignatureContext signature = funcDecl.signature();
		ParametersContext formalPars = signature.parameters();

		int line = getLine(signature);
		int col = getCol(signature);

		Parameter[] cfgArgs = new Parameter[] {};

		for (int i = 0; i < formalPars.parameterDecl().size(); i++)
			cfgArgs = ArrayUtils.addAll(cfgArgs, visitParameterDecl(formalPars.parameterDecl(i)));

		Type returnType = getGoReturnType(funcDecl.signature());

		CFGDescriptor descriptor = new CFGDescriptor(new SourceCodeLocation(file, line, col), unit, false,
				funcName,
				returnType, cfgArgs);

		return descriptor;
	}

	private CFGDescriptor buildCFGDescriptor(FunctionLitContext funcLit) {
		String funcName = "anonymousFunction" + c++;
		SignatureContext signature = funcLit.signature();
		ParametersContext formalPars = signature.parameters();

		int line = getLine(signature);
		int col = getCol(signature);

		Parameter[] cfgArgs = new Parameter[] {};

		for (int i = 0; i < formalPars.parameterDecl().size(); i++)
			cfgArgs = ArrayUtils.addAll(cfgArgs, visitParameterDecl(formalPars.parameterDecl(i)));

		return new CFGDescriptor(new SourceCodeLocation(file, line, col), program, false, funcName,
				getGoReturnType(funcLit.signature()), cfgArgs);
	}

	/**
	 * Given a signature context, returns the Go type corresponding to the
	 * return type of the signature. If the result type is missing (i.e. null)
	 * then the {@link Untyped} typed is returned.
	 * 
	 * @param signature the signature context
	 * 
	 * @return the Go type corresponding to the return type of {@code signature}
	 */
	private Type getGoReturnType(SignatureContext signature) {
		// The return type is not specified
		if (signature.result() == null)
			return Untyped.INSTANCE;
		return new GoCodeMemberVisitor(currentUnit, file, program, constants).visitResult(signature.result());
	}

	/**
	 * Visits the return type of a function.
	 * 
	 * @param ctx the function type context to visit
	 * 
	 * @return the return type
	 */
	public GoType visitFunctionType(FunctionTypeContext ctx) {
		SignatureContext sign = ctx.signature();
		Type returnType = getGoReturnType(sign);
		Parameter[] params = visitParameters(sign.parameters());

		return GoFunctionType.lookup(new GoFunctionType(returnType, params));
	}
}
