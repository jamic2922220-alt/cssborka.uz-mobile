#!/usr/bin/env python3
"""xash.wasm (npm xash3d-fwgs) xotirasi qat'iy 256 MB - HD modelli serverlarda xarita yuklanayotganda "Aborted(OOM)".
Modulning memory bo'limidagi min/max sahifalarni o'zgartiramiz (1 sahifa = 64 KB). Kod o'zgarmaydi."""
import sys
src, dst, mb = sys.argv[1], sys.argv[2], int(sys.argv[3])
pages = mb * 16
d = bytearray(open(src, 'rb').read())
assert d[:4] == b'\0asm'
def leb_read(b, i):
    r = s = 0
    while True:
        x = b[i]; i += 1; r |= (x & 0x7f) << s; s += 7
        if x < 0x80: return r, i
def leb(n):
    o = bytearray()
    while True:
        x = n & 0x7f; n >>= 7
        if n: o.append(x | 0x80)
        else: o.append(x); return bytes(o)
i = 8
while i < len(d):
    sid = d[i]; size, j = leb_read(d, i + 1); end = j + size
    if sid == 5:
        cnt, k = leb_read(d, j); assert cnt == 1
        flags = d[k]; k += 1
        mn, k = leb_read(d, k); mx = None
        if flags & 1: mx, k = leb_read(d, k)
        print('eski: min %d MB, max %s' % (mn // 16, (mx // 16) if mx is not None else '-'))
        body = leb(1) + bytes([flags]) + leb(pages) + (leb(max(pages, mx or 0)) if flags & 1 else b'')
        d[i:end] = bytes([5]) + leb(len(body)) + body
        print('yangi: %d MB' % mb)
        break
    i = end
else:
    sys.exit('memory bolimi topilmadi')
open(dst, 'wb').write(d)
