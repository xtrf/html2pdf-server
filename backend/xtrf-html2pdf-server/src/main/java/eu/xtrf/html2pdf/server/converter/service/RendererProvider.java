package eu.xtrf.html2pdf.server.converter.service;

import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;

public interface RendererProvider {
    ITextRenderer prepareRenderer(String resourcePath) throws IOException;
}
