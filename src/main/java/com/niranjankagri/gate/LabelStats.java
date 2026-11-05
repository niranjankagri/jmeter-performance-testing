package com.niranjankagri.gate;

import java.util.List;

/**
 * The numbers of one label.
 *
 * @param label        the transaction name
 * @param count        how many samples
 * @param errorPercent the share of failed samples
 * @param p95Millis    the 95th percentile of the response time
 */
record LabelStats(String label, int count, double errorPercent, long p95Millis) {

    /**
     * @param label   the label
     * @param samples its samples, at least one
     * @return their numbers
     */
    static LabelStats of(String label, List<Sample> samples) {
        long[] sorted = samples.stream().mapToLong(Sample::elapsedMillis).sorted().toArray();
        long failed = samples.stream().filter(sample -> !sample.success()).count();
        // Nearest rank: the value 95 % of the samples are at or below
        int rank = (int) Math.ceil(0.95 * sorted.length);
        return new LabelStats(label, sorted.length, 100.0 * failed / sorted.length, sorted[rank - 1]);
    }
}
