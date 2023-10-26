package eu.xtrf.html2pdf.server.converter.service;

import eu.xtrf.html2pdf.server.converter.exception.ProcessingFailureException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;

public class ConverterOpenPdfUserAgentTest {

    ConverterOpenPdfUserAgent converterOpenPdfUserAgent = new ConverterOpenPdfUserAgent(null, null, "/res", "xtrf.test.domain", "* {font-family: 'Roboto', 'Open Sans', 'Sans Serif'; font-size: 11px;} header {position: running(header);}");

    @Test(expectedExceptions = ProcessingFailureException.class,
            expectedExceptionsMessageRegExp = ".* leads to an unauthorized source.")
    public void should_throw_exception_because_external_resource_url_is_not_allowed() {
        // given
        String uri = "https://example.com/file.jpg";

        // when & then
        converterOpenPdfUserAgent.resolveAndOpenStream(uri);
    }

    @Test
    public void should_not_throw_unauthorized_source_exception_because_localhost_resource_url_is_allowed() {
        // given
        String uri = "http://localhost/file.jpg";

        // when & then (we do not expect unauthorized source only not existing file)
        try {
            converterOpenPdfUserAgent.resolveAndOpenStream(uri);
        } catch (ProcessingFailureException e) {
            Assert.assertTrue(e.getCause() instanceof FileNotFoundException || e.getCause() instanceof ConnectException);
        }
    }

    @Test(expectedExceptions = ProcessingFailureException.class,
            expectedExceptionsMessageRegExp = ".* leads to an unauthorized source.")
    public void should_throw_exception_because_local_file_url_is_not_allowed() {
        // given
        String uri = "file:" + this.getClass().getClassLoader().getResource("test_file.txt").getPath();

        // when & then
        converterOpenPdfUserAgent.resolveAndOpenStream(uri);
    }

    @Test
    public void should_not_throw_unauthorized_source_exception_because_system_tmp_file_resource_url_is_allowed() {
        // given
        String tmpDir = new File("/tmp").getAbsolutePath();
        String uri = "file:" + tmpDir + "/html2pdf/f829a2090fea607cdc43827a2fc7af921e9a4aa0/styles.css";

        // when & then (we do not expect unauthorized source only not existing file)
        try {
            converterOpenPdfUserAgent.resolveAndOpenStream(uri);
        } catch (ProcessingFailureException e) {
            Assert.assertTrue(e.getCause() instanceof FileNotFoundException);
        }
    }
}