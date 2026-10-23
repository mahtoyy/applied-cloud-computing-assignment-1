package se.bth.netinventory.site;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import se.bth.netinventory.site.error.ServiceUnavailableException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SiteControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean EquipmentClient equipmentClient;

    private static String json(String code, String name) {
        return """
            {"code":"%s","name":"%s","address":"1 Main St","city":"Ruse","type":"TOWER","status":"ACTIVE"}
            """.formatted(code, name);
    }

    private static MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void listReturnsSeededSitesOrderedByCode() throws Exception {
        mvc.perform(get("/api/sites"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].code").value("BGS-001"));
    }

    @Test
    void getReturnsSite() throws Exception {
        mvc.perform(get("/api/sites/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SOF-001"))
            .andExpect(jsonPath("$.city").value("Sofia"));
    }

    @Test
    void getMissingReturns404() throws Exception {
        mvc.perform(get("/api/sites/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Site 999 not found"));
    }

    @Test
    void createReturns201() throws Exception {
        mvc.perform(withBody(post("/api/sites"), json("RSE-001", "Ruse Tower")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.code").value("RSE-001"));
    }

    @Test
    void createDuplicateCodeReturns409() throws Exception {
        mvc.perform(withBody(post("/api/sites"), json("SOF-001", "Dup")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Site code SOF-001 already exists"));
    }

    @Test
    void createInvalidReturns400ListingFields() throws Exception {
        mvc.perform(withBody(post("/api/sites"), "{\"code\":\"\",\"type\":\"TOWER\",\"status\":\"ACTIVE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: city, code, name"));
    }

    @Test
    void createTooLongCodeReturns400() throws Exception {
        mvc.perform(withBody(post("/api/sites"), json("A".repeat(21), "Long")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEnumReturns400() throws Exception {
        String body = "{\"code\":\"X-1\",\"name\":\"X\",\"city\":\"Ruse\",\"type\":\"CASTLE\",\"status\":\"ACTIVE\"}";
        mvc.perform(withBody(post("/api/sites"), body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void updateKeepingOwnCodeSucceeds() throws Exception {
        mvc.perform(withBody(put("/api/sites/1"), json("SOF-001", "Renamed")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void updateToAnotherSitesCodeReturns409() throws Exception {
        mvc.perform(withBody(put("/api/sites/1"), json("PDV-001", "Renamed")))
            .andExpect(status().isConflict());
    }

    @Test
    void updateMissingReturns404() throws Exception {
        mvc.perform(withBody(put("/api/sites/999"), json("ZZZ-001", "Nope")))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        mvc.perform(delete("/api/sites/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteSiteWithoutEquipmentReturns204() throws Exception {
        when(equipmentClient.countBySite(4L)).thenReturn(0L);

        mvc.perform(delete("/api/sites/4"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/sites/4"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteSiteWithEquipmentReturns409() throws Exception {
        when(equipmentClient.countBySite(1L)).thenReturn(3L);

        mvc.perform(delete("/api/sites/1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Site SOF-001 still has 3 equipment items"));
        mvc.perform(get("/api/sites/1"))
            .andExpect(status().isOk());
    }

    @Test
    void deleteWhenEquipmentDownReturns503() throws Exception {
        when(equipmentClient.countBySite(1L))
            .thenThrow(new ServiceUnavailableException("Equipment service unavailable"));

        mvc.perform(delete("/api/sites/1"))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/sites/1"))
            .andExpect(status().isOk());
    }

    @Test
    void deleteMissingDoesNotCallEquipment() throws Exception {
        mvc.perform(delete("/api/sites/999"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(equipmentClient);
    }
}
