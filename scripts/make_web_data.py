#!/usr/bin/env python3
"""cs16_data.zip (Android bilan bir xil) dan brauzer uchun yengil to'plam: xc_web_data.zip.
Brauzer (ayniqsa iPhone Safari) xotirasi kam - faqat o'yinga kerakli fayllar qoladi.
Olib tashlanadi: kompyuter/telefon dasturlari (.so .dll .dylib), xarita rasmlari (overviews), bot fayllari (.nav),
Half-Life xaritalari, kam o'ynaladigan xaritalar (server ulanganda o'zi yuklab beradi)."""
import sys, zipfile, re
src, dst = sys.argv[1], sys.argv[2]
KEEP_MAPS = {'de_dust2', 'de_dust', 'de_inferno', 'de_nuke', 'de_train', 'de_aztec', 'de_cbble', 'de_prodigy',
             'cs_assault', 'cs_italy', 'cs_office', 'de_mirage', 'awp_map', 'aim_map', 'fy_iceworld', 'de_dust2_2x2'}
SKIP_EXT = ('.so', '.dll', '.dylib', '.nav', '.exe', '.pdb')
def keep(n):
    l = n.lower()
    if l.endswith('/') or l.endswith(SKIP_EXT): return False
    if '/overviews/' in l or l.startswith('valve/maps/'): return False
    if '/cl_dlls/' in l or '/dlls/' in l: return False
    m = re.match(r'cstrike/maps/([^/]+)\.(bsp|res|txt|wad)$', l)
    if m and m.group(1) not in KEEP_MAPS: return False
    return True
zi = zipfile.ZipFile(src); zo = zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED, compresslevel=9)
n = u = 0
for i in zi.infolist():
    if not keep(i.filename): continue
    zo.writestr(i.filename, zi.read(i.filename)); n += 1; u += i.file_size
zo.close()
import os
print('fayllar: %d, ochilgandagi hajm: %d MB, arxiv: %d MB' % (n, u >> 20, os.path.getsize(dst) >> 20))
