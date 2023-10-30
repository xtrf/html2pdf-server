package eu.xtrf.html2pdf.server.converter.service;

import eu.xtrf.html2pdf.server.converter.exception.ProcessingFailureException;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ConverterOpenPdfUserAgentTest {

    String resourcePath = "c/system\\temp/f829a2090fea6";
    String sampleStyleCss = "* {font-family: 'Roboto'; font-size: 11px;}";
    ConverterOpenPdfUserAgent converterOpenPdfUserAgent = new ConverterOpenPdfUserAgent(null, null, resourcePath, "xtrf.test.domain", sampleStyleCss, false);

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
            assertTrue(e.getCause() instanceof FileNotFoundException || e.getCause() instanceof ConnectException);
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
        String uri = "file:" + File.separator + resourcePath + File.separator + "styles.css";

        // when
        InputStream inputStream = converterOpenPdfUserAgent.resolveAndOpenStream(uri);

        // then
        String result = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
        assertEquals(result, sampleStyleCss);
    }
}