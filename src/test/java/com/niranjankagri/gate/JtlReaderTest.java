package com.niranjankagri.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Results file")
class JtlReaderTest {

    @TempDir
    Path folder;

    private Path file(String content) throws IOException {
        return Files.writeString(folder.resolve("results.csv"), content);
    }

    @Test
    void tc211_columnsAreFoundByName() throws IOException {
        // Given: columns in an unusual order
        Path results = file("success,label,elapsed\ntrue,Login,42\nfalse,Book seats,7\n");

        // When / Then
        assertThat(JtlReader.read(results))
                .containsExactly(new Sample("Login", 42, true), new Sample("Book seats", 7, false));
    }

    @Test
    void tc212_aCommaInAQuotedMessageDoesNotShiftTheColumns() throws IOException {
        // Given: JMeter quotes a failure message holding a comma
        Path results = file("label,failureMessage,elapsed,success\nLogin,\"sold 3, capacity 2\",42,false\n");

        // When / Then
        assertThat(JtlReader.read(results)).containsExactly(new Sample("Login", 42, false));
    }

    @Test
    void tc213_twoQuotesInACellAreOneQuote() {
        // When / Then
        assertThat(JtlReader.splitCsv("a,\"say \"\"hi\"\"\",b")).isEqualTo(List.of("a", "say \"hi\"", "b"));
    }

    @Test
    void tc214_aMissingColumnIsAnError() throws IOException {
        // Given
        Path results = file("label,success\nLogin,true\n");

        // When / Then: a changed JMeter setting must stop the gate, not make it pass
        assertThatThrownBy(() -> JtlReader.read(results)).hasMessageContaining("elapsed");
    }
}
