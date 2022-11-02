# ayunMCVNC
 Map VNC/streaming & audio for Minecraft 1.8-1.19!!

## How do I get started?

Well, since I haven't really put anything together yet, here's some snippets for parts that are completely undocumented:
```
ffmpeg -y -re -stream_loop -1 -thread_queue_size 4096 -i "my-cool.m3u8" -vf scale=512:256 -f rawvideo -c:v mjpeg -qscale:v 16 -r 20 udp://127.0.0.1:1337 -f s16le -acodec pcm_s16le -ac 2 -ar 48000 udp://127.0.0.1:1338
ffmpeg -f gdigrab -framerate 20 -i desktop -vf scale=1024:512 -f rawvideo -c:v mjpeg -qscale:v 16 -r 20 udp://127.0.0.1:1337
/mcvnc create fard 4 2 false 1000 1337;1338
DO NOT USE THE "ALT DISPLAY" """FEATURE""" IT LAGS A LOT AND IS INCOMPLETE/ABANDONED!!
example for repeating command block (holds key as long as it's powered): /mcvnc cb @ key Return
with no permission system, non-op players are only able to view screens, and nothing else.
when creating/moving a screen, it moves so that it's top left corner is at the block you are facing.
```

## Credits
- [MakiDesktop](https://github.com/ayunami2000/MakiDesktop) Project that this is a rewrite of
- [CodedRed](https://www.youtube.com/channel/UC_kPUW3XPrCCRT9a4Pnf1Tg) For ImageManager class
- [DNx5](https://github.com/dnx5) for synchronizing the maps, optimizing the code, implementing sierra2 dithering. literally do all the hard work for me
- [EzMediaCore](https://github.com/MinecraftMediaLibrary/EzMediaCore) for the dither algorithm
- [MakiScreen](https://github.com/makitsune/MakiScreen) Lagless map rendering
- [Vernacular VNC](https://github.com/shinyhut/vernacular-vnc) VNC support
- [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) For multi-version support
- [JeroMQ](https://github.com/zeromq/jeromq) For the sad attempt at a sort of "VR" mode (to send rotation information to an ffmpeg process)