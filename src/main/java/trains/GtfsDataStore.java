package ch.uzh.ifi.hase.soprafs26.trains;

import ch.uzh.ifi.hase.soprafs26.trains.model.Stop;
import ch.uzh.ifi.hase.soprafs26.trains.model.StopTime;
import ch.uzh.ifi.hase.soprafs26.trains.model.ShapePoint;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Map;



@Component
public class GtfsDataStore {

    // All maps are replaced atomically on reload — no locking needed during reads
    private volatile Map<String, Stop>            stops       = Map.of();
    private volatile Map<String, List<StopTime>>  stopTimes   = Map.of();  // tripId → sorted list
    private volatile Map<String, String>          tripShapes  = Map.of();  // tripId → shapeId
    private volatile Map<String, String>          tripNames   = Map.of();  // tripId → "S12"
    private volatile Map<String, List<ShapePoint>> shapes     = Map.of();
    private volatile Instant                      loadedAt    = Instant.EPOCH;

    // Atomic swap — called by loader after parsing is complete
    public void update(Map<String, Stop> s, Map<String, List<StopTime>> st,
                       Map<String, String> ts, Map<String, String> tn,
                       Map<String, List<ShapePoint>> sh) {
        this.stops      = Map.copyOf(s);
        this.stopTimes  = Map.copyOf(st);
        this.tripShapes = Map.copyOf(ts);
        this.tripNames  = Map.copyOf(tn);
        this.shapes     = Map.copyOf(sh);
        this.loadedAt   = Instant.now();
    }

    public boolean isLoaded() { return loadedAt != Instant.EPOCH; }

    // Getters
    public Map<String, Stop>            stops()      { return stops; }
    public Map<String, List<StopTime>>  stopTimes()  { return stopTimes; }
    public Map<String, String>          tripShapes() { return tripShapes; }
    public Map<String, String>          tripNames()  { return tripNames; }
    public Map<String, List<ShapePoint>> shapes()    { return shapes; }
    public Instant                      loadedAt()   { return loadedAt; }
}