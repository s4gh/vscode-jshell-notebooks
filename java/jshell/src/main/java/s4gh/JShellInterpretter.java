package s4gh;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import jdk.jshell.*;
import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

class JShellInterpretter {

	private static final String NEW_LINE_HTML = System.lineSeparator();

	public static final String DISPLAY_RESULT = "displayResult";

	private static String SCRIPT_CHECK_FLINK = """
		show(isFlinkEnv());
	""";

	private static String SCRIPT_INIT = """
		import java.util.*;
		
		public static void show(Object o){
				s4gh.VariablesTransfer.setVariableValue("displayResult",  o);
		}
		
    	public static boolean isFlinkEnv() {
			try {
				Class.forName("org.apache.flink.table.api.Table");
			} catch (ClassNotFoundException e) {
				return false;
			}
			return true;
    	}
		""";

	private static String SCRIPT_INIT_FLINK = """
		
		public static void showTable(org.apache.flink.table.api.Table table){
			org.apache.flink.table.api.TableResult tableResult = table.limit(1000).execute();
	
			java.util.List<String> columNames = tableResult.getResolvedSchema().getColumnNames();
			java.util.List<java.util.Map<String, String>> tableData = new java.util.ArrayList<>();
			try (org.apache.flink.util.CloseableIterator<org.apache.flink.types.Row> iterator = tableResult.collect()) {
				while (iterator.hasNext()) {
					org.apache.flink.types.Row row = iterator.next();
					java.util.Map<String, String> rowData = new java.util.HashMap<>();
					for (String columnName : columNames) {
						String value = null;
						Object fieldValue = row.getField(columnName);
						if (fieldValue != null) {
							value = fieldValue.toString();
						}
						rowData.put(columnName, value);
					}
					tableData.add(rowData);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			s4gh.VariablesTransfer.setVariableValue("displayResult",  tableData);
		}
		""";

	private JShell jshell;
	private Set<String> classPathEntries = new HashSet<>();

	public JShellInterpretter() {
		jshell = createJShell();
		execute(SCRIPT_INIT);
	}

	private void checkAndPrepareFlink() {
		executeInJShell(SCRIPT_CHECK_FLINK);
		Object result = VariablesTransfer.getVariableValue("displayResult");
		if (result instanceof Boolean && (Boolean) result) {
			executeInJShell(SCRIPT_INIT_FLINK);
		} else if (result instanceof String && result != null && result.toString().equalsIgnoreCase("true")) {
			executeInJShell(SCRIPT_INIT_FLINK);
		}
	}

	private void executeInJShell(String script) {
		List<String> codeStatements = new ArrayList<>();
		splitIntoStatements(codeStatements, script);
		for (String statement : codeStatements) {
			List<SnippetEvent> evaluationResults = jshell.eval(statement);
			for (SnippetEvent event : evaluationResults) {
				if (event.status().equals(Snippet.Status.REJECTED)) {
					String errorMessage = extractUserFriendlyError(jshell, event);
					VariablesTransfer.setVariableValue(DISPLAY_RESULT, errorMessage);
				}
				else if (event.exception() != null) {
					JShellException exception = event.exception();
					String errorMessage = "Exception: " + exception.getMessage();
					if (exception instanceof EvalException) {
						errorMessage = errorMessage + " caused by " + ((EvalException)exception).getExceptionClassName();
					}
					if (exception.getCause() != null) {
						errorMessage = errorMessage + " cause: " + exception.getCause().getMessage();
					}
					errorMessage = errorMessage + " Stack trace:" + NEW_LINE_HTML + formatStackTrace(exception.getStackTrace());
					VariablesTransfer.setVariableValue(DISPLAY_RESULT, errorMessage);
				}
			}
		}
	}

