package se.bth.netinventory.frontend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;

@WebMvcTest(SiteController.class)
class SiteControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GatewayClient gateway;

    private static Site site(long id) {
        Site s = new Site();
        s.setId(id);
        s.setCode("SOF-001");
        s.setName("Sofia Core");
        s.setCity("Sofia");
        s.setType("DATA_CENTER");
        s.setStatus("ACTIVE");
        return s;
    }

    private static Equipment equipment(long id, String status) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setHostname("sof-core-rtr-01");
        e.setVendor("Cisco");
        e.setModel("ASR9000");
        e.setSerialNumber("SN1");
        e.setType("ROUTER");
        e.setStatus(status);
        e.setIpAddress("10.0.0.1");
        e.setSiteId(1L);
        return e;
    }

    private static LinkedMultiValueMap<String, String> validParams() {
        var p = new LinkedMultiValueMap<String, String>();
        p.add("code", "SOF-001");
        p.add("name", "Sofia Core");
        p.add("address", "1 Main St");
        p.add("city", "Sofia");
        p.add("type", "DATA_CENTER");
        p.add("status", "ACTIVE");
        return p;
    }

    @Test
    void rootRedirectsToSites() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/sites"));
    }

    @Test
    void listShowsSites() throws Exception {
        given(gateway.listSites()).willReturn(List.of(site(1)));
        mvc.perform(get("/sites")).andExpect(status().isOk())
            .andExpect(content().string(containsString("SOF-001")))
            .andExpect(content().string(containsString("Sofia")));
    }

    @Test
    void detailShowsSiteAndEquipment() throws Exception {
        given(gateway.getSite(1)).willReturn(site(1));
        given(gateway.listEquipment(1)).willReturn(List.of(equipment(7, "DOWN")));
        mvc.perform(get("/sites/1")).andExpect(status().isOk())
            .andExpect(content().string(containsString("sof-core-rtr-01")))
            .andExpect(content().string(containsString("status-DOWN")))
            .andExpect(content().string(containsString("name=\"siteId\"")));
    }

    @Test
    void detailShowsEmptyMessage() throws Exception {
        given(gateway.getSite(1)).willReturn(site(1));
        given(gateway.listEquipment(1)).willReturn(List.of());
        mvc.perform(get("/sites/1")).andExpect(status().isOk())
            .andExpect(content().string(containsString("No equipment installed at this site.")));
    }

    @Test
    void createValidRedirectsToDetail() throws Exception {
        given(gateway.createSite(any())).willReturn(site(5));
        mvc.perform(post("/sites").params(validParams()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/sites/5?msg=Site%20saved"));
    }

    @Test
    void createInvalidRerendersForm() throws Exception {
        var params = validParams();
        params.set("code", "");
        mvc.perform(post("/sites").params(params)).andExpect(status().isOk())
            .andExpect(view().name("sites/form"));
        verify(gateway, never()).createSite(any());
    }

    @Test
    void createConflictShowsApiMessage() throws Exception {
        given(gateway.createSite(any()))
            .willThrow(new ApiException(409, "Site code SOF-001 already exists"));
        mvc.perform(post("/sites").params(validParams())).andExpect(status().isOk())
            .andExpect(view().name("sites/form"))
            .andExpect(content().string(containsString("Site code SOF-001 already exists")))
            .andExpect(content().string(containsString("Sofia Core")));
    }

    @Test
    void emptyAddressIsSentAsNull() throws Exception {
        given(gateway.createSite(any())).willReturn(site(5));
        var params = validParams();
        params.set("address", "  ");
        mvc.perform(post("/sites").params(params)).andExpect(status().is3xxRedirection());
        ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
        verify(gateway).createSite(captor.capture());
        assertThat(captor.getValue().getAddress()).isNull();
    }

    @Test
    void deleteBlockedRedirectsWithError() throws Exception {
        String message = "Site SOF-001 still has 4 equipment items";
        doThrow(new ApiException(409, message)).when(gateway).deleteSite(1);
        MvcResult result = mvc.perform(post("/sites/1/delete"))
            .andExpect(status().is3xxRedirection()).andReturn();
        String url = result.getResponse().getRedirectedUrl();
        String prefix = "/sites/1?error=";
        assertThat(url).startsWith(prefix);
        assertThat(URLDecoder.decode(url.substring(prefix.length()), StandardCharsets.UTF_8))
            .isEqualTo(message);
    }

    @Test
    void deleteSuccessRedirectsToList() throws Exception {
        mvc.perform(post("/sites/1/delete")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/sites?msg=Site%20deleted"));
        verify(gateway).deleteSite(1);
    }

    @Test
    void gatewayDownShowsErrorPage() throws Exception {
        given(gateway.listSites()).willThrow(new ApiException(503, "Backend unavailable"));
        mvc.perform(get("/sites")).andExpect(status().isServiceUnavailable())
            .andExpect(view().name("error-page"))
            .andExpect(content().string(containsString("Backend unavailable")));
    }

    @Test
    void messageParamIsEscaped() throws Exception {
        given(gateway.listSites()).willReturn(List.of());
        mvc.perform(get("/sites").param("msg", "<script>x</script>"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>x"))));
    }

    @Test
    void detailStillShowsSiteWhenEquipmentListFails() throws Exception {
        given(gateway.getSite(1)).willReturn(site(1));
        given(gateway.listEquipment(1)).willThrow(new ApiException(503, "Backend unavailable"));
        mvc.perform(get("/sites/1")).andExpect(status().isOk())
            .andExpect(view().name("sites/detail"))
            .andExpect(model().attribute("error", "Equipment list unavailable: Backend unavailable"))
            .andExpect(content().string(containsString("Equipment list unavailable")));
    }
}
