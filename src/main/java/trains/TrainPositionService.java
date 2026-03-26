package ch.uzh.ifi.hase.soprafs26.trains;

import ch.uzh.ifi.hase.soprafs26.trains.model.ShapePoint;
import ch.uzh.ifi.hase.soprafs26.trains.model.Stop;
import ch.uzh.ifi.hase.soprafs26.trains.model.StopTime;
import ch.uzh.ifi.hase.soprafs26.trains.model.TrainPosition;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;


@Service
@Slf4j
public class TrainPositionService {

    private final GtfsDataStore store;
    private final GtfsRtService rtService;

    public TrainPositionService(GtfsDataStore store, GtfsRtService rtService) {
        this.store     = store;
        this.rtService = rtService;
    }

    public List<TrainPosition> findLinePositions(String lineShortName) {
        FeedMessage feed;
        try {
            feed = rtService.fetchFeed();
        } catch (Exception e) {
            log.error("Failed to fetch GTFS-RT feed", e);
            return List.of();
        }

        long nowSec = LocalTime.now(ZoneId.of("Europe/Zurich")).toSecondOfDay();
        List<TrainPosition> results = new ArrayList<>();

        for (FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasTripUpdate()) continue;

            TripUpdate tu     = entity.getTripUpdate();
            String     tripId = tu.getTrip().getTripId();

            if (!lineShortName.equals(store.tripNames().get(tripId))) continue;

            List<StopTime> scheduled = store.stopTimes().get(tripId);
            if (scheduled == null || scheduled.isEmpty()) continue;

            // Build delay map: stopSeq → delay seconds
            Map<Integer, Integer> delayBySeq = new HashMap<>();
            for (TripUpdate.StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
                int delay = stu.hasDeparture() ? stu.getDeparture().getDelay()
                          : stu.hasArrival()   ? stu.getArrival().getDelay()
                          : 0;
                delayBySeq.put(stu.getStopSequence(), delay);
            }

            // Apply delays (carry forward last known delay)
            int   runningDelay = 0;
            int[] rtDep        = new int[scheduled.size()];
            for (int i = 0; i < scheduled.size(); i++) {
                StopTime st = scheduled.get(i);
                if (delayBySeq.containsKey(st.stopSeq()))
                    runningDelay = delayBySeq.get(st.stopSeq());
                rtDep[i] = st.departureSec() + runningDelay;
            }

            // Find current segment
            int prevIdx = -1;
            for (int i = 0; i < rtDep.length - 1; i++) {
                if (rtDep[i] <= nowSec && nowSec < rtDep[i + 1]) {
                    prevIdx = i;
                    break;
                }
            }
            if (prevIdx < 0) continue;

            StopTime from = scheduled.get(prevIdx);
            StopTime to   = scheduled.get(prevIdx + 1);
            double frac   = Math.max(0.0, Math.min(1.0,
                (double)(nowSec - rtDep[prevIdx]) / (rtDep[prevIdx + 1] - rtDep[prevIdx])));

            double[] pos = interpolate(tripId, from, to, frac);
            results.add(new TrainPosition(
                tripId, pos[0], pos[1],
                "Between " + from.stopId() + " → " + to.stopId(),
                runningDelay));
        }
        return results;
    }

    // ── Interpolation ──────────────────────────────────────────────────────────

    private double[] interpolate(String tripId,
                                 StopTime from, StopTime to, double frac) {
        String shapeId = store.tripShapes().get(tripId);
        if (shapeId != null && store.shapes().containsKey(shapeId)) {
            return interpolateAlongShape(shapeId, from.stopId(), to.stopId(), frac);
        }
        return linearInterpolate(from.stopId(), to.stopId(), frac);
    }

    private double[] linearInterpolate(String fromId, String toId, double frac) {
        Stop s1 = store.stops().get(fromId);
        Stop s2 = store.stops().get(toId);
        if (s1 == null || s2 == null) return new double[]{0, 0};
        return new double[]{
            s1.lat() + frac * (s2.lat() - s1.lat()),
            s1.lon() + frac * (s2.lon() - s1.lon())
        };
    }

    private double[] interpolateAlongShape(String shapeId,
                                           String fromStopId, String toStopId,
                                           double frac) {
        List<ShapePoint> pts     = store.shapes().get(shapeId);
        Stop             fromStop = store.stops().get(fromStopId);
        Stop             toStop   = store.stops().get(toStopId);
        if (fromStop == null || toStop == null) return new double[]{0, 0};

        int fromIdx = nearestShapePoint(pts, fromStop);
        int toIdx   = nearestShapePoint(pts, toStop);
        if (fromIdx > toIdx) { int t = fromIdx; fromIdx = toIdx; toIdx = t; }

        List<ShapePoint> seg = pts.subList(fromIdx, toIdx + 1);
        if (seg.size() < 2) return new double[]{fromStop.lat(), fromStop.lon()};

        double   total   = 0;
        double[] segDist = new double[seg.size() - 1];
        for (int i = 0; i < seg.size() - 1; i++) {
            segDist[i] = haversineKm(seg.get(i), seg.get(i + 1));
            total += segDist[i];
        }

        double target = frac * total, cum = 0;
        for (int i = 0; i < segDist.length; i++) {
            if (cum + segDist[i] >= target) {
                double lf = (target - cum) / segDist[i];
                ShapePoint p1 = seg.get(i), p2 = seg.get(i + 1);
                return new double[]{
                    p1.lat() + lf * (p2.lat() - p1.lat()),
                    p1.lon() + lf * (p2.lon() - p1.lon())
                };
            }
            cum += segDist[i];
        }
        ShapePoint last = seg.get(seg.size() - 1);
        return new double[]{last.lat(), last.lon()};
    }

    private int nearestShapePoint(List<ShapePoint> pts, Stop stop) {
        int best = 0; double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < pts.size(); i++) {
            double d = Math.pow(pts.get(i).lat() - stop.lat(), 2)
                     + Math.pow(pts.get(i).lon() - stop.lon(), 2);
            if (d < bestDist) { bestDist = d; best = i; }
        }
        return best;
    }

    private static double haversineKm(ShapePoint a, ShapePoint b) {
        double R = 6371,
               dLat = Math.toRadians(b.lat() - a.lat()),
               dLon = Math.toRadians(b.lon() - a.lon());
        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(a.lat()))
                 * Math.cos(Math.toRadians(b.lat()))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    }
}
