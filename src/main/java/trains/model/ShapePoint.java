package ch.uzh.ifi.hase.soprafs26.trains.model;

public record ShapePoint(
    String shapeId,
    double lat,
    double lon,
    int seq,
    double distTraveled
) {} 