package com.example.dijasaliou.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Désérialiseur Jackson flexible pour LocalDateTime.
 * Accepte les formats envoyés par le frontend Angular :
 *   - "2026-02-01T00:00:00"   (ISO avec heure)
 *   - "2026-02-01T00:00:00.000" (ISO avec millisecondes)
 *   - "2026-02-01"             (ISO date seule)
 *   - "01/02/2026"             (format français)
 */
public class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    public FlexibleLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getText();
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();

        // ISO avec heure (avec ou sans millisecondes)
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) { /* essayer le suivant */ }

        // ISO date seule → minuit
        try {
            return LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) { /* essayer le suivant */ }

        // Format français dd/MM/yyyy → minuit
        try {
            return LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IOException("Format de date non reconnu : '" + raw +
                    "'. Formats acceptés : 2026-02-01, 2026-02-01T00:00:00, 01/02/2026");
        }
    }
}
