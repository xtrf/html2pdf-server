package eu.xtrf.html2pdf.server.converter.dto;

import lombok.Value;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.stream.Collectors;

@Value
public class ConvertDocumentRequestWithHash {
    private ConvertDocumentRequestDto convertDocumentRequestDto;
    private String requestHash;

    public ConvertDocumentRequestWithHash(ConvertDocumentRequestDto convertDocumentRequestDto) {
        this.convertDocumentRequestDto = new ConvertDocumentRequestDto(convertDocumentRequestDto.getContent(),
                convertDocumentRequestDto.getHeader(),
                convertDocumentRequestDto.getFooter(),
                convertDocumentRequestDto.getClientId(),
                convertDocumentRequestDto.getStyles(),
                convertDocumentRequestDto.getResources());
        requestHash = computeRequestHash(convertDocumentRequestDto);
    }


    private String computeRequestHash(ConvertDocumentRequestDto convertDocumentRequestDto) {
        return DigestUtils.sha1Hex(convertDocumentRequestDto.getClientId() +
                convertDocumentRequestDto.getHeader() +
                convertDocumentRequestDto.getContent() +
                convertDocumentRequestDto.getFooter() +
                convertDocumentRequestDto.getResources().stream()
                        .map(ConvertDocumentRequestWithHash::getResourceDtoHash)
                        .collect(Collectors.joining()));
    }

    private static String getResourceDtoHash(ResourceDto resourceDto) {
        return DigestUtils.sha1Hex(resourceDto.getUid() + resourceDto.getData());
    }
}
