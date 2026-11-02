# Infoscreen player (phase 1)

Static full-screen player for a Sharp Android TV in kiosk use. One `index.html` (inline CSS/JS, no build step) reads `playlist.json` and shows images and videos with a fade between items. Deploy the whole folder as a Cloudflare Pages site (or any static host).

## Files
- `index.html` — the player
- `playlist.json` — what to show
- `media/*.svg` — three demo slides (1920×1080)

## What the player does
- Full-screen, black background, media scaled with `object-fit: contain` (never cropped).
- Crossfade between items. The next item is fully loaded before the fade starts, so there are no blank frames or hard cuts.
- Images show for `durationSec`. Videos play muted and advance when they end.
- Re-reads `playlist.json` every 60 s, and also when the network comes back. Changes take effect without a page reload. No `version` bump is needed.
- If the fetch fails or the JSON is invalid, it keeps the last good playlist. The last good playlist is also saved in `localStorage`, so a reload during a bad deploy still has content.
- Broken or missing media is skipped. The current slide stays on screen while the player skips.
- An empty playlist shows a dim "Ingen innhold" placeholder.
- Uses the Screen Wake Lock API when the browser supports it.
- Hides the mouse cursor. No text is drawn over the content (see `?debug` below).

## playlist.json

```json
{
  "defaultDurationSec": 8,
  "transition": "crossfade",
  "transitionMs": 800,
  "items": [
    { "type": "image", "url": "media/slide-1.svg", "durationSec": 8, "alt": "Velkommen" },
    { "type": "video", "url": "media/clip.mp4" }
  ]
}
```

| Field | Meaning |
|---|---|
| `transition` | `"crossfade"` (default) or `"fade"` (out to black, then in) |
| `transitionMs` | Fade length in ms (0–10000, default 800) |
| `defaultDurationSec` | Image duration when an item has no `durationSec` (default 10) |
| `items[].type` | `"image"` or `"video"`. Other types are ignored. |
| `items[].url` | Use a relative path like `media/...` so the playlist works on any host |
| `items[].durationSec` | Images: how long to show. Videos: optional maximum play time. |
| `items[].alt` | Label shown in debug mode |

Videos should be H.264/AAC MP4 at 1080p or lower. That is the safest format for TV hardware decoders.

## Test locally
`fetch()` does not work from `file://`, so serve the folder with any static server:

```sh
python3 -m http.server 8000
# open http://localhost:8000/?debug
```

## Open it on the TV
1. Publish the folder to Cloudflare Pages. It must be served over HTTPS for wake lock to work.
2. On the Sharp Android TV, install a browser. Android TV has no built-in browser. A full-screen kiosk browser that can start on boot works best, either from Google Play or sideloaded (for example Fully Kiosk Browser). A normal TV browser is fine for testing.
3. Open the Pages URL. Press OK on the remote (or click) once to go full screen.
4. Check that the three slides crossfade. OK/→ goes to the next slide, ← goes back, and F enters full screen.
5. TV settings (menu names differ by model):
   - Turn off the screensaver / ambient mode.
   - Turn off "auto power off" / "no operation off".
   - Set the picture size to full pixel / no overscan so the edges aren't cropped.
   - In the kiosk browser, turn on "keep screen on" and "start on boot". Many WebView-based kiosk browsers don't support the Wake Lock API, so their own setting is what keeps the screen awake.

URL options:
- `?debug`: shows the cursor and a small status line (current item, skipped files, fetch errors).
- `?wakelock=0`: turns off the wake lock request.

### Offline behaviour
After the first load, the player keeps working without network. It keeps the last playlist and holds the playlist's images in memory. Videos are not cached and are skipped while offline. If the page itself is reloaded with no network, it cannot recover, because phase 1 has no service worker.

## Phase 2 (not in this folder)
- **Drive inbox:** staff drop images and videos into a shared Google Drive folder.
- **Copy to R2/Pages:** a scheduled job copies new and changed files to Cloudflare R2 (or the Pages site) and removes deleted ones.
- **Rewrite `playlist.json`:** the same job builds the playlist from the folder (order, durations) and publishes it. The player picks it up within 60 s, with no change on the TV.

The player is not expected to change for this. Phase 2 only replaces the hand-edited `playlist.json` and `media/` with generated ones.
