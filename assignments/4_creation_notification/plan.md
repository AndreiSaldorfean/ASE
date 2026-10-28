# Assignment 4 – Post Creation Notifications + Image Processing

## Context
`assignments/4_creation_notification/description.md` asks for two backend features:
1. When a post is created, asynchronously send a formatted HTTP notification (username, post ID, timestamp) to a public service like ntfy.sh.
2. When an image is uploaded, scale it to max 300 px wide (keep aspect ratio) and stamp an "Instakill Demo" watermark bottom-right before saving — structured as a separate module that could later run as its own worker.

Existing hooks to build on:
- `PostService.createPost` (`post/application/PostService.java`) — single entry point used by REST, Thymeleaf and `DataInitializer`.
- `DomainEventPublisher` + `NoOpDomainEventPublisher` (`interaction/domain/`) — the pluggable publisher from commit 11dd92d, already used for `LikeCreatedEvent`.
- `MediaStorageService.store` (`media/application/MediaStorageService.java`) — the only place uploaded bytes are written.
- `MediaProperties` / `@ConfigurationPropertiesScan` pattern for config.
- No HTTP client in use yet; Spring Boot 3.3 ships `RestClient` with `spring-boot-starter-web` (no new dependency).

## Part 1 – Post creation notifications

**Event**
- New `post/domain/PostCreatedEvent(UUID postId, UUID authorId, Instant occurredAt)` record, null-checked like `LikeCreatedEvent`.
- `PostService` gets `DomainEventPublisher` injected; `createPost` publishes `PostCreatedEvent` after `posts.save(...)`.

**Real publisher**
- New `infrastructure/events/SpringDomainEventPublisher` (`@Component @Primary`) delegating to Spring's `ApplicationEventPublisher`. Keep `NoOpDomainEventPublisher` (tests use it directly) but remove its `@Component` so there's a single bean.

**Notification module** — new package `com.instakill.notification`:
- `NotificationProperties` (`@ConfigurationProperties("notification")`): `enabled` (default true), `baseUrl` (default `https://ntfy.sh`), `topic` (default `instakill-demo-posts`).
- `NotificationClient` interface `send(Notification n)` + `NtfyNotificationClient` using `RestClient`: `POST {baseUrl}/{topic}`, body = message text, headers `Title`, `Tags`. Logs and swallows errors (a failed notification must never break anything).
- `PostCreatedNotificationListener`:
  - `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` → runs only once the post is really saved, on a background thread, so the HTTP request never slows down or rolls back the request.
  - Looks up the username via `UserRepository.findById`, formats e.g.
    `Title: New post on Instakill` / body: `@alice published post 3f2a… at 2026-10-28T19:05:00Z`.
  - Skips when `enabled=false`.
- `infrastructure/config/AsyncConfig` with `@EnableAsync` and a small named `ThreadPoolTaskExecutor` (e.g. `notificationExecutor`, 2 threads, prefix `notify-`).

**Config**
- `application.yaml`: `notification.base-url`, `notification.topic`.
- `application-test.yaml`: `notification.enabled: false` (no real HTTP from tests).
- Note: `DataInitializer` seeds 6 posts on a fresh DB → 6 notifications on first start. Acceptable; mention in docs.

## Part 2 – Image processing module

New package `com.instakill.imageprocessing` with no web/Spring-MVC dependencies, so it could be moved to a worker later:
- `ImageProcessingProperties` (`image-processing.max-width: 300`, `image-processing.watermark-text: "Instakill Demo"`).
- `ImageProcessor` interface: `byte[] process(InputStream in, String format)`.
- `Java2dImageProcessor` (`@Component`) using built-in `ImageIO` + `Graphics2D` (no new dependency):
  1. `ImageIO.read`; `null` → throw `BadRequestException("Not a valid image")`.
  2. If width > maxWidth, scale to `maxWidth × round(h * maxWidth / w)` with bilinear/quality hints; never upscale.
  3. Draw watermark after scaling: font size ~ 4–5% of width (min ~10 px), semi-transparent white text with a dark shadow, right/bottom padding ~8 px, antialiased.
  4. JPEG output uses `TYPE_INT_RGB` (no alpha); PNG keeps `TYPE_INT_ARGB`.
  5. `ImageIO.write` to a byte array.

**Wire into storage** (`MediaStorageService.store`):
- If extension ∈ {jpg, jpeg, png} → `imageProcessor.process(...)` then `Files.write`.
- Everything else (mp4/mov/webm, and gif/webp, which ImageIO can't write correctly / animated) → existing `Files.copy` unchanged.

## Tests
- `ImageProcessorTest` (unit): 1200×600 PNG → 300×150; 200×100 stays 200×100; bottom-right region pixels differ from the source color (watermark present); JPEG round-trips; garbage bytes → `BadRequestException`.
- `MediaUploadIntegrationTest`: add an upload of a real generated 800×400 PNG; read the saved file from `build/test-media` and assert width 300. Existing `pic.png` with `{1}` only hits the auth test (401 before processing) — fine.
- `PostServiceTest` (new, unit, Mockito like `InteractionServiceTest`): `createPost` publishes `PostCreatedEvent` with correct ids/time.
- `PostCreatedNotificationListenerTest` (unit): mocked `NotificationClient` + `UserRepository` → asserts message contains username, post ID, timestamp; no send when disabled.

## Files
- Modify: `post/application/PostService.java`, `interaction/domain/NoOpDomainEventPublisher.java`, `media/application/MediaStorageService.java`, `application.yaml`, `application-test.yaml`, `backend/api.http` (example).
- Add: `post/domain/PostCreatedEvent.java`, `infrastructure/events/SpringDomainEventPublisher.java`, `infrastructure/config/AsyncConfig.java`, `notification/*` (properties, client, ntfy impl, listener), `imageprocessing/*` (properties, interface, Java2D impl), tests above.

## Verification
1. `cd backend && ./gradlew test` — all existing + new tests green.
2. Run the app (`./gradlew bootRun`), subscribe to `https://ntfy.sh/instakill-demo-posts` in a browser/phone, create a post via UI or `api.http` → notification arrives; the POST response returns immediately (check log thread name `notify-*`).
3. Upload a large JPG/PNG via the frontend → open returned `/media/<file>`: 300 px wide, "Instakill Demo" bottom-right. Upload an mp4 → still plays unchanged.
