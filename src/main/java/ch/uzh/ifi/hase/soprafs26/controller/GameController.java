package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public GameController(GameService gameService, SimpMessagingTemplate simpMessagingTemplate) {
        this.gameService = gameService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/game/{gameID}/guess")
    @SendTo("/topic/game")
    public void processGuessMessage(@DestinationVariable Long gameId, GuessMessageDTO guessMessageDTO) {
        gameService.processGuessMessage(guessMessageDTO);
    }

    @MessageMapping("/game/{gameID}/ready")
    public void updateUserStatus(@DestinationVariable Long gameID, UserGameStatus userGameStatus) {
        gameService.updateUserStatus(gameID, userGameStatus);
    }
}