	private void splitIntoStatements(List<String> statements, String code) {
		SourceCodeAnalysis.CompletionInfo completionInfo = jshell.sourceCodeAnalysis().analyzeCompletion(code);
		if (completionInfo.source() != null && completionInfo.source().equals(completionInfo.remaining())) {
			statements.add(code);
			return;
		}
		if (!completionInfo.completeness().isComplete()) {
			statements.add(code);
			return;
		}
		if (completionInfo.source() != null && !completionInfo.source().isEmpty()) {
			statements.add(completionInfo.source());
		}
		if (completionInfo.remaining() != null && !completionInfo.remaining().isEmpty()) {
			splitIntoStatements(statements, completionInfo.remaining());
		}
	}

	public List<String> generateCodeCompletion(String input) {
		SourceCodeAnalysis analysis = jshell.sourceCodeAnalysis();
		List<String> completions = new ArrayList<>();
		List<SourceCodeAnalysis.Suggestion> allSuggestions = analysis.completionSuggestions(input, input.length(), new int[] {0});
		List<SourceCodeAnalysis.Suggestion> suggestions = filterUniqueContinuations(allSuggestions);
		if (!suggestions.isEmpty()) {
			for (SourceCodeAnalysis.Suggestion suggestion : suggestions) {
				completions.add(suggestion.continuation());
			}
		}
		return completions;
	}

	public void execute(String script) {
		if (script != null && !script.isBlank() && script.trim().startsWith("/env --class-path")) {
			String classPathString = script;
			classPathString = classPathString.replaceFirst("/env --class-path", "").trim();
//			classPathString = "/home/serhiy/.m2/repository/org/apache/flink/flink-clients/1.20.0/flink-clients-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-core/1.20.0/flink-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-core-api/1.20.0/flink-core-api-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-metrics-core/1.20.0/flink-metrics-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-annotations/1.20.0/flink-annotations-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-asm-9/9.5-17.0/flink-shaded-asm-9-9.5-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-jackson/2.14.2-17.0/flink-shaded-jackson-2.14.2-17.0.jar:/home/serhiy/.m2/repository/org/snakeyaml/snakeyaml-engine/2.6/snakeyaml-engine-2.6.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-text/1.10.0/commons-text-1.10.0.jar:/home/serhiy/.m2/repository/com/esotericsoftware/kryo/kryo/2.24.0/kryo-2.24.0.jar:/home/serhiy/.m2/repository/com/esotericsoftware/minlog/minlog/1.2/minlog-1.2.jar:/home/serhiy/.m2/repository/org/objenesis/objenesis/2.1/objenesis-2.1.jar:/home/serhiy/.m2/repository/commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-compress/1.26.0/commons-compress-1.26.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-runtime/1.20.0/flink-runtime-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-rpc-core/1.20.0/flink-rpc-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-rpc-akka-loader/1.20.0/flink-rpc-akka-loader-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-queryable-state-client-java/1.20.0/flink-queryable-state-client-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-hadoop-fs/1.20.0/flink-hadoop-fs-1.20.0.jar:/home/serhiy/.m2/repository/commons-io/commons-io/2.15.1/commons-io-2.15.1.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-netty/4.1.91.Final-17.0/flink-shaded-netty-4.1.91.Final-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-zookeeper-3/3.7.1-17.0/flink-shaded-zookeeper-3-3.7.1-17.0.jar:/home/serhiy/.m2/repository/org/javassist/javassist/3.24.0-GA/javassist-3.24.0-GA.jar:/home/serhiy/.m2/repository/org/xerial/snappy/snappy-java/1.1.10.4/snappy-java-1.1.10.4.jar:/home/serhiy/.m2/repository/tools/profiler/async-profiler/2.9/async-profiler-2.9.jar:/home/serhiy/.m2/repository/org/lz4/lz4-java/1.8.0/lz4-java-1.8.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-optimizer/1.20.0/flink-optimizer-1.20.0.jar:/home/serhiy/.m2/repository/commons-cli/commons-cli/1.5.0/commons-cli-1.5.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-datastream/1.20.0/flink-datastream-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-datastream-api/1.20.0/flink-datastream-api-1.20.0.jar:/home/serhiy/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar:/home/serhiy/.m2/repository/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-java-bridge/1.20.0/flink-table-api-java-bridge-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-java/1.20.0/flink-table-api-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-bridge-base/1.20.0/flink-table-api-bridge-base-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-java/1.20.0/flink-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar:/home/serhiy/.m2/repository/com/twitter/chill-java/0.7.6/chill-java-0.7.6.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-streaming-java/1.20.0/flink-streaming-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-file-sink-common/1.20.0/flink-file-sink-common-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-guava/31.1-jre-17.0/flink-shaded-guava-31.1-jre-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-connector-datagen/1.20.0/flink-connector-datagen-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-runtime/1.20.0/flink-table-runtime-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-common/1.20.0/flink-table-common-1.20.0.jar:/home/serhiy/.m2/repository/com/ibm/icu/icu4j/67.1/icu4j-67.1.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-cep/1.20.0/flink-cep-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-connector-files/1.20.0/flink-connector-files-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-planner_2.12/1.20.0/flink-table-planner_2.12-1.20.0.jar:/home/serhiy/.m2/repository/org/immutables/value/2.8.8/value-2.8.8.jar:/home/serhiy/.m2/repository/org/immutables/value-annotations/2.8.8/value-annotations-2.8.8.jar:/home/serhiy/.m2/repository/org/codehaus/janino/commons-compiler/3.1.10/commons-compiler-3.1.10.jar:/home/serhiy/.m2/repository/org/codehaus/janino/janino/3.1.10/janino-3.1.10.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-scala_2.12/1.20.0/flink-scala_2.12-1.20.0.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-reflect/2.12.7/scala-reflect-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-library/2.12.7/scala-library-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-compiler/2.12.7/scala-compiler-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/modules/scala-xml_2.12/1.0.6/scala-xml_2.12-1.0.6.jar:/home/serhiy/.m2/repository/com/twitter/chill_2.12/0.7.6/chill_2.12-0.7.6.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-csv/1.20.0/flink-csv-1.20.0.jar";
			String[] jarPaths = classPathString.split(File.pathSeparator); //always use : instead of system's path separator
			for (int i = 0; i < jarPaths.length; i++) {
                if (!classPathEntries.contains(jarPaths[i])) {
                    classPathEntries.add(jarPaths[i]);
					jshell.addToClasspath(jarPaths[i]);
                }
            }
            checkAndPrepareFlink();
		} else {
			executeInJShell(script);
		}
	}

