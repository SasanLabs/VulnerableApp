package org.sasanlabs.analytics.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvTest {

    @TempDir Path dir;

    @Test
    void roundTrip_preservesCommasQuotesAndEmptyCells() throws Exception {
        Path file = dir.resolve("nested/out.csv");
        List<List<String>> rows =
                List.of(
                        List.of("plain", "", "a,b"),
                        List.of("say \"hi\"", "multi\nline", "p/default"));

        Csv.write(file, List.of("x", "y", "z"), rows);

        assertThat(Files.readString(file)).startsWith("x,y,z\nplain,,\"a,b\"\n\"say \"\"hi\"\"\",");
        assertThat(Csv.readRows(file)).isEqualTo(rows);
    }

    @Test
    void readRows_ofMissingFile_isEmpty() throws Exception {
        assertThat(Csv.readRows(dir.resolve("absent.csv"))).isEmpty();
    }

    @Test
    void parse_acceptsCrlfLineEndings() {
        assertThat(Csv.parse("a,b\r\n1,2\r\n"))
                .containsExactly(List.of("a", "b"), List.of("1", "2"));
    }
}
