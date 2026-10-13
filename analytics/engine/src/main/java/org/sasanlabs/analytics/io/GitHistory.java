package org.sasanlabs.analytics.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the commit history of a results file. Each nightly benchmark run commits its results file,
 * so that history is the record of past runs and the engine does not need a store of its own.
 */
public class GitHistory {

    /** A commit that changed a file, with its committer date as {@code YYYY-MM-DD}. */
    public record Revision(String commit, String date) {}

    private final Path repoRoot;

    public GitHistory(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    /** Commits that changed {@code file}, newest first. Empty if git is unavailable. */
    public List<Revision> revisions(String file) {
        List<Revision> revisions = new ArrayList<>();
        try {
            String out = git("log", "--format=%H %cs", "--", file);
            for (String line : out.split("\n")) {
                String[] parts = line.trim().split(" ");
                if (parts.length == 2) {
                    revisions.add(new Revision(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            System.err.println("WARN: could not read git history of " + file + ": " + e);
        }
        return revisions;
    }

    /** The last commit that changed {@code file}, or {@code null} if there is none. */
    public Revision latest(String file) {
        List<Revision> revisions = revisions(file);
        return revisions.isEmpty() ? null : revisions.get(0);
    }

    /** {@code file} as of {@code commit}. */
    public String show(String commit, String file) throws IOException {
        return git("show", commit + ":" + file);
    }

    private String git(String... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        Process process =
                new ProcessBuilder(command)
                        .directory(repoRoot.toFile())
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start();
        String stdout;
        try (InputStream out = process.getInputStream()) {
            stdout = readAll(out);
        }
        try {
            if (process.waitFor() != 0) {
                throw new IOException("git " + String.join(" ", args) + " failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running git", e);
        }
        return stdout;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        in.transferTo(buffer);
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
