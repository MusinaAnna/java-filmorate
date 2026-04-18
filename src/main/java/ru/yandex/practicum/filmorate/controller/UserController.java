package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @GetMapping
    public Collection<User> findAll() {
        return users.values();
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        normalizeName(user);
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}", user);
        return user;
    }

    @PutMapping
    public User update(@Valid @RequestBody User updatedUser) {
        Long id = updatedUser.getId();
        if (id == null) {
            throw new ValidationException("Id должен быть указан");
        }
        User existingUser = users.get(id);
        if (existingUser == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setLogin(updatedUser.getLogin());
        normalizeName(updatedUser);
        existingUser.setName(updatedUser.getName());
        existingUser.setBirthday(updatedUser.getBirthday());
        log.info("Обновлён пользователь: {}", existingUser);
        return existingUser;
    }

    /**
     * Устанавливает имя пользователя равным логину, если имя не указано или пустое.
     */
    private void normalizeName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
