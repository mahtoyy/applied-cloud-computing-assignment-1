package se.bth.netinventory.site.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import se.bth.netinventory.site.error.ServiceUnavailableException;

@Component
public class EquipmentClient {

    private static final Logger log = LoggerFactory.getLogger(EquipmentClient.class);

    private final RestClient restClient;

    public EquipmentClient(RestClient.Builder builder, @Value("${equipment-service.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public long countBySite(Long siteId) {
        log.info("Asking equipment-service for equipment count of site {}", siteId);
        try {
            CountResponse response = restClient.get()
                .uri("/api/equipment/count?siteId={id}", siteId)
                .retrieve()
                .body(CountResponse.class);
            if (response == null) {
                throw new ServiceUnavailableException("Equipment service unavailable");
            }
            log.info("Equipment-service reports {} items for site {}", response.count(), siteId);
            return response.count();
        } catch (RestClientException e) {
            log.warn("Equipment-service call failed for site {}: {}", siteId, e.getMessage());
            throw new ServiceUnavailableException("Equipment service unavailable");
        }
    }

    private record CountResponse(Long siteId, long count) {
    }
}
