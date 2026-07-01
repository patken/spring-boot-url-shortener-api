package com.patken.api.url_shortener.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.patken.api.url_shortener.util.UtilsForTest.BASE_URL;
import static com.patken.api.url_shortener.util.UtilsForTest.buildUrlEntity;
import static org.junit.jupiter.api.Assertions.*;

class ShortenUrlPageAssemblerTest {

    private ShortenUrlPageAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ShortenUrlPageAssembler(new UrlMapperImpl());
        ReflectionTestUtils.setField(assembler, "deployUrl", BASE_URL);
    }

    @Test
    @DisplayName("Maps records, total and a next link when more pages remain")
    void toResponseWithNextLink() {
        var pageRequest = PageRequest.of(0, 5);
        var page = new PageImpl<>(List.of(buildUrlEntity(10L), buildUrlEntity(5L)), pageRequest, 12);

        var response = assembler.toResponse(page);

        assertEquals(2, response.getRecords().size());
        assertEquals(12, response.getTotal());
        assertEquals(BASE_URL + "?page=1&limit=5", response.getNext());
    }

    @Test
    @DisplayName("No next link on the last page")
    void toResponseLastPageHasNoNext() {
        var pageRequest = PageRequest.of(0, 5);
        var page = new PageImpl<>(List.of(buildUrlEntity(10L)), pageRequest, 1);

        var response = assembler.toResponse(page);

        assertEquals(1, response.getRecords().size());
        assertNull(response.getNext());
    }

    @Test
    @DisplayName("Empty page yields no records and no next link")
    void toResponseEmpty() {
        var pageRequest = PageRequest.of(0, 5);
        var page = new PageImpl<com.patken.api.url_shortener.entity.UrlEntity>(List.of(), pageRequest, 0);

        var response = assembler.toResponse(page);

        assertTrue(response.getRecords().isEmpty());
        assertNull(response.getNext());
    }
}
