package eu.xtrf.html2pdf.server.converter.service;

import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;

public interface FontService {
    void loadFontsToRenderer(ITextRenderer renderer) throws IOException;
}
