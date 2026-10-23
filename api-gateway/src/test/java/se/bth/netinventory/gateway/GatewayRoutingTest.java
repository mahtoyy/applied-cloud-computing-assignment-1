package se.bth.netinventory.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static HttpServer upstream;

    @Value("${local.server.port}")
    int port;

    @DynamicPropertySource
    static void upstreamUrls(DynamicPropertyRegistry registry) throws IOException {
        if (upstream == null) {
            upstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            upstream.createContext("/", GatewayRoutingTest::echo);
            upstream.start();
        }
        String url = "http://localhost:" + upstream.getAddress().getPort();
        registry.add("SITE_SERVICE_URL", () -> url);
        registry.add("EQUIPMENT_SERVICE_URL", () -> url);
    }

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    private static void echo(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        int status = 200;
        String json = "{\"path\":\"%s\",\"query\":\"%s\",\"method\":\"%s\",\"body\":\"%s\"}".formatted(
            path, query == null ? "" : query, exchange.getRequestMethod(), body.replace("\"", "BS\""));
        if (path.equals("/api/sites/404")) {
            status = 404;
            json = "{\"status\":404,\"message\":\"Site 404 not found\"}";
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private RestClient client() {
        return RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(status -> true, (request, response) -> { })
            .build();
    }

    @Test
    void routesSitesList() {
        var response = client().get().uri("/api/sites").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"path\":\"/api/sites\"");
    }

    @Test
    void routesNestedSitePath() {
        var response = client().get().uri("/api/sites/1").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"path\":\"/api/sites/1\"");
    }

    @Test
    void forwardsQueryString() {
        var response = client().get().uri("/api/equipment?siteId=3").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"path\":\"/api/equipment\"", "\"query\":\"siteId=3\"");
    }

    @Test
    void forwardsPostBody() {
        var response = client().post().uri("/api/equipment")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"hostname\":\"x\"}")
            .retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"method\":\"POST\"", "hostname");
    }

    @Test
    void passesThroughErrorStatus() {
        var response = client().get().uri("/api/sites/404").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("Site 404 not found");
    }

    @Test
    void unknownPathIs404() {
        var response = client().get().uri("/api/other").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void healthIsUp() {
        var response = client().get().uri("/actuator/health/liveness").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
