package com.kkooman.lightworkflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class StatusControllerUnitTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void healthReturnsServiceUnavailableDuringMaintenance() throws Exception {
        Path marker = temporaryDirectory.resolve("maintenance.marker");
        Files.createFile(marker);

        StatusController controller = new StatusController();
        ReflectionTestUtils.setField(controller, "maintenanceFilePath", marker.toString());

        var response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo("MAINTENANCE");
        assertThat(response.getBody().data()).isNull();
    }
}
