package codex.mmxxvi.services;

import codex.mmxxvi.entity.User;

public interface JwtService {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    String extractUsername(String token);
    String extractTokenType(String token);
    boolean isValidToken(String token, String username);
    boolean isRefreshToken(String token);
}
