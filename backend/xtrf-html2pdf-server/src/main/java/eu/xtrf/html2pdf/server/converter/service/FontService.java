package eu.xtrf.html2pdf.server.converter.service;

import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;

public interface FontService {
    void loadFontsToRendererFromResources(ITextRenderer renderer, String resourcePath) throws IOException;
    void loadFontsToRenderer(String dir, ITextRenderer renderer) throws IOException;
}
