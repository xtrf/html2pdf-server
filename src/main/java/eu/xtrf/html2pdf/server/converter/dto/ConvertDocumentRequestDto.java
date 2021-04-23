package eu.xtrf.html2pdf.server.converter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Collection;
import java.util.stream.Collectors;

@JsonDeserialize(builder = ConvertDocumentRequestDto.ConvertDocumentRequestDtoBuilder.class)
@Builder
@Value
public class ConvertDocumentRequestDto extends RequestWithHash{

    String content;
    String header;
    String footer;
    String clientId;
    Collection<ResourceDto> resources;

    public ConvertDocumentRequestDto(String requestHash, String content, String header, String footer, String clientId, Collection<ResourceDto> resources) {
        super(requestHash);
        this.content = content;
        this.header = header;
        this.footer = footer;
        this.clientId = clientId;
        this.resources = resources;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class ConvertDocumentRequestDtoBuilder {
        public ConvertDocumentRequestDto build() {
            String requestHash = DigestUtils.sha1Hex(clientId + header + content + footer +
                    resources.stream().map(ResourceDto::getRequestHash).collect(Collectors.joining()));
            return new ConvertDocumentRequestDto(requestHash, content, header, footer, clientId, resources);
        }
    }
}
