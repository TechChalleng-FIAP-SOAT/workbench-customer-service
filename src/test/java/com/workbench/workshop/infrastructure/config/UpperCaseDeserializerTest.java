package com.workbench.workshop.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpperCaseDeserializerTest {

    record WithUpper(@UpperCase @JsonDeserialize(using = UpperCaseDeserializer.class) String name) {}
    record WithoutUpper(@JsonDeserialize(using = UpperCaseDeserializer.class) String name) {}

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void uppercasesAnnotatedField() throws Exception {
        WithUpper r = mapper.readValue("{\"name\":\" joão \"}", WithUpper.class);
        assertEquals("JOÃO", r.name());
    }

    @Test
    void leavesUnannotatedFieldUntouched() throws Exception {
        WithoutUpper r = mapper.readValue("{\"name\":\"joão\"}", WithoutUpper.class);
        assertEquals("joão", r.name());
    }

    @Test
    void handlesNullValue() throws Exception {
        WithUpper r = mapper.readValue("{\"name\":null}", WithUpper.class);
        assertNull(r.name());
    }
}
