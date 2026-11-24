package com.sudosleep.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(Google google, Facebook facebook) {

    public OAuthProperties {
        if (google == null) {
            google = new Google("");
        }
        if (facebook == null) {
            facebook = new Facebook("", "");
        }
    }

    public record Google(String webClientId) {
    }

    public record Facebook(String appId, String appSecret) {
    }
}
