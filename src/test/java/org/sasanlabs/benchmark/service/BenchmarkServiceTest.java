package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ScanType;
import org.sasanlabs.benchmark.model.ScannerFindings;

@ExtendWith(MockitoExtension.class)
class BenchmarkServiceTest {

    @Mock private DastBenchmarkStrategy dastStrategy;
    @Mock private SastBenchmarkStrategy sastStrategy;

    private BenchmarkService service;

    @BeforeEach
    void setUp() {
        service = new BenchmarkService(dastStrategy, sastStrategy);
    }

    @Test
    void dastScanType_routesToDastStrategy() throws Exception {
        BenchmarkResult dastResult = result("ZAP");
        ScannerFindings input = new ScannerFindings("ZAP", ScanType.DAST, Collections.emptyList());
        when(dastStrategy.compare(input)).thenReturn(dastResult);

        BenchmarkResult out = service.compare(input);

        assertThat(out).isSameAs(dastResult);
        verify(sastStrategy, never()).compare(input);
    }

    @Test
    void sastScanType_routesToSastStrategy() throws Exception {
        BenchmarkResult sastResult = result("Semgrep");
        ScannerFindings input =
                new ScannerFindings("Semgrep", ScanType.SAST, Collections.emptyList());
        when(sastStrategy.compare(input)).thenReturn(sastResult);

        BenchmarkResult out = service.compare(input);

        assertThat(out).isSameAs(sastResult);
        verify(dastStrategy, never()).compare(input);
    }

    @Test
    void omittedScanType_defaultsToDast() throws Exception {
        ScannerFindings input = new ScannerFindings("ZAP", Collections.emptyList());
        when(dastStrategy.compare(input)).thenReturn(result("ZAP"));

        service.compare(input);

        verify(sastStrategy, never()).compare(input);
    }

    private static BenchmarkResult result(String tool) {
        return new BenchmarkResult(
                tool, 0.0, 0, 0, 0, 0, Collections.emptyList(), Collections.emptyList());
    }
}
