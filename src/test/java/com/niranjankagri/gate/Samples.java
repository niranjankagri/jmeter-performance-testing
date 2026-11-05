package com.niranjankagri.gate;

import java.util.ArrayList;
import java.util.List;

/** Test data: builds a list of samples without a results file. */
final class Samples {

    private final List<Sample> samples = new ArrayList<>();

    static Samples none() {
        return new Samples();
    }

    /** Adds this many passed samples, all with the same time. */
    Samples passed(String label, int count, long millis) {
        for (int i = 0; i < count; i++) {
            samples.add(new Sample(label, millis, true));
        }
        return this;
    }

    /** Adds this many failed samples. */
    Samples failed(String label, int count) {
        for (int i = 0; i < count; i++) {
            samples.add(new Sample(label, 10, false));
        }
        return this;
    }

    List<Sample> list() {
        return samples;
    }
}
