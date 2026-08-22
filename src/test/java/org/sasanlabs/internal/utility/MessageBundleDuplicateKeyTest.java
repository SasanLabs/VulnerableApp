package org.sasanlabs.internal.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageBundleDuplicateKeyTest {

    @Test
    @DisplayName("i18n: no duplicate keys in message bundles")
    void noDuplicateKeys() throws Exception {
        List<Path> bundles = messageBundles();
        assertFalse(bundles.isEmpty(), "No i18n message bundles found");
        List<String> duplicates = new ArrayList<>();

        for (Path bundle : bundles) {
            duplicates.addAll(findDuplicates(bundle));
        }

        if (!duplicates.isEmpty()) {
            fail("Duplicate keys found in i18n bundles:\n  " + String.join("\n  ", duplicates));
        }
    }

    private static List<Path> messageBundles() throws URISyntaxException, IOException {
        URL i18n = MessageBundleDuplicateKeyTest.class.getResource("/i18n");
        if (i18n == null) {
            throw new IllegalStateException("/i18n is not on the classpath");
        }

        try (Stream<Path> entries = Files.list(Paths.get(i18n.toURI()))) {
            return entries.filter(p -> p.getFileName().toString().endsWith(".properties"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static List<String> findDuplicates(Path bundle) throws IOException {
        DuplicateTrackingProperties properties = new DuplicateTrackingProperties();

        try (Reader reader = Files.newBufferedReader(bundle, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        return properties.duplicates.stream()
                .map(key -> bundle.getFileName() + " duplicate key: " + key)
                .collect(Collectors.toList());
    }

    private static class DuplicateTrackingProperties extends Properties {

        private final Set<String> seen = new HashSet<>();
        private final List<String> duplicates = new ArrayList<>();

        @Override
        public synchronized Object put(Object key, Object value) {
            if (!seen.add(String.valueOf(key))) {
                duplicates.add(String.valueOf(key));
            }
            return super.put(key, value);
        }
    }
}


