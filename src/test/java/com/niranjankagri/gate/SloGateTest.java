package com.niranjankagri.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The gate must fail for the right reasons, and must never pass on missing data. */
@DisplayName("SLO gate")
class SloGateTest {

    private static final SloRule BOOKING = new SloRule("Book seats", 500, 1.0, 10);

    @TempDir
    Path folder;

    private List<String> misses(List<Sample> samples) {
        List<String> misses = new ArrayList<>();
        SloGate.report(samples, List.of(BOOKING), misses);
        return misses;
    }

    @Test
    void tc221_resultsWithinTheSloPass() {
        // When / Then
        assertThat(misses(Samples.none().passed("Book seats", 100, 400).list())).isEmpty();
    }

    @Test
    void tc222_aSlowP95IsAMiss() {
        // When / Then
        assertThat(misses(Samples.none().passed("Book seats", 100, 501).list()))
                .containsExactly("Book seats: p95 501 ms");
    }

    @Test
    void tc223_exactlyOnTheLimitPasses() {
        // When / Then
        assertThat(misses(Samples.none().passed("Book seats", 100, 500).list())).isEmpty();
    }

    @Test
    void tc224_tooManyErrorsAreAMiss() {
        // Given: 2 failures in 100 samples, against a limit of 1 %
        List<Sample> samples = Samples.none().passed("Book seats", 98, 100).failed("Book seats", 2).list();

        // When / Then
        assertThat(misses(samples)).containsExactly("Book seats: errors 2.00 %");
    }

    @Test
    void tc225_aLabelWithNoSamplesIsAMissNotAPass() {
        // When / Then: a renamed transaction would otherwise disappear from the gate unnoticed
        assertThat(misses(Samples.none().passed("Something else", 100, 1).list()))
                .containsExactly("Book seats: no samples");
    }

    @Test
    void tc226_tooFewSamplesAreAMiss() {
        // When / Then: nine fast samples prove nothing
        assertThat(misses(Samples.none().passed("Book seats", 9, 1).list()))
                .containsExactly("Book seats: only 9 samples");
    }

    @Test
    void tc227_theGateFailsTheBuildWhenEnforcing() throws IOException {
        // Given
        String[] args = files(true);

        // When / Then
        assertThatThrownBy(() -> SloGate.main(args)).hasMessageContaining("SLOs missed");
    }

    @Test
    void tc228_reportOnlyModeWritesTheReportAndDoesNotFail() throws IOException {
        // Given
        String[] args = files(false);

        // When
        SloGate.main(args);

        // Then
        assertThat(Files.readString(Path.of(args[2]))).contains("**MISSED**");
    }

    @Test
    void tc229_anIncompleteSloFileIsAnError() throws IOException {
        // Given
        Path slo = Files.writeString(folder.resolve("slo.properties"), "Login.p95Ms=100\n");

        // When / Then
        assertThatThrownBy(() -> SloRule.load(slo)).hasMessageContaining("Login.maxErrorPercent");
    }

    private String[] files(boolean enforce) throws IOException {
        Path results = Files.writeString(folder.resolve("r.csv"), "label,elapsed,success\nLogin,900,true\n");
        Path slo = Files.writeString(folder.resolve("s.properties"),
                "Login.p95Ms=100\nLogin.maxErrorPercent=0\nLogin.minSamples=1\n");
        return new String[] {results.toString(), slo.toString(), folder.resolve("report.md").toString(),
            String.valueOf(enforce)};
    }
}
