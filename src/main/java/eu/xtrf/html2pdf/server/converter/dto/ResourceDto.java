package eu.xtrf.html2pdf.server.converter.dto;

import lombok.Value;

import javax.validation.constraints.NotNull;


@Value
public class ResourceDto {
    String uid;
    String data;
    @NotNull
    ResourceType type;

    public String getFilename() {
        return uid + type.getFileExtension();
    }

    public enum ResourceType {
        IMAGE(""),
        TTF_FONT(".ttf"),
        OTF_FONT(".otf");

        private final String fileExtension;

        ResourceType(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String getFileExtension() {
            return fileExtension;
        }
    }

}
