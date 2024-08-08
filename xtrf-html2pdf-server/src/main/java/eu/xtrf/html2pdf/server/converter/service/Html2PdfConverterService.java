package eu.xtrf.html2pdf.server.converter.service;

import eu.xtrf.html2pdf.server.converter.dto.ConvertDocumentRequestDto;

import java.io.File;
import java.io.IOException;

public interface Html2PdfConverterService {
    File generatePdfToFile(ConvertDocumentRequestDto dto) throws IOException;
}
