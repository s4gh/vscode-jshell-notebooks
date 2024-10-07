package s4gh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static s4gh.JShellInterpretter.DISPLAY_RESULT;

    public class HttpJsonServerWithSuggestion {
    private static final int TIMEOUT_MINUTES = 20;
    private static long lastRequestTime;
    private static JShellInterpretter jshellInterpretter = new JShellInterpretter();
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.out.println("Usage: java HttpJsonServer <port> <secret>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String secret = args[1];
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            lastRequestTime = System.currentTimeMillis();

            while (true) {
                if (System.currentTimeMillis() - lastRequestTime > TimeUnit.MINUTES.toMillis(TIMEOUT_MINUTES)) {
                    System.out.println("No requests received for 20 minutes. Shutting down.");
                    System.exit(0);
                }

                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     OutputStream out = clientSocket.getOutputStream()) {

                    // Read and parse HTTP request
                    Map<String, String> headers = new HashMap<>();
                    String requestLine = in.readLine();
                    String urlPath = extractUrlPath(requestLine);

                    String line;
                    while ((line = in.readLine()) != null && !line.isEmpty()) {
                        String[] parts = line.split(": ", 2);
                        if (parts.length == 2) {
                            headers.put(parts[0], parts[1]);
                        }
                    }

                    String userSecret = headers.get("Authorization");
                    if (!secret.equals(userSecret)) {
                        String response = "auth issue";
                        String httpResponse = "HTTP/1.1 401 Unauthorized\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Content-Length: " + response.length() + "\r\n" +
                                "\r\n" +
                                response;
                        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                        continue;
                    }

                    int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

                    // Read HTTP body
                    char[] body = new char[contentLength];
                    in.read(body, 0, contentLength);
                    String requestBody = new String(body);

                    lastRequestTime = System.currentTimeMillis();

                    // Process request
                    if (requestLine.startsWith("POST")) {
                        if (urlPath.startsWith("/execute")) {
                            handleJShellScriptExecutionRequest(requestBody, out);
                        } else if (urlPath.startsWith("/suggest")) {
                            handleCodeCompletionRequest(requestBody, out);
                        }
                    } else {
                        String response = "Only POST requests are supported";
                        String httpResponse = "HTTP/1.1 405 Method Not Allowed\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: " + response.length() + "\r\n" +
                                "\r\n" +
                                response;
                        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (IOException e) {
                    System.err.println("Error handling client request: " + e.getMessage());
                }
            }
        }
    }

    private static void handleCodeCompletionRequest(String requestBody, OutputStream out) throws IOException {
//        String code = parseScriptCode(requestBody);
        String code = parseScriptCodeManually(requestBody);

        List<String> codeSuggestions = jshellInterpretter.generateCodeCompletion(code);
        StringBuilder response = new StringBuilder("""
                {
                    "completions": [
                """);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codeSuggestions.size(); i++) {
            sb.append("\"").append(codeSuggestions.get(i)).append("\"");  // Wrap each element in quotes
            if (i < codeSuggestions.size() - 1) {
                sb.append(", ");
            }
        }
        response.append(sb);
        response.append("""
                    ]
                }
                """);

        // Send HTTP response
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + response.length() + "\r\n" +
                "\r\n" +
                response;
        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
    }

    private static void handleJShellScriptExecutionRequest(String requestBody, OutputStream out) throws IOException {
//        String code = parseScriptCode(requestBody);
        String code = parseScriptCodeManually(requestBody);

        jshellInterpretter.execute(code);

        Object displayResult = VariablesTransfer.getVariableValue(DISPLAY_RESULT);
        if (displayResult != null) {
            VariablesTransfer.setVariableValue(DISPLAY_RESULT, null);
        }

        String response = "";
        if (displayResult != null) {
            response = serializeResult(displayResult);
        }
        // Send HTTP response
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + response.length() + "\r\n" +
                "\r\n" +
                response;
        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
    }

    private static String extractUrlPath(String requestLine) {
        String[] parts = requestLine.split("\\s+");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "/";
    }

    private static String parseScriptCode(String json) {
        try {
            JsonNode jsonNode = mapper.readTree(json);
            JsonNode scriptNode = jsonNode.get("script");
            return scriptNode.asText();
        } catch (JsonProcessingException e) {
            return e.getMessage();
        }
    }

    private static String serializeResult(Object object) {

        try {
            List<Map<String, String>> tableData = (List<Map<String, String>>) object;

            ArrayNode arrayNode = mapper.createArrayNode();
            for (Map<String, String> rowOfData : tableData) {
                ObjectNode rowNode = mapper.createObjectNode();
                for (Map.Entry<String,String> entry : rowOfData.entrySet()) {
                    rowNode.put(entry.getKey(), entry.getValue());
                }
                arrayNode.add(rowNode);
            }
            String response = "";
            try {
                response = mapper.writeValueAsString(arrayNode);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return response;
        } catch (Exception exception) {
            //ignore
        }

        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return e.getMessage();
        }
    }

    private static String parseScriptCodeManually(String json) {
//        int objectStartIndex = json.indexOf("{");
//        int objectEndIndex = json.lastIndexOf("}");
//        String result = json.substring(objectStartIndex+1, objectEndIndex);
//
//        result = result.replaceFirst("\"script\"", "");
//        result = result.replaceFirst(":", "");
//        result = result.replaceFirst("\"", "");
//        int lastQuoteIndex = result.lastIndexOf("\"");
//        result = result.substring(0, lastQuoteIndex);
        String result = json;
        result = result.replaceAll("\\n\\r" ," ");
        result = result.replaceAll("\\n" ," ");
        result = result.replaceAll("\\\\n", " ");
//        result = result.replaceAll("\\\\\"", "\"");

        return result;
    }

    public record CodeCompletionResponse(List<String> completions){};
}
