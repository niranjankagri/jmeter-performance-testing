package com.niranjankagri.gate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Statistics")
class LabelStatsTest {

    @Test
    void tc201_p95IsTheValueNinetyFivePercentAreAtOrBelow() {
        // Given: 1 to 100 ms, in reverse order, because the input need not be sorted
        List<Sample> samples = new ArrayList<>();
        for (long millis = 100; millis >= 1; millis--) {
            samples.add(new Sample("x", millis, true));
        }

        // When / Then
        assertThat(LabelStats.of("x", samples).p95Millis()).isEqualTo(95);
    }

    @Test
    void tc202_oneSampleIsItsOwnP95() {
        // When / Then: the smallest case of the rank calculation
        assertThat(LabelStats.of("x", List.of(new Sample("x", 7, true))).p95Millis()).isEqualTo(7);
    }

    @Test
    void tc203_theErrorShareCountsFailedSamples() {
        // Given
        List<Sample> samples = Samples.none().passed("x", 3, 10).failed("x", 1).list();

        // When / Then
        assertThat(LabelStats.of("x", samples).errorPercent()).isEqualTo(25.0);
    }
}
