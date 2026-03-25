package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "status", target = "status")
	UserGetDTO convertEntityToUserGetDTO(User user);

	@Mapping(source = "lobbyName", target = "lobbyName")
	@Mapping(source = "size", target = "size")
	@Mapping(source = "visibility", target = "visibility")
	@Mapping(source = "maxRounds", target = "maxRounds")
	@Mapping(source = "lobbyState", target = "lobbyState")
	@Mapping(source = "lobbyCode", target = "lobbyCode")
	@Mapping(source = "lobbyId", target = "lobbyId")

	LobbyDTO convertEntityToLobbyDTO(Lobby lobby);	

	@Mapping(source = "lobbyId", target = "lobbyId")
	@Mapping(source = "lobbyCode", target = "lobbyCode")
	@Mapping(source = "lobbyName", target = "lobbyName")
	@Mapping(source = "admin", target = "admin")
	@Mapping(source = "size", target = "size")
	@Mapping(source = "visibility", target = "visibility")
	@Mapping(source = "users", target = "users")
	@Mapping(source = "currentRound", target = "currentRound")
	@Mapping(source = "maxRounds", target = "maxRounds")
	@Mapping(source = "scores", target = "scores")
	@Mapping(source = "lobbyState", target = "lobbyState")

	MyLobbyDTO convertEntityToMyLobbyDTO(Lobby lobby);

	@Mapping(source = "lobbyId", target = "lobbyId")
	@Mapping(source = "lobbyCode", target = "lobbyCode")

	LobbyAccessDTO convertEntityToLobbyAccessDTO(Lobby lobby);
	
	@Mapping(source = "lobbyCode", target = "lobbyCode")

	Lobby convertLobbyCodePostDTOtoEntity(LobbyCodePostDTO lobbyCodePostDTO);

	
}
