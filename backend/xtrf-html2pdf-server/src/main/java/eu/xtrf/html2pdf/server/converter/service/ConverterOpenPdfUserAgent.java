package eu.xtrf.html2pdf.server.converter.service;

import eu.xtrf.html2pdf.server.converter.exception.ProcessingFailureException;
import org.apache.commons.io.IOUtils;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import static java.lang.String.format;

public class ConverterOpenPdfUserAgent extends ITextUserAgent {

    private final String resourcePath;
    private final String systemDomain;
    private final String styleCss;
    private final String styleCssUri;

    public ConverterOpenPdfUserAgent(ITextOutputDevice outputDevice, SharedContext sharedContext, String resourcePath, String systemDomain, String styleCss) {
        super(outputDevice);
        setSharedContext(sharedContext);
        this.resourcePath = resourcePath;
        this.systemDomain = systemDomain;
        this.styleCss = styleCss;
        this.styleCssUri = ("file:/" + resourcePath + "styles.css").replace("\\", "/");
    }


    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        if (isStylesCssFileUri(uri)) {
            return IOUtils.toInputStream(styleCss, Charset.defaultCharset());
        }
        try {
            URL url = new URL(uri);
            if (!isAllowedSource(url)) {
                throw new ProcessingFailureException(format("URL %s leads to an unauthorized source.", removeResourceSubPath(uri)));
            }
            return url.openStream();
        } catch (MalformedURLException e) {
            throw new ProcessingFailureException(format("URL %s malformed.", removeResourceSubPath(uri)));
        } catch (IOException e) {
            throw new ProcessingFailureException(format("IOException when trying to read from %s", removeResourceSubPath(uri)), e);
        }
    }

    private String removeResourceSubPath(String path) {
        return path.replace(resourcePath, "");
    }

    private boolean isAllowedSource(URL url) {
        if (!isValidProtocol(url)) {
            return false;
        }
        String host = url.getHost();
        if (host == null) {
            return false;
        }
        if (host.equals(systemDomain)) {
            return true;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            return inetAddress.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean isValidProtocol(URL url) {
        String protocol = url.getProtocol();
        return "http".equals(protocol) || "https".equals(protocol);
    }

    private boolean isStylesCssFileUri(String uri) {
        return uri != null && uri.replace("\\", "/").equals(styleCssUri);
    }
}
