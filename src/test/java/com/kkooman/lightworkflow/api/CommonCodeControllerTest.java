package com.kkooman.lightworkflow.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommonCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void commonCodesEndpointReturnsEnumValues() throws Exception {
        mockMvc.perform(get("/api/common-codes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.DisplayStatus[0].code").value("ACTIVE"))
                .andExpect(jsonPath("$.DisplayStatus[0].label").value("활성"))
                .andExpect(jsonPath("$.UserType[0].code").value("ADMIN"))
                .andExpect(jsonPath("$.UserType[0].label").value("관리자"));
    }

    @Test
    void singleEnumEndpointReturnsValues() throws Exception {
        mockMvc.perform(get("/api/common-codes/DisplayStatus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ACTIVE"))
                .andExpect(jsonPath("$[0].label").value("활성"));
    }
}
