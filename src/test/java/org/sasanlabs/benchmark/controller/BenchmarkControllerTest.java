package org.sasanlabs.benchmark.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.sasanlabs.benchmark.service.BenchmarkResultWriter;
import org.sasanlabs.benchmark.service.BenchmarkService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BenchmarkControllerTest {

    @Mock private BenchmarkService benchmarkService;

    @Mock private BenchmarkResultWriter benchmarkResultWriter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BenchmarkController controller =
                new BenchmarkController(benchmarkService, benchmarkResultWriter);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void validRequest_returns200_andWritesResultFile() throws Exception {
        BenchmarkResult mockResult =
                new BenchmarkResult(
                        "ZAP", 100.0, 1, 1, 0, 0, Collections.emptyList(), Collections.emptyList());
        when(benchmarkService.compare(any(ScannerFindings.class))).thenReturn(mockResult);
        when(benchmarkResultWriter.write(any(BenchmarkResult.class)))
                .thenReturn(Paths.get("benchmarks/zap-results.json"));

        String body =
                "{\"tool\":\"ZAP\",\"findings\":["
                        + "{\"url\":\"/SQLInjection/LEVEL_1\",\"type\":\"BLIND_SQL_INJECTION\"}]}";

        mockMvc.perform(
                        post("/scanner/benchmark")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool").value("ZAP"))
                .andExpect(jsonPath("$.coverage").value(100.0))
                .andExpect(jsonPath("$.totalExpected").value(1))
                .andExpect(jsonPath("$.detected").value(1));

        verify(benchmarkResultWriter, times(1)).write(argThat(r -> "ZAP".equals(r.getTool())));
    }

    @Test
    void missingToolField_returns400() throws Exception {
        String body =
                "{\"findings\":["
                        + "{\"url\":\"/SQLInjection/LEVEL_1\",\"type\":\"BLIND_SQL_INJECTION\"}]}";

        mockMvc.perform(
                        post("/scanner/benchmark")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void blankToolField_returns400() throws Exception {
        String body =
                "{\"tool\":\"   \",\"findings\":["
                        + "{\"url\":\"/SQLInjection/LEVEL_1\",\"type\":\"BLIND_SQL_INJECTION\"}]}";

        mockMvc.perform(
                        post("/scanner/benchmark")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingFindingsField_returns400() throws Exception {
        String body = "{\"tool\":\"ZAP\"}";

        mockMvc.perform(
                        post("/scanner/benchmark")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(
                        post("/scanner/benchmark")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not valid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void writerFailure_stillReturns200WithBody() throws Exception {
        BenchmarkResult mockResult =
                new BenchmarkResult(
                        "ZAP", 50.0, 2, 1, 1, 0, Collections.emptyList(), Collections.emptyList());
        when(benchmarkService.compare(any(ScannerFindings.class))).thenReturn(mockResult);
        when(benchmarkResultWriter.write(any(BenchmarkResult.class)))
                .thenThrow(new IOException("disk full"));

        String body = "{\"tool\":\"ZAP\",\"findings\":[]}";

        mockMvc.perform(
                        post("/scanner/benchmark")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool").value("ZAP"))
                .andExpect(jsonPath("$.coverage").value(50.0));
    }
}
