package eu.xtrf.html2pdf.server.converter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RequestWithHash {
    private final String requestHash;
}
