package ru.yandex.practicum.filmorate.storage.friendship;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FriendshipDbStorage implements FriendshipStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sql = "MERGE INTO friendship (user_id, friend_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Пользователь {} добавил в друзья {}", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = """
            SELECT u.*
            FROM users u
            JOIN friendship f ON u.id = f.friend_id
            WHERE f.user_id = ?
        """;
        return jdbcTemplate.query(sql, new UserDbStorage.UserRowMapper(), userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        String sql = """
            SELECT u.*
            FROM users u
            JOIN friendship f1 ON u.id = f1.friend_id AND f1.user_id = ?
            JOIN friendship f2 ON u.id = f2.friend_id AND f2.user_id = ?
        """;
        return jdbcTemplate.query(sql, new UserDbStorage.UserRowMapper(), userId, otherId);
    }
}
