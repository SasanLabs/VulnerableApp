package org.sasanlabs.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Alessandro Arosio - 03/10/2020 10:26
 */
@SpringBootTest
@AutoConfigureMockMvc
class VulnerableAppRestControllerTest {

    @Autowired
    VulnerableAppRestController vulnerableAppRestController;

    @Autowired
    MockMvc mvc;

    @Test
    void allEndPoints_ReturnsHttpStatus200AndListOfEndpoints() throws Exception {
        mvc.perform(
                get("/allEndPoint")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    void sitemapForPassiveScanners_ReturnsStatus200() throws Exception {

        mvc.perform(
                get("/sitemap.xml")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(jsonPath("$", notNullValue()));
    }
}