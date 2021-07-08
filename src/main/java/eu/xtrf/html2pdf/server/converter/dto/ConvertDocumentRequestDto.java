package eu.xtrf.html2pdf.server.converter.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.Collections;

@Jacksonized
@Builder
@Value
@Validated
public class ConvertDocumentRequestDto {

    String documentContent;
    String themeContent;
    @NotBlank
    String clientId;
    String styles;
    @Builder.Default
    @Valid
    Collection<ResourceDto> resources = Collections.emptyList();
}
