DROP TABLE IF EXISTS film_genre;
DROP TABLE IF EXISTS film_likes;
DROP TABLE IF EXISTS friendship;
DROP TABLE IF EXISTS film;
DROP TABLE IF EXISTS genre;
DROP TABLE IF EXISTS mpa_rating;
DROP TABLE IF EXISTS users;

CREATE TABLE mpa_rating (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(10) NOT NULL
);

CREATE TABLE genre (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE film (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    release_date DATE,
    duration INTEGER,
    mpa_rating_id INTEGER NOT NULL,
    FOREIGN KEY (mpa_rating_id) REFERENCES mpa_rating(id)
);

CREATE TABLE film_genre (
    film_id INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    genre_order INTEGER NOT NULL,
    PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES film(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genre(id) ON DELETE CASCADE
);

CREATE TABLE film_likes (
    film_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    PRIMARY KEY (film_id, user_id),
    FOREIGN KEY (film_id) REFERENCES film(id) ON DELETE CASCADE
);

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    login VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255),
    birthday DATE
);

CREATE TABLE friendship (
    user_id INTEGER NOT NULL,
    friend_id INTEGER NOT NULL,
    confirmed BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
);
