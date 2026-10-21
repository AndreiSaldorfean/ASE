# Week 3 – Lab Assignment

## Goal

Add simple upload support so the frontend can upload images/videos to a local `/media` directory, and posts can reference them via a URL stored on the `Post`.

## Acceptance Criteria

- A `POST /api/media` endpoint that accepts `multipart/form-data` with field name `file` and returns JSON `{ "mediaUrl": "/media/<filename>" }` (`201` on success).
- Files are physically saved in a configurable local directory (default `./media`), and the directory is created if missing.
- Spring serves `/media/**` from that directory (so a browser can request `/media/<filename>` and play/download it).
- `spring.servlet.multipart` max file size settings are configured so videos up to a reasonable size are accepted.
- The frontend can upload a file, get the `mediaUrl`, and set it on the `Post` payload (re-using `imageUrl` is acceptable for this minimal path).

### Extra (optional)

- Add simple validation (e.g. limit by extension or content-type) and restrict the endpoint to authenticated users.

## Task List

1. **Add `application.properties` entries** (or `application.yml`):

   ```properties
   media.base-path=media
   spring.servlet.multipart.max-file-size=200MB
   spring.servlet.multipart.max-request-size=200MB
   ```

2. **Add a `WebMvcConfigurer`** that maps `/media/**` → `file:<media.base-path>/` so Spring static-serving will expose files saved in the media folder.

3. **Add a `MediaController` REST endpoint:**
   - `POST /api/media`
   - Accepts a multipart file (param name `file`)
   - Saves the file with a generated filename (UUID + original extension) under the media base path
   - Returns `201` with JSON `{ "mediaUrl": "/media/<filename>" }`

4. **Ensure the media directory exists.** The backend should create it at startup if missing (the controller constructor or config can also create it lazily on first upload).

5. **Frontend:**
   - Modify the UI where users create posts. When a file is chosen:
     1. Upload it to `POST /api/media` (`multipart/form-data`).
     2. Extract the returned `mediaUrl` and set it as the Post's `imageUrl` (or `mediaUrl` if you add that).
     3. Submit the Post as before.
   - Alternatively, allow manual paste of a URL to use external images.

6. **(Optional) Add basic server-side checks:**
   - File is not empty
   - Size is not too large
   - Optional whitelist of extensions (e.g. `.mp4`, `.mov`, `.jpg`, `.png`)

7. **Test in the browser:** upload a small `.mp4`, then open the returned `/media/<file>` URL in the browser and ensure it plays (no transcoding involved).
