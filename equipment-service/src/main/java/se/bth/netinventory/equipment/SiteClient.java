package se.bth.netinventory.equipment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import se.bth.netinventory.equipment.error.ServiceUnavailableException;

@Component
public class SiteClient {

    private static final Logger log = LoggerFactory.getLogger(SiteClient.class);

    private final RestClient restClient;

    public SiteClient(RestClient.Builder builder, @Value("${site-service.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public boolean siteExists(Long siteId) {
        log.info("Checking with site-service that site {} exists", siteId);
        try {
            boolean exists = restClient.get()
                .uri("/api/sites/{id}", siteId)
                .exchange((request, response) -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return true;
                    }
                    if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return false;
                    }
                    throw new ServiceUnavailableException("Site service unavailable");
                });
            log.info("Site-service says site {} exists: {}", siteId, exists);
            return exists;
        } catch (ServiceUnavailableException e) {
            log.warn("Site-service returned an unexpected status for site {}", siteId);
            throw e;
        } catch (RestClientException e) {
            log.warn("Site-service call failed for site {}: {}", siteId, e.getMessage());
            throw new ServiceUnavailableException("Site service unavailable");
        }
    }
}
