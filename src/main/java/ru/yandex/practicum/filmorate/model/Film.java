package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import ru.yandex.practicum.filmorate.validation.ReleaseDateConstraint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Film {
    private Long id;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @Size(max = 200, message = "Описание не должно превышать 200 символов")
    private String description;

    @NotNull(message = "Дата релиза обязательна")
    @ReleaseDateConstraint
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность должна быть указана")
    @Positive(message = "Продолжительность должна быть положительной")
    private Integer duration;

    private MpaRating mpa;

    private List<Genre> genres = new ArrayList<>();
}
