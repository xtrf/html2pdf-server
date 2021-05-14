package eu.xtrf.html2pdf.server.converter.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.Collections;

@Jacksonized
@Builder
@Value
@Validated
public class ConvertDocumentRequestDto {

    @NotBlank
    String content;
    @Builder.Default
    String header = "<table></table>";
    @Builder.Default
    String footer = "<table></table>";
    @NotBlank
    String clientId;
    String styles;
    @Builder.Default
    Collection<ResourceDto> resources = Collections.emptyList();
}
