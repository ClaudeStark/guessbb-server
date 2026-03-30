package ch.uzh.ifi.hase.soprafs26.service;

import ch.qos.logback.classic.pattern.Util;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;

import java.util.ArrayList;
import java.util.List;

public class GameService {
    private AuthService authService;

    private List<Game> activeGames;

    private LobbyService lobbyService;

    private GameTrainsService gameTrainsService;

    public Game setupGame(Long lobbyId, List<User> users, Integer maxRounds) {
        List<Train> trains = new ArrayList<>(); //replace with fetchTrains
        List<Round> rounds = new ArrayList<>();

        Lobby currentLobby = lobbyService.getLobbyById(lobbyId); //needs to change to long
        List<User> players = currentLobby.getUsers();

        List<UserGameStatus> allUsersReadyStatus = new ArrayList<>();
        List<GuessMessage> guessMessages = new ArrayList<>();

        for  (User user : players) {
            allUsersReadyStatus.add(new UserGameStatus(user.getUserId()));
            guessMessages.add(new GuessMessage(currentLobby, user.getUserId()));
        }

        for (Integer i = 0; i < maxRounds; i++) {
            rounds.add(new Round(i+1, trains.get(i), guessMessages,  allUsersReadyStatus));
        }

        Game newGame = new Game(currentLobby.getLobbyId(), rounds, trains);

        activeGames.add(newGame);

        return newGame;
    }

    public void proccessGuessMessage(GuessMessageDTO guessMessageDTO){
        Long currentGameId = guessMessageDTO.getLobbyId();
    }

    public void updateReadyStatus(Long gameId, Long userId) {

    }

}
