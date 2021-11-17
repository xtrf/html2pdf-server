package eu.xtrf.html2pdf.server.converter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;

@Component
public class RendererProviderImpl implements RendererProvider {
    private final FontService fontService;

    @Autowired
    public RendererProviderImpl(FontService fontService) {
        this.fontService = fontService;
    }

    @Override
    public ITextRenderer prepareRenderer(String resourcePath) throws IOException {
        ITextRenderer renderer = new ITextRenderer();
        SharedContext sharedContext = renderer.getSharedContext();
        sharedContext.setPrint(true);
        sharedContext.setInteractive(false);
        sharedContext.setUserAgentCallback(new ConverterOpenPdfUserAgent(renderer.getOutputDevice(), sharedContext, resourcePath));
        sharedContext.getTextRenderer().setSmoothingThreshold(0);

        fontService.loadFontsToRendererFromResources(renderer, resourcePath);
        return renderer;
    }
}
