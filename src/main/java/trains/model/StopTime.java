package ch.uzh.ifi.hase.soprafs26.trains.model;


public record StopTime(
    String tripId,
    String stopId,
    int stopSeq,
    int arrivalSec,    // seconds since midnight
    int departureSec
) {}
