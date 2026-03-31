package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import ch.uzh.trains.TrainPositionFetcher;
import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;


import websocket.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;

public class GameService {
    private AuthService authService;

    private List<Game> activeGames;

    private LobbyService lobbyService;

    private TrainPositionFetcher trainPositionFetcher;

    private final SimpMessagingTemplate messagingTemplate;

    public GameService(AuthService authService, LobbyService lobbyService, TrainPositionFetcher trainPositionFetcher, SimpMessagingTemplate messagingTemplate) {
        this.authService = authService;
        this.lobbyService = lobbyService;
        this.trainPositionFetcher = trainPositionFetcher;
        this.messagingTemplate = messagingTemplate;
        this.activeGames = new ArrayList<>();
    }

    public Game setupGame(long lobbyId) {
        Lobby currentLobby = lobbyService.getLobbyById(lobbyId);

        List<Train> trains = trainPositionFetcher.fetchTrainsMock(currentLobby.getMaxRounds()); 
        for (Train train : trains) {    
            trainPositionFetcher.interpolatePosition(train);
        }

        List<Round> rounds = new ArrayList<>();

        List<User> players = currentLobby.getUsers();

        List<UserGameStatus> allUsersGameStatus = new ArrayList<>();
        List<GuessMessageDTO> guessMessages = new ArrayList<>();

        for  (User user : players) {
            allUsersGameStatus.add(new UserGameStatus(user.getUserId()));
            guessMessages.add(new GuessMessageDTO(currentLobby.getLobbyId(), user.getUserId()));
        }

        for (Integer i = 0; i < currentLobby.getMaxRounds(); i++) {
            rounds.add(new Round(i+1, trains.get(i), guessMessages, allUsersGameStatus));
        }

        Game newGame = new Game(currentLobby.getLobbyId(), rounds, trains);

        currentLobby.setRounds(rounds);

        activeGames.add(newGame);

        //send broadcast message to clients that the game has started
        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(currentLobby);
        Message message = new Message(MessageType.GAME_START, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + currentLobby.getLobbyId(), message);

        return newGame;
    }

    public void proccessGuessMessage(GuessMessageDTO guessMessageDTO){
        Long currentGameId = guessMessageDTO.getLobbyId();
    }

    public void updateReadyStatus(Long gameId, Long userId) {

    }

}
