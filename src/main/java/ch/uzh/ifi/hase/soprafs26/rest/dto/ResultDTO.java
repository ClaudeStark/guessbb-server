package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.objects.Score;

import java.util.List;

public class ResultDTO {
    private List<Score> totalScores;

    private List<Score> roundScores;

    public List<Score> getTotalScores() {return totalScores;}
    public void setTotalScores(List<Score> totalScores) {this.totalScores = totalScores;}

    public List<Score> getRoundScores() {return  roundScores;}
    public void setRoundScores(List<Score> roundScores) {this.roundScores = roundScores;}



}
