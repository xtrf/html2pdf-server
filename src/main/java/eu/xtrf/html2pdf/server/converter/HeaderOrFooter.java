package eu.xtrf.html2pdf.server.converter;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.renderer.TableRenderer;

import java.io.ByteArrayOutputStream;

public class HeaderOrFooter implements IEventHandler {
    private Type type;
    private Document doc;
    private Table table;
    private float tableHeight;
    private float tableWidth;

    HeaderOrFooter(Type type, Document doc, Table table) {
        this.type = type;
        this.doc = doc;
        this.table = table;
        TableRenderer renderer = (TableRenderer) table.createRendererSubTree();
        renderer.setParent(new Document(new PdfDocument(new PdfWriter(new ByteArrayOutputStream()))).getRenderer());
        tableHeight = renderer.layout(new LayoutContext(new LayoutArea(0, PageSize.A4))).getOccupiedArea().getBBox().getHeight();
        tableWidth = renderer.layout(new LayoutContext(new LayoutArea(0, PageSize.A4))).getOccupiedArea().getBBox().getWidth();
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();

        PdfStream pdfStream = null;
        switch (type) {
            case HEADER: pdfStream = page.newContentStreamBefore(); break;
            case FOOTER: pdfStream = page.newContentStreamAfter(); break;
        }
        PdfCanvas canvas = new PdfCanvas(pdfStream, page.getResources(), pdfDoc);
        float x = pdfDoc.getDefaultPageSize().getX() + doc.getLeftMargin();
        float y = 0;
        switch (type) {
            case HEADER: y = pdfDoc.getDefaultPageSize().getTop() - doc.getTopMargin(); break;
            case FOOTER: y = pdfDoc.getDefaultPageSize().getBottom() + doc.getBottomMargin(); break;
        }
        Rectangle rect = new Rectangle(x, y, tableWidth, tableHeight);
        new Canvas(canvas, pdfDoc, rect).add(table);
    }

    public enum Type {
        HEADER,
        FOOTER
    }

}
