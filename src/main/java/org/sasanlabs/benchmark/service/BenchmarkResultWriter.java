package org.sasanlabs.benchmark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Persists a {@link BenchmarkResult} to a JSON file under the configured benchmarks directory.
 * Filename is derived from the (sanitised) tool name; existing files are overwritten so callers
 * always see the latest run for a given tool.
 */
@Component
public class BenchmarkResultWriter {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final String defaultBenchmarksDir;

    public BenchmarkResultWriter(
            @Value("${benchmark.output.dir:benchmarks}") String defaultBenchmarksDir) {
        this.defaultBenchmarksDir = defaultBenchmarksDir;
    }

    public Path write(BenchmarkResult result) throws IOException {
        return write(result, defaultBenchmarksDir);
    }

    public Path write(BenchmarkResult result, String benchmarksDir) throws IOException {
        Path dir = Paths.get(benchmarksDir);
        Files.createDirectories(dir);
        String fileName = sanitizeToolName(result.getTool()) + "-results.json";
        Path target = dir.resolve(fileName);
        Path temp = Files.createTempFile(dir, fileName + ".", ".tmp");
        try {
            OBJECT_MAPPER.writeValue(temp.toFile(), result);
            moveAtomicallyOrReplace(temp, target);
        } catch (IOException ioe) {
            Files.deleteIfExists(temp);
            throw ioe;
        }
        return target;
    }

    private static void moveAtomicallyOrReplace(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException notSupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static String sanitizeToolName(String tool) {
        if (tool == null) {
            return "unknown";
        }
        String lowered = tool.trim().toLowerCase(Locale.ROOT);
        String cleaned = lowered.replaceAll("[^a-z0-9_-]", "");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }
}
