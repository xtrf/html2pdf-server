package eu.xtrf.html2pdf.server.converter;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.AreaBreakType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class Html2PdfConverter {
    private static final Logger LOGGER = Logger.getLogger(Html2PdfConverter.class.getName());
    private static final String NEW_PAGE_TAG = "#np#";

    @RequestMapping(path = "/v1/html2pdf", method = RequestMethod.POST)
    public ResponseEntity greeting(@RequestParam(name = "content") String content,
                                   @RequestParam(name = "header") String header,
                                   @RequestParam(name = "footer") String footer,
                                   @RequestParam(name = "clientId") String clientId,
                                   @RequestParam(name = "resourcesPath", required = false) String resourcesPath) {
        try {
            String toHash = clientId + header + footer + content;
            String fileHash = DigestUtils.md5DigestAsHex(toHash.getBytes());

            String cacheLocation = System.getProperty("html2pdf.cache", "/tmp/html2pdf");
            String cacheSubdir = cacheLocation + File.separator + fileHash.substring(0, 1) + File.separator;
            String cacheFileName = String.format("%s", fileHash);

            if (Files.isReadable(Paths.get(cacheSubdir + cacheFileName))) {
                LOGGER.log(Level.INFO, String.format(
                        "[%s] Serving unchanged file %s from cache",
                        clientId, cacheFileName
                ));
                InputStreamResource pdfFileResource = new InputStreamResource(new FileInputStream(cacheSubdir + cacheFileName));
                return ResponseEntity
                        .ok()
                        .contentLength(Files.size(Paths.get(cacheSubdir + cacheFileName)))
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .header("X-Cache-hit: hit")
                        .body(pdfFileResource);
            }

            if (!Files.isWritable(Paths.get(cacheSubdir))) {
                Files.createDirectories(Paths.get(cacheSubdir));
            }

            // Create a PDF document
            Instant start = Instant.now();
            File tempPdfFile = generatePdfToFile(content, header, footer, resourcesPath);
            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();  //in millis

            LOGGER.log(Level.INFO, String.format(
                    "[%s] Rendered document in %s ms, saving to %s",
                    clientId, timeElapsed, cacheFileName
            ));

            Path newPath = tempPdfFile.toPath();
            try {
                newPath = Files.move(tempPdfFile.toPath(), Paths.get(cacheSubdir + cacheFileName));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, String.format(
                        "[%s] Failed to move temporary file %s to %s; error: %s",
                        clientId, tempPdfFile, cacheFileName, e.getMessage()
                ));
            }

            InputStreamResource pdfFileResource = new InputStreamResource(new FileInputStream(newPath.toString()));
            return ResponseEntity
                    .ok()
                    .contentLength(Files.size(newPath))
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(pdfFileResource);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, String.format("[%s] Unable to process a request", clientId), ex);

            return new ResponseEntity(String.format("Unable to process the request, error: %s", ex.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private File generatePdfToFile(String content, String header, String footer, String resourcesPath) throws IOException {
        List<String> pages = divideOnPages(content);
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(resourcesPath);

        File tempPdfFile = File.createTempFile("generated_", ".pdf");

        PdfWriter writer = new PdfWriter(tempPdfFile);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);

        // Convert to PDF elements with Table as root element
        List<IElement> headerElements = HtmlConverter.convertToElements(header, converterProperties);
        HeaderOrFooter headerHandler = new HeaderOrFooter(HeaderOrFooter.Type.HEADER, new Document(pdfDocument),
                (Table) headerElements.get(0));
        List<IElement> footerElements = HtmlConverter.convertToElements(footer, converterProperties);
        HeaderOrFooter footerHandler = new HeaderOrFooter(HeaderOrFooter.Type.FOOTER, new Document(pdfDocument),
                (Table) footerElements.get(0));

        // Assign event handlers
        pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

        // Convert
        for (String htmlPageContent : pages) {
            List<IElement> elements = HtmlConverter.convertToElements(htmlPageContent, converterProperties);
            for (IElement element : elements) {
                document.add((IBlockElement) element);
            }
            if (!pages.get(pages.size() - 1).equals(htmlPageContent)) {
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }
        }

        // Close the PDF document
        document.close();
        pdfDocument.close();

        return tempPdfFile;
    }

    private List<String> divideOnPages(String content) {
        List<String> pages = new ArrayList<>();

        StringBuilder currentPageContent = new StringBuilder();
        for (String line : content.split(System.lineSeparator())) {
            if (line.toLowerCase().contains(NEW_PAGE_TAG)) {
                int newPageTagePosition = line.toLowerCase().indexOf(NEW_PAGE_TAG);
                while (newPageTagePosition != -1) {
                    currentPageContent.append(line, 0, newPageTagePosition);
                    pages.add(currentPageContent.toString());

                    currentPageContent.setLength(0);
                    line = line.substring(newPageTagePosition + NEW_PAGE_TAG.length());
                    newPageTagePosition = line.toLowerCase().indexOf(NEW_PAGE_TAG);
                }
            } else {
                currentPageContent.append(line);
            }
        }
        pages.add(currentPageContent.toString());

        return pages;
    }
}
