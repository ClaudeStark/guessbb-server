package ch.uzh.ifi.hase.soprafs26.entity;

public class UserGameStatus {

    private String userId;

    private Boolean isReady;


    public UserGameStatus(String userId) {
        this.userId = userId;
        this.isReady = false;
    }

    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}

    public Boolean getIsReady() {return isReady;}
    public void setIsReady(Boolean isReady) {this.isReady = isReady;}
}
