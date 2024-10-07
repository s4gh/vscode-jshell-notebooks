package s4gh;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpPost {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("start execution");

        String bodyPrepare = "/env --class-path /home/serhiy/node/flink-libs1/flink-rpc-core-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-connector-files-1.19.0.jar:/home/serhiy/node/flink-libs1/scala-compiler-2.12.7.jar:/home/serhiy/node/flink-libs1/flink-scala_2.12-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-csv-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-shaded-jackson-2.14.2-17.0.jar:/home/serhiy/node/flink-libs1/flink-metrics-core-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-java-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-rpc-akka-loader-1.19.0.jar:/home/serhiy/node/flink-libs1/commons-cli-1.5.0.jar:/home/serhiy/node/flink-libs1/flink-table-api-bridge-base-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-core-1.19.0.jar:/home/serhiy/node/flink-libs1/commons-io-2.11.0.jar:/home/serhiy/node/flink-libs1/objenesis-2.1.jar:/home/serhiy/node/flink-libs1/flink-shaded-guava-31.1-jre-17.0.jar:/home/serhiy/node/flink-libs1/flink-cep-1.19.0.jar:/home/serhiy/node/flink-libs1/commons-compiler-3.1.10.jar:/home/serhiy/node/flink-libs1/flink-table-api-java-1.19.0.jar:/home/serhiy/node/flink-libs1/scala-library-2.12.7.jar:/home/serhiy/node/flink-libs1/janino-3.1.10.jar:/home/serhiy/node/flink-libs1/snakeyaml-engine-2.6.jar:/home/serhiy/node/flink-libs1/commons-lang3-3.12.0.jar:/home/serhiy/node/flink-libs1/flink-clients-1.19.0.jar:/home/serhiy/node/flink-libs1/scala-reflect-2.12.7.jar:/home/serhiy/node/flink-libs1/value-annotations-2.8.8.jar:/home/serhiy/node/flink-libs1/flink-file-sink-common-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-shaded-zookeeper-3-3.7.1-17.0.jar:/home/serhiy/node/flink-libs1/snappy-java-1.1.10.4.jar:/home/serhiy/node/flink-libs1/commons-math3-3.6.1.jar:/home/serhiy/node/flink-libs1/commons-text-1.10.0.jar:/home/serhiy/node/flink-libs1/slf4j-api-1.7.36.jar:/home/serhiy/node/flink-libs1/flink-annotations-1.19.0.jar:/home/serhiy/node/flink-libs1/minlog-1.2.jar:/home/serhiy/node/flink-libs1/jsr305-1.3.9.jar:/home/serhiy/node/flink-libs1/flink-shaded-netty-4.1.91.Final-17.0.jar:/home/serhiy/node/flink-libs1/flink-hadoop-fs-1.19.0.jar:/home/serhiy/node/flink-libs1/value-2.8.8.jar:/home/serhiy/node/flink-libs1/icu4j-67.1.jar:/home/serhiy/node/flink-libs1/flink-shaded-asm-9-9.5-17.0.jar:/home/serhiy/node/flink-libs1/scala-xml_2.12-1.0.6.jar:/home/serhiy/node/flink-libs1/flink-table-planner_2.12-1.19.0.jar:/home/serhiy/node/flink-libs1/async-profiler-2.9.jar:/home/serhiy/node/flink-libs1/commons-collections-3.2.2.jar:/home/serhiy/node/flink-libs1/javassist-3.24.0-GA.jar:/home/serhiy/node/flink-libs1/flink-table-runtime-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-streaming-java-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-queryable-state-client-java-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-runtime-1.19.0.jar:/home/serhiy/node/flink-libs1/commons-compress-1.24.0.jar:/home/serhiy/node/flink-libs1/chill-java-0.7.6.jar:/home/serhiy/node/flink-libs1/flink-table-common-1.19.0.jar:/home/serhiy/node/flink-libs1/lz4-java-1.8.0.jar:/home/serhiy/node/flink-libs1/kryo-2.24.0.jar:/home/serhiy/node/flink-libs1/flink-connector-datagen-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-optimizer-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-table-api-java-bridge-1.19.0.jar:/home/serhiy/node/flink-libs1/chill_2.12-0.7.6.jar";
//        String bodyPrepare = "";

        String body = """
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.bridge.java.internal.StreamTableEnvironmentImpl;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;

import java.util.HashMap;
import java.util.List;

        Configuration conf = new Configuration();
        StreamExecutionEnvironment senv = StreamExecutionEnvironment.createRemoteEnvironment(
                "127.0.0.1",
                8081,
                null);
        StreamTableEnvironment tableEnv = StreamTableEnvironmentImpl.create(senv, EnvironmentSettings.newInstance().withConfiguration(conf).build());


        DataType rowType = DataTypes.ROW(
                DataTypes.FIELD("NAME", DataTypes.STRING()),
                DataTypes.FIELD("ADDRESS", DataTypes.STRING())
        );

        Table sampleTable = tableEnv.fromValues(
                rowType,
                Row.of("Alice Johnson", "123 Main St, Anytown"),
                Row.of("Bob Smith", "456 Oak Ave, Somewhere"),
                Row.of("Charlie Brown", "789 Pine Rd, Elsewhere")
        );

        tableEnv.createTemporaryView("people", sampleTable);

        Table result = tableEnv.from("people");
        
        result.execute().print();
        System.out.println("-------------------");
        showFlink(result);
        System.out.println("-------------------");
                """;
        HttpRequest.BodyPublisher preparePublisher = HttpRequest.BodyPublishers.ofString(bodyPrepare);
        HttpRequest.BodyPublisher executePublisher = HttpRequest.BodyPublishers.ofString(body);

//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("http://localhost:8080/execute"))
//                .header("content-type", "application/text")
//                .header("Authorization", "test")
//                .method("POST", preparePublisher)
//                .build();
//        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
//        System.out.println("============ prepare =================");
//        System.out.println(response.body());
        System.out.println("=============================");
        HttpRequest requestSuggest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/execute"))
                .header("content-type", "application/text")
                .header("Authorization", "test")
                .method("POST", executePublisher)
                .build();
        HttpResponse<String> responseSuggest = HttpClient.newHttpClient().send(requestSuggest, HttpResponse.BodyHandlers.ofString());
        System.out.println(responseSuggest.body());
        System.out.println("=============================");
    }
}
