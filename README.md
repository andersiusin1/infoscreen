# Infoscreen player (phase 1)

Static full-screen player with crossfade. Deploy the whole folder as a Cloudflare Pages site (or any static host).

## Files
- `index.html` — player (fade/crossfade, image + video, reloads playlist every 60s)
- `playlist.json` — sample playlist
- `media/*.svg` — three demo slides

## Deploy
Publish this directory to Cloudflare Pages. Public URL should serve `index.html` at `/` and `playlist.json` at `/playlist.json`.

## TV test
1. Open the Pages URL on the Sharp Android TV browser (or a kiosk browser).
2. Confirm three slides crossfade.
3. Tap/click advances one slide (for testing).

Phase 2 (not in this folder): Drive inbox → copy to R2/Pages → rewrite playlist.json.
