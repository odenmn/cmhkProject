package com.cmhk.business.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cmhk.admin")
public class AdminProperties {
    private String username;
    private String password;
    private String tokenSecret;
    private long accessTokenTtlHours = 12;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTokenSecret() { return tokenSecret; }
    public void setTokenSecret(String tokenSecret) { this.tokenSecret = tokenSecret; }
    public long getAccessTokenTtlHours() { return accessTokenTtlHours; }
    public void setAccessTokenTtlHours(long accessTokenTtlHours) { this.accessTokenTtlHours = accessTokenTtlHours; }
}
