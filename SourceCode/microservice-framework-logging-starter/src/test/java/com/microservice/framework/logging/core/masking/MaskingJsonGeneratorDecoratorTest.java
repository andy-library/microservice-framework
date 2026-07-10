package com.microservice.framework.logging.core.masking;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskingJsonGeneratorDecoratorTest {

    @Test
    @DisplayName("Custom masking rules should be applied to JSON string values")
    void customMaskingRulesShouldApplyToJsonStringValues() throws Exception {
        MaskingJsonGeneratorDecorator decorator = new MaskingJsonGeneratorDecorator();
        decorator.setCustomRules(List.of(new MaskingJsonGeneratorDecorator.CustomRule(
                "MEMBER_CARD",
                "(MC-)(\\d{4})(\\d{4})",
                "$1****$3")));

        StringWriter writer = new StringWriter();
        JsonGenerator generator = decorator.decorate(new JsonFactory().createGenerator(writer));
        generator.writeStartObject();
        generator.writeStringField("memberCard", "MC-12345678");
        generator.writeEndObject();
        generator.close();

        assertTrue(writer.toString().contains("MC-****5678"), writer.toString());
    }
}
