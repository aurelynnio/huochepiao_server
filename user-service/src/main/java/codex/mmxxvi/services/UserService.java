package codex.mmxxvi.services;

import codex.mmxxvi.dto.request.CreateUserRequest;
import codex.mmxxvi.dto.request.ForgotPasswordRequest;
import codex.mmxxvi.dto.request.LoginRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.ResetPasswordRequest;
import codex.mmxxvi.dto.request.UpdateUserRequest;
import codex.mmxxvi.dto.response.JwtResponse;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.PasswordResetResponse;
import codex.mmxxvi.dto.response.UserResponse;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<PageResponse<UserResponse>> getAllUsers(PageRequestDto pageRequestDto);
    Mono<UserResponse> registerUser(CreateUserRequest user);
    Mono<JwtResponse> login(LoginRequest request);
    Mono<JwtResponse> refreshAccessToken(String refreshToken);
    Mono<PasswordResetResponse> requestPasswordReset(ForgotPasswordRequest request);
    Mono<Void> resetPassword(ResetPasswordRequest request);
    Mono<Void> delete(String id);
    Mono<UserResponse> update(String id, UpdateUserRequest request);

}
