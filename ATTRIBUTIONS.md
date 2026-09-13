# LockENS attributions and asset provenance

The root MIT license applies to original LockENS software. It does not relicense
third-party photographs or override their source licenses, public-domain status,
attribution requirements, or embedded provenance data.

## Remote demo pass artwork

These images are referenced by public demo-pass metadata and rendered remotely
by the Android app. Their source binaries are not packaged in this repository.
At display time LockENS downloads, decodes, resizes, and center-crops an image to
fit the pass card; it does not publish a modified source file.

| Asset and use | Creator/source | Status and required attribution | LockENS modification |
| --- | --- | --- | --- |
| `Standard-lock-key.jpg` — Full Access Showcase pass | Evan-Amos, [Wikimedia Commons source page](https://commons.wikimedia.org/wiki/File:Standard-lock-key.jpg) | Dedicated to the public domain by the copyright holder. Attribution is not required; courtesy credit: “Photo by Evan-Amos.” | Runtime resize/crop for display only; no derivative file is stored here. |
| `Front_door_(23200352782).jpg` — Front Door Showcase pass | Susan Dennis, [Wikimedia Commons source page](https://commons.wikimedia.org/wiki/File:Front_door_(23200352782).jpg) | Marked as a Public Domain Work at the original Flickr source and reviewed by Wikimedia Commons. Attribution is not required; courtesy credit: “Susan Dennis.” | Runtime resize/crop for display only; no derivative file is stored here. |
| `Stop_sign.png` — Suspended Showcase pass | U.S. Manual on Uniform Traffic Control Devices; transferred from English Wikipedia by users including Denelson83, [Wikimedia Commons source page](https://commons.wikimedia.org/wiki/File:Stop_sign.png) | Public domain: the design is from the MUTCD and the image is also described as simple geometry/text below the threshold of originality. No copyright attribution is required. | Runtime resize/crop for display only; no derivative file is stored here. |
| `Server_racks,_one_empty_-_IMG_3636.jpg` — Server Room Showcase pass | Jemimus, [Wikimedia Commons source page](https://commons.wikimedia.org/wiki/File:Server_racks,_one_empty_-_IMG_3636.jpg) | [Creative Commons Attribution 2.0 Generic](https://creativecommons.org/licenses/by/2.0/). Required credit: “Server racks, one empty” by Jemimus, CC BY 2.0, source linked here. | Displayed with runtime resize/center-crop. This presentation adaptation does not imply endorsement by the creator. |

## Packaged Gate Stand backgrounds

The Android package contains three local portrait backgrounds:

- `mobile/android/app/src/main/res/drawable-nodpi/gate_scene_front_door.png`
- `mobile/android/app/src/main/res/drawable-nodpi/gate_scene_lab.png`
- `mobile/android/app/src/main/res/drawable-nodpi/gate_scene_server_room.png`

All three were generated specifically for LockENS through OpenAI's image
generation capability invoked from Codex, using participant-directed scene and
layout requirements. Codex selected and integrated the returned images; the
participant reviewed and approved the Gate Stand result. The repository/session
evidence does not identify a specific image model, so none is claimed.

The exact generation prompts are preserved in
[docs/ai/PROMPTS.md](docs/ai/PROMPTS.md). The returned PNGs were copied
byte-for-byte into the Android resources and were not cropped, resized, or
converted before packaging. Each committed PNG retains its original C2PA
`caBX` provenance manifest chunk. Runtime Android rendering may center-crop the
background to the device view. Do not strip the C2PA chunk when optimizing or
transcoding these assets.

These generated project assets are disclosed separately from the MIT-licensed
software. The MIT notice does not remove their C2PA provenance or override terms
that apply to generated media.
