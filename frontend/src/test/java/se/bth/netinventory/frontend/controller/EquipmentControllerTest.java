package se.bth.netinventory.frontend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import se.bth.netinventory.frontend.client.ApiException;
import se.bth.netinventory.frontend.client.GatewayClient;
import se.bth.netinventory.frontend.model.Equipment;
import se.bth.netinventory.frontend.model.Site;

@WebMvcTest(EquipmentController.class)
class EquipmentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GatewayClient gateway;

    private static Site site(long id, String code, String name) {
        Site s = new Site();
        s.setId(id);
        s.setCode(code);
        s.setName(name);
        return s;
    }

    private static Equipment equipment(long id) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setHostname("var-rad-01");
        e.setVendor("Nokia");
        e.setModel("AirScale");
        e.setSerialNumber("SN7");
        e.setType("RADIO");
        e.setStatus("IN_SERVICE");
        e.setIpAddress("10.1.0.7");
        e.setSiteId(3L);
        return e;
    }

    private static MockHttpServletRequestBuilder withValidParams(MockHttpServletRequestBuilder b,
                                                                 String siteId) {
        return withParams(b, siteId, "var-rad-01", "10.1.0.7");
    }

    private static MockHttpServletRequestBuilder withParams(MockHttpServletRequestBuilder b,
                                                             String siteId, String hostname, String ip) {
        return b.param("hostname", hostname).param("vendor", "Nokia")
            .param("model", "AirScale").param("serialNumber", "SN7")
            .param("type", "RADIO").param("status", "IN_SERVICE")
            .param("ipAddress", ip).param("siteId", siteId);
    }

    @BeforeEach
    void sites() {
        given(gateway.listSites()).willReturn(List.of(
            site(1, "SOF-001", "Sofia Core"), site(3, "VAR-001", "Varna Tower")));
    }

    @Test
    void newFormPreselectsSite() throws Exception {
        mvc.perform(get("/sites/3/equipment/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("equipment/form"))
            .andExpect(model().attribute("equipment", org.hamcrest.Matchers.hasProperty("siteId", is(3L))))
            .andExpect(content().string(containsString("VAR-001 – Varna Tower")));
    }

    @Test
    void editFormLoadsEquipment() throws Exception {
        given(gateway.getEquipment(7)).willReturn(equipment(7));

        mvc.perform(get("/equipment/7/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("equipment/form"))
            .andExpect(model().attribute("equipment",
                org.hamcrest.Matchers.hasProperty("hostname", is("var-rad-01"))))
            .andExpect(content().string(containsString("SOF-001 – Sofia Core")));
    }

    @Test
    void createRedirectsToSite() throws Exception {
        given(gateway.createEquipment(any())).willReturn(equipment(7));

        mvc.perform(withValidParams(post("/equipment"), "3"))
            .andExpect(redirectedUrl("/sites/3?msg=Equipment%20saved"));
    }

    @Test
    void updateMovesAndRedirectsToNewSite() throws Exception {
        mvc.perform(withValidParams(post("/equipment/7"), "1"))
            .andExpect(redirectedUrl("/sites/1?msg=Equipment%20saved"));

        verify(gateway).updateEquipment(eq(7L), argThat(e -> e.getSiteId() == 1L));
    }

    @Test
    void validationErrorRerendersForm() throws Exception {
        mvc.perform(withParams(post("/equipment"), "3", " ", "10.1.0.7"))
            .andExpect(status().isOk())
            .andExpect(view().name("equipment/form"));
    }

    @Test
    void missingSiteShowsApiMessage() throws Exception {
        given(gateway.createEquipment(any()))
            .willThrow(new ApiException(400, "Site 99 does not exist"));

        mvc.perform(withValidParams(post("/equipment"), "99"))
            .andExpect(status().isOk())
            .andExpect(view().name("equipment/form"))
            .andExpect(content().string(containsString("Site 99 does not exist")));
    }

    @Test
    void siteServiceDownShowsApiMessage() throws Exception {
        given(gateway.createEquipment(any()))
            .willThrow(new ApiException(503, "Site service unavailable"));

        mvc.perform(withValidParams(post("/equipment"), "3"))
            .andExpect(status().isOk())
            .andExpect(view().name("equipment/form"))
            .andExpect(content().string(containsString("Site service unavailable")))
            .andExpect(content().string(containsString("var-rad-01")));
    }

    @Test
    void formStaysUsableWhenSiteListAlsoFails() throws Exception {
        given(gateway.createEquipment(any()))
            .willThrow(new ApiException(503, "Site service unavailable"));
        given(gateway.listSites())
            .willThrow(new ApiException(503, "Backend unavailable, please try again later"));

        mvc.perform(withValidParams(post("/equipment"), "3"))
            .andExpect(status().isOk())
            .andExpect(view().name("equipment/form"))
            .andExpect(content().string(containsString("Site service unavailable")))
            .andExpect(content().string(containsString("var-rad-01")))
            .andExpect(content().string(containsString("Site #3")));
    }

    @Test
    void blankIpIsSentAsNull() throws Exception {
        given(gateway.createEquipment(any())).willReturn(equipment(7));

        mvc.perform(withParams(post("/equipment"), "3", "var-rad-01", "  "))
            .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Equipment> sent = ArgumentCaptor.forClass(Equipment.class);
        verify(gateway).createEquipment(sent.capture());
        org.assertj.core.api.Assertions.assertThat(sent.getValue().getIpAddress()).isNull();
    }

    @Test
    void deleteRedirectsToSite() throws Exception {
        mvc.perform(post("/equipment/7/delete").param("siteId", "3"))
            .andExpect(redirectedUrl("/sites/3?msg=Equipment%20deleted"));

        verify(gateway).deleteEquipment(7);
    }

    @Test
    void deleteFailureRedirectsWithError() throws Exception {
        org.mockito.Mockito.doThrow(new ApiException(503, "Equipment service unavailable"))
            .when(gateway).deleteEquipment(7);

        mvc.perform(post("/equipment/7/delete").param("siteId", "3"))
            .andExpect(redirectedUrl("/sites/3?error=Equipment%20service%20unavailable"));
    }

    @Test
    void newFormForUnknownSiteShows404Page() throws Exception {
        given(gateway.getSite(999)).willThrow(new ApiException(404, "Site 999 not found"));

        mvc.perform(get("/sites/999/equipment/new"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error-page"));
    }
}
