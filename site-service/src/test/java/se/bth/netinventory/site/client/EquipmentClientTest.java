package se.bth.netinventory.site.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import se.bth.netinventory.site.error.ServiceUnavailableException;

class EquipmentClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final EquipmentClient client = new EquipmentClient(builder, "http://equipment");

    @Test
    void returnsCount() {
        server.expect(requestTo("http://equipment/api/equipment/count?siteId=1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"siteId\":1,\"count\":3}", MediaType.APPLICATION_JSON));

        assertThat(client.countBySite(1L)).isEqualTo(3);
        server.verify();
    }

    @Test
    void unreachableThrows503() {
        server.expect(requestTo("http://equipment/api/equipment/count?siteId=1"))
            .andRespond(withException(new ConnectException()));

        assertThatThrownBy(() -> client.countBySite(1L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessage("Equipment service unavailable");
    }

    @Test
    void serverErrorThrows503() {
        server.expect(requestTo("http://equipment/api/equipment/count?siteId=1"))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.countBySite(1L))
            .isInstanceOf(ServiceUnavailableException.class);
    }
}
