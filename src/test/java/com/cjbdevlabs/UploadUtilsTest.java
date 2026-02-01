package com.cjbdevlabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UploadUtilsTest {

    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @Test
    void buildFileNameShouldReturnHeaderPlusExtensionWhenHeaderIsValid() {
        var fileNameHeader = "my-report-2026";
        var actual = UploadUtils.buildFileName(fileNameHeader);
        var expected  = "my-report-2026.tar";
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Should generate UUID for blank input: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void buildFileNameShouldGenerateUuidWhenHeaderIsBlank(String blankHeader) {
        var result = UploadUtils.buildFileName(blankHeader);
        assertNotNull(result);
        assertTrue(result.endsWith(".tar"), "File name must end with .tar extension");
        var uuidPart = result.replace(".tar", "");
        assertTrue(uuidPart.matches(UUID_REGEX), () -> "Expected a valid UUID but got: " + uuidPart);
    }
}
