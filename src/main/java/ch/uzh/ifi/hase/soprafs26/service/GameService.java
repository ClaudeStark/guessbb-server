package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import org.springframework.http.HttpStatus;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;


@Service
@Transactional
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

    public Game getGameById(Long gameId) {
        for (Game game : activeGames) {
            if (game.getGameId().equals(gameId)) {
                return game;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
    }

    public Game setupGame(long lobbyId) {
        Lobby currentLobby = lobbyService.getLobbyById(lobbyId);

        try {
            List<Train> trains = trainPositionFetcher.fetchTrainsMock(currentLobby.getMaxRounds());

            for (Train train : trains) {
                trainPositionFetcher.interpolatePosition(train);
            }

        List<Round> rounds = new ArrayList<>();

        List<User> players = currentLobby.getUsers();

        List<UserGameStatus> allUsersGameStatus = new ArrayList<>();
        List<GuessMessageDTO> guessMessages = new ArrayList<>();

        for  (User user : players) {
            allUsersGameStatus.add(new UserGameStatus(user.getUserId(), false));
            guessMessages.add(new GuessMessageDTO(currentLobby.getLobbyId(), user.getUserId()));
        }

        for (int i = 0; i < currentLobby.getMaxRounds(); i++) {
            rounds.add(new Round(i+1, trains.get(i), guessMessages, allUsersGameStatus));
        }

        Game newGame = new Game(currentLobby.getLobbyId(), rounds, trains);

        activeGames.add(newGame);

        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(currentLobby);
        Message message = new Message(MessageType.GAME_START, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + currentLobby.getLobbyId(), message);

        return newGame;

        } catch (Exception e) {
            throw new Error("Failed to fetch mock trains", e);
        }
    }

    public void processGuessMessage(GuessMessageDTO guessMessage){
        Long gameId = guessMessage.getLobbyId();
        Long userId = guessMessage.getUserId();


        Lobby currentLobby = lobbyService.getLobbyById(gameId);
        Game currentGame = getGameById(gameId);
        Integer roundNumber = currentLobby.getCurrentRound();
        List<Round> rounds = currentGame.getRounds();


        UserGameStatus userGameStatus = new UserGameStatus(userId, true);
        updateUserGameStatus(gameId, userGameStatus);

    }

    public void updateUserGameStatus(Long gameId, UserGameStatus userGameStatus) {
        Game currentGame = getGameById(gameId);
        Lobby currentLobby = lobbyService.getLobbyById(gameId);
        List<Round> rounds = currentGame.getRounds();
        Round currentRound =  rounds.get(currentLobby.getCurrentRound());
        List<UserGameStatus> allUsersGameStatuses = currentRound.getAllUserGameStatuses();

        for  (UserGameStatus usGaSt : allUsersGameStatuses) {
            if(usGaSt.getUserId().equals(userGameStatus.getUserId())) {
                usGaSt.setIsReady(userGameStatus.getIsReady());
            }
        }

    }

    public void publishRoundStart(){

    }
}
