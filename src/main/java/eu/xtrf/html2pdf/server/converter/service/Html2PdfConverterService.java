package eu.xtrf.html2pdf.server.converter.service;

import java.io.File;
import java.io.IOException;

public interface Html2PdfConverterService {
    File generatePdfToFile(String content, String header, String footer, String styles, String resourcesPath) throws IOException;

    File generatePdfToFile(String themeContent, String documentContent, String styles, String resourcesPath) throws IOException;
}
