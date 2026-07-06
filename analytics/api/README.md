# API — «Шеф-стол»

OpenAPI 3.0.3 спецификация REST API, выведенная из аналитических артефактов проекта.
Файл: [openapi.yaml](openapi.yaml).

## Источники

| Артефакт | Что взято |
|---|---|
| [er-model-cooking.md](../design/er-model-cooking.md) | Сущности, поля, enum'ы, матрица доступа «кто читает / кто меняет» → схемы и security по ролям |
| [sequence-create-booking-cooking.md](../design/sequence-create-booking-cooking.md) | Контракт `POST /bookings`: ветки 201/409/410, предусловия 401/403 |
| [functional-requirements-cooking.md](../requirements/functional-requirements-cooking.md) | ФТ-01…ФТ-25 → операции |
| [use-cases-cooking.md](../requirements/use-cases-cooking.md) | Основные/альтернативные потоки → коды ответов |
| [non-functional-requirements-cooking.md](../requirements/non-functional-requirements-cooking.md) | НФТ-04 (лимиты кода входа → 429/400), НФТ-02/03/07 |
| [design-decisions-cooking.md](../design-brief/design-decisions-cooking.md) | Р-01…Р-16: гостевой доступ, лимиты текстов, видимость комментариев и др. |
| [screen-registry-cooking.md](../design-brief/screen-registry-cooking.md) | Проверка полноты: каждый экран SCR-01…SCR-17 обслужен эндпоинтами |

## Трассировка: эндпоинт → требования

| Эндпоинт | Требования / правила | Экран |
|---|---|---|
| `POST /auth/code/request` | ФТ-13, UC-05, НФТ-04 (60 сек → 429) | SCR-01 |
| `POST /auth/code/verify` | ФТ-13, ФТ-21 (молчаливое создание клиента), НФТ-04 (10 мин / 5 попыток), Р-01 | SCR-02 |
| `POST /auth/logout` | завершение сессии (длительность сессии — открытый вопрос дизайна) | — |
| `GET /me`, `PATCH /me` | ФТ-18 (аллергия), ФТ-21/Р-12 (имя при первом входе), Р-06 (видимость блокировки), Р-16 (email не редактируется) | SCR-08, SCR-02 |
| `GET /me/bookings` | UC-02 (предусловие), Р-14, Р-16 (вся история) | SCR-06 |
| `GET /programs`, `GET /programs/{id}` | чтение для всех + гость (Р-16) | SCR-03/04, SCR-11 |
| `POST /programs`, `PUT /programs/{id}` | ФТ-01, ФТ-02/Р-04, UC-07, Р-15 (без архивации) | SCR-11/12 |
| `GET /slots` | ФТ-20 (7 дней по умолчанию, фильтр по датам), Р-11, гость (Р-16) | SCR-03, SCR-13 |
| `GET /slots/{id}` | ФТ-01/02, ФТ-12 (рейтинг шефа — только агрегат, Р-07), гость (Р-16) | SCR-04 |
| `POST /slots` | ФТ-03 (180 мин фикс.), ФТ-02, ФТ-24/D-10 (пересечение → 409), Р-03 (без редактирования) | SCR-14 |
| `POST /slots/{id}/cancel` | ФТ-04 (причина обязательна), UC-03 (каскад броней), ФТ-15/D-07/НФТ-03 (email best-effort), D-04 | SCR-15 |
| `GET /slots/{id}/bookings` | ФТ-18 (аллергии участников), UC-06, ФТ-22 (вход админа) | SCR-10, SCR-13 |
| `POST /bookings` | UC-01, ФТ-05/06/07, D-02 (403 + blockedUntil), D-03 (409), D-04 (410), D-06 (снапшот цены), НФТ-02 | SCR-05 |
| `POST /bookings/{id}/cancel` | UC-02, ФТ-08 (isLateCancellation, D-01), ФТ-09/D-02 (блокировка) | SCR-06 |
| `POST /bookings/{id}/no-show` | UC-06, ФТ-10, Р-13 (только после начала), D-02 | SCR-10 |
| `DELETE /bookings/{id}/no-show` | ФТ-22, D-11, UC-06/A1 | SCR-13 |
| `POST /bookings/{id}/review` | UC-04, ФТ-11, D-05 (409 повтор / 422 не завершён), ФТ-12 (пересчёт агрегатов) | SCR-07 |
| `GET /bookings/{id}/review` | Р-16 (отзыв read-only) | SCR-06 |
| `GET /reviews` | Р-07 (тексты комментариев — только ADMIN), БТ-05 | SCR-16 |
| `GET /chef/slots` | UC-06 (предусловие), US-07, Р-16 (вся история) | SCR-09 |
| `GET /chefs`, `POST /chefs` | UC-08, ФТ-14, D-08, Р-15, Р-16 (email занят → 409, решается вручную) | SCR-16 |
| `PATCH /chefs/{id}` | ФТ-25, Р-05 | SCR-16 |
| `GET /studio-settings`, `PUT /studio-settings` | ФТ-17, US-12 | SCR-17 |

## Что сознательно не является эндпоинтом

- **Завершение слота** — только система по `startAt + 180 мин` (D-09/ФТ-23, UC-10); ручной операции нет.
- **Редактирование слота** — не предусмотрено (Р-03): исправление = `POST /slots/{id}/cancel` + `POST /slots`.
- **Редактирование/удаление отзыва** — один отзыв на бронь, read-only после создания (D-05, Р-16).
- **Оплата** — вне скоупа MVP (БТ-08); цена проката в ответах справочная.
- **Пересчёт производных полей** (`freeSpots`, `lateCancelCount`, `blockedUntil`, `averageRating`) — только система (D-02/D-03/D-09/D-11); в схемах помечены `readOnly`.
- **Email-уведомления** (EML-01…EML-04) — фоновые процессы UC-09/UC-10, best-effort (НФТ-03), в HTTP-контракт не входят.

## Соглашения

- Базовый URL: `http://localhost:8080/api` (Spring Boot, БТ-01/НФТ-01).
- Аутентификация: bearer-токен из `POST /auth/code/verify`; passwordless для всех ролей (БТ-06).
- Гостевой доступ (Р-16): `GET /slots*`, `GET /programs*`, `GET /studio-settings`, эндпоинты `/auth/*`.
- Ошибки: единая схема `Error { code, message, details? }`; отказ по блокировке — `BlockedError` c `blockedUntil`.
- `409` — временный конфликт (можно повторить/выбрать другое), `410` — окончательная недоступность (не ретраить) — различие зафиксировано в sequence-диаграмме.
