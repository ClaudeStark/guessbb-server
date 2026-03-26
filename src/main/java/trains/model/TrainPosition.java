package ch.uzh.ifi.hase.soprafs26.trains.model;

public record TrainPosition(
    String tripId,
    double lat,
    double lon,
    String status,        // e.g. "Between STOP_A → STOP_B"
    int delaySeconds
) {} 