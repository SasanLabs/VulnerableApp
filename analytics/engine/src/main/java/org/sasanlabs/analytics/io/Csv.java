package org.sasanlabs.analytics.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 CSV reader and writer. Rows end with {@code \n} so the files diff cleanly in git
 * on every platform.
 */
public final class Csv {

    private Csv() {}

    public static void write(Path file, List<String> header, List<List<String>> rows)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, header);
        for (List<String> row : rows) {
            appendRow(sb, row);
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Reads every data row, skipping the header. Returns an empty list if the file is absent. */
    public static List<List<String>> readRows(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<List<String>> rows = parse(Files.readString(file, StandardCharsets.UTF_8));
        return rows.isEmpty() ? rows : new ArrayList<>(rows.subList(1, rows.size()));
    }

    static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    private static void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append('\n');
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0
                && value.indexOf('"') < 0
                && value.indexOf('\n') < 0
                && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
