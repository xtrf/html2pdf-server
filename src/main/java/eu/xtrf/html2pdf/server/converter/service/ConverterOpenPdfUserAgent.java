package eu.xtrf.html2pdf.server.converter.service;

import eu.xtrf.html2pdf.server.converter.exception.ProcessingFailureException;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ConverterOpenPdfUserAgent extends ITextUserAgent {

    private final String resourcePath;

    public ConverterOpenPdfUserAgent(ITextOutputDevice outputDevice, SharedContext sharedContext, String resourcePath) {
        super(outputDevice);
        setSharedContext(sharedContext);
        this.resourcePath = resourcePath;
    }


    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        File file = new File(uri);
        if (file.exists()) {
            try {
                return new FileInputStream(new File(uri));
            } catch (IOException e) {
                throw new ProcessingFailureException(e.getMessage());
            }
        } else {
            try {
                return new URL(uri).openStream();
            } catch (MalformedURLException e) {
                throw new ProcessingFailureException("URL " + removeResourceSubPath(uri) + " malformed.");
            } catch (IOException e) {
                throw new ProcessingFailureException(e.getMessage());
            }
        }
    }

    private String removeResourceSubPath(String path) {
        return path.replace(resourcePath, "");
    }
}
