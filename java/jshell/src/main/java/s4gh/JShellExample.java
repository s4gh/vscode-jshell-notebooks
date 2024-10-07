package s4gh;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.execution.LocalExecutionControlProvider;
import jdk.jshell.spi.ExecutionControlProvider;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JShellExample {
    public static void main(String[] args) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        JShell jshell = createJShell(ps);

        String body = """
int a = 1;
System.out.println(1
int b = 2;
                """;
        List<String> codeStatements = new ArrayList<>();
        splitIntoStatements(codeStatements, body, jshell);

        for (String statement : codeStatements) {
            baos.reset();
            List<SnippetEvent> evaluationResults = jshell.eval(statement);
            System.out.println("++++++++++++++++++++");
            System.out.println(baos.toString());
            System.out.println("++++++++++++++++++++");
            for (SnippetEvent event : evaluationResults) {
                if (event.exception() != null) {
                    System.out.println("==============");
                    System.out.println(event.causeSnippet());
                    event.exception().printStackTrace();
                    System.out.println("==============");
                }
            }
        }

        System.out.println("============== " + VariablesTransfer.getVariableValue("displayResult"));
    }

    private static JShell recreateJShell(JShell jshell) {
        // Save the current state
        List<Snippet> snippets = jshell.snippets().collect(Collectors.toList());

        // Close the current JShell
        jshell.close();

        // Create a new JShell with updated classpath
        JShell newJshell = createJShell(System.out);

        // Replay the saved snippets
        for (Snippet snippet : snippets) {
            System.out.println("apply " + snippet.source());
            newJshell.eval(snippet.source());
        }
        return newJshell;
    }

    private static JShell createJShell(PrintStream ps) {
        String currentClasspath = System.getProperty("java.class.path");
        String flinkJars = "/home/serhiy/.m2/repository/org/apache/flink/flink-clients/1.20.0/flink-clients-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-core/1.20.0/flink-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-core-api/1.20.0/flink-core-api-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-metrics-core/1.20.0/flink-metrics-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-annotations/1.20.0/flink-annotations-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-asm-9/9.5-17.0/flink-shaded-asm-9-9.5-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-jackson/2.14.2-17.0/flink-shaded-jackson-2.14.2-17.0.jar:/home/serhiy/.m2/repository/org/snakeyaml/snakeyaml-engine/2.6/snakeyaml-engine-2.6.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-text/1.10.0/commons-text-1.10.0.jar:/home/serhiy/.m2/repository/com/esotericsoftware/kryo/kryo/2.24.0/kryo-2.24.0.jar:/home/serhiy/.m2/repository/com/esotericsoftware/minlog/minlog/1.2/minlog-1.2.jar:/home/serhiy/.m2/repository/org/objenesis/objenesis/2.1/objenesis-2.1.jar:/home/serhiy/.m2/repository/commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-compress/1.26.0/commons-compress-1.26.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-runtime/1.20.0/flink-runtime-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-rpc-core/1.20.0/flink-rpc-core-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-rpc-akka-loader/1.20.0/flink-rpc-akka-loader-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-queryable-state-client-java/1.20.0/flink-queryable-state-client-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-hadoop-fs/1.20.0/flink-hadoop-fs-1.20.0.jar:/home/serhiy/.m2/repository/commons-io/commons-io/2.15.1/commons-io-2.15.1.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-netty/4.1.91.Final-17.0/flink-shaded-netty-4.1.91.Final-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-zookeeper-3/3.7.1-17.0/flink-shaded-zookeeper-3-3.7.1-17.0.jar:/home/serhiy/.m2/repository/org/javassist/javassist/3.24.0-GA/javassist-3.24.0-GA.jar:/home/serhiy/.m2/repository/org/xerial/snappy/snappy-java/1.1.10.4/snappy-java-1.1.10.4.jar:/home/serhiy/.m2/repository/tools/profiler/async-profiler/2.9/async-profiler-2.9.jar:/home/serhiy/.m2/repository/org/lz4/lz4-java/1.8.0/lz4-java-1.8.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-optimizer/1.20.0/flink-optimizer-1.20.0.jar:/home/serhiy/.m2/repository/commons-cli/commons-cli/1.5.0/commons-cli-1.5.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-datastream/1.20.0/flink-datastream-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-datastream-api/1.20.0/flink-datastream-api-1.20.0.jar:/home/serhiy/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar:/home/serhiy/.m2/repository/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-java-bridge/1.20.0/flink-table-api-java-bridge-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-java/1.20.0/flink-table-api-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-api-bridge-base/1.20.0/flink-table-api-bridge-base-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-java/1.20.0/flink-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar:/home/serhiy/.m2/repository/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar:/home/serhiy/.m2/repository/com/twitter/chill-java/0.7.6/chill-java-0.7.6.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-streaming-java/1.20.0/flink-streaming-java-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-file-sink-common/1.20.0/flink-file-sink-common-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-shaded-guava/31.1-jre-17.0/flink-shaded-guava-31.1-jre-17.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-connector-datagen/1.20.0/flink-connector-datagen-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-runtime/1.20.0/flink-table-runtime-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-common/1.20.0/flink-table-common-1.20.0.jar:/home/serhiy/.m2/repository/com/ibm/icu/icu4j/67.1/icu4j-67.1.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-cep/1.20.0/flink-cep-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-connector-files/1.20.0/flink-connector-files-1.20.0.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-table-planner_2.12/1.20.0/flink-table-planner_2.12-1.20.0.jar:/home/serhiy/.m2/repository/org/immutables/value/2.8.8/value-2.8.8.jar:/home/serhiy/.m2/repository/org/immutables/value-annotations/2.8.8/value-annotations-2.8.8.jar:/home/serhiy/.m2/repository/org/codehaus/janino/commons-compiler/3.1.10/commons-compiler-3.1.10.jar:/home/serhiy/.m2/repository/org/codehaus/janino/janino/3.1.10/janino-3.1.10.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-scala_2.12/1.20.0/flink-scala_2.12-1.20.0.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-reflect/2.12.7/scala-reflect-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-library/2.12.7/scala-library-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/scala-compiler/2.12.7/scala-compiler-2.12.7.jar:/home/serhiy/.m2/repository/org/scala-lang/modules/scala-xml_2.12/1.0.6/scala-xml_2.12-1.0.6.jar:/home/serhiy/.m2/repository/com/twitter/chill_2.12/0.7.6/chill_2.12-0.7.6.jar:/home/serhiy/.m2/repository/org/apache/flink/flink-csv/1.20.0/flink-csv-1.20.0.jar";
        if (currentClasspath != null && !currentClasspath.isBlank()) {
            flinkJars = currentClasspath + ":" + flinkJars;
        }
        String[] compilerArgs = new String[]{"--class-path", flinkJars};
        ExecutionControlProvider executionProvider = new LocalExecutionControlProvider();
        final JShellInterpretter.AccessDirectExecutionControl accessDirectExecutionControl = new JShellInterpretter.AccessDirectExecutionControl();
        String[] jars = flinkJars.split(":");
        JShell jshell =  JShell.builder()
//                .executionEngine(executionProvider, null)
                .executionEngine(new JShellInterpretter.AccessDirectExecutionControlProvider(accessDirectExecutionControl), null)
//                .compilerOptions(compilerArgs)
//                .out(System.out)
                .out(ps)
                .build();
        for (int i = 0; i < jars.length; i++) {
            jshell.addToClasspath(jars[i]);
        }
        return jshell;
    }

    private static void splitIntoStatements(List<String> statements, String code, JShell jshell) {
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
            splitIntoStatements(statements, completionInfo.remaining(), jshell);
        }
    }
}