	private JShell createJShell() {

		final JShellInterpretter.AccessDirectExecutionControl accessDirectExecutionControl = new JShellInterpretter.AccessDirectExecutionControl();

		String currentClasspath = System.getProperty("java.class.path");
		String flinkJars = "/home/serhiy/.m2/repository/org/apache/flink/flink-clients/1.20.0/flink-clients-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-core/1.20.0/flink-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-core-api/1.20.0/flink-core-api-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-metrics-core/1.20.0/flink-metrics-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-annotations/1.20.0/flink-annotations-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-asm-9/9.5-17.0/flink-shaded-asm-9-9.5-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-jackson/2.14.2-17.0/flink-shaded-jackson-2.14.2-17.0.jar:/home/serhiy/.m2/repository/org/snakeyaml/snakeyaml-engine/2.6/snakeyaml-engine-2.6.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-text/1.10.0/commons-text-1.10.0.jar:/home/serhiy/.m2/repository/com/esotericsoftware/kryo/kryo/2.24.0/kryo-2.24.0.jar:/home/serhiy/.m2/repository/com/esotericsoftware/minlog/minlog/1.2/minlog-1.2.jar:/home/serhiy/.m2/repository/org/objenesis/objenesis/2.1/objenesis-2.1.jar:/home/serhiy/.m2/repository/commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-compress/1.26.0/commons-compress-1.26.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-runtime/1.20.0/flink-runtime-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-rpc-core/1.20.0/flink-rpc-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-rpc-akka-loader/1.20.0/flink-rpc-akka-loader-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-queryable-state-client-java/1.20.0/flink-queryable-state-client-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-hadoop-fs/1.20.0/flink-hadoop-fs-1.20.0.jar:/home/serhiy/.m2/repository/commons-io/commons-io/2.15.1/commons-io-2.15.1.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-netty/4.1.91.Final-17.0/flink-shaded-netty-4.1.91.Final-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-zookeeper-3/3.7.1-17.0/flink-shaded-zookeeper-3-3.7.1-17.0.jar:/home/serhiy/.m2/repository/org/javassist/javassist/3.24.0-GA/javassist-3.24.0-GA.jar:/home/serhiy/.m2/repository/org/xerial/snappy/snappy-java/1.1.10.4/snappy-java-1.1.10.4.jar:/home/serhiy/.m2/repository/tools/profiler/async-profiler/2.9/async-profiler-2.9.jar:/home/serhiy/.m2/repository/org/lz4/lz4-java/1.8.0/lz4-java-1.8.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-optimizer/1.20.0/flink-optimizer-1.20.0.jar:/home/serhiy/.m2/repository/commons-cli/commons-cli/1.5.0/commons-cli-1.5.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-datastream/1.20.0/flink-datastream-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-datastream-api/1.20.0/flink-datastream-api-1.20.0.jar:/home/serhiy/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar:/home/serhiy/.m2/repository/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-java-bridge/1.20.0/flink-table-api-java-bridge-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-java/1.20.0/flink-table-api-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-bridge-base/1.20.0/flink-table-api-bridge-base-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-java/1.20.0/flink-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar:/home/serhiy/.m2/repository/com/twitter/chill-java/0.7.6/chill-java-0.7.6.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-streaming-java/1.20.0/flink-streaming-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-file-sink-common/1.20.0/flink-file-sink-common-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-guava/31.1-jre-17.0/flink-shaded-guava-31.1-jre-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-connector-datagen/1.20.0/flink-connector-datagen-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-runtime/1.20.0/flink-table-runtime-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-common/1.20.0/flink-table-common-1.20.0.jar:/home/serhiy/.m2/repository/com/ibm/icu/icu4j/67.1/icu4j-67.1.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-cep/1.20.0/flink-cep-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-connector-files/1.20.0/flink-connector-files-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-planner_2.12/1.20.0/flink-table-planner_2.12-1.20.0.jar:/home/serhiy/.m2/repository/org/immutables/value/2.8.8/value-2.8.8.jar:/home/serhiy/.m2/repository/org/immutables/value-annotations/2.8.8/value-annotations-2.8.8.jar:/home/serhiy/.m2/repository/org/codehaus/janino/commons-compiler/3.1.10/commons-compiler-3.1.10.jar:/home/serhiy/.m2/repository/org/codehaus/janino/janino/3.1.10/janino-3.1.10.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-scala_2.12/1.20.0/flink-scala_2.12-1.20.0.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-reflect/2.12.7/scala-reflect-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-library/2.12.7/scala-library-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-compiler/2.12.7/scala-compiler-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/modules/scala-xml_2.12/1.0.6/scala-xml_2.12-1.0.6.jar:/home/serhiy/.m2/repository/com/twitter/chill_2.12/0.7.6/chill_2.12-0.7.6.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-csv/1.20.0/flink-csv-1.20.0.jar";
		if (currentClasspath != null && !currentClasspath.isBlank()) {
			flinkJars = currentClasspath + File.pathSeparator + flinkJars;
		}
//		String[] compilerArgs = new String[]{"--class-path", flinkJars};
		JShell.Builder builder = JShell.builder()
				.executionEngine(new JShellInterpretter.AccessDirectExecutionControlProvider(accessDirectExecutionControl), null)
//				.compilerOptions(compilerArgs)
				;

		JShell jshell = builder.build();
//		String[] jars = flinkJars.split(":");
//		for (int i = 0; i < jars.length; i++) {
//			jshell.addToClasspath(jars[i]);
//		}

		return jshell;
	}

