# Шеф-стол

Сервис онлайн-бронирования кулинарной студии: клиенты выбирают программу и записываются на слот,
шеф ведёт расписание своих классов, администратор управляет каталогом программ, шефами и настройками
студии. Полное описание идеи — в `analytics/customer-brief/brief-cooking.md`.

## Структура репозитория

| Папка | Назначение |
|---|---|
| `backend/` | REST API на Spring Boot |
| `frontend/` | SPA на React + TypeScript |
| `analytics/` | Вся документация проекта: требования, доменная модель, дизайн, API-контракт, ТЗ |
| `docker-compose.yml` | Поднимает postgres + backend + frontend одной командой |

## Backend (`backend/`)

Стек: **Spring Boot 4.1**, Java 21, Spring Data JPA, Spring Security, Lombok.
БД: H2 in-memory по умолчанию (для локальной разработки), PostgreSQL — в Docker (`postgres` профиль).
Аутентификация — passwordless (одноразовый код → bearer-токен), без паролей. Базовый путь API: `/api`.

Пакеты `backend/src/main/java/org/example/projectcooking/`:

- `domain/` — JPA-сущности (Booking, ChefProfile, ClientProfile, Program, Review, Slot, StudioSettings, User) и `domain/enums/`
- `repository/` — Spring Data JPA репозитории для каждой сущности
- `dto/` — request/response-модели, сгруппированные по фиче (auth, booking, chef, program, review, slot, studio, user, error)
- `mapper/` — преобразование entity ↔ DTO
- `service/` — бизнес-логика (бронирования, аутентификация, программы, отзывы, слоты, настройки студии, уведомления)
- `security/` — bearer-токен фильтр и обработчики ошибок аутентификации/доступа
- `config/` — конфигурация Spring Security и наполнение БД тестовыми данными (`DataInitializer`)
- `web/` — REST-контроллеры и глобальный обработчик ошибок
- `scheduler/` — фоновые задачи (авто-завершение слотов, напоминания о бронированиях)
- `exception/` — доменные исключения API

Запуск локально (H2, без Docker):

```bash
cd backend
./mvnw spring-boot:run
```

## Frontend (`frontend/`)

Стек: **React 18 + TypeScript**, Vite, Sass, иконки — lucide-react.

- `src/App.tsx` — корневой компонент
- `src/api/client.ts` — клиент для обращения к backend API
- `src/styles/` — стили (Sass)
- `src/types.ts` — общие типы

Запуск локально:

```bash
cd frontend
npm install
npm run dev
```

## Docker Compose

```bash
docker compose up --build
```

Поднимает три сервиса:

- `postgres` — PostgreSQL 16, порт `5432`
- `backend` — Spring Boot API (профиль `postgres`), порт `8080`
- `frontend` — собранный SPA за nginx, порт `5173` → `80`

## Документация проекта

Вся аналитика и спецификации лежат в `analytics/`:

- `requirements/` — бизнес-, функциональные и нефункциональные требования, use-case'ы, user story
- `domain/domain-model-cooking.md` — доменная модель
- `design/` — ER-модель, матрица доступа, диаграмма последовательности для создания бронирования
- `design-brief/` — дизайн-решения, реестр экранов, брифы по каждому экрану (`briefs/scr-01..17`)
- `api/` — OpenAPI-спецификация (`openapi.yaml`) и трассировка эндпоинтов к требованиям ([README](analytics/api/README.md))
- `tz/` — технические задания по модулям для разработки ([README](analytics/tz/README.md))
- `customer-brief/` — исходный бриф от заказчика

## Промпты и разбор багов

> 📁 Google Drive: https://drive.google.com/drive/folders/1W8BhFfoMj21KUPG6p24yM4Mvidqap12N?usp=sharing
