package se.bth.netinventory.equipment.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import se.bth.netinventory.equipment.error.ServiceUnavailableException;

class SiteClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final SiteClient client = new SiteClient(builder, "http://site");

    @Test
    void existingSiteReturnsTrue() {
        server.expect(requestTo("http://site/api/sites/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        assertThat(client.siteExists(1L)).isTrue();
        server.verify();
    }

    @Test
    void missingSiteReturnsFalse() {
        server.expect(requestTo("http://site/api/sites/99"))
            .andRespond(withResourceNotFound());

        assertThat(client.siteExists(99L)).isFalse();
        server.verify();
    }

    @Test
    void unreachableThrows503() {
        server.expect(requestTo("http://site/api/sites/1"))
            .andRespond(withException(new ConnectException()));

        assertThatThrownBy(() -> client.siteExists(1L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessage("Site service unavailable");
    }

    @Test
    void serverErrorThrows503() {
        server.expect(requestTo("http://site/api/sites/1"))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.siteExists(1L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessage("Site service unavailable");
    }
}
