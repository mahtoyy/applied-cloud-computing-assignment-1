package se.bth.netinventory.frontend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GatewayClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GatewayClient client = new GatewayClient(builder, "http://gateway");

    @Test
    void listSitesParsesJson() {
        server.expect(requestTo("http://gateway/api/sites"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                [{"id":1,"code":"SOF-001","name":"Sofia Central","address":"1 Main St",
                  "city":"Sofia","type":"CENTRAL_OFFICE","status":"ACTIVE"}]""",
                MediaType.APPLICATION_JSON));

        List<Site> sites = client.listSites();

        assertThat(sites).hasSize(1);
        assertThat(sites.get(0).getCode()).isEqualTo("SOF-001");
        server.verify();
    }

    @Test
    void listEquipmentFiltersBySite() {
        server.expect(requestTo("http://gateway/api/equipment?siteId=3"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.listEquipment(3)).isEmpty();
        server.verify();
    }

    @Test
    void conflictBecomesApiException() {
        server.expect(requestTo("http://gateway/api/sites/1"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":409,\"error\":\"Conflict\","
                    + "\"message\":\"Site SOF-001 still has 4 equipment items\"}"));

        assertThatThrownBy(() -> client.deleteSite(1))
            .isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(409);
                assertThat(e.getMessage()).isEqualTo("Site SOF-001 still has 4 equipment items");
            });
    }

    @Test
    void unparseableErrorBodyFallsBack() {
        server.expect(requestTo("http://gateway/api/equipment/9"))
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("<html>oops</html>"));

        assertThatThrownBy(() -> client.getEquipment(9))
            .isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(502);
                assertThat(e.getMessage()).isEqualTo("Request failed (502)");
            });
    }

    @Test
    void unreachableBecomes503() {
        server.expect(requestTo("http://gateway/api/sites"))
            .andRespond(withException(new ConnectException()));

        assertThatThrownBy(client::listSites)
            .isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(503);
                assertThat(e.getMessage()).isEqualTo("Backend unavailable, please try again later");
            });
    }
}
