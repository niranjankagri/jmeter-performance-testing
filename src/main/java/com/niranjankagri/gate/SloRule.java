package com.niranjankagri.gate;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

/**
 * What one label must achieve.
 *
 * @param label           the transaction name
 * @param p95Millis       the highest allowed 95th percentile
 * @param maxErrorPercent the highest allowed share of failures
 * @param minSamples      the fewest samples that count as a real measurement
 */
record SloRule(String label, long p95Millis, double maxErrorPercent, int minSamples) {

    /**
     * Reads a file with lines such as {@code Book seats.p95Ms=500}.
     *
     * @param file the properties file
     * @return one rule per label
     * @throws IOException if the file can't be read
     */
    static List<SloRule> load(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        TreeSet<String> labels = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            labels.add(key.substring(0, key.lastIndexOf('.')));
        }
        List<SloRule> rules = new ArrayList<>();
        for (String label : labels) {
            rules.add(new SloRule(label,
                    Long.parseLong(required(properties, label + ".p95Ms")),
                    Double.parseDouble(required(properties, label + ".maxErrorPercent")),
                    Integer.parseInt(required(properties, label + ".minSamples"))));
        }
        return rules;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing SLO setting: " + key);
        }
        return value;
    }
}
