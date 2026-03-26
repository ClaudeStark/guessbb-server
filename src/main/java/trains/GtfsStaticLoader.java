package ch.uzh.ifi.hase.soprafs26.trains;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import ch.uzh.ifi.hase.soprafs26.trains.model.*;



@Service
@Slf4j
public class GtfsStaticLoader {

    @Value("${gtfs.static-url}") private String staticUrl;
    @Value("${gtfs.api-token}")  private String apiToken;
    @Value("${gtfs.local-zip-path}") private String localZipPath;

    private final GtfsDataStore store;

    public GtfsStaticLoader(GtfsDataStore store) { this.store = store; }

    // Run once on startup, then on schedule
    @PostConstruct
    public void loadOnStartup() {
        CompletableFuture.runAsync(this::downloadAndParse);
    }

    @Scheduled(cron = "${gtfs.reload-cron}")
    public void scheduledReload() {
        log.info("Scheduled GTFS static reload starting...");
        downloadAndParse();
    }

    private void downloadAndParse() {
        try {
            Path zipPath = Path.of(localZipPath);
            Files.createDirectories(zipPath.getParent());

            log.info("Downloading GTFS static ZIP...");
            downloadZip(zipPath);

            log.info("Parsing GTFS static data (this may take a minute)...");
            parseAndLoad(zipPath);

            log.info("GTFS static loaded at {}", store.loadedAt());
        } catch (Exception e) {
            log.error("Failed to load GTFS static data", e);
        }
    }

    private void downloadZip(Path dest) throws Exception {
        // The dataset page redirects to the actual file download
        // You can also hardcode the direct resource URL if you know it
        HttpURLConnection conn = (HttpURLConnection)
            new URL(staticUrl).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("User-Agent", "SwissTrainTracker/1.0");

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("ZIP downloaded: {} MB", Files.size(dest) / 1_000_000);
    }

    private void parseAndLoad(Path zipPath) throws Exception {
        Map<String, Stop>            stops     = new HashMap<>(50_000);
        Map<String, List<StopTime>>  stopTimes = new HashMap<>(100_000);
        Map<String, String>          tripShapes = new HashMap<>(100_000);
        Map<String, String>          tripNames  = new HashMap<>(100_000);
        Map<String, List<ShapePoint>> shapes   = new HashMap<>(10_000);

        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            parseStops(zip, stops);
            parseTrips(zip, tripShapes, tripNames);
            parseStopTimes(zip, stopTimes);
            parseShapes(zip, shapes);
        }

        // Sort stop sequences
        stopTimes.values().forEach(list ->
            list.sort(Comparator.comparingInt(StopTime::stopSeq)));
        shapes.values().forEach(list ->
            list.sort(Comparator.comparingInt(ShapePoint::seq)));

        store.update(stops, stopTimes, tripShapes, tripNames, shapes);
    }

    // ── Parsing helpers (stream line-by-line to avoid OOM) ───────────────────

    private void parseStops(ZipFile zip, Map<String, Stop> out) throws Exception {
        try (BufferedReader r = entryReader(zip, "stops.txt")) {
            String[] h = r.readLine().split(",");
            int idI = col(h, "stop_id"), latI = col(h, "stop_lat"),
                lonI = col(h, "stop_lon");
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = splitLine(line);
                out.put(strip(f[idI]), new Stop(strip(f[idI]),
                    Double.parseDouble(strip(f[latI])),
                    Double.parseDouble(strip(f[lonI]))));
            }
        }
    }

    private void parseTrips(ZipFile zip, Map<String, String> shapes,
                            Map<String, String> names) throws Exception {
        try (BufferedReader r = entryReader(zip, "trips.txt")) {
            String[] h = r.readLine().split(",");
            int tripI = col(h, "trip_id"), nameI = col(h, "trip_short_name"),
                shpI  = colOpt(h, "shape_id");
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = splitLine(line);
                String tid = strip(f[tripI]);
                names.put(tid, strip(f[nameI]));
                if (shpI >= 0 && !f[shpI].isBlank())
                    shapes.put(tid, strip(f[shpI]));
            }
        }
    }

    private void parseStopTimes(ZipFile zip,
                                Map<String, List<StopTime>> out) throws Exception {
        try (BufferedReader r = entryReader(zip, "stop_times.txt")) {
            String[] h = r.readLine().split(",");
            int tripI = col(h, "trip_id"), stopI = col(h, "stop_id"),
                seqI  = col(h, "stop_sequence"),
                arrI  = col(h, "arrival_time"), depI = col(h, "departure_time");
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = splitLine(line);
                String tid = strip(f[tripI]);
                out.computeIfAbsent(tid, k -> new ArrayList<>())
                   .add(new StopTime(tid, strip(f[stopI]),
                       Integer.parseInt(strip(f[seqI])),
                       parseHms(strip(f[arrI])), parseHms(strip(f[depI]))));
            }
        }
    }

    private void parseShapes(ZipFile zip,
                             Map<String, List<ShapePoint>> out) throws Exception {
        ZipEntry e = zip.getEntry("shapes.txt");
        if (e == null) { log.warn("No shapes.txt in ZIP"); return; }
        try (BufferedReader r = entryReader(zip, "shapes.txt")) {
            String[] h = r.readLine().split(",");
            int shpI = col(h, "shape_id"), latI = col(h, "shape_pt_lat"),
                lonI = col(h, "shape_pt_lon"), seqI = col(h, "shape_pt_sequence"),
                dstI = colOpt(h, "shape_dist_traveled");
            String line; int autoSeq = 0;
            while ((line = r.readLine()) != null) {
                String[] f = splitLine(line);
                double dist = (dstI >= 0 && !strip(f[dstI]).isEmpty())
                    ? Double.parseDouble(strip(f[dstI])) : autoSeq++;
                out.computeIfAbsent(strip(f[shpI]), k -> new ArrayList<>())
                   .add(new ShapePoint(strip(f[shpI]),
                       Double.parseDouble(strip(f[latI])),
                       Double.parseDouble(strip(f[lonI])),
                       Integer.parseInt(strip(f[seqI])), dist));
            }
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private static BufferedReader entryReader(ZipFile zip, String name)
            throws Exception {
        return new BufferedReader(
            new InputStreamReader(zip.getInputStream(zip.getEntry(name)), "UTF-8"),
            1 << 16);  // 64 KB buffer for large files
    }

    private static int parseHms(String s) {
        if (s == null || s.isBlank()) return 0;
        String[] p = s.split(":");
        return Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60
             + Integer.parseInt(p[2]);
    }

    private static String strip(String s) {
        return s == null ? "" : s.trim().replace("\"", "");
    }

    private static String[] splitLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private static int col(String[] h, String name) {
        for (int i = 0; i < h.length; i++)
            if (h[i].trim().replace("\"","").equalsIgnoreCase(name)) return i;
        throw new IllegalArgumentException("Column not found: " + name);
    }
    private static int colOpt(String[] h, String name) {
        for (int i = 0; i < h.length; i++)
            if (h[i].trim().replace("\"","").equalsIgnoreCase(name)) return i;
        return -1;
    }
}