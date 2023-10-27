package eu.xtrf.html2pdf.server.converter.service;

import com.google.common.collect.ImmutableMap;
import eu.xtrf.html2pdf.server.converter.dto.ConvertDocumentRequestDto;
import eu.xtrf.html2pdf.server.converter.dto.ConvertDocumentRequestWithHash;
import eu.xtrf.html2pdf.server.converter.dto.ResourceDto;
import eu.xtrf.html2pdf.server.converter.exception.ProcessingFailureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class Html2PdfConverterServiceImpl implements Html2PdfConverterService {
    private AtomicInteger requestCounter = new AtomicInteger();

    static final Map<String, String> replacementMap = ImmutableMap.<String, String>builder()
            .put("&nbsp;", "&#160;")
            .put("&lt;", "&#60;")
            .put("&gt;", "&#62;")
            .put("&amp;", "&#38;")
            .put("&quot;", "&#34;")
            .put("&apos;", "&#39;")
            .put("&cent;", "&#162;")
            .put("&pound;", "&#163;")
            .put("&yen;", "&#165;")
            .put("&euro;", "&#8364;")
            .put("&copy;", "&#169;")
            .put("&reg;", "&#174;")
            .build();

    private final RendererProvider rendererProvider;
    private final FontService fontService;

    @Autowired
    public Html2PdfConverterServiceImpl(RendererProvider rendererProvider, FontService fontService) {
        this.rendererProvider = rendererProvider;
        this.fontService = fontService;
    }

    @Override
    public File generatePdfToFile(ConvertDocumentRequestDto dto) {
        ConvertDocumentRequestWithHash dtoWithHash = new ConvertDocumentRequestWithHash(dto, requestCounter.incrementAndGet());
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String resourcesPath = prepareResourcesDir(dtoWithHash);
            File tempPdfFile = generatePdfToFile(dtoWithHash.getConvertDocumentRequestDto().getThemeContent(),
                    dtoWithHash.getConvertDocumentRequestDto().getDocumentContent(),
                    dtoWithHash.getConvertDocumentRequestDto().getStyles(),
                    resourcesPath, dtoWithHash.getConvertDocumentRequestDto().getSystemDomain());
            stopWatch.stop();

            log.info(String.format(
                    "[%s] Rendered document for request %s in %s",
                    dtoWithHash.getConvertDocumentRequestDto().getClientId(), dtoWithHash.getRequestHash(), Duration.ofNanos(stopWatch.getNanoTime()).toString()));

            return tempPdfFile;
        } catch (Exception ex) {
            log.warn(String.format("[%s] Unable to process a request %s: %s", dtoWithHash.getConvertDocumentRequestDto().getClientId(), dtoWithHash.getRequestHash(), ex.getMessage()));

            throw new ProcessingFailureException(String.format("Unable to process the request, error: %s", ex.getMessage()), ex);
        } finally {
            try {
                clearResources(dtoWithHash.getRequestHash(), dto.getTempDirectoryPath());
            } catch (IOException e) {
                log.warn("Cannot clear resource dir for request " + dtoWithHash.getRequestHash());
            }
        }
    }

    private String prepareResourcesDir(ConvertDocumentRequestWithHash requestWithHash) throws IOException {
        File resourcesDirPath = getResourcePath(requestWithHash.getRequestHash(), requestWithHash.getConvertDocumentRequestDto().getTempDirectoryPath());
        Files.createDirectories(resourcesDirPath.toPath());
        requestWithHash.getConvertDocumentRequestDto().getResources().forEach(resource -> saveResource(resourcesDirPath, resource));
        return resourcesDirPath.getAbsolutePath();
    }

    private void clearResources(String requestHash, String tempDirectoryPath) throws IOException {
        FileUtils.deleteDirectory(getResourcePath(requestHash, tempDirectoryPath));
    }

    private File getResourcePath(String requestHash, String tempDirectoryPath) {
        return new File(tempDirectoryPath, requestHash);
    }

    private void saveResource(File dirPath, ResourceDto dto) {
        byte[] data = Base64.decodeBase64(dto.getData());
        File resourcePath = new File(dirPath, dto.getFilename());
        try (OutputStream os = Files.newOutputStream(resourcePath.toPath())) {
            os.write(data);
        } catch (IOException exception) {
            throw new ProcessingFailureException(exception.getMessage(), exception);
        }
    }

    private File generatePdfToFile(String themeContent, String documentContent, String styles, String resourcesPath, String systemDomain) throws IOException {
        ITextRenderer renderer = rendererProvider.prepareRenderer(resourcesPath, systemDomain, styles);
        fontService.loadFontsToRenderer(resourcesPath, renderer);

        File tempPdfFile = File.createTempFile("generated_", ".pdf");

        String pageWithTheme = themeContent.replace("#DOCUMENT_CONTENT", documentContent);

        htmlToPdf(renderer, pageWithTheme, tempPdfFile, resourcesPath);

        return tempPdfFile;
    }

    private static String replaceHtmlEntitiesNamesWithNumbers(String xhtml) {
        for (Map.Entry<String, String> entry : replacementMap.entrySet()) {
            xhtml = xhtml.replace(entry.getKey(), entry.getValue());
        }
        return xhtml;
    }

    private static String htmlToXhtml(String inputHTML) {
        Document document = Jsoup.parse(inputHTML, "UTF-8");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return replaceHtmlEntitiesNamesWithNumbers(document.html());
    }

    private void htmlToPdf(ITextRenderer renderer, String html, File outputPdf, String resourcesPath) throws IOException {
        renderer.setDocumentFromString(htmlToXhtml(html), toUrlString(resourcesPath));
        renderer.layout();
        try (OutputStream outputStream = Files.newOutputStream(outputPdf.toPath())) {
            renderer.createPDF(outputStream);
        }
    }

    private String toUrlString(String resourcePath) {
        try {
            return new File(resourcePath).toURI().toURL().toString();
        } catch (MalformedURLException e) {
            log.error("Invalid resourcePath URL");
            throw new ProcessingFailureException(String.format("Invalid resourcePath URL: %s", e.getMessage()), e);
        }
    }

}
