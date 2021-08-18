package eu.xtrf.html2pdf.server.converter.controller;

import eu.xtrf.html2pdf.server.converter.dto.ConvertDocumentRequestDto;
import eu.xtrf.html2pdf.server.converter.dto.ConvertDocumentRequestWithHash;
import eu.xtrf.html2pdf.server.converter.dto.ResourceDto;
import eu.xtrf.html2pdf.server.converter.exception.ProcessingFailureException;
import eu.xtrf.html2pdf.server.converter.service.Html2PdfConverterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.validation.Valid;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Slf4j
public class Html2PdfController {
    private final Html2PdfConverterService converterService;
    private AtomicInteger requestCounter = new AtomicInteger();

    @Autowired
    public Html2PdfController(Html2PdfConverterService converterService) {
        this.converterService = converterService;
    }

    @GetMapping("")
    public String getHome() {
        return "xtrf to html home";
    }

    @PostMapping(path = "/v1/convert")
    public ResponseEntity convertDocument(@Valid @RequestBody ConvertDocumentRequestDto dto) {
        try {
            return buildResponse(converterService.generatePdfToFile(dto));
        } catch (Exception ex) {
            return new ResponseEntity(String.format("Unable to process the request, error: %s", ex.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {IllegalArgumentException.class})
    protected void handleIllegalArgumentException(RuntimeException e, WebRequest request) {
    }

    private ResponseEntity buildResponse(File resultPdfFile) throws IOException {
        InputStreamResource pdfFileResource = new InputStreamResource(new FileInputStream(resultPdfFile));
        return ResponseEntity
                .ok()
                .contentLength(resultPdfFile.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(pdfFileResource);
    }
}
