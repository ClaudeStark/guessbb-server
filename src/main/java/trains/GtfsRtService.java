package ch.uzh.ifi.hase.soprafs26.trains;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;


@Service
@Slf4j
public class GtfsRtService {

    private final GtfsConfig config;

    public GtfsRtService(GtfsConfig config) { this.config = config; }

    public FeedMessage fetchFeed() throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
            new URL(config.getRtUrl()).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Authorization", "Bearer " + config.getApiToken());
        conn.setRequestProperty("User-Agent", "SwissTrainTracker/1.0");

        try (InputStream in = conn.getInputStream()) {
            return FeedMessage.parseFrom(in);
        }
    }
}