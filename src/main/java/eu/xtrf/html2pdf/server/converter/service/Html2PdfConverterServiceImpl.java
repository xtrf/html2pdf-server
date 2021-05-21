package eu.xtrf.html2pdf.server.converter.service;

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
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class Html2PdfConverterServiceImpl implements Html2PdfConverterService {
    private static final String NEW_PAGE_TAG = "#np#";

    @Override
    public File generatePdfToFile(String content, String header, String footer, String styles, String resourcesPath) throws IOException {
        List<String> pages = divideOnPages(content);
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(resourcesPath);
        prepareStylesFile(styles, resourcesPath);

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

    @Override
    public File generatePdfToFile(String themeContent, String documentContent, String styles, String resourcesPath) throws IOException {
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(resourcesPath);
        prepareStylesFile(styles, resourcesPath);

        File tempPdfFile = File.createTempFile("generated_", ".pdf");
        FileOutputStream fos = new FileOutputStream(tempPdfFile);

        String pageWithTheme = themeContent.replace("#DOCUMENT_CONTENT", documentContent);
        HtmlConverter.convertToPdf(pageWithTheme, fos, converterProperties);
        fos.close();

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

    private void prepareStylesFile(String styles, String resourcesPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(resourcesPath + "/styles.css"))) {
            fos.write(styles.getBytes());
        }
    }
}
