package codex.mmxxvi.services;

import codex.mmxxvi.dto.request.CreateUserRequest;
import codex.mmxxvi.dto.request.LoginRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.UpdateUserRequest;
import codex.mmxxvi.dto.response.JwtResponse;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.UserResponse;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<PageResponse<UserResponse>> getAllUsers(PageRequestDto pageRequestDto);
    Mono<UserResponse> registerUser(CreateUserRequest user);
    Mono<JwtResponse> login(LoginRequest request);
    Mono<Void> delete(String id);
    Mono<UserResponse> update(String id, UpdateUserRequest request);

}
