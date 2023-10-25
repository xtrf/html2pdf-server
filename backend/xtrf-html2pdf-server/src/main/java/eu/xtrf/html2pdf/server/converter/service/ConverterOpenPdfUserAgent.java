package eu.xtrf.html2pdf.server.converter.service;

import eu.xtrf.html2pdf.server.converter.exception.ProcessingFailureException;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class ConverterOpenPdfUserAgent extends ITextUserAgent {

    private final String resourcePath;
    private final String systemDomain;

    public ConverterOpenPdfUserAgent(ITextOutputDevice outputDevice, SharedContext sharedContext, String resourcePath, String systemDomain) {
        super(outputDevice);
        setSharedContext(sharedContext);
        this.resourcePath = resourcePath;
        this.systemDomain = systemDomain;
    }


    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        File file = new File(uri);
        if (file.exists()) {
            try {
                return new FileInputStream(new File(uri));
            } catch (IOException e) {
                throw new ProcessingFailureException(e.getMessage(), e);
            }
        } else {
            try {
                URL url = new URL(uri);
                if (!isFileFromSystemTmpDir(url) && !isAllowedSource(url)) {
                    throw new ProcessingFailureException("URL " + removeResourceSubPath(uri) + " leads to an unauthorized source.");
                }
                return url.openStream();
            } catch (MalformedURLException e) {
                throw new ProcessingFailureException("URL " + removeResourceSubPath(uri) + " malformed.", e);
            } catch (IOException e) {
                throw new ProcessingFailureException(e.getMessage(), e);
            }
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

    private boolean isFileFromSystemTmpDir(URL url) {
        File tmpFile = new File("/tmp");
        if (tmpFile.exists()) {
            String tmpDirPath = tmpFile.getAbsolutePath().replace("\\", "/");
            String urlFilePath = new File(url.getFile()).getPath().replace("\\", "/");
            return urlFilePath.startsWith(tmpDirPath);
        }
        return false;
    }
}
