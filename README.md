# Cloud API (диплом)

Это бэкенд‑часть дипломного проекта «Облачное хранилище». Приложение написано на Spring Boot и предоставляет REST‑интерфейс, совместимый с готовым Vue‑фронтендом. Основные сценарии — авторизация по токену, загрузка, просмотр, переименование и удаление файлов пользователя.

## Архитектура и стек
- Spring Boot 3, Spring MVC, Spring Security. Аутентификация по токену, заголовок `auth-token`.
- PostgreSQL 17 + Flyway. В БД четыре таблицы: `users` (логины), `auth_tokens` (активные токены), `files` (метаданные) и `file_blobs` (содержимое).
- Файлы хранятся прямо в базе (колонка BYTEA). В метаданных фиксируются размер, MIME‑тип и SHA‑256.
- Все настройки (порт, context-path `/cloud`, CORS, параметры подключения к БД) задаются в `src/main/resources/application.yml` и могут переопределяться переменными окружения.

## Как запустить
1. Поднимите PostgreSQL (локально или `docker compose up --build`, в составе уже есть БД и pgAdmin).
2. Локальный старт приложения:
   ```bash
   ./gradlew bootRun
   ```
   Сервис доступен по адресу `http://localhost:8080/cloud`.
3. Прогон тестов:
   ```bash
   ./gradlew test
   ```
   Выполняются unit‑тесты и интеграционные сценарии на Testcontainers (нужен Docker).
4. Сборка артефакта:
   ```bash
   ./gradlew clean bootJar
   ```
   Полученный jar используется в Dockerfile.

## Настройка фронтенда
1. Во фронтовом `.env` укажите `VUE_APP_BASE_URL=http://localhost:8080/cloud`.
2. При необходимости добавьте адрес фронта в `app.cors.allowed-origins` (по умолчанию разрешён `http://localhost:8081`).
3. Для авторизации используйте логин `user1` и пароль `pass` (создаются автоматически миграцией `V2__seed_test_user.sql`).
4. Фронт запрашивает токен через `POST /login`, сохраняет его в `auth-token` и использует для вызова остальных методов (`/list`, `/file`, `/logout`).

## REST API
- `POST /login` — принимает логин/пароль, возвращает `{ "auth-token": "..." }`.
- `POST /logout` — деактивирует токен.
- `GET /list?limit=10` — список файлов (название и размер в байтах).
- `POST /file?filename=name.txt` — загрузка файла (multipart: части `file` и `hash`).
- `GET /file?filename=name.txt` — скачивание файла и контрольной суммы в multipart‑ответе.
- `PUT /file?filename=old.txt` + body `{ "name": "new.txt" }` — переименование.
- `DELETE /file?filename=name.txt` — удаление файла.
