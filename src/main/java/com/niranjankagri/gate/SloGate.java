package com.niranjankagri.gate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JMeter reports numbers but has no built-in pass or fail for them. This gate adds it: it compares the results with
 * the SLOs, writes a Markdown table, and fails the build when an SLO is missed.
 */
public final class SloGate {

    private SloGate() {
    }

    /**
     * @param args the results file, the SLO file, the report file, and "true" to fail on a missed SLO
     * @throws IOException if a file can't be read or written
     */
    public static void main(String[] args) throws IOException {
        List<Sample> samples = JtlReader.read(Path.of(args[0]));
        List<SloRule> rules = SloRule.load(Path.of(args[1]));
        List<String> misses = new ArrayList<>();
        String report = report(samples, rules, misses);
        Files.writeString(Path.of(args[2]), report);
        System.out.println(report);
        if (!misses.isEmpty() && Boolean.parseBoolean(args[3])) {
            throw new IllegalStateException("SLOs missed: " + String.join("; ", misses));
        }
    }

    /**
     * Compares the samples with the rules.
     *
     * @param samples the results
     * @param rules   the SLOs
     * @param misses  receives one line per missed SLO
     * @return the report as Markdown
     */
    static String report(List<Sample> samples, List<SloRule> rules, List<String> misses) {
        Map<String, List<Sample>> byLabel = samples.stream().collect(Collectors.groupingBy(Sample::label));
        StringBuilder table = new StringBuilder("| Transaction | Samples | Errors | p95 | SLO | Result |\n");
        table.append("|---|---:|---:|---:|---|---|\n");
        for (SloRule rule : rules) {
            List<Sample> mine = byLabel.get(rule.label());
            // A label with no samples is a miss: a broken test plan must not look like a pass
            Optional<LabelStats> stats = Optional.ofNullable(mine).map(list -> LabelStats.of(rule.label(), list));
            List<String> problems = check(rule, stats);
            problems.forEach(problem -> misses.add(rule.label() + ": " + problem));
            table.append(String.format(Locale.ROOT, "| %s | %d | %.2f %% | %d ms | p95 <= %d ms, errors <= %.2f %% | %s |%n",
                    rule.label(), stats.map(LabelStats::count).orElse(0),
                    stats.map(LabelStats::errorPercent).orElse(0.0), stats.map(LabelStats::p95Millis).orElse(0L),
                    rule.p95Millis(), rule.maxErrorPercent(),
                    problems.isEmpty() ? "pass" : "**MISSED**: " + String.join(", ", problems)));
        }
        return table.toString();
    }

    private static List<String> check(SloRule rule, Optional<LabelStats> found) {
        if (found.isEmpty()) {
            return List.of("no samples");
        }
        LabelStats stats = found.get();
        List<String> problems = new ArrayList<>();
        if (stats.count() < rule.minSamples()) {
            problems.add("only " + stats.count() + " samples");
        }
        if (stats.p95Millis() > rule.p95Millis()) {
            problems.add("p95 " + stats.p95Millis() + " ms");
        }
        if (stats.errorPercent() > rule.maxErrorPercent()) {
            problems.add(String.format(Locale.ROOT, "errors %.2f %%", stats.errorPercent()));
        }
        return problems;
    }
}
