package eu.xtrf.html2pdf.server.converter.service;

import eu.xtrf.html2pdf.server.converter.exception.ProcessingFailureException;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

public class ConverterOpenPdfUserAgent extends ITextUserAgent {

    public ConverterOpenPdfUserAgent(ITextOutputDevice outputDevice, SharedContext sharedContext) {
        super(outputDevice);
        setSharedContext(sharedContext);
    }


    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        try {
            File file = new File(uri);
            if (file.exists()) {
                return new FileInputStream(new File(uri));
            } else {
                return new URL(uri).openStream();
            }
        } catch (Exception e2) {
            throw new ProcessingFailureException();
        }

    }
}