	private String extractUserFriendlyError(JShell jshell, SnippetEvent event) {
		StringBuilder resultBuilder = new StringBuilder();
		String sourceCode = event.snippet().source();
		List<Diag> diagnostics = jshell.diagnostics(event.snippet()).toList();

		for (Diag diag : diagnostics) {
			String[] lines = sourceCode.split(System.lineSeparator());
			long errorPosition = diag.getPosition();

			// Find error line and column
			int currentPos = 0;
			int errorLine = 0;
			int errorColumn = 0;

			for (int i = 0; i < lines.length; i++) {
				if (errorPosition >= currentPos &&
						errorPosition < currentPos + lines[i].length() + 1) {
					errorLine = i;
					errorColumn = (int)(errorPosition - currentPos);
					break;
				}
				currentPos += lines[i].length() + 1;
			}

			// Build error message
			resultBuilder.append("Error: ")
					.append(diag.getMessage(Locale.ENGLISH))
					.append(NEW_LINE_HTML);

			resultBuilder.append("Line ")
					.append(errorLine + 1)
					.append(":" + NEW_LINE_HTML);

			if (errorLine < lines.length) {
				resultBuilder.append(lines[errorLine])
						.append(NEW_LINE_HTML);

				// Create pointer line with '^'
				StringBuilder pointer = new StringBuilder();
				for (int i = 0; i < errorColumn; i++) {
					pointer.append(' ');
				}
				pointer.append('^');
				resultBuilder.append(pointer)
						.append(NEW_LINE_HTML + NEW_LINE_HTML);
			}
		}

		return resultBuilder.toString();
	}

