package ch.uzh.ifi.hase.soprafs26.security;

import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final LobbyService lobbyService;

    public AuthService(UserRepository userRepository, LobbyService lobbyService) {
        this.userRepository = userRepository;
        this.lobbyService = lobbyService;
    }

    public Boolean authUser(AuthHeader authHeader) {
        if(authHeader.getUserId() == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user could not be found");
        }
        User user = userRepository.findById(authHeader.getUserId()).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user could not be found");
        }
        if (user.getToken() == null) {
            return false;
        }
        return user.getToken().equals(authHeader.getToken());
    }

    public Boolean isUserInLobby(Long userId, String token, Long lobbyId) {
        if (!authUser(new AuthHeader(userId, token))) {
            return false;
        }
        return lobbyService.getLobbyById(lobbyId).existsUser(userId);
    }
}
