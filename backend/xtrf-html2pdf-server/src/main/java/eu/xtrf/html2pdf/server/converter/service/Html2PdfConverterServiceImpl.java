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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    public File generatePdfToFile(ConvertDocumentRequestDto dto) throws IOException {
        ConvertDocumentRequestWithHash dtoWithHash = new ConvertDocumentRequestWithHash(dto, requestCounter.incrementAndGet());
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String resourcesPath = prepareResourcesDir(dtoWithHash);
            File tempPdfFile = generatePdfToFile(dtoWithHash.getConvertDocumentRequestDto().getThemeContent(),
                    dtoWithHash.getConvertDocumentRequestDto().getDocumentContent(),
                    dtoWithHash.getConvertDocumentRequestDto().getStyles(),
                    resourcesPath);
            stopWatch.stop();

            log.info(String.format(
                    "[%s] Rendered document for request %s in %s",
                    dtoWithHash.getConvertDocumentRequestDto().getClientId(), dtoWithHash.getRequestHash(), Duration.ofNanos(stopWatch.getNanoTime()).toString()));

            return tempPdfFile;
        } catch (Exception ex) {
            log.warn(String.format("[%s] Unable to process a request %s: %s", dtoWithHash.getConvertDocumentRequestDto().getClientId(), dtoWithHash.getRequestHash(), ex.getMessage()));

            throw new ProcessingFailureException(String.format("Unable to process the request, error: %s", ex.getMessage()));
        } finally {
            try {
                clearResources(dtoWithHash.getRequestHash());
            } catch (IOException e) {
                log.warn("Cannot clear resource dir for request " + dtoWithHash.getRequestHash());
            }
        }
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
        return new File("/tmp/html2pdf" + File.separator + requestHash + File.separator).getAbsolutePath();
    }

    private void saveResource(String dirPath, ResourceDto dto) {
        byte[] data = Base64.decodeBase64(dto.getData());
        try (OutputStream os = new FileOutputStream(dirPath + File.separator + dto.getFilename())) {
            os.write(data);
        } catch (IOException exception) {
            throw new ProcessingFailureException(exception.getMessage());
        }
    }

    private File generatePdfToFile(String themeContent, String documentContent, String styles, String resourcesPath) throws IOException {
        prepareStylesFile(styles, resourcesPath);
        ITextRenderer renderer = rendererProvider.prepareRenderer(resourcesPath);
        fontService.loadFontsToRenderer(resourcesPath, renderer);

        File tempPdfFile = File.createTempFile("generated_", ".pdf");

        String pageWithTheme = themeContent.replace("#DOCUMENT_CONTENT", documentContent);

        htmlToPdf(renderer, pageWithTheme, tempPdfFile, resourcesPath);

        return tempPdfFile;
    }

    private void prepareStylesFile(String styles, String resourcesPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(resourcesPath + "/styles.css"))) {
            fos.write(styles.getBytes());
        }
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
        try (OutputStream outputStream = new FileOutputStream(outputPdf)) {
            renderer.createPDF(outputStream);
        }
    }

    private String toUrlString(String resourcePath) {
        try {
            return new URL("file:/" + resourcePath + "/").toString();
        } catch (MalformedURLException e) {
            log.error("Invalid resourcePath URL");
            throw new ProcessingFailureException(String.format("Invalid resourcePath URL: %s", e.getMessage()));
        }
    }
}
