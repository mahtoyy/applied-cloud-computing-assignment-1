package se.bth.netinventory.equipment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import se.bth.netinventory.equipment.error.ServiceUnavailableException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipmentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired EquipmentRepository repository;
    @MockitoBean SiteClient siteClient;

    @BeforeEach
    void siteExistsByDefault() {
        when(siteClient.siteExists(anyLong())).thenReturn(true);
    }

    private static String json(String hostname, String serial, long siteId) {
        return """
            {"hostname":"%s","vendor":"Cisco","model":"ASR 1001","serialNumber":"%s","type":"ROUTER","status":"IN_SERVICE","ipAddress":"10.9.9.1","siteId":%d}
            """.formatted(hostname, serial, siteId);
    }

    private static MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private Long idOf(String hostname) {
        return repository.findAllByOrderByHostnameAsc().stream()
            .filter(e -> e.getHostname().equals(hostname)).findFirst().orElseThrow().getId();
    }

    @Test
    void listAllReturns11() throws Exception {
        mvc.perform(get("/api/equipment"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(11));
    }

    @Test
    void listBySiteFilters() throws Exception {
        mvc.perform(get("/api/equipment").param("siteId", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].siteId", everyItem(equalTo(3))));
    }

    @Test
    void countBySite() throws Exception {
        mvc.perform(get("/api/equipment/count").param("siteId", "1"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"siteId\":1,\"count\":4}", true));
        mvc.perform(get("/api/equipment/count").param("siteId", "99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void countWithoutSiteIdReturns400() throws Exception {
        mvc.perform(get("/api/equipment/count"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getMissingReturns404() throws Exception {
        mvc.perform(get("/api/equipment/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Equipment 999 not found"));
    }

    @Test
    void createReturns201() throws Exception {
        mvc.perform(withBody(post("/api/equipment"), json("new-rtr-01", "SN-NEW-1", 4)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.hostname").value("new-rtr-01"))
            .andExpect(jsonPath("$.siteId").value(4));
        verify(siteClient).siteExists(4L);
    }

    @Test
    void createForMissingSiteReturns400() throws Exception {
        when(siteClient.siteExists(99L)).thenReturn(false);
        mvc.perform(withBody(post("/api/equipment"), json("new-rtr-01", "SN-NEW-1", 99)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Site 99 does not exist"));
        mvc.perform(get("/api/equipment"))
            .andExpect(jsonPath("$.length()").value(11));
    }

    @Test
    void createWhenSiteServiceDownReturns503() throws Exception {
        when(siteClient.siteExists(anyLong())).thenThrow(new ServiceUnavailableException("site-service unavailable"));
        mvc.perform(withBody(post("/api/equipment"), json("new-rtr-01", "SN-NEW-1", 1)))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/equipment"))
            .andExpect(jsonPath("$.length()").value(11));
    }

    @Test
    void createDuplicateHostnameReturns409() throws Exception {
        mvc.perform(withBody(post("/api/equipment"), json("var-rad-01", "SN-NEW-1", 1)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Hostname var-rad-01 already exists"));
    }

    @Test
    void createInvalidReturns400() throws Exception {
        String body = """
            {"vendor":"Cisco","model":"ASR 1001","serialNumber":"SN-NEW-1","type":"ROUTER","status":"IN_SERVICE"}
            """;
        mvc.perform(withBody(post("/api/equipment"), body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: hostname, siteId"));
    }

    @Test
    void blankIpIsStoredAsNull() throws Exception {
        String body = json("new-rtr-01", "SN-NEW-1", 1).replace("\"10.9.9.1\"", "\"\"");
        mvc.perform(withBody(post("/api/equipment"), body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ipAddress", nullValue()));
    }

    @Test
    void updateMovesToAnotherSite() throws Exception {
        Long id = idOf("var-rad-01");
        mvc.perform(withBody(put("/api/equipment/" + id), json("var-rad-01", "ER2302Q8R8", 1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.siteId").value(1));
        mvc.perform(get("/api/equipment").param("siteId", "1"))
            .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void updateKeepingOwnHostnameSucceeds() throws Exception {
        Long id = idOf("var-rad-01");
        mvc.perform(withBody(put("/api/equipment/" + id), json("var-rad-01", "ER2302Q8R8", 3)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hostname").value("var-rad-01"));
    }

    @Test
    void updateWhenSiteServiceDownReturns503() throws Exception {
        Long id = idOf("var-rad-01");
        when(siteClient.siteExists(anyLong())).thenThrow(new ServiceUnavailableException("site-service unavailable"));
        mvc.perform(withBody(put("/api/equipment/" + id), json("var-rad-01", "ER2302Q8R8", 1)))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/equipment/" + id))
            .andExpect(jsonPath("$.siteId").value(3));
    }

    @Test
    void deleteReturns204ThenGone() throws Exception {
        Long id = idOf("var-rad-01");
        mvc.perform(delete("/api/equipment/" + id))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/equipment/" + id))
            .andExpect(status().isNotFound());
    }
}
