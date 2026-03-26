package ch.uzh.ifi.hase.soprafs26.trains;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "gtfs")
@Component
public class GtfsConfig {
    private String apiToken;
    private String staticUrl;
    private String rtUrl;
    private String localZipPath;
    private String reloadCron;

    // standard getters/setters
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }  
    public String getStaticUrl() { return staticUrl; }
    public void setStaticUrl(String staticUrl) { this.staticUrl = staticUrl; }
    public String getRtUrl() { return rtUrl; }
    public void setRtUrl(String rtUrl) { this.rtUrl = rtUrl; }
    public String getLocalZipPath() { return localZipPath; }
    public void setLocalZipPath(String localZipPath) { this.localZipPath = localZipPath; }
    public String getReloadCron() { return reloadCron; }
    public void setReloadCron(String reloadCron) { this.reloadCron = reloadCron; }
}