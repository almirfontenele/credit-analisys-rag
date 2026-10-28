package com.credit.analysis.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class GeminiOpenAiCompatConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public RestClientCustomizer geminiOpenAiRestClientCustomizer() {
        return restClientBuilder -> restClientBuilder.requestInterceptor((request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request, body);
            String path = request.getURI().getPath();
            if (path != null && path.contains("/embeddings") && response.getStatusCode().is2xxSuccessful()) {
                byte[] responseBytes = response.getBody().readAllBytes();
                try {
                    JsonNode root = objectMapper.readTree(responseBytes);
                    if (root.isObject() && !root.has("usage")) {
                        ((ObjectNode) root).putObject("usage")
                                .put("prompt_tokens", 0)
                                .put("total_tokens", 0);
                        byte[] modified = objectMapper.writeValueAsBytes(root);
                        return new ModifiedClientHttpResponse(response, modified);
                    }
                } catch (Exception ignored) {
                }
                return new ModifiedClientHttpResponse(response, responseBytes);
            }
            return response;
        });
    }

    private static class ModifiedClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse original;
        private final byte[] body;
        private final HttpHeaders headers;

        public ModifiedClientHttpResponse(ClientHttpResponse original, byte[] body) {
            this.original = original;
            this.body = body;
            this.headers = new HttpHeaders();
            this.headers.putAll(original.getHeaders());
            this.headers.setContentLength(body.length);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return original.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return original.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public void close() {
            original.close();
        }
    }
}
