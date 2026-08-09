package org.sasanlabs.internal.utility;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the i18n bundles against the defect fixed in #326 and again in #729.
 *
 * <p>A {@code .properties} entry continued with a trailing backslash has the leading whitespace of
 * the continuation line stripped by the parser. So when the character right before the backslash is
 * not a space, the last word of the line and the first word of the next one are loaded as a single
 * word: {@code code,\} followed by {@code generally} loads as {@code code,generally}. The space has
 * to sit before the backslash, which is the side the parser leaves alone.
 *
 * <p>Rather than guess which characters end a word, this test rebuilds each entry the way the
 * parser does and inspects every seam between two physical lines. That matters for entries where a
 * continuation line is empty once its leading whitespace is gone, since the text then joins across
 * it.
 */
class MessageBundleContinuationTest {

    /** Scripts that do not separate words with spaces, where a joined pair is not a defect. */
    private static final Set<Character.UnicodeScript> SCRIPTS_WITHOUT_WORD_SPACES =
            EnumSet.of(
                    Character.UnicodeScript.HAN,
                    Character.UnicodeScript.HIRAGANA,
                    Character.UnicodeScript.KATAKANA,
                    Character.UnicodeScript.THAI);

    /** Longest HTML entity the bundles use, {@code &quot;}, plus room to spare. */
    private static final int LONGEST_ENTITY = 12;

    @Test
    @DisplayName("i18n: no entry joins two words together across a line continuation")
    void noContinuationGluesTwoWords() throws Exception {
        List<Path> bundles = messageBundles();
        assertTrue(bundles.size() > 1, "expected to find the i18n bundles, found " + bundles);

        List<String> failures = new ArrayList<>();
        for (Path bundle : bundles) {
            failures.addAll(gluedContinuations(bundle));
        }

        if (!failures.isEmpty()) {
            fail(
                    "A continuation line joins two words together. Move the space to the end of the"
                            + " previous line, before the backslash:\n  "
                            + String.join("\n  ", failures));
        }
    }

    private static List<Path> messageBundles() throws URISyntaxException, IOException {
        URL i18n = MessageBundleContinuationTest.class.getResource("/i18n");
        assertNotNull(i18n, "/i18n is not on the classpath");
        try (Stream<Path> entries = Files.list(Paths.get(i18n.toURI()))) {
            return entries.filter(p -> p.getFileName().toString().endsWith(".properties"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static List<String> gluedContinuations(Path bundle) {
        List<String> lines;
        try {
            lines = Files.readAllLines(bundle, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + bundle, e);
        }

        List<String> found = new ArrayList<>();
        for (int i = 0; i < lines.size(); ) {
            if (!startsEntry(lines.get(i))) {
                i++;
                continue;
            }

            // Rebuild one entry, checking the seam wherever a continuation adds text.
            StringBuilder value = new StringBuilder();
            int lineOfLastText = i + 1;
            boolean firstLine = true;
            while (true) {
                String segment = lines.get(i);
                boolean continued = endsWithContinuation(segment);
                if (continued) {
                    segment = segment.substring(0, segment.length() - 1);
                }
                if (!firstLine) {
                    segment = stripLeading(segment);
                    if (!segment.isEmpty() && glues(value, segment)) {
                        found.add(
                                bundle.getFileName()
                                        + ":"
                                        + lineOfLastText
                                        + " loads as \""
                                        + lastWord(value.toString())
                                        + firstWord(segment)
                                        + "\"");
                    }
                }
                value.append(segment);
                if (!segment.isEmpty()) {
                    lineOfLastText = i + 1;
                }
                firstLine = false;

                i++;
                if (!continued || i == lines.size()) {
                    break;
                }
            }
        }
        return found;
    }

    /**
     * True when the text so far and the continuation about to be appended would load as one word. A
     * trailing hyphen is excluded because a continuation there is meant to join, {@code X-\}
     * followed by {@code Frame-Options}. Markup is excluded because {@code <br/>} followed by a
     * word is a line break, not a joined word.
     */
    private static boolean glues(CharSequence value, String continuation) {
        if (value.length() == 0) {
            return false;
        }
        int left = Character.codePointBefore(value, value.length());
        int right = continuation.codePointAt(0);
        return !Character.isWhitespace(left)
                && left != '-'
                && !endsWithMarkup(value)
                && isWordCharacter(right)
                && !writtenWithoutWordSpaces(left)
                && !writtenWithoutWordSpaces(right);
    }

    /** True for a tag such as {@code <br/>} and for an entity such as {@code &lt;/ol&gt;}. */
    private static boolean endsWithMarkup(CharSequence value) {
        int end = value.length() - 1;
        if (value.charAt(end) == '>') {
            return true;
        }
        if (value.charAt(end) != ';') {
            return false;
        }
        for (int i = end - 1; i >= 0 && i >= end - LONGEST_ENTITY; i--) {
            char c = value.charAt(i);
            if (c == '&') {
                return true;
            }
            if (!Character.isLetterOrDigit(c) && c != '#') {
                return false;
            }
        }
        return false;
    }

    /** A blank line and a comment do not start an entry, and a comment is never continued. */
    private static boolean startsEntry(String line) {
        String trimmed = stripLeading(line);
        return !trimmed.isEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("!");
    }

    /**
     * True when the line ends with an odd number of backslashes, the last one escaping the newline.
     */
    private static boolean endsWithContinuation(String line) {
        int backslashes = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    private static boolean isWordCharacter(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isLetterOrDigit(codePoint)
                || type == Character.LETTER_NUMBER
                || type == Character.OTHER_NUMBER
                // Combining marks are part of a word too: the last code point of "था" is a vowel
                // sign.
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK;
    }

    private static boolean writtenWithoutWordSpaces(int codePoint) {
        return SCRIPTS_WITHOUT_WORD_SPACES.contains(Character.UnicodeScript.of(codePoint));
    }

    private static String stripLeading(String line) {
        return line.stripLeading();
    }

    private static String lastWord(String text) {
        String[] parts = text.split("\\s");
        return parts[parts.length - 1];
    }

    private static String firstWord(String text) {
        return text.split("\\s")[0];
    }
}
