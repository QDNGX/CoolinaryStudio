# Sequence-диаграмма: создание брони (`createBooking`) — «Шеф-стол»

> Сценарий — [UC-01 «Клиент бронирует слот»](../requirements/use-cases-cooking.md); инварианты —
> D-03 (запрет двойной брони), D-04 (запрет записи на отменённый слот), D-06 (снапшот цены
> проката) из [domain-model-cooking.md](../domain/domain-model-cooking.md). Экран — [SCR-05](../design-brief/briefs/scr-05-client-booking.md).
>
> `createBooking` — имя операции API, реализующей UC-01 (условно `POST /api/bookings`); это
> единственная операция системы, где пересекаются оба правила D-03 и D-04, поэтому у неё три
> исхода: **201** (успех), **409** (конфликт мест/версий), **410** (слот отменён безвозвратно).

## Диаграмма

```mermaid
sequenceDiagram
    autonumber
    actor Client as Клиент (браузер, SCR-05)
    participant API as Backend (Spring Boot)
    participant DB as БД

    Client->>API: POST /api/bookings {slotId, equipmentChoice: OWN|RENTAL}

    Note over API: Предусловия вне веток 201/409/410:<br/>не аутентифицирован → 401;<br/>активна блокировка клиента (D-02) → 403 с датой разблокировки

    activate API
    API->>DB: BEGIN TRANSACTION
    API->>DB: SELECT slot (status, freeSpots, rentalSetsAvailable, rentalPricePerSet, version)
    DB-->>API: slot

    alt slot.status = CANCELLED_BY_STUDIO (D-04)
        API->>DB: ROLLBACK
        API-->>Client: 410 Gone — «класс отменён студией» + причина
        Note over Client: слот «ушёл» безвозвратно:<br/>предложение вернуться к расписанию (SCR-03)

    else freeSpots = 0 (или RENTAL при rentalSetsAvailable = 0)
        API->>DB: ROLLBACK
        API-->>Client: 409 Conflict — «места закончились»
        Note over Client: честный отказ без вины пользователя,<br/>переход к расписанию (SCR-03)

    else места есть, слот в SCHEDULED
        opt equipmentChoice = RENTAL
            Note over API: rentalPriceSnapshot := slot.rentalPricePerSet (D-06),<br/>rentalSetsAvailable − 1
        end
        API->>DB: INSERT booking (status = CONFIRMED, snapshot цены при RENTAL)
        API->>DB: UPDATE slot SET freeSpots = freeSpots − 1, version = version + 1<br/>WHERE id = :slotId AND version = :readVersion

        alt версия совпала — конкурентов не было
            DB-->>API: 1 row updated
            API->>DB: COMMIT
            API-->>Client: 201 Created + бронь (класс, время, экипировка, цена проката)
            Note over Client: экран успеха: «место ваше»,<br/>напоминания придут за 24ч и 2ч (ФТ-16)

        else конфликт версии — параллельная бронь успела раньше (D-03)
            DB-->>API: 0 rows updated (OptimisticLockException)
            API->>DB: ROLLBACK
            Note over API: один retry: перечитать слот и повторить;<br/>если мест уже нет или снова конфликт — отказ
            API-->>Client: 409 Conflict — «место только что заняли»
            Note over Client: предложение выбрать другой класс (SCR-03)
        end
    end
    deactivate API
```

## Ветки ответов

| HTTP | Когда | Правило | Что видит клиент (SCR-05) |
|---|---|---|---|
| **201 Created** | Слот `SCHEDULED`, место (и прокатный набор, если `RENTAL`) зарезервированы в одной транзакции с пересчётом `freeSpots` | D-03, D-06, ФТ-06/07 | Подтверждение брони с зафиксированной ценой проката и напоминанием про письма за 24ч/2ч |
| **409 Conflict** | Мест нет на момент фиксации **или** конфликт оптимистичной блокировки после retry (двое подтверждали одновременно) | D-03, НФТ-02 | «Место только что заняли» — без вины пользователя, с переходом к расписанию |
| **410 Gone** | Слот в `CANCELLED_BY_STUDIO` — бронирование запрещено навсегда (в отличие от временного 409) | D-04, ФТ-05 | «Класс отменён студией» + причина, переход к расписанию |

## Пояснения

- **Почему 409 и 410 — разные коды.** 409 — состояние *может измениться* (кто-то отменит бронь,
  админ создаст новый слот той же программы); 410 — ресурс недоступен *окончательно*, повторять
  запрос бессмысленно. Это различие важно и для UI-текстов (SCR-05 показывает разные состояния),
  и для клиентского кода (после 410 — не ретраить).
- **Retry при конфликте версий — ровно один, внутри бэкенда.** Если после повторного чтения мест
  нет или версия снова ушла — клиенту возвращается 409, а не бесконечный цикл (D-03: «retry или
  явный отказ»). Клиент никогда не видит «тихую» ошибку или вечный спиннер (НФТ-02).
- **Проверки вне трёх веток задания.** Аутентификация (401) и активная блокировка клиента
  (D-02 → 403 с датой разблокировки) отсекаются до основного сценария — они показаны на
  диаграмме одной заметкой, чтобы не смешивать авторизацию с конкурентной логикой брони.
- **Уведомления в транзакцию не входят** (D-07/НФТ-03): письма-напоминания планируются отдельным
  процессом (UC-09) и на исход `createBooking` не влияют.
