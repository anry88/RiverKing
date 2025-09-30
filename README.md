# RiverKing

Full MVP codebase: Kotlin/Ktor + Exposed + SQLite, Telegram Mini App.

## Запуск

1. Укажи конфигурацию в `src/main/resources/config.properties` (минимум `BOT_TOKEN` и `PUBLIC_BASE_URL`).
   В разработке можно использовать фиктивные значения и включить `DEV_MODE=true`:
   ```properties
   BOT_TOKEN=TEST
   PUBLIC_BASE_URL=http://localhost:8080
   DEV_MODE=true
   ```
   Затем запусти:
   ```bash
   gradle run
   ```
2. Открой [http://localhost:8080/app](http://localhost:8080/app) — мини‑апп работает (в `DEV_MODE=true` можно и без Telegram).
3. Из бота открой мини‑апп кнопкой `web_app`:
   ```
   {
     "keyboard": [[{ "text": "🎣 Играть", "web_app": { "url": "https://YOUR_DOMAIN/app?tgId=USER_ID" } }]],
     "resize_keyboard": true
   }
   ```
4. Внутри Telegram mini app клиент отправит `initData` → сервер проверит подпись и создаст сессию (`/api/auth/telegram`).
5. API готово: `/api/me`, `/api/daily`, `/api/location/{id}`, `/api/cast`.

## Турниры

- Турнирные призы поддерживают как наборы приманок, так и внутриигровые монеты. Администратор может выбрать пакет или монеты через встроенные кнопки и задать количество прямо из интерфейса бота.
- Игроки получают монеты напрямую на баланс при получении соответствующего приза.
- Помимо метрик «самая большая», «самая маленькая» и «количество», доступна метрика «суммарный вес» для расчёта результатов.
