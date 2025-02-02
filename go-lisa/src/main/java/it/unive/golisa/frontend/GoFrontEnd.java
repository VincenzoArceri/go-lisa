package it.unive.golisa.frontend;

import it.unive.golisa.antlr.GoLexer;
import it.unive.golisa.antlr.GoParser;
import it.unive.golisa.antlr.GoParser.ConstDeclContext;
import it.unive.golisa.antlr.GoParser.ConstSpecContext;
import it.unive.golisa.antlr.GoParser.DeclarationContext;
import it.unive.golisa.antlr.GoParser.ExpressionContext;
import it.unive.golisa.antlr.GoParser.ExpressionListContext;
import it.unive.golisa.antlr.GoParser.FunctionDeclContext;
import it.unive.golisa.antlr.GoParser.IdentifierListContext;
import it.unive.golisa.antlr.GoParser.ImportDeclContext;
import it.unive.golisa.antlr.GoParser.ImportPathContext;
import it.unive.golisa.antlr.GoParser.ImportSpecContext;
import it.unive.golisa.antlr.GoParser.MethodDeclContext;
import it.unive.golisa.antlr.GoParser.PackageClauseContext;
import it.unive.golisa.antlr.GoParser.SourceFileContext;
import it.unive.golisa.antlr.GoParser.String_Context;
import it.unive.golisa.antlr.GoParser.TypeDeclContext;
import it.unive.golisa.antlr.GoParser.TypeSpecContext;
import it.unive.golisa.antlr.GoParserBaseVisitor;
import it.unive.golisa.cfg.runtime.conversion.GoToString;
import it.unive.golisa.cfg.runtime.conversion.ToInt64;
import it.unive.golisa.cfg.type.GoBoolType;
import it.unive.golisa.cfg.type.GoNilType;
import it.unive.golisa.cfg.type.GoStringType;
import it.unive.golisa.cfg.type.composite.GoArrayType;
import it.unive.golisa.cfg.type.composite.GoChannelType;
import it.unive.golisa.cfg.type.composite.GoErrorType;
import it.unive.golisa.cfg.type.composite.GoFunctionType;
import it.unive.golisa.cfg.type.composite.GoInterfaceType;
import it.unive.golisa.cfg.type.composite.GoMapType;
import it.unive.golisa.cfg.type.composite.GoPointerType;
import it.unive.golisa.cfg.type.composite.GoSliceType;
import it.unive.golisa.cfg.type.composite.GoStructType;
import it.unive.golisa.cfg.type.composite.GoTupleType;
import it.unive.golisa.cfg.type.composite.GoVariadicType;
import it.unive.golisa.cfg.type.numeric.floating.GoFloat32Type;
import it.unive.golisa.cfg.type.numeric.floating.GoFloat64Type;
import it.unive.golisa.cfg.type.numeric.signed.GoInt16Type;
import it.unive.golisa.cfg.type.numeric.signed.GoInt32Type;
import it.unive.golisa.cfg.type.numeric.signed.GoInt64Type;
import it.unive.golisa.cfg.type.numeric.signed.GoInt8Type;
import it.unive.golisa.cfg.type.numeric.signed.GoIntType;
import it.unive.golisa.cfg.type.numeric.unsigned.GoUInt16Type;
import it.unive.golisa.cfg.type.numeric.unsigned.GoUInt32Type;
import it.unive.golisa.cfg.type.numeric.unsigned.GoUInt64Type;
import it.unive.golisa.cfg.type.numeric.unsigned.GoUInt8Type;
import it.unive.golisa.cfg.type.untyped.GoUntypedFloat;
import it.unive.golisa.cfg.type.untyped.GoUntypedInt;
import it.unive.golisa.golang.util.GoLangAPISignatureMapper;
import it.unive.golisa.golang.util.GoLangUtils;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.assignment.OrderPreservingAssigningStrategy;
import it.unive.lisa.program.cfg.statement.call.assignment.ParameterAssigningStrategy;
import it.unive.lisa.program.cfg.statement.call.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.cfg.statement.call.resolution.RuntimeTypesMatchingStrategy;
import it.unive.lisa.program.cfg.statement.call.traversal.HierarcyTraversalStrategy;
import it.unive.lisa.program.cfg.statement.call.traversal.SingleInheritanceTraversalStrategy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class manages the translation from a Go program to the corresponding
 * LiSA {@link CFG}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class GoFrontEnd extends GoParserBaseVisitor<Object> implements GoRuntimeLoader {

	private static final Logger log = LogManager.getLogger(GoFrontEnd.class);

	private final String filePath;

	private final Program program;

	private Map<String, ExpressionContext> constants;

	private GoLangAPISignatureMapper mapper = GoLangAPISignatureMapper.getGoApiSignatures();

	private CompilationUnit packageUnit;

	/**
	 * The parameter assigning strategy for calls.
	 */
	public static final ParameterAssigningStrategy PARAMETER_ASSIGN_STRATEGY = OrderPreservingAssigningStrategy.INSTANCE;

	/**
	 * The strategy of traversing super-unit to search for target call
	 * implementation.
	 */
	public static final HierarcyTraversalStrategy HIERARCY_TRAVERSAL_STRATEGY = SingleInheritanceTraversalStrategy.INSTANCE;

	/**
	 * The parameter matching strategy for matching function calls.
	 */
	public static final ParameterMatchingStrategy FUNCTION_MATCHING_STRATEGY = RuntimeTypesMatchingStrategy.INSTANCE;

	/**
	 * The parameter matching strategy for matching method calls.
	 */
	public static final ParameterMatchingStrategy METHOD_MATCHING_STRATEGY = RuntimeTypesMatchingStrategy.INSTANCE;

	/**
	 * Builds a Go frontend for a given Go program given at the location
	 * {@code filePath}.
	 * 
	 * @param filePath file path to a Go program.
	 */
	private GoFrontEnd(String filePath) {
		this.filePath = filePath;
		this.program = new Program();
		this.constants = new HashMap<>();
		GoCodeMemberVisitor.c = 0;
	}

	/**
	 * Returns the parsed file path.
	 * 
	 * @return the parsed file path
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Processes the Go program located at {@code filePath} and returns the LiSA
	 * program corresponding to the parsed file.
	 * 
	 * @param filePath the file path
	 * 
	 * @return the LiSA program corresponding to the parsed file
	 * 
	 * @throws IOException if something wrong happens while reading the file
	 */
	public static Program processFile(String filePath) throws IOException {
		return new GoFrontEnd(filePath).toLiSAProgram();
	}

	/**
	 * Returns a {@link Program} corresponding to the Go program located to
	 * {@code filePath}.
	 * 
	 * @return a {@link Program} corresponding to the Go program located to
	 *             {@code filePath}
	 * 
	 * @throws IOException if something wrong happens while reading the file
	 */
	private Program toLiSAProgram() throws IOException {
		log.info("Go front-end setup...");
		log.info("Reading file... " + filePath);

		clearTypes();

		long start = System.currentTimeMillis();

		InputStream stream = new FileInputStream(getFilePath());

		log.info("LOCS: " + Files.lines(Paths.get(getFilePath())).count());

		GoLexer lexer = new GoLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
		GoParser parser = new GoParser(new CommonTokenStream(lexer));
		parser.setErrorHandler(new BailErrorStrategy());

		ParseTree tree = parser.sourceFile();
		long parsingTime = System.currentTimeMillis();

		Program result = visitSourceFile((SourceFileContext) tree);

		log.info("PARSING TIME: " + (parsingTime - start) + " CFG time: " + (System.currentTimeMillis() - parsingTime));

		stream.close();

		program.addCompilationUnit(it.unive.golisa.golang.runtime.EmptyInterface.INSTANCE);

		// Register all the types
		registerGoTypes(program);

		return result;
	}

	private void clearTypes() {
		GoArrayType.clearAll();
		GoStructType.clearAll();
		GoSliceType.clearAll();
		GoPointerType.clearAll();
		GoMapType.clearAll();
		GoTupleType.clearAll();
		GoChannelType.clearAll();
		GoFunctionType.clearAll();
		GoVariadicType.clearAll();
	}

	private void registerGoTypes(Program program) {
		program.registerType(GoBoolType.INSTANCE);
		program.registerType(GoFloat32Type.INSTANCE);
		program.registerType(GoFloat64Type.INSTANCE);
		program.registerType(GoIntType.INSTANCE);
		program.registerType(GoUntypedInt.INSTANCE);
		program.registerType(GoUntypedFloat.INSTANCE);
		program.registerType(GoInt8Type.INSTANCE);
		program.registerType(GoUInt8Type.INSTANCE);
		program.registerType(GoInt16Type.INSTANCE);
		program.registerType(GoUInt16Type.INSTANCE);
		program.registerType(GoInt32Type.INSTANCE);
		program.registerType(GoUInt32Type.INSTANCE);
		program.registerType(GoInt64Type.INSTANCE);
		program.registerType(GoUInt64Type.INSTANCE);
		program.registerType(GoUntypedFloat.INSTANCE);
		program.registerType(GoStringType.INSTANCE);
		program.registerType(GoErrorType.INSTANCE);
		program.registerType(GoNilType.INSTANCE);
		GoArrayType.all().forEach(program::registerType);
		GoStructType.all().forEach(program::registerType);
		GoSliceType.all().forEach(program::registerType);
		GoPointerType.all().forEach(program::registerType);
		GoMapType.all().forEach(program::registerType);
		GoTupleType.all().forEach(program::registerType);
		GoChannelType.all().forEach(program::registerType);
		GoFunctionType.all().forEach(program::registerType);
		GoVariadicType.all().forEach(program::registerType);
	}

	@Override
	public Program visitSourceFile(SourceFileContext ctx) {
		String packageName = visitPackageClause(ctx.packageClause());

		packageUnit = new CompilationUnit(new SourceCodeLocation(filePath, 0, 0), packageName, false);
		program.addCompilationUnit(packageUnit);

		GoInterfaceType.lookup("EMPTY_INTERFACE", packageUnit);

		for (ImportDeclContext imp : ctx.importDecl())
			visitImportDecl(imp);

		loadCore();

		for (DeclarationContext decl : IterationLogger.iterate(log, ctx.declaration(), "Parsing global declarations...",
				"Global declarations"))
			visitDeclarationContext(decl);

		updateUnitReferences();

		for (MethodDeclContext decl : IterationLogger.iterate(log, ctx.methodDecl(), "Parsing method declarations...",
				"Method declarations"))
			visitMethodDecl(decl);

		// method declaration must be linked to compilation unit of a
		// declaration context, for the function declaration is not needed
		// Visit of each FunctionDeclContext populating the corresponding cfg
		for (FunctionDeclContext funcDecl : IterationLogger.iterate(log, ctx.functionDecl(),
				"Visiting function declarations...", "Function declarations"))
			visitFunctionDecl(funcDecl);

		return program;
	}

	private void updateUnitReferences() {
		for (CompilationUnit unit : program.getUnits())
			GoStructType.updateReference(unit.getName(), unit);
	}

	private void visitDeclarationContext(DeclarationContext decl) {
		if (decl.typeDecl() != null)
			for (CompilationUnit unit : visitTypeDecl(decl.typeDecl()))
				program.addCompilationUnit(unit);

		if (decl.constDecl() != null)
			visitConstDeclContext(decl.constDecl());

	}

	private void visitConstDeclContext(ConstDeclContext ctx) {
		for (ConstSpecContext constSpec : ctx.constSpec())
			visitConstSpecContext(constSpec);
	}

	private void visitConstSpecContext(ConstSpecContext ctx) {
		IdentifierListContext ids = ctx.identifierList();
		ExpressionListContext exps = ctx.expressionList();

		for (int i = 0; i < ids.IDENTIFIER().size(); i++)
			if (exps != null && exps.expression(i) != null)
				constants.put(ids.IDENTIFIER(i).getText(), exps.expression(i));
	}

	@Override
	public Collection<CompilationUnit> visitTypeDecl(TypeDeclContext ctx) {
		HashSet<CompilationUnit> units = new HashSet<>();
		for (TypeSpecContext typeSpec : ctx.typeSpec()) {
			String unitName = typeSpec.IDENTIFIER().getText();
			CompilationUnit unit = new CompilationUnit(
					new SourceCodeLocation(filePath, GoCodeMemberVisitor.getLine(typeSpec),
							GoCodeMemberVisitor.getCol(typeSpec)),
					unitName, false);
			units.add(unit);
			new GoTypeVisitor(filePath, unit, program, constants).visitTypeSpec(typeSpec);
		}
		return units;
	}

	@Override
	public String visitPackageClause(PackageClauseContext ctx) {
		return ctx.IDENTIFIER().getText();
	}

	@Override
	public Statement visitImportDecl(ImportDeclContext ctx) {
		for (ImportSpecContext impSpec : ctx.importSpec())
			visitImportSpec(impSpec);
		return null;
	}

	@Override
	public Statement visitImportSpec(ImportSpecContext ctx) {
		return visitImportPath(ctx.importPath());
	}

	@Override
	public String visitString_(String_Context ctx) {
		return ctx.getText().substring(1, ctx.getText().length() - 1);
	}

	@Override
	public Statement visitImportPath(ImportPathContext ctx) {
		String module = visitString_(ctx.string_());
		File moduleDirectory = new File(new File(filePath).getParent(), module);

		if (moduleDirectory.exists() && moduleDirectory.isDirectory()) {
			File[] listOfFiles = moduleDirectory.listFiles();

			for (int i = 0; i < listOfFiles.length; i++)
				if (listOfFiles[i].getName().endsWith(".go"))
					try {
						Program moduleProgram = GoFrontEnd.processFile(listOfFiles[i].toString());

						for (CompilationUnit cu : moduleProgram.getUnits())
							program.addCompilationUnit(cu);

					} catch (IOException e) {
						throw new RuntimeException("Cannot find package + " + listOfFiles[i]);
					}
		} else
			loadRuntime(module, program, mapper);
		return null;
	}

	private void loadCore() {
		SourceCodeLocation unknownLocation = new SourceCodeLocation(GoLangUtils.GO_RUNTIME_SOURCE, 0, 0);
		packageUnit.addConstruct(new GoToString(unknownLocation, packageUnit));
		packageUnit.addConstruct(new ToInt64(unknownLocation, packageUnit));
	}

	@Override
	public Pair<Statement, Statement> visitFunctionDecl(FunctionDeclContext ctx) {
		return new GoFunctionVisitor(ctx, packageUnit, filePath, program, constants).visitFunctionDecl(ctx);
	}

	@Override
	public CFG visitMethodDecl(MethodDeclContext ctx) {
		return new GoCodeMemberVisitor(packageUnit, ctx, filePath, program, constants).visitCodeMember(ctx);
	}

}