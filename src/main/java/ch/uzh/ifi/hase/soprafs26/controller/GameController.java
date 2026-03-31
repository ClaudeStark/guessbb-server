package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;


@Controller
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public GameController(GameService gameService, SimpMessagingTemplate simpMessagingTemplate) {
        this.gameService = gameService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/game/{gameId}/guess")
    public void processGuessMessage(@DestinationVariable Long gameId, Message guessMessage) {
        gameService.processGuessMessage(guessMessage);
    }

    @MessageMapping("/game/{gameId}/ready")
    public void updateUserGameStatus(@DestinationVariable Long gameID, Message readyForNextRoundMessage) {
        gameService.updateUserGameStatus(gameID, readyForNextRoundMessage);
    }
}
