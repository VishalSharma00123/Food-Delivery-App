package com.restaurent_service.restaurent_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    /** Absolute or relative directory on the host/EC2 disk (not S3). */
    private String dir = "./uploads";
}
