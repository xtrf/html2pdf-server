package eu.xtrf.html2pdf.server.converter.exception;

public class ProcessingFailureException extends RuntimeException{
    public ProcessingFailureException(String msg) {
        super(msg);
    }
}
