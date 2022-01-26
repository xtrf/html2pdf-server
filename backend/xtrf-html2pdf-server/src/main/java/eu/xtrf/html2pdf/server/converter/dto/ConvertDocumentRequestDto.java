package eu.xtrf.html2pdf.server.converter.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;
import java.util.Collections;

@Jacksonized
@Builder
@Value
public class ConvertDocumentRequestDto {

    String documentContent;
    String themeContent;
    String clientId;
    String styles;
    @Builder.Default
    Collection<ResourceDto> resources = Collections.emptyList();
}
