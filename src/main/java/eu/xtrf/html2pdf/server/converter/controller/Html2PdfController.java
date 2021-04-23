package eu.xtrf.html2pdf.server.converter.controller;

import eu.xtrf.html2pdf.server.converter.dto.ConvertDocumentRequestDto;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

@RestController
@Slf4j
public class Html2PdfController {
    private final Html2PdfConverterService converterService;

    @Autowired
    public Html2PdfController(Html2PdfConverterService converterService) {
        this.converterService = converterService;
    }

    @PostMapping(path = "/v1/convert")
    public ResponseEntity convertDocument(@RequestBody ConvertDocumentRequestDto dto) {
        InputStreamResource pdfFileResource = null;
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String resourcesPath = prepareResourcesDir(dto);
            File tempPdfFile = converterService.generatePdfToFile(dto.getContent(), dto.getHeader(), dto.getFooter(), resourcesPath);
            stopWatch.stop();

            pdfFileResource = new InputStreamResource(new FileInputStream(tempPdfFile));
            log.info(String.format(
                    "[%s] Rendered document for request %s in %s",
                    dto.getClientId(), dto.getRequestHash(), Duration.ofNanos(stopWatch.getNanoTime()).toString()));

            return ResponseEntity
                    .ok()
                    .contentLength(tempPdfFile.length())
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(pdfFileResource);
        } catch (Exception ex) {
            log.warn(String.format("[%s] Unable to process a request %s: %s", dto.getClientId(), dto.getRequestHash(), ex.getMessage()));

            return new ResponseEntity(String.format("Unable to process the request, error: %s", ex.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                clearResources(dto);
            } catch (IOException e) {
                log.warn("Cannot clear resource dir for request " + dto.getRequestHash());
            }
        }
    }

    private String prepareResourcesDir(ConvertDocumentRequestDto dto) throws IOException {
        String resourcesDirPath = getResourcePath(dto);
        Files.createDirectories(Paths.get(resourcesDirPath));
        dto.getResources().forEach(resource -> saveResource(resourcesDirPath, resource));
        return resourcesDirPath;
    }

    private void clearResources(ConvertDocumentRequestDto dto) throws IOException {
        FileUtils.deleteDirectory(new File(getResourcePath(dto)));
    }

    private String getResourcePath(ConvertDocumentRequestDto dto) {
        return "/tmp/html2pdf" + File.separator + dto.getRequestHash();
    }

    private void saveResource(String dirPath, ResourceDto dto) {
        byte[] data = Base64.decodeBase64(dto.getData());
        try (OutputStream os = new FileOutputStream(dirPath + File.separator + dto.getUid())) {
            os.write(data);
        } catch(IOException exception) {
            throw new ProcessingFailureException();
        }
    }
}
