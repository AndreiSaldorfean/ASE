# Week 4 – Lab Assignment

## Post Creation Notifications

- Whenever a new post is created, send an HTTP request to a public notification service (e.g., [ntfy.sh](https://ntfy.sh)) containing basic info about the post (e.g., username, post ID, timestamp).
- The message should be formatted and sent asynchronously.

## Image Processing Service

- When an image is uploaded, process it before saving:
  - Add a small watermark (e.g., "Instakill Demo") at the bottom-right corner.
  - Scale the image to a maximum width of 300 px while preserving aspect ratio.
- The processing can use any suitable image library.
- Encourage students to structure it as a separate service or module that could later run independently (e.g., via message queue or background worker).
