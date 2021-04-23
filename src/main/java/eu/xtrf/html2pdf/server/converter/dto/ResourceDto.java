package eu.xtrf.html2pdf.server.converter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.codec.digest.DigestUtils;

@JsonDeserialize(builder = ResourceDto.ResourceDtoBuilder.class)
@Builder
@Value
public class ResourceDto extends RequestWithHash{
    String uid;
    String data;

    public ResourceDto(String requestHash, String uid, String data) {
        super(requestHash);
        this.uid = uid;
        this.data = data;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class ResourceDtoBuilder {
        public ResourceDto build() {
            String requestHash = DigestUtils.sha1Hex(uid + data);
            return new ResourceDto(requestHash, uid, data);
        }
    }
}
