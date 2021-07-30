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

    @PostMapping(path = "/v1/convert")
    public ResponseEntity convertDocument(@Valid @RequestBody ConvertDocumentRequestDto dto) {
        return convertDocument(new ConvertDocumentRequestWithHash(dto, requestCounter.incrementAndGet()));
    }

    private ResponseEntity convertDocument(ConvertDocumentRequestWithHash dto) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String resourcesPath = prepareResourcesDir(dto);
            File tempPdfFile = converterService.generatePdfToFile(dto.getConvertDocumentRequestDto().getThemeContent(),
                    dto.getConvertDocumentRequestDto().getDocumentContent(),
                    dto.getConvertDocumentRequestDto().getStyles(),
                    resourcesPath);
            stopWatch.stop();

            log.info(String.format(
                    "[%s] Rendered document for request %s in %s",
                    dto.getConvertDocumentRequestDto().getClientId(), dto.getRequestHash(), Duration.ofNanos(stopWatch.getNanoTime()).toString()));

            return buildResponse(tempPdfFile);
        } catch (Exception ex) {
            log.warn(String.format("[%s] Unable to process a request %s: %s", dto.getConvertDocumentRequestDto().getClientId(), dto.getRequestHash(), ex.getMessage()));

            return new ResponseEntity(String.format("Unable to process the request, error: %s", ex.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                clearResources(dto.getRequestHash());
            } catch (IOException e) {
                log.warn("Cannot clear resource dir for request " + dto.getRequestHash());
            }
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

    private String prepareResourcesDir(ConvertDocumentRequestWithHash requestWithHash) throws IOException {
        String resourcesDirPath = getResourcePath(requestWithHash.getRequestHash());
        Files.createDirectories(Paths.get(resourcesDirPath));
        requestWithHash.getConvertDocumentRequestDto().getResources().forEach(resource -> saveResource(resourcesDirPath, resource));
        return resourcesDirPath;
    }

    private void clearResources(String requestHash) throws IOException {
        FileUtils.deleteDirectory(new File(getResourcePath(requestHash)));
    }

    private String getResourcePath(String requestHash) {
        return "/tmp/html2pdf" + File.separator + requestHash + File.separator;
    }

    private void saveResource(String dirPath, ResourceDto dto) {
        byte[] data = Base64.decodeBase64(dto.getData());
        try (OutputStream os = new FileOutputStream(dirPath + File.separator + dto.getFilename())) {
            os.write(data);
        } catch (IOException exception) {
            throw new ProcessingFailureException(exception.getMessage());
        }
    }
}
