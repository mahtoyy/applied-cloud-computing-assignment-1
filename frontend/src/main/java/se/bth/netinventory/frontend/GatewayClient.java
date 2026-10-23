package se.bth.netinventory.frontend;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The only way the frontend talks to the backend: REST calls to the API gateway. */
@Component
public class GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayClient.class);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final RestClient restClient;

    public GatewayClient(RestClient.Builder builder, @Value("${gateway.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<Site> listSites() {
        return call("GET /api/sites", () -> restClient.get().uri("/api/sites")
            .retrieve().onStatus(HttpStatusCode::isError, this::fail)
            .body(new ParameterizedTypeReference<List<Site>>() { }));
    }

    public Site getSite(long id) {
        return call("GET /api/sites/" + id, () -> restClient.get().uri("/api/sites/{id}", id)
            .retrieve().onStatus(HttpStatusCode::isError, this::fail).body(Site.class));
    }

    public Site createSite(Site site) {
        return call("POST /api/sites", () -> restClient.post().uri("/api/sites").body(site)
            .retrieve().onStatus(HttpStatusCode::isError, this::fail).body(Site.class));
    }

    public Site updateSite(long id, Site site) {
        return call("PUT /api/sites/" + id, () -> restClient.put().uri("/api/sites/{id}", id)
            .body(site).retrieve().onStatus(HttpStatusCode::isError, this::fail).body(Site.class));
    }

    public void deleteSite(long id) {
        call("DELETE /api/sites/" + id, () -> restClient.delete().uri("/api/sites/{id}", id)
            .retrieve().onStatus(HttpStatusCode::isError, this::fail).toBodilessEntity());
    }

    public List<Equipment> listEquipment(long siteId) {
        return call("GET /api/equipment?siteId=" + siteId, () -> restClient.get()
            .uri("/api/equipment?siteId={id}", siteId)
            .retrieve().onStatus(HttpStatusCode::isError, this::fail)
            .body(new ParameterizedTypeReference<List<Equipment>>() { }));
    }

    public Equipment getEquipment(long id) {
        return call("GET /api/equipment/" + id, () -> restClient.get()
            .uri("/api/equipment/{id}", id)
            .retrieve().onStatus(HttpStatusCode::isError, this::fail).body(Equipment.class));
    }

    public Equipment createEquipment(Equipment equipment) {
        return call("POST /api/equipment", () -> restClient.post().uri("/api/equipment")
            .body(equipment)
            .retrieve().onStatus(HttpStatusCode::isError, this::fail).body(Equipment.class));
    }

    public Equipment updateEquipment(long id, Equipment equipment) {
        return call("PUT /api/equipment/" + id, () -> restClient.put()
            .uri("/api/equipment/{id}", id).body(equipment)
            .retrieve().onStatus(HttpStatusCode::isError, this::fail).body(Equipment.class));
    }

    public void deleteEquipment(long id) {
        call("DELETE /api/equipment/" + id, () -> restClient.delete()
            .uri("/api/equipment/{id}", id)
            .retrieve().onStatus(HttpStatusCode::isError, this::fail).toBodilessEntity());
    }

    private <T> T call(String description, Supplier<T> request) {
        log.info("Calling gateway: {}", description);
        try {
            return request.get();
        } catch (ApiException e) {
            log.warn("Gateway call {} failed with {}: {}", description, e.getStatus(), e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.warn("Gateway call {} failed: {}", description, e.getMessage());
            throw new ApiException(503, "Backend unavailable, please try again later");
        }
    }

    private void fail(HttpRequest request,
                      ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String message = "Request failed (" + status + ")";
        boolean hasMessage = false;
        try {
            JsonNode node = MAPPER.readTree(response.getBody());
            if (node.hasNonNull("message")) {
                message = node.get("message").asString();
                hasMessage = true;
            }
        } catch (RuntimeException e) {
            // body missing or not JSON: keep the generic message
        }
        if (status >= 500 && !hasMessage) {
            // a bare Boot error body means the backend itself is broken or unreachable
            status = 503;
            message = "Backend unavailable, please try again later";
        }
        throw new ApiException(status, message);
    }
}
