package su.xash.cs16client;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * CS 1.6 boost-top Mobile - boshlang'ich ekran.
 * boost-top.com monitoringidan TOP / BOOST / VIP / Sevimlilar serverlarini ko'rsatadi,
 * o'yin fayllarini yuklab beradi va Xash3D FWGS dvijokini kerakli server bilan ishga tushiradi.
 */
public class MainActivity extends Activity {

    // ---------------- sozlamalar (saytdagi mobile.json ularni almashtira oladi) ----------------
    private static final String SERVERS_URL = "http://mmb.boost-top.com/servers.php";
    private static final String CONFIG_URL = "http://mmb.boost-top.com/mobile.json";
    // Dvijok bu manzilga "/v1/servers/cstrike" qo'shib so'raydi
    private static final String STATS_URL = "http://mmb.boost-top.com/stats.php";
    private static final String MASTER_URL = "http://mmb.boost-top.com/ms.php?q=";
    private String dataUrl = "http://mmb.boost-top.com/mobile/cs16_data.zip";
    private String engineUrl = "https://github.com/FWGS/xash3d-fwgs/releases/tag/continuous";
    private String siteUrl = "https://boost-top.com";
    private int dataSizeMb = 250;

    // ---------------- ranglar ----------------
    private static final int C_BG = 0xFF0A1426;
    private static final int C_CARD = 0xFF13213A;
    private static final int C_LINE = 0xFF22385C;
    private static final int C_TEXT = 0xFFEAF2FF;
    private static final int C_MUTED = 0xFF9FB6D6;
    private static final int C_ACCENT = 0xFF1E8CFF;
    private static final int C_GOLD = 0xFFFFB21E;
    private static final int C_MAP = 0xFFF39A1E;
    private static final int C_PL = 0xFF8E5CF0;
    private static final int C_OFF = 0xFF5A6476;

    private LinearLayout content;
    private TextView status;
    private final Map<String, Bitmap> flags = new HashMap<String, Bitmap>();
    private String lang = "uz";
    private volatile boolean downloading = false;
    private ProgressBar dlBar;
    private TextView dlText;
    private JSONObject lastData;

