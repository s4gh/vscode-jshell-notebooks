# mvn dependency:build-classpath
# /home/serhiy/node/flink-libs1/flink-rpc-core-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-connector-files-1.19.0.jar:/home/serhiy/node/flink-libs1/scala-compiler-2.12.7.jar:/home/serhiy/node/flink-libs1/flink-scala_2.12-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-csv-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-shaded-jackson-2.14.2-17.0.jar:/home/serhiy/node/flink-libs1/flink-metrics-core-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-java-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-rpc-akka-loader-1.19.0.jar:/home/serhiy/node/flink-libs1/commons-cli-1.5.0.jar:/home/serhiy/node/flink-libs1/flink-table-api-bridge-base-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-core-1.19.0.jar:/home/serhiy/node/flink-libs1/commons-io-2.11.0.jar:/home/serhiy/node/flink-libs1/objenesis-2.1.jar:/home/serhiy/node/flink-libs1/flink-shaded-guava-31.1-jre-17.0.jar:/home/serhiy/node/flink-libs1/flink-cep-1.19.0.jar:/home/serhiy/node/flink-libs1/commons-compiler-3.1.10.jar:/home/serhiy/node/flink-libs1/flink-table-api-java-1.19.0.jar:/home/serhiy/node/flink-libs1/scala-library-2.12.7.jar:/home/serhiy/node/flink-libs1/janino-3.1.10.jar:/home/serhiy/node/flink-libs1/snakeyaml-engine-2.6.jar:/home/serhiy/node/flink-libs1/commons-lang3-3.12.0.jar:/home/serhiy/node/flink-libs1/flink-clients-1.19.0.jar:/home/serhiy/node/flink-libs1/scala-reflect-2.12.7.jar:/home/serhiy/node/flink-libs1/value-annotations-2.8.8.jar:/home/serhiy/node/flink-libs1/flink-file-sink-common-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-shaded-zookeeper-3-3.7.1-17.0.jar:/home/serhiy/node/flink-libs1/snappy-java-1.1.10.4.jar:/home/serhiy/node/flink-libs1/commons-math3-3.6.1.jar:/home/serhiy/node/flink-libs1/commons-text-1.10.0.jar:/home/serhiy/node/flink-libs1/slf4j-api-1.7.36.jar:/home/serhiy/node/flink-libs1/flink-annotations-1.19.0.jar:/home/serhiy/node/flink-libs1/minlog-1.2.jar:/home/serhiy/node/flink-libs1/jsr305-1.3.9.jar:/home/serhiy/node/flink-libs1/flink-shaded-netty-4.1.91.Final-17.0.jar:/home/serhiy/node/flink-libs1/flink-hadoop-fs-1.19.0.jar:/home/serhiy/node/flink-libs1/value-2.8.8.jar:/home/serhiy/node/flink-libs1/icu4j-67.1.jar:/home/serhiy/node/flink-libs1/flink-shaded-asm-9-9.5-17.0.jar:/home/serhiy/node/flink-libs1/scala-xml_2.12-1.0.6.jar:/home/serhiy/node/flink-libs1/flink-table-planner_2.12-1.19.0.jar:/home/serhiy/node/flink-libs1/async-profiler-2.9.jar:/home/serhiy/node/flink-libs1/commons-collections-3.2.2.jar:/home/serhiy/node/flink-libs1/javassist-3.24.0-GA.jar:/home/serhiy/node/flink-libs1/flink-table-runtime-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-streaming-java-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-queryable-state-client-java-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-runtime-1.19.0.jar:/home/serhiy/node/flink-libs1/commons-compress-1.24.0.jar:/home/serhiy/node/flink-libs1/chill-java-0.7.6.jar:/home/serhiy/node/flink-libs1/flink-table-common-1.19.0.jar:/home/serhiy/node/flink-libs1/lz4-java-1.8.0.jar:/home/serhiy/node/flink-libs1/kryo-2.24.0.jar:/home/serhiy/node/flink-libs1/flink-connector-datagen-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-optimizer-1.19.0.jar:/home/serhiy/node/flink-libs1/flink-table-api-java-bridge-1.19.0.jar:/home/serhiy/node/flink-libs1/chill_2.12-0.7.6.jar
# mvn dependency:copy-dependencies -DoutputDirectory=/home/serhiy/node/vscode-jshell-notebook/java/libs/
mvn clean package
rm ../libs/jshell.jar
cp target/jshell-0.0.1-SNAPSHOT.jar ../libs/jshell.jar