	private String formatStackTrace(StackTraceElement[] stackTrace) {
		if (stackTrace == null || stackTrace.length == 0) {
			return "No stack trace available";
		}

		StringBuilder result = new StringBuilder(NEW_LINE_HTML);

		// Add header
		result.append("Stack trace summary:" + NEW_LINE_HTML);

		for (int i = 0; i < stackTrace.length; i++) {
			StackTraceElement element = stackTrace[i];

			// Add numbering and indentation
			result.append(String.format("%2d) ", i + 1));

			// Add class and method
			String className = element.getClassName();
			String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
			result.append(simpleClassName)
					.append('.')
					.append(element.getMethodName());

			// Add file info if available
			String fileName = element.getFileName();
			int lineNumber = element.getLineNumber();
			if (fileName != null) {
				result.append(" (")
						.append(fileName);

				if (lineNumber >= 0) {
					result.append(':')
							.append(lineNumber);
				}

				result.append(')');
			}

			// Add native method indicator
			if (element.isNativeMethod()) {
				result.append(" [Native Method]");
			}

			result.append(NEW_LINE_HTML);
		}

		return result.toString();
	}

	private List<SourceCodeAnalysis.Suggestion> filterUniqueContinuations(List<SourceCodeAnalysis.Suggestion> suggestions) {
		return suggestions.stream()
				.collect(Collectors.groupingBy(
						SourceCodeAnalysis.Suggestion::continuation,
						Collectors.reducing((s1, s2) -> s1) // Keep the first suggestion for each continuation
				))
				.values()
				.stream()
				.filter(java.util.Optional::isPresent)
				.map(java.util.Optional::get)
				.collect(Collectors.toList());
	}

	public static class AccessDirectExecutionControl extends DirectExecutionControl {
		private Object lastValue;

		@Override
		protected String invoke(Method doitMethod) throws Exception {
			lastValue = doitMethod.invoke(null);
			return valueString(lastValue);
		}

		public Object getLastValue() {
			return lastValue;
		}
	}

	public static class AccessDirectExecutionControlProvider implements ExecutionControlProvider {
		private AccessDirectExecutionControl accessDirectExecutionControl;

		AccessDirectExecutionControlProvider(AccessDirectExecutionControl accessDirectExecutionControl) {
			this.accessDirectExecutionControl = accessDirectExecutionControl;
		}

		@Override
		public String name() {
			return "accessdirect";
		}

		@Override
		public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) throws Throwable {
			return accessDirectExecutionControl;
		}
	}
}