package com.microservice.framework.logging.core.masking;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import net.logstash.logback.mask.ValueMasker;
import net.logstash.logback.decorate.JsonGeneratorDecorator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Logstash JSON generator decorator that masks sensitive string values.
 *
 * @author Andy Yang
 */
public class MaskingJsonGeneratorDecorator implements JsonGeneratorDecorator, DeferredProcessingAware {

    private final List<ValueMasker> valueMaskers = new ArrayList<>();
    private List<CustomRule> customRules = new ArrayList<>();

    public void setCustomRules(List<CustomRule> customRules) {
        this.customRules = customRules == null ? new ArrayList<>() : new ArrayList<>(customRules);
        this.valueMaskers.clear();
        for (CustomRule rule : this.customRules) {
            this.valueMaskers.add((context, value) -> rule.mask(value));
        }
    }

    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        return new JsonGeneratorDelegate(generator) {
            @Override
            public void writeString(String text) throws java.io.IOException {
                super.writeString(mask(text));
            }

            @Override
            public void writeStringField(String fieldName, String value) throws java.io.IOException {
                super.writeStringField(fieldName, mask(value));
            }
        };
    }

    @Override
    public void prepareForDeferredProcessing() {
        // No mutable event state is retained.
    }

    private String mask(String value) {
        Object masked = value;
        for (ValueMasker masker : valueMaskers) {
            masked = masker.mask(null, masked);
        }
        return masked == null ? null : masked.toString();
    }

    public static final class CustomRule {
        private final String name;
        private final Pattern pattern;
        private final String replacement;

        public CustomRule(String name, String regex, String replacement) {
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.pattern = Pattern.compile(Objects.requireNonNull(regex, "regex must not be null"));
            this.replacement = Objects.requireNonNull(replacement, "replacement must not be null");
        }

        public String name() {
            return name;
        }

        String mask(Object value) {
            if (value == null) {
                return null;
            }
            return pattern.matcher(value.toString()).replaceAll(replacement);
        }
    }
}
