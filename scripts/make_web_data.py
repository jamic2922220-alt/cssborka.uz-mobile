#!/usr/bin/env python3
"""cs16_data.zip (Android bilan bir xil) dan brauzer uchun yengil to'plam: xc_web_data.zip.
Brauzer (ayniqsa iPhone Safari) xotirasi kam - faqat o'yinga kerakli fayllar qoladi:
 - kompyuter/telefon dasturlari (.so .dll .dylib), xarita rasmlari (overviews), bot fayllari (.nav) - yo'q
 - xaritalar: faqat KEEP_MAPS (qolganini server ulanganda o'zi yuklab beradi)
 - .wad teksturalar va osmon (gfx/env) rasmlari: faqat qolgan xaritalar ichida (BSP entity) ko'rsatilganlari
 - Half-Life ning CS da ishlatilmaydigan ovozlari (olimlar, qo'riqchilar, Xen jonzotlari...)"""
import sys, zipfile, re, struct, os
src, dst = sys.argv[1], sys.argv[2]
KEEP_MAPS = {'de_dust2', 'de_dust', 'de_inferno', 'de_nuke', 'de_train', 'de_aztec', 'de_cbble', 'de_prodigy',
             'cs_assault', 'cs_italy', 'cs_office', 'de_mirage', 'awp_map', 'aim_map', 'fy_iceworld', 'de_dust2_2x2'}
BASE_WADS = {'decals', 'cached', 'gfx', 'fonts', 'spraypaint', 'tempdecal', 'pldecal', 'halflife'}
HL_SOUNDS = {'scientist', 'barney', 'tride', 'holo', 'hgrunt', 'zombie', 'houndeye', 'bullchicken', 'gman', 'nihilanth', 'x',
             'agrunt', 'aslave', 'controller', 'garg', 'headcrab', 'hassault', 'ichy', 'leech', 'roach', 'squeek', 'tentacle',
             'turret', 'apache', 'osprey', 'bigmomma', 'boid', 'babygarg', 'fvox', 'vox', 'hornet', 'gonarch', 'kingpin'}
# .so/.dll/.dylib (o'yin kutubxonalari) brauzerda ishlamaydi, lekin QOLADI: server anti-chitlari (Vanga va b.) ularning
# hash ini tekshiradi - fayl bo'lmasa (hash 00000000) "Anti Smoke" deb chiqarib yuboradi
SKIP_EXT = ('.nav', '.exe', '.pdb')
zi = zipfile.ZipFile(src)
names = [i.filename for i in zi.infolist()]

def map_of(n):
    m = re.match(r'cstrike/maps/([^/]+)\.(bsp|res|txt|wad)$', n.lower())
    return m.group(1) if m else None

# qolgan xaritalarning wad va osmon (skyname) ro'yxati - BSP v30: 0-lump = entity matni
wads, skies = set(BASE_WADS), set()
for n in names:
    if n.lower().endswith('.bsp') and map_of(n) in KEEP_MAPS:
        b = zi.read(n)
        off, ln = struct.unpack('<II', b[4:12])
        ent = b[off:off + ln].decode('latin1', 'replace')
        w = re.search(r'"wad"\s+"([^"]*)"', ent)
        if w:
            for x in w.group(1).replace('\\', '/').split(';'):
                if x.strip(): wads.add(os.path.splitext(os.path.basename(x.strip()))[0].lower())
        s = re.search(r'"skyname"\s+"([^"]*)"', ent)
        skies.add((s.group(1) if s else 'desert').lower())
print('wad:', sorted(wads)); print('osmon:', sorted(skies))

def keep(n):
    l = n.lower()
    if l.endswith('/') or l.endswith(SKIP_EXT): return False
    if '/overviews/' in l or l.startswith('valve/maps/'): return False
    m = map_of(n)
    if m and m not in KEEP_MAPS: return False
    if re.match(r'(valve|cstrike)/[^/]+\.wad$', l) and os.path.splitext(os.path.basename(l))[0] not in wads: return False
    e = re.match(r'(valve|cstrike)/gfx/env/(.+)(up|dn|lf|rt|ft|bk)\.(tga|bmp)$', l)
    if e and e.group(2) not in skies: return False
    s = re.match(r'valve/sound/([^/]+)/', l)
    if s and s.group(1) in HL_SOUNDS: return False
    return True

zo = zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED, compresslevel=9)
n = u = 0
for i in zi.infolist():
    if not keep(i.filename): continue
    zo.writestr(i.filename, zi.read(i.filename)); n += 1; u += i.file_size
zo.close()
print('fayllar: %d, ochilgandagi hajm: %d MB, arxiv: %d MB' % (n, u >> 20, os.path.getsize(dst) >> 20))
