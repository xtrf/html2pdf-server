package eu.xtrf.html2pdf.server.converter.dto;

import lombok.Value;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.stream.Collectors;

@Value
public class ConvertDocumentRequestWithHash {
    private ConvertDocumentRequestDto convertDocumentRequestDto;
    private int requestCounter;
    private String requestHash;


    public ConvertDocumentRequestWithHash(ConvertDocumentRequestDto convertDocumentRequestDto, int requestCounter) {
        this.convertDocumentRequestDto = new ConvertDocumentRequestDto(convertDocumentRequestDto.getDocumentContent(),
                convertDocumentRequestDto.getThemeContent(),
                convertDocumentRequestDto.getClientId(),
                convertDocumentRequestDto.getStyles(),
                convertDocumentRequestDto.getSystemDomain(),
                convertDocumentRequestDto.getTempDirectoryPath(),
                convertDocumentRequestDto.getResources());
        this.requestCounter = requestCounter;
        requestHash = computeRequestHash();
    }


    private String computeRequestHash() {
        return DigestUtils.sha1Hex(convertDocumentRequestDto.getClientId() +
                convertDocumentRequestDto.getThemeContent() +
                convertDocumentRequestDto.getDocumentContent() +
                convertDocumentRequestDto.getResources().stream()
                        .map(ConvertDocumentRequestWithHash::getResourceDtoHash)
                        .collect(Collectors.joining()) +
                requestCounter);
    }

    private static String getResourceDtoHash(ResourceDto resourceDto) {
        return DigestUtils.sha1Hex(resourceDto.getUid() + resourceDto.getData());
    }
}
