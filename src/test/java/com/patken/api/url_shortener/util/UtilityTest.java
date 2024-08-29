package com.patken.api.url_shortener.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import static org.junit.jupiter.api.Assertions.*;

class UtilityTest {

    @Test
    @DisplayName("Test buildShortenUrl Successfully")
    void testBuildShortenUrlWithSuccess(){
        var result = Utility.buildShortenUrl();
        assertAll("Grouped Assertions for shorten result",
                () -> assertTrue(StringUtils.isNotBlank(result)),
                () -> assertEquals(10, result.length()));
    }
}