    // ---------------- matnlar ----------------
    private String t(String key) {
        String[][] tr = {
                // key, uz, ru, en
                {"title", "CS 1.6 boost-top", "CS 1.6 boost-top", "CS 1.6 boost-top"},
                {"subtitle", "Eng yaxshi serverlar", "Лучшие серверы", "Best servers"},
                {"join", "ULANISH", "ИГРАТЬ", "PLAY"},
                {"play", "O'YINNI OCHISH", "ОТКРЫТЬ ИГРУ", "OPEN GAME"},
                {"refresh", "Yangilash", "Обновить", "Refresh"},
                {"loading", "Serverlar yuklanmoqda...", "Загрузка серверов...", "Loading servers..."},
                {"neterr", "Internetga ulanib bo'lmadi. Yangilash tugmasini bosing.", "Нет соединения. Нажмите «Обновить».", "No connection. Tap Refresh."},
                {"empty", "Hozircha serverlar yo'q", "Пока нет серверов", "No servers yet"},
                {"offline", "OFFLINE", "OFFLINE", "OFFLINE"},
                {"setup", "O'yinni sozlash (bir marta)", "Настройка игры (один раз)", "Game setup (one time)"},
                {"s1", "1. Xash3D dvijokini o'rnating", "1. Установите движок Xash3D", "1. Install the Xash3D engine"},
                {"s1b", "YUKLAB OLISH", "СКАЧАТЬ", "DOWNLOAD"},
                {"s2", "2. Dvijokni bir marta oching va fayllarga ruxsat bering, keyin shu yerga qayting", "2. Откройте движок один раз и разрешите доступ к файлам, затем вернитесь сюда", "2. Open the engine once, allow file access, then come back"},
                {"s2b", "DVIJOKNI OCHISH", "ОТКРЫТЬ ДВИЖОК", "OPEN ENGINE"},
                {"s3", "O'yin fayllarini yuklab oling (bir marta)", "Скачайте файлы игры (один раз)", "Download game files (one time)"},
                {"s3b", "YUKLASH", "СКАЧАТЬ", "DOWNLOAD"},
                {"done", "✓ Tayyor", "✓ Готово", "✓ Done"},
                {"perm", "Fayllarni saqlash uchun ruxsat bering", "Разрешите доступ к файлам для сохранения", "Allow file access to save files"},
                {"dlwarn", "Taxminan %d MB yuklanadi. Wi-Fi tavsiya etiladi. Davom etamizmi?", "Будет загружено около %d МБ. Рекомендуется Wi-Fi. Продолжить?", "About %d MB will be downloaded. Wi-Fi recommended. Continue?"},
                {"yes", "Ha", "Да", "Yes"},
                {"no", "Yo'q", "Нет", "No"},
                {"dling", "Yuklanmoqda: ", "Загрузка: ", "Downloading: "},
                {"dlok", "O'yin fayllari tayyor!", "Файлы игры готовы!", "Game files are ready!"},
                {"dlerr", "Yuklashda xato: ", "Ошибка загрузки: ", "Download error: "},
                {"needsetup", "Avval o'yinni sozlang (yuqoridagi qadamlar)", "Сначала настройте игру (шаги выше)", "Set up the game first (steps above)"},
                {"settings", "Sozlamalar", "Настройки", "Settings"},
                {"lang", "Til", "Язык", "Language"},
                {"sens", "Sichqoncha / ekran sezgirligi", "Чувствительность", "Sensitivity"},
                {"xsize", "Pritsel o'lchami", "Размер прицела", "Crosshair size"},
                {"xcolor", "Pritsel rangi", "Цвет прицела", "Crosshair color"},
                {"auto", "Avto", "Авто", "Auto"},
                {"small", "Kichik", "Малый", "Small"},
                {"medium", "O'rta", "Средний", "Medium"},
                {"large", "Katta", "Большой", "Large"},
                {"save", "Saqlash", "Сохранить", "Save"},
                {"cancel", "Bekor qilish", "Отмена", "Cancel"},
                {"saved", "Saqlandi. O'yinda qo'llanadi.", "Сохранено. Применится в игре.", "Saved. Applied in game."},
                {"ad", "Serveringizni shu yerga chiqarish: boost-top.com", "Разместить свой сервер здесь: boost-top.com", "Promote your server here: boost-top.com"},
        };
        int col = "ru".equals(lang) ? 2 : ("en".equals(lang) ? 3 : 1);
        for (String[] row : tr) if (row[0].equals(key)) return row[col];
        return key;
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private GradientDrawable round(int color, float radiusDp, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) d.setStroke(dp(1), strokeColor);
        return d;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private Button button(String s, int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(round(color, 8, 0));
        b.setPadding(dp(14), dp(6), dp(14), dp(6));
        b.setMinHeight(dp(38));
        b.setMinimumHeight(dp(38));
        return b;
    }

    // ---------------- hayotiy sikl ----------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String l = Locale.getDefault().getLanguage();
        String defLang = "ru".equals(l) || "uk".equals(l) || "be".equals(l) || "kk".equals(l) ? "ru" : ("en".equals(l) ? "en" : "uz");
        lang = prefs().getString("lang", defLang);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(C_BG);
            getWindow().setNavigationBarColor(C_BG);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(C_BG);

        // sarlavha
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), dp(14), dp(12), dp(10));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text(t("title"), 20, C_TEXT, true));
        titles.addView(text(t("subtitle"), 13, C_GOLD, true));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button refresh = button(t("refresh"), C_LINE);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadServers();
            }
        });
        Button gear = button("⚙", C_LINE);
        gear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        gear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.setMargins(0, 0, dp(8), 0);
        head.addView(gear, glp);
        head.addView(refresh);
        root.addView(head);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), 0, dp(12), dp(12));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // pastki panel
        LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.VERTICAL);
        foot.setPadding(dp(12), dp(8), dp(12), dp(12));
        foot.setBackgroundColor(0xFF081020);
        Button play = button(t("play"), C_ACCENT);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame(null);
            }
        });
        foot.addView(play, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        TextView ad = text(t("ad"), 12, C_MUTED, false);
        ad.setGravity(Gravity.CENTER);
        ad.setPadding(0, dp(8), 0, 0);
        ad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(siteUrl);
            }
        });
        foot.addView(ad);
        root.addView(foot);

        setContentView(root);
        loadConfig();
        sendStat("open", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        loadServers();
    }

    // ---------------- dvijok va o'yin fayllari ----------------
    // Dvijok (Xash3D FWGS) shu ilovaning ichida - alohida o'rnatish shart emas.
    // O'yin fayllari ilovaning o'z papkasida saqlanadi - hech qanday ruxsat kerak emas.
    private File baseDir() {
        File ext = getExternalFilesDir(null);
        if (ext == null) ext = getFilesDir();
        return new File(ext, "xash");
    }

    private boolean hasData() {
        File b = baseDir();
        return new File(b, "valve/liblist.gam").exists() && new File(b, "cstrike/liblist.gam").exists();
    }

    private boolean setupDone() {
        return hasData();
    }

    /** O'yin ichidagi "Favorites" bo'limiga saytdagi serverlarni yozadi (cstrike/favorite_servers.lst). */
    private void writeFavorites() {
        try {
            JSONArray sections = lastData == null ? null : lastData.optJSONArray("sections");
            if (sections == null) sections = new JSONArray();
            StringBuilder sb = new StringBuilder();
            java.util.HashSet<String> seen = new java.util.HashSet<String>();
            for (int i = 0; i < sections.length(); i++) {
                JSONObject s = sections.optJSONObject(i);
                JSONArray list = s == null ? null : s.optJSONArray("servers");
                if (list == null) continue;
                for (int k = 0; k < list.length(); k++) {
                    JSONObject sv = list.optJSONObject(k);
                    String ip = sv == null ? "" : sv.optString("ip", "");
                    if (ip.length() > 0 && seen.add(ip)) sb.append(ip).append(" gs\n");
                }
            }
            File dir = new File(baseDir(), "cstrike");
            if (!dir.exists()) return;
            // master-server manzili qo'shtirnoq ichida bo'lishi shart ("//" izoh deb o'qilmasin)
            FileOutputStream ms = new FileOutputStream(new File(dir, "boosttop_ms.cfg"));
            ms.write(("addmasterstatic \"" + MASTER_URL + "\"\n").getBytes("UTF-8"));
            ms.close();
            if (sb.length() == 0) return;
            FileOutputStream fo = new FileOutputStream(new File(dir, "favorite_servers.lst"));
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.close();
        } catch (Exception ignored) {
        }
    }

    private void launchGame(String ip) {
        if (!hasData()) {
            toastLike(t("needsetup"));
            return;
        }
        writeFavorites();
        writeSettings();
        // O'yin ichidagi server ro'yxatida faqat boost-top.com serverlari chiqsin:
        // standart (begona) master-serverlarni o'chirib, o'zimiznikini qo'shamiz.
        String argv = "-log -dll @yapb +clearmasters +exec boosttop_ms.cfg +exec boosttop_settings.cfg";
        if (ip != null) {
            argv += " +connect " + ip;
            sendStat("connect", ip);
        }
        try {
            startActivity(new Intent().setComponent(new ComponentName(getPackageName(), "su.xash.engine.XashActivity"))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("gamedir", "cstrike")
                    .putExtra("gamelibdir", getApplicationInfo().nativeLibraryDir)
                    .putExtra("basedir", baseDir().getAbsolutePath())
                    .putExtra("argv", argv)
                    .putExtra("package", getPackageName()));
        } catch (Exception e) {
            toastLike(String.valueOf(e.getMessage()));
        }
    }

    // ---------------- sozlamalar: til, sezgirlik, pritsel ----------------
    private static final String[] LANGS = {"uz", "ru", "en"};
    private static final String[] LANG_NAMES = {"O'zbekcha", "Русский", "English"};
    private static final String[] XSIZES = {"auto", "small", "medium", "large"};
    private static final int[] XCOLORS = {0xFF32FA32, 0xFFFA3232, 0xFF3232FA, 0xFFFAFA32, 0xFF32FAFA, 0xFFFFFFFF, 0xFFFA32FA};

    private SharedPreferences prefs() {
        return getSharedPreferences("boosttop", MODE_PRIVATE);
    }

    private static String engineLang(String l) {
        return "ru".equals(l) ? "russian" : ("en".equals(l) ? "english" : "uzbek");
    }

    /** Sozlamalarni o'yin konfiguratsiyasiga yozadi (cstrike/boosttop_settings.cfg va config.cfg). */
    private void writeSettings() {
        try {
            SharedPreferences p = prefs();
            int c = p.getInt("xcolor", XCOLORS[0]);
            String color = ((c >> 16) & 0xff) + " " + ((c >> 8) & 0xff) + " " + (c & 0xff);
            String[][] cv = {
                    {"ui_language", engineLang(lang)},
                    {"sensitivity", String.format(Locale.US, "%.1f", p.getFloat("sens", 3.0f))},
                    {"cl_crosshair_size", p.getString("xsize", "auto")},
                    {"cl_crosshair_color", color},
                    {"xhair_enable", "0"},
                    {"developer", "0"},
            };
            File dir = new File(baseDir(), "cstrike");
            if (!dir.exists()) return;
            StringBuilder sb = new StringBuilder();
            for (String[] v : cv) sb.append(v[0]).append(" \"").append(v[1]).append("\"\n");
            FileOutputStream fo = new FileOutputStream(new File(dir, "boosttop_settings.cfg"));
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.close();
            // config.cfg ga ham yozamiz - til menyu ochilishidan oldin o'qilishi uchun
            File cfg = new File(dir, "config.cfg");
            if (!cfg.exists()) return; // birinchi ishga tushishda dvijok o'zi yaratadi
            StringBuilder out = new StringBuilder();
            {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(cfg), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    String tl = line.trim();
                    boolean ours = false;
                    for (String[] v : cv) if (tl.startsWith(v[0] + " ") || tl.equals(v[0])) ours = true;
                    if (!ours) out.append(line).append('\n');
                }
                br.close();
            }
            out.append(sb);
            FileOutputStream co = new FileOutputStream(cfg);
            co.write(out.toString().getBytes("UTF-8"));
            co.close();
        } catch (Exception ignored) {
        }
    }

    /** Pritsel ko'rinishi (namuna). */
    private class CrossView extends View {
        int color = XCOLORS[0];
        String size = "auto";
        final Paint paint = new Paint();

        CrossView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas cv) {
            cv.drawColor(0xFF2A3A2A);
            paint.setColor(color);
            paint.setStrokeWidth(dp(2));
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float len = dp("small".equals(size) ? 6 : "large".equals(size) ? 14 : 10);
            float gap = dp("small".equals(size) ? 3 : "large".equals(size) ? 7 : 5);
            cv.drawLine(cx - gap - len, cy, cx - gap, cy, paint);
            cv.drawLine(cx + gap, cy, cx + gap + len, cy, paint);
            cv.drawLine(cx, cy - gap - len, cx, cy - gap, paint);
            cv.drawLine(cx, cy + gap, cx, cy + gap + len, paint);
        }
    }

    private LinearLayout chipRow() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(6), 0, dp(4));
        return r;
    }

    private void showSettings() {
        final SharedPreferences p = prefs();
        final String[] selLang = {lang};
        final float[] selSens = {p.getFloat("sens", 3.0f)};
        final String[] selSize = {p.getString("xsize", "auto")};
        final int[] selColor = {p.getInt("xcolor", XCOLORS[0])};

        ScrollView sv = new ScrollView(this);
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(14), dp(18), dp(6));
        box.setBackgroundColor(C_CARD);
        sv.addView(box);

        // til
        box.addView(text(t("lang"), 14, C_GOLD, true));
        final LinearLayout langRow = chipRow();
        final Button[] langBtns = new Button[LANGS.length];
        for (int i = 0; i < LANGS.length; i++) {
            final int k = i;
            langBtns[i] = button(LANG_NAMES[i], C_LINE);
            langBtns[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selLang[0] = LANGS[k];
                    for (int j = 0; j < LANGS.length; j++) langBtns[j].setBackground(round(j == k ? C_ACCENT : C_LINE, 8, 0));
                }
            });
            langBtns[i].setBackground(round(LANGS[i].equals(selLang[0]) ? C_ACCENT : C_LINE, 8, 0));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(0, 0, dp(6), 0);
            langRow.addView(langBtns[i], lp);
        }
        box.addView(langRow);

        // sezgirlik
        final TextView sensLabel = text("", 14, C_GOLD, true);
        sensLabel.setPadding(0, dp(12), 0, 0);
        sensLabel.setText(t("sens") + ": " + String.format(Locale.US, "%.1f", selSens[0]));
        box.addView(sensLabel);
        SeekBar sb = new SeekBar(this);
        sb.setMax(95); // 0.5 .. 10.0
        sb.setProgress(Math.round((selSens[0] - 0.5f) * 10));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean u) {
                selSens[0] = 0.5f + v / 10f;
                sensLabel.setText(t("sens") + ": " + String.format(Locale.US, "%.1f", selSens[0]));
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        box.addView(sb);

        // pritsel namunasi
        final CrossView cross = new CrossView(this);
        cross.color = selColor[0];
        cross.size = selSize[0];
        LinearLayout.LayoutParams cvp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70));
        cvp.setMargins(0, dp(12), 0, 0);
        box.addView(cross, cvp);

        // o'lcham
        TextView xs = text(t("xsize"), 14, C_GOLD, true);
        xs.setPadding(0, dp(10), 0, 0);
        box.addView(xs);
        LinearLayout sizeRow = chipRow();
        final Button[] sizeBtns = new Button[XSIZES.length];
        for (int i = 0; i < XSIZES.length; i++) {
            final int k = i;
            sizeBtns[i] = button(t(XSIZES[i]), C_LINE);
            sizeBtns[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            sizeBtns[i].setBackground(round(XSIZES[i].equals(selSize[0]) ? C_ACCENT : C_LINE, 8, 0));
            sizeBtns[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selSize[0] = XSIZES[k];
                    cross.size = XSIZES[k];
                    cross.invalidate();
                    for (int j = 0; j < XSIZES.length; j++) sizeBtns[j].setBackground(round(j == k ? C_ACCENT : C_LINE, 8, 0));
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(0, 0, dp(5), 0);
            sizeRow.addView(sizeBtns[i], lp);
        }
        box.addView(sizeRow);

        // rang
        TextView xc = text(t("xcolor"), 14, C_GOLD, true);
        xc.setPadding(0, dp(10), 0, 0);
        box.addView(xc);
        LinearLayout colorRow = chipRow();
        final View[] sw = new View[XCOLORS.length];
        for (int i = 0; i < XCOLORS.length; i++) {
            final int k = i;
            sw[i] = new View(this);
            sw[i].setBackground(round(XCOLORS[i], 18, XCOLORS[i] == selColor[0] ? Color.WHITE : C_LINE));
            sw[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selColor[0] = XCOLORS[k];
                    cross.color = XCOLORS[k];
                    cross.invalidate();
                    for (int j = 0; j < XCOLORS.length; j++) {
                        GradientDrawable d = round(XCOLORS[j], 18, j == k ? Color.WHITE : C_LINE);
                        if (j == k) d.setStroke(dp(3), Color.WHITE);
                        sw[j].setBackground(d);
                    }
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
            lp.setMargins(0, 0, dp(8), 0);
            colorRow.addView(sw[i], lp);
        }
        box.addView(colorRow);

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                .setTitle("⚙ " + t("settings"))
                .setView(sv)
                .setPositiveButton(t("save"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        boolean langChanged = !selLang[0].equals(lang);
                        p.edit().putString("lang", selLang[0]).putFloat("sens", selSens[0])
                                .putString("xsize", selSize[0]).putInt("xcolor", selColor[0]).apply();
                        lang = selLang[0];
                        writeSettings();
                        toastLike(t("saved"));
                        if (langChanged) recreate();
                    }
                })
                .setNegativeButton(t("cancel"), null)
                .show();
    }

    private void confirmDownload() {
        if (downloading) return;
        new AlertDialog.Builder(this)
                .setMessage(String.format(Locale.US, t("dlwarn"), dataSizeMb))
                .setPositiveButton(t("yes"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        startDownload();
                    }
                })
                .setNegativeButton(t("no"), null)
                .show();
    }

    private void startDownload() {
        downloading = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        render();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String error = null;
                HttpURLConnection c = null;
                try {
                    File base = baseDir();
                    if (!base.exists() && !base.mkdirs()) throw new Exception("mkdir " + base);
                    String basePath = base.getCanonicalPath() + File.separator;
                    c = (HttpURLConnection) new URL(dataUrl).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.setInstanceFollowRedirects(true);
                    if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                    final long total = c.getContentLength() > 0 ? c.getContentLength() : (long) dataSizeMb * 1024 * 1024;
                    CountingStream in = new CountingStream(c.getInputStream());
                    ZipInputStream zip = new ZipInputStream(in);
                    ZipEntry e;
                    byte[] buf = new byte[65536];
                    long lastUi = 0;
                    while ((e = zip.getNextEntry()) != null) {
                        String name = normalizeEntry(e.getName());
                        if (name == null) continue;
                        File out = new File(base, name);
                        if (!out.getCanonicalPath().startsWith(basePath)) continue; // xavfsizlik
                        if (e.isDirectory()) {
                            out.mkdirs();
                            continue;
                        }
                        File parent = out.getParentFile();
                        if (parent != null) parent.mkdirs();
                        OutputStream os = new FileOutputStream(out);
                        int n;
                        while ((n = zip.read(buf)) > 0) {
                            os.write(buf, 0, n);
                            long now = System.currentTimeMillis();
                            if (now - lastUi > 300) {
                                lastUi = now;
                                final long done = in.count;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showProgress(done, total);
                                    }
                                });
                            }
                        }
                        os.close();
                    }
                    zip.close();
                    if (!hasData()) throw new Exception("valve/cstrike?");
                } catch (Exception ex) {
                    error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                } finally {
                    if (c != null) c.disconnect();
                }
                final String err = error;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloading = false;
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        toastLike(err == null ? t("dlok") : t("dlerr") + err);
                        render();
                    }
                });
            }
        }).start();
    }

    /** Zip ichida ortiqcha papka bo'lsa ham (masalan cs16_data/cstrike/...) to'g'ri joyga ochadi. */
    private static String normalizeEntry(String n) {
        n = n.replace('\\', '/');
        String[] roots = {"valve/", "cstrike/"};
        for (String r : roots) {
            if (n.startsWith(r)) return n;
            int k = n.indexOf("/" + r);
            if (k >= 0) return n.substring(k + 1);
        }
        return null;
    }

    private void showProgress(long done, long total) {
        if (dlBar == null || dlText == null) return;
        int pct = (int) Math.min(100, done * 100 / Math.max(1, total));
        dlBar.setProgress(pct);
        dlText.setText(t("dling") + (done / (1024 * 1024)) + " / " + (total / (1024 * 1024)) + " MB (" + pct + "%)");
    }

    private static class CountingStream extends java.io.FilterInputStream {
        volatile long count = 0;

        CountingStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws java.io.IOException {
            int r = super.read();
            if (r >= 0) count++;
            return r;
        }

        @Override
        public int read(byte[] b, int off, int len) throws java.io.IOException {
            int r = super.read(b, off, len);
            if (r > 0) count += r;
            return r;
        }
    }

    // ---------------- statistika (boost-top.com) ----------------
    /** Telefonning shifrlangan raqami (asl ANDROID_ID saytga yuborilmaydi). */
    private String deviceHash() {
        try {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == null) id = "unknown";
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest((getPackageName() + "|" + id).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) sb.append(String.format(Locale.US, "%02x", h[i] & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return "00000000000000000000000000000000";
        }
    }

    /** Fonda saytga xabar yuboradi: ilova ochildi yoki serverga ulandi. */
    private void sendStat(final String action, final String ip) {
        final String dev = deviceHash();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = STATS_URL + "?a=" + action + "&d=" + dev;
                    if (ip != null) url += "&ip=" + java.net.URLEncoder.encode(ip, "UTF-8");
                    httpGet(url);
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    // ---------------- tarmoq ----------------
    private static String httpGet(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        c.setUseCaches(false);
        try {
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            return bo.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    private void loadConfig() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject j = new JSONObject(httpGet(CONFIG_URL + "?t=" + System.currentTimeMillis()));
                    dataUrl = j.optString("data_url", dataUrl);
                    engineUrl = j.optString("engine_url", engineUrl);
                    siteUrl = j.optString("site", siteUrl);
                    dataSizeMb = j.optInt("data_size_mb", dataSizeMb);
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private void loadServers() {
        if (status != null) status.setText(t("loading"));
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject data = null;
                try {
                    data = new JSONObject(httpGet(SERVERS_URL + "?t=" + System.currentTimeMillis()));
                } catch (Exception ignored) {
                }
                final JSONObject d = data;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (d != null) lastData = d;
                        render();
                        if (d == null && status != null) status.setText(t("neterr"));
                    }
                });
            }
        }).start();
    }

    private void loadFlag(final String cc, final String base, final ImageView iv) {
        if (TextUtils.isEmpty(cc)) return;
        Bitmap b = flags.get(cc);
        if (b != null) {
            iv.setImageBitmap(b);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(base + cc).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    final Bitmap bmp = BitmapFactory.decodeStream(c.getInputStream());
                    c.disconnect();
                    if (bmp == null) return;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            flags.put(cc, bmp);
                            iv.setImageBitmap(bmp);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    private void toastLike(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
    }

    // ---------------- ekranni chizish ----------------
    private void render() {
        if (content == null) return;
        content.removeAllViews();
        dlBar = null;
        dlText = null;

        if (!setupDone() || downloading) content.addView(setupCard());

        status = text("", 13, C_MUTED, false);
        status.setPadding(dp(4), dp(10), dp(4), dp(4));
        content.addView(status);

        if (lastData == null) {
            status.setText(t("loading"));
            return;
        }
        JSONArray sections = lastData.optJSONArray("sections");
        String flagBase = lastData.optString("flag_url", "http://mmb.boost-top.com/flag.php?c=");
        int shown = 0;
        if (sections != null) {
            for (int i = 0; i < sections.length(); i++) {
                JSONObject s = sections.optJSONObject(i);
                if (s == null) continue;
                JSONArray list = s.optJSONArray("servers");
                if (list == null || list.length() == 0) continue;
                String title = s.optString("title", "");
                JSONObject titles = s.optJSONObject("titles");
                if (titles != null && titles.has(lang)) title = titles.optString(lang, title);
                content.addView(sectionHeader(title, s.optString("key", "")));
                for (int k = 0; k < list.length(); k++) {
                    JSONObject sv = list.optJSONObject(k);
                    if (sv != null) {
                        content.addView(serverRow(sv, k + 1, flagBase));
                        shown++;
                    }
                }
            }
        }
        status.setText(shown == 0 ? t("empty") : "");
        status.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }

    private View setupCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setBackground(round(C_CARD, 12, C_GOLD));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(8));
        card.setLayoutParams(lp);
        card.addView(text(t("setup"), 16, C_GOLD, true));

        boolean data = hasData();
        card.addView(step(t("s3"), (data || downloading) ? null : t("s3b"), data, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDownload();
            }
        }));
        if (downloading) {
            dlBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            dlBar.setMax(100);
            card.addView(dlBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
            dlText = text(t("dling") + "...", 12, C_MUTED, false);
            card.addView(dlText);
        }
        return card;
    }

    private View step(String label, String btn, boolean done, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, 0);
        TextView tv = text(label, 13, done ? C_MUTED : C_TEXT, false);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (done) {
            row.addView(text(t("done"), 13, 0xFF35C46A, true));
        } else if (btn != null) {
            Button b = button(btn, C_ACCENT);
            b.setOnClickListener(click);
            row.addView(b);
        }
        return row;
    }

    private View sectionHeader(String title, String key) {
        TextView h = text(title.toUpperCase(Locale.ROOT), 14, Color.WHITE, true);
        h.setLetterSpacing(0.08f);
        h.setPadding(dp(4), dp(14), dp(4), dp(6));
        int color = "top".equals(key) ? 0xFFFF8A00 : "boost".equals(key) ? 0xFF22B8E0 : "vip".equals(key) ? 0xFFFFCC33 : 0xFFEC4899;
        h.setTextColor(color);
        return h;
    }

    private View serverRow(JSONObject sv, int rank, String flagBase) {
        final String ip = sv.optString("ip");
        boolean online = sv.optBoolean("online", false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackground(round(C_CARD, 10, C_LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);

        TextView rk = text(String.valueOf(rank), 14, rank == 1 ? 0xFF1A1200 : C_TEXT, true);
        rk.setGravity(Gravity.CENTER);
        rk.setBackground(round(rank == 1 ? C_GOLD : rank == 2 ? 0xFFC9D3E0 : rank == 3 ? 0xFFD98A4E : C_LINE, 6, 0));
        if (rank == 2 || rank == 3) rk.setTextColor(0xFF10161F);
        row.addView(rk, new LinearLayout.LayoutParams(dp(28), dp(28)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(10), 0, dp(8), 0);

        LinearLayout nameLine = new LinearLayout(this);
        nameLine.setOrientation(LinearLayout.HORIZONTAL);
        nameLine.setGravity(Gravity.CENTER_VERTICAL);
        ImageView flag = new ImageView(this);
        flag.setAdjustViewBounds(true);
        flag.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(13));
        flp.setMargins(0, 0, dp(6), 0);
        nameLine.addView(flag, flp);
        loadFlag(sv.optString("cc", ""), flagBase, flag);
        TextView name = text(sv.optString("name", ip), 14, C_TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        nameLine.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        info.addView(nameLine);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding(0, dp(5), 0, 0);
        if (online) {
            meta.addView(pill(sv.optString("map", "").toUpperCase(Locale.ROOT), C_MAP, 0xFF1B0F00));
            meta.addView(pill(sv.optInt("players", 0) + "/" + sv.optInt("max", 0), C_PL, Color.WHITE));
        } else {
            meta.addView(pill(t("offline"), C_OFF, Color.WHITE));
        }
        TextView ipv = text(ip, 11, C_MUTED, false);
        ipv.setSingleLine(true);
        meta.addView(ipv);
        info.addView(meta);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button join = button(t("join"), C_ACCENT);
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame(ip);
            }
        });
        row.addView(join);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame(ip);
            }
        });
        return row;
    }

    private TextView pill(String s, int bg, int fg) {
        TextView p = text(s, 10, fg, true);
        p.setBackground(round(bg, 4, 0));
        p.setPadding(dp(6), dp(1), dp(6), dp(1));
        p.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(6), 0);
        p.setLayoutParams(lp);
        return p;
    }
}
