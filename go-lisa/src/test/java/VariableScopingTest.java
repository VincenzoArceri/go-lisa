
import static it.unive.lisa.LiSAFactory.getDefaultFor;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import it.unive.golisa.cfg.VariableScopingCFG;
import it.unive.golisa.frontend.GoFrontEnd;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.cfg.CFG;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;

public class VariableScopingTest extends GoAnalysisTestExecutor {

	private static CompilationUnit findUnit(Program prog, String name) {
		CompilationUnit unit = prog.getUnits().stream().filter(u -> u.getName().equals(name)).findFirst().get();
		assertNotNull("'" + name + "' unit not found", unit);
		return unit;
	}

	private static CFG findCFG(CompilationUnit unit, String name) {
		CFG cfg = unit.getCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst().get();
		assertNotNull("'" + unit.getName() + "' unit does not contain cfg '" + name + "'", cfg);
		return cfg;
	}

	@Test
	public void testSingle() throws IOException, ProgramValidationException {
		Program prog = GoFrontEnd.processFile("go-testcases/variablescoping/scoping.go");
		prog.validateAndFinalize();
		// we just check that no exception is thrown
	}

	@Ignore
	public void testForLoopVariableScoping() throws IOException, ProgramValidationException {
		assertTrue((new File("go-testcases/variablescoping/scoping.go")).exists());
		Program prog = GoFrontEnd.processFile("go-testcases/variablescoping/scoping.go");
		prog.validateAndFinalize();

		CompilationUnit main = findUnit(prog, "main");

		CFG test = findCFG(main, "test");

		assertTrue(test instanceof VariableScopingCFG);
		VariableScopingCFG vscfg_test = (VariableScopingCFG) test;

		Map<String, Set<String>> currentResults = new HashMap<>();
		vscfg_test.getNodes().forEach(
				node -> currentResults.put(node.toString(), new HashSet<>(vscfg_test.getVisibleIds(node).keySet())));

		Map<String, Set<String>> expectedResult = expectedResultForLoopVariableScooping();

		assertTrue(compareResults(currentResults, expectedResult));
	}

	@Test
	public void shadowingTest() throws IOException, AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.setJsonOutput(true)

				.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Interval(),
						new InferredTypes()))
				.setDumpAnalysis(true);

		perform("variablescoping", "shadowing.go", conf);
	}

	private Map<String, Set<String>> expectedResultForLoopVariableScooping() {

		Map<String, Set<String>> expectedResult = new HashMap<>();

		expectedResult.put("a := 0", Set.of("a", "b"));
		expectedResult.put("sum := 0", Set.of("a", "b", "sum"));
		expectedResult.put("i := 0", Set.of("a", "b", "sum", "i"));
		expectedResult.put("<(i, 10)", Set.of("a", "b", "sum", "i"));
		expectedResult.put("i = +(i, 1)", Set.of("a", "b", "sum", "i"));
		expectedResult.put("sum = +(sum, i)", Set.of("a", "b", "sum", "i"));
		expectedResult.put("b = +(b, 1)", Set.of("a", "b", "sum", "i"));
		expectedResult.put("c := a", Set.of("a", "b", "sum", "i", "c"));
		expectedResult.put("c = +(c, b)", Set.of("a", "b", "sum", "i", "c"));
		expectedResult.put("a = +(a, b)", Set.of("a", "b", "sum", "i", "c"));
		expectedResult.put("==(a, i)", Set.of("a", "b", "sum", "i", "c"));
		expectedResult.put("d := 9", Set.of("a", "b", "sum", "i", "c", "d"));
		expectedResult.put("j := 2", Set.of("a", "b", "sum", "i", "c", "d", "j"));
		expectedResult.put("<(j, 4)", Set.of("a", "b", "sum", "i", "c", "d", "j"));
		expectedResult.put("j = +(j, 1)", Set.of("a", "b", "sum", "i", "c", "d", "j"));
		expectedResult.put("d = +(d, j)", Set.of("a", "b", "sum", "i", "c", "d", "j"));
		expectedResult.put("a = +(d, 4)", Set.of("a", "b", "sum", "i", "c", "d"));
		expectedResult.put("e := 8", Set.of("a", "b", "sum", "i", "c", "e"));
		expectedResult.put("b = +(e, 3)", Set.of("a", "b", "sum", "i", "c", "e"));
		expectedResult.put("a = +(b, 2)", Set.of("a", "b", "sum"));
		expectedResult.put("ret", Set.of("a", "b", "sum"));

		return expectedResult;
	}

	private boolean compareResults(Map<String, Set<String>> currentResults, Map<String, Set<String>> expectedResult) {

		if (!currentResults.keySet().containsAll(expectedResult.keySet())
				|| !expectedResult.keySet().containsAll(currentResults.keySet()))
			return false;

		for (String key : currentResults.keySet())
			if (!currentResults.get(key).containsAll(expectedResult.get(key))
					|| !expectedResult.get(key).containsAll(currentResults.get(key)))
				return false;

		return true;
	}

}