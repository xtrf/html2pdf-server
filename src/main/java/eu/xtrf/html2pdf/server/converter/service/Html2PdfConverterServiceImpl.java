package eu.xtrf.html2pdf.server.converter.service;

import com.google.common.collect.ImmutableMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Component
public class Html2PdfConverterServiceImpl implements Html2PdfConverterService {
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
    public File generatePdfToFile(String themeContent, String documentContent, String styles, String resourcesPath) throws IOException {
        prepareStylesFile(styles, resourcesPath);
        ITextRenderer renderer = rendererProvider.prepareRenderer();
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
        renderer.setDocumentFromString(htmlToXhtml(html), resourcesPath);
        renderer.layout();
        try (OutputStream outputStream = new FileOutputStream(outputPdf)) {
            renderer.createPDF(outputStream);
        }
    }
}
