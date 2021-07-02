package eu.xtrf.html2pdf.server.converter.service;

import com.google.common.collect.ImmutableMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class Html2PdfConverterServiceImpl implements Html2PdfConverterService {
    static final Map<String, String> replacementMap = ImmutableMap.<String, String>builder()
            .put("&nbsp;","&#160;")
            .put("&lt;","&#60;")
            .put("&gt;","&#62;")
            .put("&amp;","&#38;")
            .put("&quot;","&#34;")
            .put("&apos;","&#39;")
            .put("&cent;","&#162;")
            .put("&pound;","&#163;")
            .put("&yen;","&#165;")
            .put("&euro;","&#8364;")
            .put("&copy;","&#169;")
            .put("&reg;","&#174;")
            .build();

    private static final Map<String, String> fontFamilies = ImmutableMap.<String, String>builder()
            .put("Lato", "Lato")
            .put("Noto_Sans_JP", "Noto Sans JP")
            .put("Noto_Sans_SC", "Noto Sans SC")
            .put("Open_Sans", "Open Sans")
            .put("Poppins", "Poppins")
            .put("Roboto", "Roboto")
            .put("Source_Sans_Pro", "Source Sans Pro")
            .put("Source_Serif_Pro", "Source Serif Pro")
            .build();

    private ITextRenderer renderer;

    public Html2PdfConverterServiceImpl() throws IOException {
        renderer = prepareRenderer();
    }

    @Override
    public File generatePdfToFile(String themeContent, String documentContent, String styles, String resourcesPath) throws IOException {
        prepareStylesFile(styles, resourcesPath);

        File tempPdfFile = File.createTempFile("generated_", ".pdf");

        String pageWithTheme = themeContent.replace("#DOCUMENT_CONTENT", documentContent);

        htmlToPdf(pageWithTheme, tempPdfFile, resourcesPath);

        return tempPdfFile;
    }

    private void prepareStylesFile(String styles, String resourcesPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(resourcesPath + "/styles.css"))) {
            fos.write(styles.getBytes());
        }
    }

    private static String replaceHtmlEntitiesNamesWithNumbers(String xhtml) {
        for (Map.Entry<String, String> entry: replacementMap.entrySet()) {
            xhtml = xhtml.replace(entry.getKey(), entry.getValue());
        }
        return xhtml;
    }

    private static String htmlToXhtml(String inputHTML) {
        Document document = Jsoup.parse(inputHTML, "UTF-8");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return replaceHtmlEntitiesNamesWithNumbers(document.html());
    }

    private ITextRenderer prepareRenderer() throws IOException {
        renderer = new ITextRenderer();
        SharedContext sharedContext = renderer.getSharedContext();
        sharedContext.setPrint(true);
        sharedContext.setInteractive(false);
        sharedContext.setUserAgentCallback(new ConverterOpenPdfUserAgent(renderer.getOutputDevice(), sharedContext));
        sharedContext.getTextRenderer().setSmoothingThreshold(0);

        loadFontsToRenderer(renderer);

        return renderer;
    }

    private void htmlToPdf(String html, File outputPdf, String resourcesPath) throws IOException {
        renderer.setDocumentFromString(htmlToXhtml(html), resourcesPath);
        renderer.layout();
        try (OutputStream outputStream = new FileOutputStream(outputPdf)) {
            renderer.createPDF(outputStream);
        }
    }


    private static void loadFontsToRenderer(ITextRenderer renderer) throws IOException {
        // this path to fonts directory works only inside docker, for local execution change to: ./src/main/resources/fonts
        for (String ttfPath : getTTFFiles("/fonts")) { // TODO: move path to configuration file
            String fontFamilyToOverride = findFamilyFont(ttfPath);
            if (fontFamilyToOverride != null) {
                renderer.getFontResolver().addFont(ttfPath, fontFamilyToOverride, "Identity-H", true, null);
            } else {
                renderer.getFontResolver().addFont(ttfPath, "Identity-H", true);
            }
        }
    }

    private static String findFamilyFont(String ttfPath) {
        for (Map.Entry<String, String> entry : fontFamilies.entrySet()) {
            if (ttfPath.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static List<String> getTTFFiles(String dir) {
        File fontsDir = new File(dir);
        List<String> ttfFilesPaths = new LinkedList<>();

        for (File file : fontsDir.listFiles()) {
            if (file.getName().endsWith(".ttf")) {
                ttfFilesPaths.add(file.getAbsolutePath());
            } else if (file.isDirectory()) {
                ttfFilesPaths.addAll(getTTFFiles(file.getAbsolutePath()));
            }
        }
        return ttfFilesPaths;
    }
}
