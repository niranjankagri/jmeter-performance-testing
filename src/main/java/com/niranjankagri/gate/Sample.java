package com.niranjankagri.gate;

/**
 * One row of JMeter's results file.
 *
 * @param label         the sampler or transaction name
 * @param elapsedMillis how long it took
 * @param success       whether JMeter counted it as passed
 */
record Sample(String label, long elapsedMillis, boolean success) {
}
