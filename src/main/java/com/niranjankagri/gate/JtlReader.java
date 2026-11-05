package com.niranjankagri.gate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads JMeter's CSV results file (a JTL file). Columns are found by name, so a change of column order is harmless. */
final class JtlReader {

    private JtlReader() {
    }

    /**
     * @param file the results file
     * @return every sample
     * @throws IOException if the file can't be read
     */
    static List<Sample> read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Empty results file: " + file);
        }
        List<String> header = splitCsv(lines.get(0));
        int label = column(header, "label");
        int elapsed = column(header, "elapsed");
        int success = column(header, "success");
        List<Sample> samples = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = splitCsv(line);
            samples.add(new Sample(cells.get(label), Long.parseLong(cells.get(elapsed)),
                    Boolean.parseBoolean(cells.get(success))));
        }
        return samples;
    }

    private static int column(List<String> header, String name) {
        int index = header.indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("No column '" + name + "' in the results file");
        }
        return index;
    }

    /** Splits one CSV line. A failure message can hold commas, so quoted cells stay whole. */
    static List<String> splitCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // Two quotes inside a quoted cell stand for one quote
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }
}
