# JShell Notebook

This plugin provides support for notebools which can have markdown documentation cells and code cells. It uses JShell to evaluate code snippets

# Features
- Interactive notebooks where you can have combination of code cells and markdown cells
- Code editor with
    - Syntax highligh
    - Code completion
    - Hints about errors in code snoppets
![Code editor features](https://raw.githubusercontent.com/s4gh/vscode-jshell-notebooks/refs/heads/main/images/features.png)
- Abilityt to set/change classapth in runtime - use ```/env --class-path /path-to-library1.jar:/path-to-library2.jar:...``` code to set classparh. On Windows ```;``` is used as class path entries separator
- Apache Flink support
![Flink support|100](https://raw.githubusercontent.com/s4gh/vscode-jshell-notebooks/refs/heads/main/images/flink-support.png)

# Preparation

- Install the extension
- Install Plugin which renders JSON output as table https://github.com/RandomFractals/vscode-data-table
- Optionally install 
Plugin to render dfferent visualtaions of data in jupyper format https://github.com/Microsoft/vscode-notebook-renderers (not currently used. There are plans to use it in future to support visualization of data as charts)

# Example

- create file with .jshnb file, "Open With..."
- Select "JShell Notebook"
- Add markdown or code cells. Use build in ```show(Object o)``` method do display cell output. There is also ```showTable(org.apache.flink.table.api.Table table)``` method to execute Flink table and render it as JSON
- Create cell with the command to set flink class path:

```
/env --class-path /home/user/.m2/repository/org/apache/flink/flink-clients/1.20.0/flink-clients-1.20.0.jar: ... :/home/user/.m2/repository/org/apache/flink/flink-csv/1.20.0/flink-csv-1.20.0.jar
```
If you have Maven project with Flink dependencies set you can print classpath by running ```mvn dependency:build-classpath``` command.
Also you can copy Maven project dependencies into separate directory by running ```mvn dependency:copy-dependencies -DoutputDirectory=/home/target-dir``` command.
You can use these command to generate class path which you can use in this notebook plugin to set Flink classpath.
Example of basic pom.xml file with Flink dependencies is below. You may need to add more dependencies (e.g. flink-avro or other libraries).
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>flink-example</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <flink.version>1.20.0</flink.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-clients</artifactId>
            <version>${flink.version}</version>
        </dependency>

        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-table-api-java-bridge</artifactId>
            <version>${flink.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-java</artifactId>
            <version>${flink.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-streaming-java</artifactId>
            <version>${flink.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-table-runtime</artifactId>
            <version>${flink.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-files</artifactId>
            <version>${flink.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-table-planner_2.12</artifactId>
            <version>${flink.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-csv</artifactId>
            <version>${flink.version}</version>
        </dependency>
    </dependencies>

</project>
```
- Add cell with sample Flink code. Example below used remote axecution environment and expects that Flink is started as a separate process

```java
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
        
        showTable(result);
```
- Execute cells

## Notes
Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. 
