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
import android.widget.EditText;
import android.widget.CheckBox;
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
    // avtomatik yangilanish: saytdagi mobile.json -> apk_build / apk_url
    private volatile int apkBuild = 0;
    private volatile String apkUrl = "";
    // yangiliklar: saytdagi mobile.json -> "news"
    private volatile JSONArray news = null;

    // ---------------- ranglar ----------------
    private static final int C_BG = 0xFF0F0A0E;
    private static final int C_CARD = 0xFF1C1318;
    private static final int C_LINE = 0xFF3A2428;
    private static final int C_TEXT = 0xFFECE6EA;
    private static final int C_MUTED = 0xFF9A8F98;
    private static final int C_ACCENT = 0xFFE5333B;
    private static final int C_GOLD = 0xFFFFB21E;
    private static final int C_MAP = 0xFFF39A1E;
    private static final int C_PL = 0xFF8E5CF0;
    private static final int C_OFF = 0xFF5A6476;

    private LinearLayout content;
    private TextView status;
    private final Map<String, Bitmap> flags = new HashMap<String, Bitmap>();
    private String lang = "uz";
    private volatile boolean downloading = false;
    private boolean retryPending = false;
    private ProgressBar dlBar;
    private TextView dlText;
    private JSONObject lastData;
    // ping: telefondan har bir serverga UDP so'rov (A2S_INFO) javob vaqti, ms
    private final Map<String, Integer> pings = new java.util.concurrent.ConcurrentHashMap<String, Integer>();
    private final Map<String, TextView> pingViews = new HashMap<String, TextView>();
    private volatile boolean pinging = false;

    // ---------------- matnlar ----------------
    private String t(String key) {
        String[][] tr = {
                // key, uz, ru, en
                {"title", "X CLIENT", "X CLIENT", "X CLIENT"},
                {"subtitle", "Eng yaxshi serverlar", "Лучшие серверы", "Best servers"},
                {"join", "ULANISH", "ИГРАТЬ", "PLAY"},
                {"play", "O'YINNI OCHISH", "ОТКРЫТЬ ИГРУ", "OPEN GAME"},
                {"refresh", "Yangilash", "Обновить", "Refresh"},
                {"loading", "Serverlar yuklanmoqda...", "Загрузка серверов...", "Loading servers..."},
                {"neterr", "Internetga ulanib bo'lmadi. Yangilash tugmasini bosing.", "Нет соединения. Нажмите «Обновить».", "No connection. Tap Refresh."},
                {"sitedown", "Serverlar ro'yxati saytdan kelmadi - oxirgi ro'yxat ko'rsatilmoqda. O'ynash mumkin.", "Сайт со списком серверов недоступен - показан последний список. Играть можно.", "Server list site is unreachable - showing the last list. You can still play."},
                {"empty", "Hozircha serverlar yo'q", "Пока нет серверов", "No servers yet"},
                {"offline", "O'CHIQ", "ОФЛАЙН", "OFFLINE"},
                {"setup", "O'yinni sozlash (bir marta)", "Настройка игры (один раз)", "Game setup (one time)"},
                {"s3", "O'yin fayllarini yuklab oling (bir marta)", "Скачайте файлы игры (один раз)", "Download game files (one time)"},
                {"s3b", "YUKLASH", "СКАЧАТЬ", "DOWNLOAD"},
                {"done", "✓ Tayyor", "✓ Готово", "✓ Done"},
                {"dlwarn", "Taxminan %d MB yuklanadi. Wi-Fi tavsiya etiladi. Davom etamizmi?", "Будет загружено около %d МБ. Рекомендуется Wi-Fi. Продолжить?", "About %d MB will be downloaded. Wi-Fi recommended. Continue?"},
                {"yes", "Ha", "Да", "Yes"},
                {"no", "Yo'q", "Нет", "No"},
                {"dling", "Yuklanmoqda: ", "Загрузка: ", "Downloading: "},
                {"dlok", "O'yin fayllari tayyor!", "Файлы игры готовы!", "Game files are ready!"},
                {"dlerr", "Yuklashda xato: ", "Ошибка загрузки: ", "Download error: "},
                {"needsetup", "Avval o'yinni sozlang (yuqoridagi qadamlar)", "Сначала настройте игру (шаги выше)", "Set up the game first (steps above)"},
                {"mic_ask", "Ovozli chat uchun mikrofonga ruxsat bering", "Разрешите микрофон для голосового чата", "Allow the microphone for voice chat"},
                {"upd", "Yangi versiya chiqdi!", "Вышла новая версия!", "New version available!"},
                {"updb", "YANGILASH", "ОБНОВИТЬ", "UPDATE"},
                {"nick", "O'yindagi nik", "Ник в игре", "In-game nickname"},
                {"gfx", "Grafika", "Графика", "Graphics"},
                {"gfx_high", "Yuqori", "Высокая", "High"},
                {"gfx_mid", "O'rta", "Средняя", "Medium"},
                {"gfx_low", "Past (tez)", "Низкая (быстро)", "Low (fast)"},
                {"fps", "FPS ni ko'rsatish", "Показывать FPS", "Show FPS"},
                {"favs", "⭐ Mening serverlarim", "⭐ Мои серверы", "⭐ My servers"},
                {"srv_add", "➕ Server qo'shish (IP)", "➕ Добавить сервер (IP)", "➕ Add server (IP)"},
                {"sv_title", "Serverlar", "Серверы", "Servers"},
                {"sv_open", "🌐 Serverlar", "🌐 Серверы", "🌐 Servers"},
                {"sv_net", "Internet", "Интернет", "Internet"},
                {"sv_fav", "Sevimlilar", "Избранное", "Favorites"},
                {"sv_hist", "Tarix", "История", "History"},
                {"sv_top", "X TOP", "X TOP", "X TOP"},
                {"sv_search", "Qidirish: nom, IP yoki xarita", "Поиск: название, IP или карта", "Search: name, IP or map"},
                {"sv_map", "Xarita", "Карта", "Map"},
                {"sv_all", "hammasi", "все", "all"},
                {"sv_ping", "Ping < 50", "Пинг < 50", "Ping < 50"},
                {"sv_nempty", "Bo'sh emas", "Не пустые", "Not empty"},
                {"sv_nfull", "To'la emas", "Не полные", "Not full"},
                {"sv_quick", "Tez o'yin", "Быстрая игра", "Quick play"},
                {"sv_col_srv", "SERVER", "СЕРВЕР", "SERVER"},
                {"sv_col_pl", "O'YINCHI", "ИГРОКИ", "PLAYERS"},
                {"sv_col_map", "XARITA", "КАРТА", "MAP"},
                {"sv_col_ping", "PING", "ПИНГ", "PING"},
                {"sv_count", "{s} ta server · {p} o'yinchi onlayn", "{s} серверов · {p} игроков онлайн", "{s} servers · {p} players online"},
                {"sv_refresh", "Yangilash", "Обновить", "Refresh"},
                {"sv_join", "▶ Kirish", "► Войти", "► Join"},
                {"sv_tofav", "★ Sevimlilarga", "★ В избранное", "★ Favorite"},
                {"sv_unfav", "☆ Olib tashlash", "☆ Убрать", "☆ Unfavorite"},
                {"sv_copy", "Nusxa", "Копировать", "Copy"},
                {"sv_copied", "IP nusxalandi", "IP скопирован", "IP copied"},
                {"sv_players", "O'YINCHILAR", "ИГРОКИ", "PLAYERS"},
                {"sv_none", "Bu yerda hozircha server yo'q", "Здесь пока нет серверов", "No servers here yet"},
                {"sv_pick", "Avval serverni tanlang", "Сначала выберите сервер", "Select a server first"},
                {"sv_noplayers", "Serverda hozir hech kim yo'q", "На сервере сейчас никого нет", "Nobody is on the server now"},
                {"srv_add_msg", "Istalgan server IP manzilini yozing. U «Mening serverlarim»ga va o'yindagi «Избранное»ga qo'shiladi.", "Введите IP любого сервера. Он появится в «Мои серверы» и в «Избранное» в игре.", "Enter any server IP. It will appear in “My servers” and in the game's Favorites."},
                {"srv_add_btn", "Qo'shish", "Добавить", "Add"},
                {"srv_join_btn", "Qo'shish va kirish", "Добавить и войти", "Add and join"},
                {"srv_bad", "IP noto'g'ri. Masalan: 51.89.39.107:27015", "Неверный IP. Пример: 51.89.39.107:27015", "Invalid IP. Example: 51.89.39.107:27015"},
                {"srv_added", "Server qo'shildi", "Сервер добавлен", "Server added"},
                {"pl_nick", "Nik", "Ник", "Name"},
                {"pl_score", "Frag", "Фраги", "Score"},
                {"pl_time", "Vaqt", "Время", "Time"},
                {"pl_none", "Hozir serverda hech kim yo'q", "Сейчас на сервере никого нет", "Nobody is playing right now"},
                {"pl_err", "Server javob bermadi", "Сервер не ответил", "Server did not respond"},
                {"close", "Yopish", "Закрыть", "Close"},
                {"updperm", "Ruxsat bering: \"Shu manbadan o'rnatish\" ni yoqing, keyin qaytib YANGILASH ni bosing", "Разрешите установку из этого источника, затем вернитесь и нажмите ОБНОВИТЬ", "Allow installs from this source, then come back and tap UPDATE"},
                {"updwarn", "Yangi versiya yuklanadi. Keyin telefonning O'RNATISH oynasi chiqadi: \"Yangilash/O'rnatish\" ni, tugagach \"Ochish\" ni bosing. O'yin fayllari saqlanib qoladi.", "Новая версия скачается. Затем откроется окно УСТАНОВКИ телефона: нажмите \"Обновить/Установить\", а после - \"Открыть\". Файлы игры сохранятся.", "The new version will download. Then your phone's INSTALL screen opens: tap \"Update/Install\", then \"Open\". Game files are kept."},
                {"updok", "Davom etish", "Продолжить", "Continue"},
                {"upderr", "Yangilab bo'lmadi", "Не удалось обновить", "Update failed"},
                {"updretry", "Qayta urinish", "Повторить", "Retry"},
                {"updbrowser", "Brauzer orqali", "Через браузер", "Via browser"},
                {"settings", "Sozlamalar", "Настройки", "Settings"},
                {"lang", "Til", "Язык", "Language"},
                {"sens", "Ekran (nishon) sezgirligi", "Чувствительность экрана (прицел)", "Screen (aim) sensitivity"},
                {"xsize", "Pritsel o'lchami", "Размер прицела", "Crosshair size"},
                {"xcolor", "Pritsel rangi", "Цвет прицела", "Crosshair color"},
                {"xthick", "Pritsel qalinligi", "Толщина прицела", "Crosshair thickness"},
                {"xt0", "Klassik", "Классика", "Classic"},
                {"xt1", "Ingichka", "Тонкий", "Thin"},
                {"xt2", "Qalin", "Толстый", "Thick"},
                {"xt3", "Juda qalin", "Очень толстый", "Extra thick"},
                {"xdot", "Markazda nuqta", "Точка в центре", "Center dot"},
                {"ctrl", "Boshqaruv", "Управление", "Controls"},
                {"gyro", "Giroskop bilan nishon olish (PRO)", "Прицеливание гироскопом (PRO)", "Gyro aiming (PRO)"},
                {"gyrosens", "Giroskop sezgirligi", "Чувствительность гироскопа", "Gyro sensitivity"},
                {"ctrl_pro", "PRO (2 ta o'q tugmasi)", "PRO (2 кнопки огня)", "PRO (2 fire buttons)"},
                {"ctrl_std", "Standart", "Стандарт", "Standard"},
                {"auto", "Avto", "Авто", "Auto"},
                {"small", "Kichik", "Малый", "Small"},
                {"medium", "O'rta", "Средний", "Medium"},
                {"large", "Katta", "Большой", "Large"},
                {"save", "Saqlash", "Сохранить", "Save"},
                {"cancel", "Bekor qilish", "Отмена", "Cancel"},
                {"saved", "Saqlandi. O'yinda qo'llanadi.", "Сохранено. Применится в игре.", "Saved. Applied in game."},
                {"ad", "Serveringizni shu yerga chiqarish: boost-top.com", "Разместить свой сервер здесь: boost-top.com", "Promote your server here: boost-top.com"},
                {"acc_t", "X CLIENT akkaunti", "Аккаунт X CLIENT", "X CLIENT account"},
                {"acc_s", "Kompyuterdagi akkaunting bilan kir: daraja, XP, do'stlar va kunlik sovg'a umumiy", "Войдите аккаунтом с компьютера: уровень, XP, друзья и ежедневный подарок общие", "Sign in with your PC account: level, XP, friends and daily gift are shared"},
                {"acc_in", "Kirish", "Войти", "Sign in"},
                {"acc_code", "Kompyuterdagi kod", "Код с компьютера", "Code from PC"},
                {"acc_codeh", "Kompyuterda X CLIENT menyusi → «Telefonda ham o'yna» → «Kod olish». 6 xonali kodni shu yerga yozing:", "На компьютере в меню X CLIENT → «Играй и на телефоне» → «Получить код». Введите 6 цифр:", "On PC: X CLIENT menu → «Play on phone» → «Get code». Enter the 6 digits:"},
                {"acc_pass", "Login va parol", "Логин и пароль", "Login and password"},
                {"acc_login", "Login yoki email", "Логин или email", "Login or email"},
                {"acc_pw", "Parol", "Пароль", "Password"},
                {"acc_lvl", "Daraja", "Уровень", "Level"},
                {"acc_daily", "Kunlik sovg'a", "Ежедневный подарок", "Daily gift"},
                {"acc_got", "Olindi: +%d XP (seriya %d kun)", "Получено: +%d XP (серия %d дн.)", "Got: +%d XP (streak %d days)"},
                {"acc_today", "Bugungi sovg'a olingan - ertaga yana", "Сегодняшний подарок получен - завтра снова", "Today's gift taken - come back tomorrow"},
                {"acc_fr", "Onlayn do'stlar", "Друзья онлайн", "Friends online"},
                {"acc_nofr", "Hozir onlayn do'st yo'q", "Сейчас нет друзей онлайн", "No friends online now"},
                {"acc_menu", "menyuda", "в меню", "in menu"},
                {"acc_phone", "telefonda", "на телефоне", "on phone"},
                {"acc_out", "Chiqish", "Выйти", "Sign out"},
                {"acc_err", "Internet yo'q yoki sayt javob bermadi", "Нет интернета или сайт не ответил", "No internet or the site did not respond"},
                {"acc_xp", "O'yindagi vaqt uchun XP kompyuterdagidek yoziladi", "XP за время в игре начисляется как на компьютере", "XP for play time is counted like on PC"},
                {"acc_tg", "Telegram orqali", "Через Telegram", "Via Telegram"},
                {"acc_tgwait", "Telegram ochildi - «Start» ni bosing, keyin shu yerga qayting", "Открылся Telegram - нажмите «Start» и вернитесь сюда", "Telegram opened - tap «Start», then come back here"},
                {"acc_tgok", "Telegram orqali kirildi", "Вход через Telegram выполнен", "Signed in via Telegram"},
                {"acc_tgexp", "Telegram'dan tasdiq kelmadi. Qayta urinib ko'ring", "Подтверждение из Telegram не пришло. Попробуйте ещё раз", "No confirmation from Telegram. Try again"},
                {"acc_prof", "Profil", "Профиль", "Profile"},
                {"acc_pic", "Profil rasmini tanlash", "Выбрать фото профиля", "Choose profile picture"},
                {"acc_picdel", "Rasmni o'chirish", "Удалить фото", "Remove picture"},
                {"acc_name", "Ismni o'zgartirish", "Изменить имя", "Change name"},
                {"acc_nc", "Nik rangi", "Цвет ника", "Nickname color"},
                {"acc_ncnone", "Oddiy (oq)", "Обычный (белый)", "Default (white)"},
                {"acc_saved", "Saqlandi - kompyuterda ham ko'rinadi", "Сохранено - видно и на компьютере", "Saved - also visible on PC"},
                {"nc_red", "Qizil", "Красный", "Red"},
                {"nc_orange", "To'q sariq", "Оранжевый", "Orange"},
                {"nc_yellow", "Sariq", "Жёлтый", "Yellow"},
                {"nc_green", "Yashil", "Зелёный", "Green"},
                {"nc_cyan", "Moviy", "Голубой", "Cyan"},
                {"nc_blue", "Ko'k", "Синий", "Blue"},
                {"nc_purple", "Binafsha", "Фиолетовый", "Purple"},
                {"nc_pink", "Pushti", "Розовый", "Pink"},
                {"off_t", "Internet yo'qmi? Muammo emas", "Нет интернета? Не проблема", "No internet? No problem"},
                {"off_s", "Botlar bilan o'yna - internet kerak emas", "Играйте с ботами - интернет не нужен", "Play with bots - no internet needed"},
                {"off_b", "Botlar bilan", "С ботами", "With bots"},
                {"acc_tglink", "Telegram'ni ulash", "Подключить Telegram", "Connect Telegram"},
                {"acc_tglinked", "Telegram allaqachon ulangan - endi Telegram orqali kira olasiz", "Telegram уже подключён - можно входить через Telegram", "Telegram is already connected - you can sign in via Telegram"},
                {"acc_tglinkgo", "Telegram ochildi - «Start» ni bosing. Ulangach sandiq sovg'a!", "Открылся Telegram - нажмите «Start». После подключения - сундук в подарок!", "Telegram opened - tap «Start». You get a chest once connected!"},
                {"bot_t", "Botlar bilan mashq", "Тренировка с ботами", "Practice with bots"},
                {"bot_s", "Internet kerak emas - o'zingga mos qiyinlikni tanla", "Интернет не нужен - выберите сложность", "No internet needed - pick a difficulty"},
                {"bot_b", "O'ynash", "Играть", "Play"},
                {"bot_lv", "Botlar darajasi", "Сложность ботов", "Bot difficulty"},
                {"bot_l0", "Oson", "Лёгкие", "Easy"},
                {"bot_l1", "O'rta", "Средние", "Medium"},
                {"bot_l2", "Qiyin", "Сложные", "Hard"},
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
        foot.setBackgroundColor(0xFF0B080B);
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
        registerInstallReceiver();
        sendStat("open", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reportGameIssues();
        presence = false;
        accLoad();
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

    /** Saytdagi ro'yxatdan qat'i nazar, o'yin ichidagi "Favorites" ning boshida doim turadigan serverlar. */
    private static final String[] FAV_FIXED = { "212.47.58.89:27015", "212.47.58.40:27015" };

    /** O'yin ichidagi "Favorites" bo'limiga saytdagi serverlarni yozadi (cstrike/favorite_servers.lst). */
    private void writeFavorites() {
        try {
            JSONArray sections = lastData == null ? null : lastData.optJSONArray("sections");
            if (sections == null) sections = new JSONArray();
            StringBuilder sb = new StringBuilder();
            java.util.HashSet<String> seen = new java.util.HashSet<String>();
            // har doim "Favorites" da turadigan serverlar (egasining so'rovi) - saytdan ro'yxat kelmasa ham
            for (String ip : FAV_FIXED) if (seen.add(ip)) sb.append(ip).append(" gs\n");
            // ilovada qo'shilgan / yulduzcha bosilgan serverlar
            java.util.Iterator<String> fit = favs().keys();
            while (fit.hasNext()) {
                String ip = fit.next();
                if (seen.add(ip)) sb.append(ip).append(" gs\n");
            }
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
            // O'yin menyusidagi "Случайный сервер" tugmasi uchun: ro'yxatimizdan tasodifiy onlayn server
            java.util.List<String> online = new java.util.ArrayList<String>();
            for (int i = 0; i < sections.length(); i++) {
                JSONObject s = sections.optJSONObject(i);
                JSONArray list = s == null ? null : s.optJSONArray("servers");
                if (list != null) for (int k = 0; k < list.length(); k++) {
                    JSONObject sv = list.optJSONObject(k);
                    if (sv != null && sv.optBoolean("online", false)) online.add(sv.optString("ip"));
                }
            }
            String rnd = online.isEmpty() ? "" : online.get(new java.util.Random().nextInt(online.size()));
            FileOutputStream rf = new FileOutputStream(new File(dir, "boosttop_random.cfg"));
            rf.write((rnd.length() > 0 ? "connect " + rnd + "\n" : "echo no servers\n").getBytes("UTF-8"));
            rf.close();
            // Menyu tugmalari kompyuterdagidek matn ko'rinishida (tanlangan tilda)
            File lib = new File(dir, "liblist.gam");
            if (lib.exists()) {
                String txt = new String(readAll(lib), "UTF-8");
                if (!txt.contains("render_picbutton_text")) {
                    FileOutputStream lo = new FileOutputStream(lib, true);
                    lo.write("\nrender_picbutton_text \"1\"\n".getBytes("UTF-8"));
                    lo.close();
                }
            }
            if (sb.length() == 0) return;
            // o'yin ichida "Add server" orqali qo'shilganlarni o'chirib yubormaymiz:
            // oldin biz yozmagan qatorlar - o'yinchiniki, ular saqlanadi
            File favf = new File(dir, "favorite_servers.lst");
            java.util.HashSet<String> ours = new java.util.HashSet<String>(java.util.Arrays.asList(prefs().getString("fav_ours", "").split("\n")));
            String ourNow = sb.toString();
            if (favf.exists()) {
                for (String line : new String(readAll(favf), "UTF-8").split("\n")) {
                    String ln = line.trim();
                    if (ln.length() == 0) continue;
                    String ip = ln.split("\\s+")[0];
                    if (!ours.contains(ip) && seen.add(ip)) sb.append(ln).append('\n');
                }
            }
            StringBuilder ob = new StringBuilder();
            for (String line : ourNow.split("\n")) if (line.trim().length() > 0) ob.append(line.trim().split("\\s+")[0]).append('\n');
            prefs().edit().putString("fav_ours", ob.toString()).apply();
            FileOutputStream fo = new FileOutputStream(favf);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.close();
        } catch (Exception ignored) {
        }
    }

    private static byte[] readAll(File f) throws java.io.IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toByteArray();
        } finally {
            in.close();
        }
    }

    // ---------------- mikrofon (ovozli chat) ----------------
    private boolean micAsked = false;
    private String micPendingIp = null;

    /** O'yindan oldin mikrofon ruxsatini so'raymiz (bir sessiyada bir marta, 3 marta rad etilsa boshqa so'ramaymiz). */
    private boolean askMic(String ip) {
        if (Build.VERSION.SDK_INT < 23 || micAsked) return false;
        try {
            if (checkSelfPermission("android.permission.RECORD_AUDIO") == android.content.pm.PackageManager.PERMISSION_GRANTED) return false;
            int n = prefs().getInt("mic_ask", 0);
            if (n >= 3) return false;
            prefs().edit().putInt("mic_ask", n + 1).apply();
            micAsked = true;
            micPendingIp = ip;
            toastLike(t("mic_ask"));
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 79);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req == 79) {
            if (res.length > 0 && res[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)
                prefs().edit().putInt("mic_ask", 0).apply();
            launchGame(micPendingIp);
        }
    }

    private void launchGame(String ip) {
        if (!hasData()) {
            toastLike(t("needsetup"));
            return;
        }
        if (askMic(ip)) return;
        writeFavorites();
        writeSettings();
        // O'yin ichidagi server ro'yxatida faqat boost-top.com serverlari chiqsin:
        // standart (begona) master-serverlarni o'chirib, o'zimiznikini qo'shamiz.
        String argv = "-log -dll @yapb +clearmasters +exec boosttop_ms.cfg +exec boosttop_settings.cfg" + touchPresetArg();
        if (ip != null) {
            addHist(ip);
            argv += " +connect " + ip;
            sendStat("connect", ip);
        }
        startPresence(ip);
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

    // ---------------- PRO boshqaruv (tugmalar joylashuvi) ----------------
    private static final String TOUCH_PRO = "// boost-top PRO boshqaruv: chap qo'lda ham O'Q tugmasi (o'ng barmoq nishonda qoladi)\ntouch_removeall\ntouch_pitch \"50\"\ntouch_yaw \"50\"\ntouch_precise_amount \"0.5\"\ntouch_move_indicator \"3\"\ntouch_addbutton \"move\" \"\" \"_move\" 0.000000 0.110000 0.480000 1.000000 255 255 255 150 0\ntouch_addbutton \"look\" \"\" \"_look\" 0.480000 0.110000 1.000000 1.000000 255 255 255 150 0\ntouch_addbutton \"attack_l\" \"touch/gfx/attack\" \"+attack\" 0.040000 0.300000 0.150000 0.500000 255 90 90 190 0 1\ntouch_addbutton \"attack\" \"touch/gfx/attack\" \"+attack\" 0.780000 0.420000 0.920000 0.670000 255 90 90 190 0 1\ntouch_addbutton \"attack2\" \"touch/gfx/attack2\" \"+attack2\" 0.680000 0.250000 0.770000 0.410000 255 255 255 170 0 1\ntouch_addbutton \"jump\" \"touch/gfx/jump\" \"+jump\" 0.910000 0.200000 1.000000 0.380000 255 255 255 160 0 1\ntouch_addbutton \"duck\" \"touch/gfx/duck\" \"+duck\" 0.900000 0.700000 1.000000 0.880000 255 255 255 160 512 1\ntouch_addbutton \"reload\" \"touch/gfx/reload\" \"+reload\" 0.800000 0.720000 0.890000 0.880000 255 255 255 160 0 1\ntouch_addbutton \"use\" \"touch/gfx/use\" \"+use\" 0.920000 0.400000 1.000000 0.540000 255 255 255 150 0 1\ntouch_addbutton \"w1\" \"touch/gfx/w_rifle\" \"slot1\" 0.100000 0.000000 0.160000 0.106667 255 255 255 150 0 1\ntouch_addbutton \"w2\" \"touch/gfx/w_pistol\" \"slot2\" 0.180000 0.000000 0.240000 0.106667 255 255 255 150 0 1\ntouch_addbutton \"w3\" \"touch/gfx/w_knife\" \"slot3\" 0.260000 0.000000 0.320000 0.106667 255 255 255 150 0 1\ntouch_addbutton \"w4\" \"touch/gfx/w_grenade\" \"slot4\" 0.340000 0.000000 0.400000 0.106667 255 255 255 150 0 1\ntouch_addbutton \"w5\" \"touch/gfx/w_c4\" \"slot5\" 0.660000 0.000000 0.720000 0.106667 255 255 255 150 0 1\ntouch_addbutton \"buy\" \"touch/gfx/buy\" \"buy\" 0.460000 0.860000 0.540000 1.000000 255 255 255 150 0 1\ntouch_addbutton \"score\" \"touch/gfx/score\" \"scoreboard\" 0.920000 0.000000 1.000000 0.142222 255 255 255 150 0 1\ntouch_addbutton \"touch_edit\" \"touch/gfx/settings\" \"togglemenu\" 0.460000 0.000000 0.540000 0.142222 255 255 255 150 0 1\ntouch_addbutton \"drop\" \"touch/gfx/drop\" \"drop\" 0.580000 0.000000 0.640000 0.106667 255 255 255 150 0 1\ntouch_addbutton \"chat\" \"touch/gfx/chat_all\" \"messagemode\" 0.000000 0.000000 0.060000 0.106667 255 255 255 150 0 1\ntouch_addbutton \"change_team\" \"touch/gfx/change_team\" \"chooseteam\" 0.000000 0.130000 0.060000 0.240000 255 255 255 150 0 1\ntouch_addbutton \"radio\" \"touch/gfx/radio\" \"showvguimenu 38\" 0.740000 0.000000 0.800000 0.106667 255 255 255 150 0 1\ntouch_addbutton \"walk\" \"touch/gfx/walk\" \"+speed\" 0.180000 0.800000 0.270000 0.960000 255 255 255 140 0 1\ntouch_addbutton \"invprev\" \"touch/gfx/left\" \"invprev\" 0.000000 0.560000 0.060000 0.660000 255 255 255 150 0 1\ntouch_addbutton \"invnext\" \"touch/gfx/right\" \"invnext\" 0.000000 0.680000 0.060000 0.780000 255 255 255 150 0 1\ntouch_addbutton \"voicechat\" \"touch/gfx/voicechat\" \"+voicerecord\" 0.810000 0.140000 0.890000 0.280000 255 255 255 160 0 1\n";
    // ovozli chat tugmasi (eski o'yinchilarga joylashuvini buzmasdan qo'shiladi)
    private static final String TOUCH_VOICE = "touch_addbutton \"voicechat\" \"touch/gfx/voicechat\" \"+voicerecord\" 0.810000 0.140000 0.890000 0.280000 255 255 255 160 0 1\n";
    private static final String TOUCH_V = "|v3";

    /** Boshqaruv tugmalarini o'yinning o'z fayliga (cstrike/touch.cfg) yozamiz.
     *  Dvijok ishga tushganda shu faylni o'qiydi va argv dagi buyruqlarni bosib ketadi,
     *  shuning uchun tugmalarni +exec orqali emas, to'g'ridan-to'g'ri faylga qo'yamiz.
     *  - yangi o'rnatish yoki sozlamalarda boshqaruv almashtirilgan: tanlangan joylashuv to'liq yoziladi;
     *  - qolgan hollarda o'yinchining joylashuvi saqlanadi, faqat ovoz tugmasi yo'q bo'lsa qo'shiladi. */
    private String touchPresetArg() {
        SharedPreferences p = prefs();
        String lay = p.getString("touchlay", "pro");
        String applied = p.getString("touch_applied", "");
        File tc = new File(baseDir(), "cstrike/touch.cfg");
        try {
            File pd = new File(baseDir(), "cstrike/touch_presets");
            pd.mkdirs();
            FileOutputStream po = new FileOutputStream(new File(pd, "boosttop_pro.cfg"));
            po.write(TOUCH_PRO.getBytes("UTF-8"));
            po.close();
            String cur = tc.exists() ? new String(readAll(tc), "UTF-8") : "";
            String out = null;
            if (!tc.exists() || applied.length() == 0) {
                String preset;
                if ("pro".equals(lay)) preset = TOUCH_PRO;
                else {
                    InputStream in = getAssets().open("touch_presets/phone_ahsim.cfg");
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                    in.close();
                    preset = bo.toString("UTF-8");
                }
                if (!preset.contains("+voicerecord")) preset += "\n" + TOUCH_VOICE;
                out = preset + "touch_config_file \"touch.cfg\"\n";
            } else if (!cur.contains("+voicerecord")) {
                // touch_config_file qatoridan oldin qo'shamiz
                int k = cur.lastIndexOf("touch_config_file");
                out = k >= 0 ? cur.substring(0, k) + TOUCH_VOICE + cur.substring(k) : cur + "\n" + TOUCH_VOICE;
            }
            if (out != null) {
                new File(baseDir(), "cstrike").mkdirs();
                FileOutputStream fo = new FileOutputStream(tc);
                fo.write(out.getBytes("UTF-8"));
                fo.close();
            }
            p.edit().putString("touch_applied", lay + TOUCH_V).apply();
        } catch (Exception ignored) {
        }
        return "";
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
    /** Tarjima tuzatishlari (o'yin arxividagi fayllar ustidan, arxivni qayta yuklamasdan; versiya o'zgarganda yoziladi):
     *  o'zbekcha - ~230 qator inglizcha qolgan edi (jamoa va qurol tavsiflari, yordamchi maslahatlar, radio);
     *  ruscha - HUD (Деньги, HP, Набор), "Покинуть игру" va MOTD oynasi tugmalari faylda umuman yo'q edi. */
    private static final int LOC_VER = 2;
    private static final String[][] LOC_FILES = {
            {"xc_loc/cstrike_uzbek.txt", "cstrike/resource/cstrike_uzbek.txt"},
            {"xc_loc/cstrike_russian.txt", "cstrike/resource/cstrike_russian.txt"},
            {"xc_loc/gameui_russian.txt", "valve/resource/gameui_russian.txt"},
            {"xc_loc/valve_russian.txt", "valve/resource/valve_russian.txt"},
            // o'yin menyusi foni: X CLIENT
            {"xc_bg/xc_bg.tga", "cstrike/gfx/xc_bg.tga"},
            {"xc_bg/xc_BackgroundLayout.txt", "cstrike/resource/xc_BackgroundLayout.txt"},
    };
    private void installLoc() {
        if (prefs().getInt("loc_v", 0) == LOC_VER) return;
        boolean all = true;
        for (String[] f : LOC_FILES) {
            try {
                File dst = new File(baseDir(), f[1]);
                if (!dst.getParentFile().exists()) { all = false; continue; }
                InputStream in = getAssets().open(f[0]);
                FileOutputStream fo = new FileOutputStream(dst);
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
                fo.close();
                in.close();
            } catch (Exception e) {
                all = false;
            }
        }
        if (all) prefs().edit().putInt("loc_v", LOC_VER).apply();
    }

    private void writeSettings() {
        installLoc();
        try {
            SharedPreferences p = prefs();
            int c = p.getInt("xcolor", XCOLORS[0]);
            String color = ((c >> 16) & 0xff) + " " + ((c >> 8) & 0xff) + " " + (c & 0xff);
            String[][] cv = {
                    {"ui_language", engineLang(lang)},
                    {"sensitivity", String.format(Locale.US, "%.1f", p.getFloat("sens", 3.0f))},
                    {"cl_crosshair_size", p.getString("xsize", "auto")},
                    {"cl_crosshair_color", color},
                    {"xhair_enable", p.getInt("xthick", 2) > 0 ? "1" : "0"},
                    {"xhair_thick", String.valueOf(Math.max(1, p.getInt("xthick", 2)))},
                    {"xhair_size", "small".equals(p.getString("xsize", "auto")) ? "3" : "large".equals(p.getString("xsize", "auto")) ? "8" : "5"},
                    {"xhair_gap", "small".equals(p.getString("xsize", "auto")) ? "1" : "large".equals(p.getString("xsize", "auto")) ? "3" : "2"},
                    {"xhair_pad", "1"},
                    {"xhair_dot", p.getBoolean("xdot", false) ? "1" : "0"},
                    {"xhair_t", "0"},
                    {"xhair_additive", "0"},
                    {"xhair_dynamic_scale", "0"},
                    {"xhair_color", color + " 255"},
                    // nishon barqaror bo'lishi uchun: ekran chayqalmasin, qurol tez almashsin
                    {"cl_bob", "0"},
                    {"cl_bobup", "0"},
                    {"cl_dynamiccrosshair", "0"},
                    {"hud_fastswitch", "1"},
                    // giroskop PRO: titrashni yumshatish, mayda harakatda aniqlik
                    {"gyro_yaw", String.format(Locale.US, "%.1f", p.getFloat("gyrosens", 2.0f))},
                    {"gyro_pitch", String.format(Locale.US, "%.1f", p.getFloat("gyrosens", 2.0f))},
                    {"gyro_roll", "0"},
                    {"gyro_yaw_deadzone", "0.8"},
                    {"gyro_pitch_deadzone", "0.8"},
                    {"gyro_smooth", "0.5"},
                    {"gyro_soft", "1"},
                    {"cl_showfps", p.getBoolean("fps", false) ? "1" : "0"},
                    {"gl_picmip", "low".equals(p.getString("gfx", "high")) ? "1" : "0"},
                    {"r_dynamic", "high".equals(p.getString("gfx", "high")) ? "1" : "0"},
                    {"fps_max", "low".equals(p.getString("gfx", "high")) ? "60" : "100"},
                    {"developer", "0"},
                    // ovozli chat: doim yoqilgan (tugmani bosib turib gapiriladi)
                    {"voice_enable", "1"},
                    {"voice_modenable", "1"},
                    {"voice_scale", "1"},
                    {"ui_prefer_won_background", "0"},
                    // dvijok har ochilishda boshlang'ich tugmalarni qayta qo'ymasin (telefon o'yinni yopganda
                    // config.cfg saqlanmay qolib, o'yinchi joylashtirgan tugmalar va ovoz tugmasi o'chib ketardi)
                    {"bt_touch_layout", "5"},
                    // tarmoq: o'qlar aniq hisoblanishi uchun (server o'z chegarasigacha cheklaydi)
                    {"rate", "100000"},
                    {"cl_updaterate", "101"},
                    {"cl_cmdrate", "101"},
                    {"ex_interp", "0.02"},
                    {"cl_lw", "1"},
                    {"cl_lc", "1"},
            };
            File dir = new File(baseDir(), "cstrike");
            if (!dir.exists()) return;
            StringBuilder sb = new StringBuilder();
            for (String[] v : cv) sb.append(v[0]).append(" \"").append(v[1]).append("\"\n");
            if (p.contains("gyro")) sb.append("gyro_enable \"").append(p.getBoolean("gyro", false) ? "1" : "0").append("\"\n");
            String nick = p.getString("nick", "").replace("\"", "").replace(";", "").trim();
            if (nick.length() > 0) sb.append("name \"").append(nick).append("\"\n");
            // serverdagi bt_mobile plagini telefon o'yinchisini shu belgi orqali taniydi
            sb.append("setinfo _bt \"m\"\n");
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
                    if (p.getString("nick", "").trim().length() > 0 && tl.startsWith("name ")) ours = true;
                    if (tl.startsWith("setinfo _bt")) ours = true;
                    if (p.contains("gyro") && tl.startsWith("gyro_enable ")) ours = true;
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
        int thick = 2;
        boolean dot = false;
        final Paint paint = new Paint();

        CrossView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas cv) {
            cv.drawColor(0xFF2A3A2A);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float len = dp("small".equals(size) ? 6 : "large".equals(size) ? 14 : 10);
            float gap = dp("small".equals(size) ? 3 : "large".equals(size) ? 7 : 5);
            float w = dp(thick <= 0 ? 1.5f : thick * 1.5f);
            paint.setStyle(Paint.Style.FILL);
            // qora hoshiya (PRO pritselda) - har qanday fonda ko'rinadi
            for (int pass = (thick > 0 ? 0 : 1); pass < 2; pass++) {
                float o = pass == 0 ? dp(1) : 0;
                paint.setColor(pass == 0 ? 0xFF000000 : color);
                cv.drawRect(cx - gap - len - o, cy - w / 2 - o, cx - gap + o, cy + w / 2 + o, paint);
                cv.drawRect(cx + gap - o, cy - w / 2 - o, cx + gap + len + o, cy + w / 2 + o, paint);
                cv.drawRect(cx - w / 2 - o, cy - gap - len - o, cx + w / 2 + o, cy - gap + o, paint);
                cv.drawRect(cx - w / 2 - o, cy + gap - o, cx + w / 2 + o, cy + gap + len + o, paint);
                if (dot && thick > 0) cv.drawRect(cx - w / 2 - o, cy - w / 2 - o, cx + w / 2 + o, cy + w / 2 + o, paint);
            }
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
        final int[] selThick = {p.getInt("xthick", 2)};

        ScrollView sv = new ScrollView(this);
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(14), dp(18), dp(6));
        box.setBackgroundColor(C_CARD);
        sv.addView(box);

        // nik
        box.addView(text(t("nick"), 14, C_GOLD, true));
        final EditText nickEd = new EditText(this);
        nickEd.setSingleLine(true);
        nickEd.setTextColor(C_TEXT);
        nickEd.setHintTextColor(C_MUTED);
        String curNick = p.getString("nick", "");
        if (curNick.length() == 0) curNick = playerName();
        nickEd.setText(curNick);
        nickEd.setHint("Player");
        nickEd.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(31)});
        box.addView(nickEd);

        // til
        TextView lt = text(t("lang"), 14, C_GOLD, true);
        lt.setPadding(0, dp(10), 0, 0);
        box.addView(lt);
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
        cross.thick = selThick[0];
        cross.dot = p.getBoolean("xdot", false);
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

        // qalinlik
        TextView xt = text(t("xthick"), 14, C_GOLD, true);
        xt.setPadding(0, dp(10), 0, 0);
        box.addView(xt);
        LinearLayout thickRow = chipRow();
        final Button[] thickBtns = new Button[4];
        for (int i = 0; i < 4; i++) {
            final int k = i;
            thickBtns[i] = button(t("xt" + i), C_LINE);
            thickBtns[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            thickBtns[i].setBackground(round(i == selThick[0] ? C_ACCENT : C_LINE, 8, 0));
            thickBtns[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selThick[0] = k;
                    cross.thick = k;
                    cross.invalidate();
                    for (int j = 0; j < 4; j++) thickBtns[j].setBackground(round(j == k ? C_ACCENT : C_LINE, 8, 0));
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(0, 0, dp(4), 0);
            thickRow.addView(thickBtns[i], lp);
        }
        box.addView(thickRow);
        final CheckBox dotCb = new CheckBox(this);
        dotCb.setText(t("xdot"));
        dotCb.setTextColor(C_TEXT);
        dotCb.setChecked(p.getBoolean("xdot", false));
        dotCb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                cross.dot = on;
                cross.invalidate();
            }
        });
        box.addView(dotCb);

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

        // giroskop
        final CheckBox gyroCb = new CheckBox(this);
        gyroCb.setText(t("gyro"));
        gyroCb.setTextColor(C_TEXT);
        gyroCb.setChecked(p.getBoolean("gyro", false));
        box.addView(gyroCb);
        final float[] selGyro = {p.getFloat("gyrosens", 2.0f)};
        final TextView gyroLabel = text("", 13, C_MUTED, false);
        gyroLabel.setText(t("gyrosens") + ": " + String.format(Locale.US, "%.1f", selGyro[0]));
        box.addView(gyroLabel);
        SeekBar gsb = new SeekBar(this);
        gsb.setMax(45); // 0.5 .. 5.0
        gsb.setProgress(Math.round((selGyro[0] - 0.5f) * 10));
        gsb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean u) {
                selGyro[0] = 0.5f + v / 10f;
                gyroLabel.setText(t("gyrosens") + ": " + String.format(Locale.US, "%.1f", selGyro[0]));
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        box.addView(gsb);

        // boshqaruv
        final String[] LAYS = {"pro", "std"};
        final String[] selLay = {p.getString("touchlay", "pro")};
        TextView ct = text(t("ctrl"), 14, C_GOLD, true);
        ct.setPadding(0, dp(10), 0, 0);
        box.addView(ct);
        LinearLayout layRow = chipRow();
        final Button[] layBtns = new Button[LAYS.length];
        for (int i = 0; i < LAYS.length; i++) {
            final int k = i;
            layBtns[i] = button(t("ctrl_" + LAYS[i]), C_LINE);
            layBtns[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            layBtns[i].setBackground(round(LAYS[i].equals(selLay[0]) ? C_ACCENT : C_LINE, 8, 0));
            layBtns[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selLay[0] = LAYS[k];
                    for (int j = 0; j < LAYS.length; j++) layBtns[j].setBackground(round(j == k ? C_ACCENT : C_LINE, 8, 0));
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(0, 0, dp(5), 0);
            layRow.addView(layBtns[i], lp);
        }
        box.addView(layRow);

        // grafika
        final String[] GFX = {"high", "mid", "low"};
        final String[] selGfx = {p.getString("gfx", "high")};
        TextView gt = text(t("gfx"), 14, C_GOLD, true);
        gt.setPadding(0, dp(10), 0, 0);
        box.addView(gt);
        LinearLayout gfxRow = chipRow();
        final Button[] gfxBtns = new Button[GFX.length];
        for (int i = 0; i < GFX.length; i++) {
            final int k = i;
            gfxBtns[i] = button(t("gfx_" + GFX[i]), C_LINE);
            gfxBtns[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            gfxBtns[i].setBackground(round(GFX[i].equals(selGfx[0]) ? C_ACCENT : C_LINE, 8, 0));
            gfxBtns[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selGfx[0] = GFX[k];
                    for (int j = 0; j < GFX.length; j++) gfxBtns[j].setBackground(round(j == k ? C_ACCENT : C_LINE, 8, 0));
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(0, 0, dp(5), 0);
            gfxRow.addView(gfxBtns[i], lp);
        }
        box.addView(gfxRow);

        final CheckBox fpsCb = new CheckBox(this);
        fpsCb.setText(t("fps"));
        fpsCb.setTextColor(C_TEXT);
        fpsCb.setChecked(p.getBoolean("fps", false));
        box.addView(fpsCb);

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                .setTitle("⚙ " + t("settings"))
                .setView(sv)
                .setPositiveButton(t("save"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        boolean langChanged = !selLang[0].equals(lang);
                        p.edit().putString("nick", nickEd.getText().toString().trim())
                                .putString("gfx", selGfx[0]).putBoolean("fps", fpsCb.isChecked())
                                .putString("lang", selLang[0]).putFloat("sens", selSens[0])
                                .putString("xsize", selSize[0]).putInt("xcolor", selColor[0])
                                .putInt("xthick", selThick[0]).putBoolean("xdot", dotCb.isChecked())
                                .putString("touchlay", selLay[0])
                                .putBoolean("gyro", gyroCb.isChecked()).putFloat("gyrosens", selGyro[0]).apply();
                        if (!(selLay[0] + TOUCH_V).equals(p.getString("touch_applied", "")))
                            p.edit().putString("touch_applied", "").apply();
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

    // ---------------- X CLIENT akkaunti (kompyuter bilan bitta akkaunt) ----------------
    private static final String XC_API = "https://mmb.boost-top.com/xc_api.php";
    private volatile JSONObject accMe = null;
    private volatile JSONArray accFriends = null;
    private volatile boolean accLoading = false;
    private volatile boolean presence = false;

    private String accToken() {
        return prefs().getString("xc_token", "");
    }

    /** POST so'rov (token faqat POST da - manzillar loglarga tushmasin). Javob JSON. */
    // ---------------- yiqilish va kirish hisobotlari (admin panel > Muammolar) ----------------
    /** O'yin yiqilgan / qotgan (Android 11+ tizim yozuvi) yoki serverga kira olmagan bo'lsa - saytga yuboramiz.
     *  Serverga kirish: engine.log da oxirgi "Connecting to" dan keyin "BT: in game" bo'lmasa - kirolmagan. */
    private volatile boolean issuesBusy = false;
    private void reportGameIssues() {
        if (issuesBusy) return;
        issuesBusy = true;
        final String dev = deviceHash();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences p = prefs();
                    String tail = logTail();
                    if (Build.VERSION.SDK_INT >= 30) {
                        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                        java.util.List<android.app.ApplicationExitInfo> l = am.getHistoricalProcessExitReasons(null, 0, 5);
                        long last = p.getLong("exit_t", 0), max = last;
                        if (last == 0) max = System.currentTimeMillis();       // birinchi marta: eskilarini yubormaymiz
                        else for (android.app.ApplicationExitInfo e : l) {
                            if (e.getTimestamp() <= last) continue;
                            if (e.getTimestamp() > max) max = e.getTimestamp();
                            int r = e.getReason();
                            String k = r == android.app.ApplicationExitInfo.REASON_CRASH || r == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE ? "mob_crash"
                                    : r == android.app.ApplicationExitInfo.REASON_ANR ? "mob_anr"
                                    : r == android.app.ApplicationExitInfo.REASON_LOW_MEMORY ? "mob_lowmem" : null;
                            if (k == null) continue;
                            String x = "r=" + r + (r == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE ? "(native)" : "") + " desc=" + e.getDescription()
                                    + " pss=" + (e.getPss() / 1024) + "MB " + phoneInfo() + " log=" + tail;
                            sendProb(k, dev, x);
                        }
                        p.edit().putLong("exit_t", max).apply();
                    }
                    File log = new File(baseDir(), "engine.log");
                    if (log.exists() && p.getInt("issues_v", 0) < 1) {        // eski dvijok yozgan logda "BT: in game" belgisi yo'q - o'tkazib yuboramiz
                        p.edit().putInt("issues_v", 1).putLong("log_t", log.lastModified()).apply();
                    }
                    if (log.exists() && log.lastModified() > p.getLong("log_t", 0)) {
                        p.edit().putLong("log_t", log.lastModified()).apply();
                        String txt = new String(readAll(log), "UTF-8");
                        int i = txt.lastIndexOf("Connecting to ");
                        if (i >= 0 && txt.indexOf("BT: in game", i) < 0) {
                            String after = txt.substring(i);
                            String ip = after.substring(14, Math.min(after.length(), 60)).split("[.]{3}|\\s")[0];
                            String lastLine = "";
                            for (String ln : after.split("\n")) if (ln.trim().length() > 0) lastLine = ln.trim();
                            // demo/lokal o'yin (botlar) emas
                            if (!ip.startsWith("127.") && !ip.startsWith("localhost") && !ip.startsWith("loopback"))
                                sendProb("mob_join", dev, "ip=" + ip + " files=" + count(after, "Downloading") + " fail=" + count(after, "failed")
                                        + " last=" + (lastLine.length() > 150 ? lastLine.substring(0, 150) : lastLine) + " " + phoneInfo());
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    issuesBusy = false;
                }
            }
        }).start();
    }

    private static int count(String s, String w) {
        int n = 0, i = 0;
        while ((i = s.indexOf(w, i)) >= 0) { n++; i += w.length(); }
        return n;
    }

    private String phoneInfo() {
        String ram = "";
        try {
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            ((android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
            ram = (mi.totalMem >> 20) + "/" + (mi.availMem >> 20) + "MB";
        } catch (Exception ignored) {
        }
        return "phone=" + Build.MANUFACTURER + " " + Build.MODEL + " android=" + Build.VERSION.SDK_INT + " ram=" + ram + " build=" + myBuild();
    }

    /** engine.log ning oxirgi 3 ta mazmunli qatori (yiqilish oldidan nima bo'lgan). */
    private String logTail() {
        try {
            File log = new File(baseDir(), "engine.log");
            if (!log.exists()) return "-";
            java.io.RandomAccessFile f = new java.io.RandomAccessFile(log, "r");
            long n = f.length(), from = Math.max(0, n - 4096);
            byte[] b = new byte[(int) (n - from)];
            f.seek(from);
            f.readFully(b);
            f.close();
            String[] ls = new String(b, "UTF-8").split("\n");
            StringBuilder sb = new StringBuilder();
            int got = 0;
            for (int i = ls.length - 1; i >= 0 && got < 3; i--) {
                String t = ls[i].trim();
                if (t.length() == 0) continue;
                sb.insert(0, (t.length() > 120 ? t.substring(0, 120) : t) + (got > 0 ? " | " : ""));
                got++;
            }
            return sb.toString();
        } catch (Exception e) {
            return "-";
        }
    }

    private static void sendProb(String k, String dev, String x) {
        try {
            xcPost("a=prob&k=" + enc(k) + "&d=" + enc(dev) + "&x=" + enc(x.length() > 1000 ? x.substring(0, 1000) : x));
        } catch (Exception ignored) {
        }
    }

    private static JSONObject xcPost(String params) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(XC_API).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        c.setUseCaches(false);
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        try {
            OutputStream o = c.getOutputStream();
            o.write(params.getBytes("UTF-8"));
            o.close();
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            return new JSONObject(bo.toString("UTF-8"));
        } finally {
            c.disconnect();
        }
    }

    private static String enc(String v) {
        try {
            return java.net.URLEncoder.encode(v == null ? "" : v, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    /** Profil va do'stlar - fonda, keyin ekran yangilanadi. Token eskirgan bo'lsa - chiqib ketadi. */
    private void accLoad() {
        final String tok = accToken();
        if (tok.length() == 0 || accLoading) return;
        accLoading = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject me = xcPost("a=me&token=" + enc(tok));
                    if (!me.optBoolean("ok") && "auth".equals(me.optString("err"))) {
                        prefs().edit().remove("xc_token").apply();
                        accMe = null;
                    } else if (me.optBoolean("ok")) {
                        accMe = me.optJSONObject("me");
                        JSONObject fr = xcPost("a=friends&token=" + enc(tok));
                        if (fr.optBoolean("ok")) accFriends = fr.optJSONArray("friends");
                    }
                } catch (Exception ignored) {
                }
                accLoading = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        render();
                    }
                });
            }
        }).start();
    }

    private static final String XC_SITE = "https://mmb.boost-top.com/";
    private static final int PICK_AV = 7301;
    private volatile Bitmap accAv = null;
    private volatile String accAvKey = "";
    private volatile boolean tgPolling = false;
    private volatile boolean netDown = false;

    /** Nik rangi (menyudagi nomlar) -> ekrandagi rang. */
    private static int ncColor(String nc) {
        if (nc == null) return C_TEXT;
        switch (nc) {
            case "red": return 0xFFFF4D4D;
            case "orange": return 0xFFFF9A3C;
            case "yellow": return 0xFFFFD93C;
            case "green": return 0xFF4CD964;
            case "cyan": return 0xFF3CD6FF;
            case "blue": return 0xFF4C8DFF;
            case "purple": return 0xFFA66BFF;
            case "pink": return 0xFFFF6BC8;
            case "gold": return 0xFFFFC83C;
            case "neon": return 0xFF39FF14;
            case "ice": return 0xFFA8E6FF;
            case "toxic": return 0xFF9DFF3C;
            case "rainbow": return 0xFFFF9A3C;
            default: return C_TEXT;
        }
    }

    /** Profil rasmi (saytdan, kesh bilan). */
    private void accLoadAv(final String rel) {
        if (rel == null || rel.length() == 0) { accAv = null; accAvKey = ""; return; }
        if (rel.equals(accAvKey)) return;
        accAvKey = rel;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(XC_SITE + rel).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(10000);
                    Bitmap b = BitmapFactory.decodeStream(c.getInputStream());
                    c.disconnect();
                    if (b != null) {
                        accAv = b;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                render();
                            }
                        });
                    }
                } catch (Exception ignored) {
                    accAvKey = "";
                }
            }
        }).start();
    }

    /** Profil: rasm, ism, nik rangi - kompyuterdagi menyu bilan umumiy. */
    private void showProfile() {
        final String[] items = {t("acc_pic"), t("acc_picdel"), t("acc_name"), t("acc_nc"), t("acc_tglink")};
        new AlertDialog.Builder(this).setTitle(t("acc_prof")).setItems(items, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int w) {
                if (w == 0) {
                    try {
                        startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), t("acc_pic")), PICK_AV);
                    } catch (Exception e) {
                        toastLike(String.valueOf(e.getMessage()));
                    }
                } else if (w == 1) accEdit("a=avatar&img=none");
                else if (w == 2) showNameEdit();
                else if (w == 3) showNickColor();
                else accTgLink();
            }
        }).setNegativeButton(t("cancel"), null).show();
    }

    private void showNameEdit() {
        final EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(accMe != null ? accMe.optString("name") : "");
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this).setTitle(t("acc_name")).setView(box)
                .setPositiveButton(t("save"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        accEdit("a=profile&name=" + enc(e.getText().toString().trim()));
                    }
                }).setNegativeButton(t("cancel"), null).show();
    }

    private void showNickColor() {
        final String[] keys = {"", "red", "orange", "yellow", "green", "cyan", "blue", "purple", "pink"};
        String[] names = new String[keys.length];
        for (int i = 0; i < keys.length; i++) names[i] = i == 0 ? t("acc_ncnone") : t("nc_" + keys[i]);
        new AlertDialog.Builder(this).setTitle(t("acc_nc")).setItems(names, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int w) {
                accEdit("a=equip&nc=" + enc(keys[w]));
            }
        }).setNegativeButton(t("cancel"), null).show();
    }

    /** Profilni o'zgartirish so'rovi: javobdagi xabar (masalan "10-darajadan ochiladi") ko'rsatiladi. */
    private void accEdit(final String params) {
        final String tok = accToken();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    JSONObject r = xcPost(params + "&token=" + enc(tok));
                    if (r.optBoolean("ok")) {
                        if (r.optJSONObject("me") != null) accMe = r.optJSONObject("me");
                        msg = t("acc_saved");
                    } else msg = r.optString("msg", t("acc_err"));
                } catch (Exception e) {
                    msg = t("acc_err");
                }
                final String m = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toastLike(m);
                        accAvKey = "";
                        render();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK_AV || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            InputStream in = getContentResolver().openInputStream(data.getData());
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 4;   // telefon rasmlari katta - xotirani tejaymiz
            Bitmap src = BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (src == null) return;
            int sz = Math.min(src.getWidth(), src.getHeight());
            Bitmap sq = Bitmap.createBitmap(src, (src.getWidth() - sz) / 2, (src.getHeight() - sz) / 2, sz, sz);
            Bitmap sm = Bitmap.createScaledBitmap(sq, 128, 128, true);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            sm.compress(Bitmap.CompressFormat.JPEG, 82, bo);
            accEdit("a=avatar&img=" + enc("data:image/jpeg;base64," + android.util.Base64.encodeToString(bo.toByteArray(), android.util.Base64.NO_WRAP)));
        } catch (Exception e) {
            toastLike(String.valueOf(e.getMessage()));
        }
    }

    /** Telegram orqali kirish: kod olinadi, Telegram ochiladi, "Start" bosilgach ilova o'zi kiradi (10 daqiqa kutadi). */
    private void accTelegram() {
        if (tgPolling) return;
        tgPolling = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                try {
                    JSONObject r = xcPost("a=tgl_code");
                    if (!r.optBoolean("ok")) err = r.optString("msg", t("acc_err"));
                    else {
                        final String link = r.optString("link");
                        final String code = r.optString("code"), sec = r.optString("sec");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toastLike(t("acc_tgwait"));
                                openUrl(link);
                            }
                        });
                        long end = System.currentTimeMillis() + 10 * 60 * 1000;
                        err = t("acc_tgexp");
                        while (System.currentTimeMillis() < end) {
                            Thread.sleep(3000);
                            JSONObject st;
                            try {
                                st = xcPost("a=tgl_st&code=" + enc(code) + "&sec=" + enc(sec));
                            } catch (Exception e) {
                                continue;
                            }
                            if (st.optInt("wait") == 1) continue;
                            if (st.optBoolean("ok") && st.optString("token").length() == 40) {
                                prefs().edit().putString("xc_token", st.optString("token")).apply();
                                accMe = st.optJSONObject("me");
                                err = null;
                            } else err = st.optString("msg", t("acc_err"));
                            break;
                        }
                    }
                } catch (Exception e) {
                    err = t("acc_err");
                }
                tgPolling = false;
                final String m = err;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toastLike(m != null ? m : t("acc_tgok"));
                        if (m == null) accLoad();
                        render();
                    }
                });
            }
        }).start();
    }

    /** Telegram'ni akkauntga ulash (keyin ilovaga "Telegram orqali" parolsiz kiriladi, eslatmalar ham keladi). */
    private void accTgLink() {
        final String tok = accToken();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg, link = null;
                try {
                    JSONObject r = xcPost("a=tg_code&token=" + enc(tok));
                    if (!r.optBoolean("ok")) msg = r.optString("msg", t("acc_err"));
                    else if (r.optBoolean("linked")) msg = t("acc_tglinked");
                    else if (r.optString("link").startsWith("https://t.me/")) { link = r.optString("link"); msg = t("acc_tglinkgo"); }
                    else msg = t("acc_err");
                } catch (Exception e) {
                    msg = t("acc_err");
                }
                final String m = msg, l = link;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toastLike(m);
                        if (l != null) openUrl(l);
                    }
                });
            }
        }).start();
    }

    /** Bosh ekranda doim: botlar bilan mashq (internetsiz), qiyinlik tanlanadi. */
    private View botsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackground(round(C_CARD, 12, C_LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(2), 0, dp(8));
        card.setLayoutParams(lp);
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(text(t("bot_t"), 15, C_TEXT, true));
        tx.addView(text(t("bot_s"), 12, C_MUTED, false));
        card.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button b = button(t("bot_b"), 0xFFB07A10);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBotLevel();
            }
        });
        card.addView(b);
        return card;
    }

    private void showBotLevel() {
        String[] lv = {t("bot_l0"), t("bot_l1"), t("bot_l2")};
        new AlertDialog.Builder(this).setTitle(t("bot_lv")).setItems(lv, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int w) {
                prefs().edit().putInt("botlv", w).apply();
                launchBots();
            }
        }).setNegativeButton(t("cancel"), null).show();
    }

    /** Internet yoki sayt ishlamasa: botlar bilan o'yin (o'yin fayllari telefonda, internet kerak emas). */
    private View offlineCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(C_CARD, 12, C_GOLD));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(8));
        card.setLayoutParams(lp);
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(text(t("off_t"), 15, C_TEXT, true));
        tx.addView(text(t("off_s"), 12, C_MUTED, false));
        card.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button b = button(t("off_b"), 0xFFB07A10);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchBots();
            }
        });
        card.addView(b);
        return card;
    }

    private void launchBots() {
        if (!hasData()) {
            toastLike(t("needsetup"));
            return;
        }
        writeSettings();
        String argv = "-log -dll @yapb +exec boosttop_settings.cfg" + touchPresetArg() + " +sv_lan 1 +maxplayers 10 +yb_quota 5 +yb_difficulty " + new int[]{0, 2, 3}[Math.max(0, Math.min(2, prefs().getInt("botlv", 1)))] + " +map de_dust2";
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

    private View accountCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(0xFF2A1218, 12, 0xFFE0353F));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(8));
        card.setLayoutParams(lp);
        final JSONObject me = accMe;
        if (accToken().length() == 0 || me == null) {
            if (accToken().length() > 0 && me == null) accLoad();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.addView(text(t("acc_t"), 15, C_TEXT, true));
            tx.addView(text(t("acc_s"), 12, C_MUTED, false));
            row.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button b = button(t("acc_in"), 0xFFE0353F);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAccLogin();
                }
            });
            row.addView(b);
            card.addView(row);
            return card;
        }
        // kirgan: ism, daraja, XP, kunlik sovg'a, onlayn do'stlar
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        accLoadAv(me.optString("av", ""));
        if (accAv != null && accAvKey.equals(me.optString("av", ""))) {
            ImageView av = new ImageView(this);
            av.setImageBitmap(accAv);
            av.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(44), dp(44));
            alp.setMargins(0, 0, dp(10), 0);
            row.addView(av, alp);
        }
        TextView nm = text(me.optString("name", me.optString("login")), 16, ncColor(me.optString("nc", "")), true);
        tx.addView(nm);
        tx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProfile();
            }
        });
        tx.addView(text(t("acc_lvl") + " " + me.optInt("level") + "  ·  " + me.optInt("lvlxp") + " / " + me.optInt("lvlmax", 1000) + " XP", 12, C_GOLD, true));
        row.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button gift = button(t("acc_daily"), 0xFFE0353F);
        gift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accDaily();
            }
        });
        row.addView(gift);
        card.addView(row);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, me.optInt("lvlmax", 1000)));
        bar.setProgress(me.optInt("lvlxp"));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        blp.setMargins(0, dp(8), 0, dp(8));
        card.addView(bar, blp);
        card.addView(text(t("acc_fr"), 13, C_TEXT, true));
        int on = 0;
        JSONArray fr = accFriends;
        if (fr != null) {
            for (int i = 0; i < fr.length() && on < 6; i++) {
                JSONObject f = fr.optJSONObject(i);
                if (f == null || !f.optBoolean("online")) continue;
                on++;
                String where = f.optString("server", "");
                String st = f.optInt("mob") == 1 ? t("acc_phone") + (where.length() > 0 ? " · " + where : "") : (where.length() > 0 ? where : t("acc_menu"));
                TextView ft = text("●  " + f.optString("name") + "  —  " + st, 13, C_MUTED, false);
                ft.setPadding(0, dp(3), 0, dp(3));
                final String ip = where;
                if (ip.length() > 0) {
                    ft.setTextColor(C_TEXT);
                    ft.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            launchGame(ip);
                        }
                    });
                }
                card.addView(ft);
            }
        }
        if (on == 0) card.addView(text(t("acc_nofr"), 12, C_MUTED, false));
        LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        foot.setPadding(0, dp(8), 0, 0);
        foot.addView(text(t("acc_xp"), 11, C_MUTED, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView out = text(t("acc_out"), 12, C_MUTED, true);
        out.setPadding(dp(10), dp(4), 0, dp(4));
        out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String tok = accToken();
                prefs().edit().remove("xc_token").apply();
                accMe = null;
                accFriends = null;
                render();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            xcPost("a=logout&token=" + enc(tok));
                        } catch (Exception ignored) {
                        }
                    }
                }).start();
            }
        });
        foot.addView(out);
        card.addView(foot);
        return card;
    }

    /** Kirish: kompyuterdagi 6 xonali kod (parolsiz) yoki login/parol. */
    private void showAccLogin() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        TextView h = text(t("acc_codeh"), 13, 0xFF333333, false);
        box.addView(h);
        final EditText code = new EditText(this);
        code.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        code.setHint("123456");
        code.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        code.setGravity(Gravity.CENTER);
        box.addView(code);
        final AlertDialog[] dlg = new AlertDialog[1];
        Button tg = button(t("acc_tg"), 0xFF2AA3DE);
        tg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dlg[0] != null) dlg[0].dismiss();
                accTelegram();
            }
        });
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        tlp.setMargins(0, dp(10), 0, 0);
        box.addView(tg, tlp);
        dlg[0] = new AlertDialog.Builder(this)
                .setTitle(t("acc_code"))
                .setView(box)
                .setPositiveButton(t("acc_in"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        accDo("a=mclaim&code=" + enc(code.getText().toString().trim()));
                    }
                })
                .setNeutralButton(t("acc_pass"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        showAccPass();
                    }
                })
                .setNegativeButton(t("cancel"), null)
                .create();
        dlg[0].show();
    }

    private void showAccPass() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        final EditText login = new EditText(this);
        login.setHint(t("acc_login"));
        login.setSingleLine(true);
        box.addView(login);
        final EditText pw = new EditText(this);
        pw.setHint(t("acc_pw"));
        pw.setSingleLine(true);
        pw.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(pw);
        new AlertDialog.Builder(this)
                .setTitle(t("acc_pass"))
                .setView(box)
                .setPositiveButton(t("acc_in"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        accDo("a=login&mob=1&login=" + enc(login.getText().toString().trim()) + "&pass=" + enc(pw.getText().toString()));
                    }
                })
                .setNegativeButton(t("cancel"), null)
                .show();
    }

    /** Kirish so'rovi: muvaffaqiyatli bo'lsa token saqlanadi va profil yuklanadi. */
    private void accDo(final String params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    JSONObject r = xcPost(params);
                    if (r.optBoolean("ok") && r.optString("token").length() == 40) {
                        prefs().edit().putString("xc_token", r.optString("token")).apply();
                        accMe = r.optJSONObject("me");
                        msg = null;
                    } else msg = r.optString("msg", t("acc_err"));
                } catch (Exception e) {
                    msg = t("acc_err");
                }
                final String m = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (m != null) toastLike(m);
                        else accLoad();
                        render();
                    }
                });
            }
        }).start();
    }

    private void accDaily() {
        final String tok = accToken();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    JSONObject r = xcPost("a=daily&token=" + enc(tok));
                    if (!r.optBoolean("ok")) msg = r.optString("msg", t("acc_err"));
                    else if (r.optInt("got") > 0) msg = String.format(Locale.US, t("acc_got"), r.optInt("got"), r.optInt("streak"));
                    else msg = t("acc_today");
                } catch (Exception e) {
                    msg = t("acc_err");
                }
                final String m = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toastLike(m);
                        accLoad();
                    }
                });
            }
        }).start();
    }

    /** O'yin paytida har daqiqada "onlayn" belgisi (kompyuterdagi menyu kabi): do'stlar "telefonda" ko'radi, o'yin vaqti uchun XP yoziladi.
     *  Ilova bosh ekrani qayta ochilganda (onResume) to'xtaydi, eng ko'pi 4 soat. */
    private void startPresence(final String ip) {
        final String tok = accToken();
        if (tok.length() == 0) return;
        presence = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                long end = System.currentTimeMillis() + 4L * 3600 * 1000;
                while (presence && System.currentTimeMillis() < end) {
                    try {
                        xcPost("a=ping&mob=1&token=" + enc(tok) + "&server=" + enc(ip == null ? "" : ip) + "&map=");
                    } catch (Exception ignored) {
                    }
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
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

    /** O'yindagi nik (cstrike/config.cfg dagi name "..."). */
    private String playerName() {
        try {
            File cfg = new File(baseDir(), "cstrike/config.cfg");
            if (!cfg.exists()) return "";
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(cfg), "UTF-8"));
            String line, name = "";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("name ")) {
                    name = line.substring(5).trim();
                    if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) name = name.substring(1, name.length() - 1);
                }
            }
            br.close();
            return name.length() > 32 ? name.substring(0, 32) : name;
        } catch (Exception e) {
            return "";
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
                    String nick = playerName();
                    if (nick.length() > 0) url += "&n=" + java.net.URLEncoder.encode(nick, "UTF-8");
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
                    apkBuild = j.optInt("apk_build", 0);
                    apkUrl = j.optString("apk_url", "");
                    Object nw = j.opt("news");
                    if (nw instanceof JSONArray) news = (JSONArray) nw;
                    else if (nw instanceof JSONObject) news = new JSONArray().put(nw);
                    else news = null;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            render();
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    // ---------------- sevimli serverlar (telefonda saqlanadi) ----------------

    // ================= Serverlar oynasi (kompyuterdagi X CLIENT "Серверы" oynasi kabi) =================
    private static final int SV_BG = 0xFF141015, SV_PANEL = 0xFF1B1519, SV_ROW_SEL = 0xFF4A1A20, SV_RED = 0xFFE5333B, SV_HEAD = 0xFF7D7178;
    private String svTab = "net", svQuery = "", svMap = "", svSel = null;
    private boolean svPing = false, svNotEmpty = false, svNotFull = false;
    private LinearLayout svList;
    private TextView svCount;
    private android.app.Dialog svDlg;
    private final Map<String, TextView> svPingViews = new HashMap<String, TextView>();

    private static String cleanName(String v) {
        if (v == null) return "";
        return v.replaceAll("[\\x00-\\x1f\\x7f-\\x9f\\ufffd\\u2580-\\u259f\\u25a0-\\u25ff]", " ").replaceAll("\\s{2,}", " ").trim();
    }

    private void addHist(String ip) {
        try {
            JSONArray h = new JSONArray(prefs().getString("hist", "[]"));
            JSONArray n = new JSONArray();
            n.put(ip);
            for (int i = 0; i < h.length() && n.length() < 30; i++) if (!ip.equals(h.optString(i))) n.put(h.optString(i));
            prefs().edit().putString("hist", n.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private TextView svChip(String label, boolean on) {
        TextView c = text(label, 12, on ? Color.WHITE : C_TEXT, false);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(12), dp(8), dp(12), dp(8));
        c.setBackground(round(on ? SV_ROW_SEL : SV_PANEL, 3, on ? SV_RED : 0xFF3A3036));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(6), 0);
        c.setLayoutParams(lp);
        return c;
    }

    private void showServers() {
        svDlg = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SV_BG);
        svDlg.setContentView(root);
        svDlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                svDlg = null;
                svList = null;
                svPingViews.clear();
            }
        });
        buildServers(root);
        svDlg.show();
        // ping natijalari kelgan sari yangilanadi
        root.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (svDlg == null) return;
                for (Map.Entry<String, TextView> e : svPingViews.entrySet()) svPingText(e.getKey(), e.getValue());
                if (svList != null) svList.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void buildServers(final LinearLayout root) {
        root.removeAllViews();
        // sarlavha
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(14), dp(12), dp(8), dp(8));
        ImageView logo = new ImageView(this);
        logo.setImageResource(getResources().getIdentifier("ic_launcher", "mipmap", getPackageName()));
        head.addView(logo, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView ttl = text(t("sv_title"), 20, Color.WHITE, false);
        ttl.setPadding(dp(12), 0, 0, 0);
        head.addView(ttl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = text("✕", 22, C_MUTED, false);
        close.setPadding(dp(14), dp(4), dp(14), dp(4));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svDlg != null) svDlg.dismiss();
            }
        });
        head.addView(close);
        root.addView(head);

        // yorliqlar
        android.widget.HorizontalScrollView tsv = new android.widget.HorizontalScrollView(this);
        tsv.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(8), 0, dp(8), 0);
        final String[] TABS = {"net", "fav", "hist", "top"};
        for (final String tk : TABS) {
            String lb = t("sv_" + tk);
            if (tk.equals("fav")) lb += " (" + favs().length() + ")";
            LinearLayout tb = new LinearLayout(this);
            tb.setOrientation(LinearLayout.VERTICAL);
            TextView tt = text(lb, 14, tk.equals(svTab) ? Color.WHITE : C_MUTED, false);
            tt.setPadding(dp(14), dp(10), dp(14), dp(10));
            tb.addView(tt);
            View ul = new View(this);
            ul.setBackgroundColor(tk.equals(svTab) ? SV_RED : 0);
            tb.addView(ul, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
            if (tk.equals(svTab)) tb.setBackgroundColor(0xFF221A1F);
            tb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    svTab = tk;
                    buildServers(root);
                }
            });
            tabs.addView(tb);
        }
        tsv.addView(tabs);
        root.addView(tsv);
        View sep = new View(this);
        sep.setBackgroundColor(0xFF2E2529);
        root.addView(sep, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // qidiruv va filtrlar
        LinearLayout fbox = new LinearLayout(this);
        fbox.setOrientation(LinearLayout.VERTICAL);
        fbox.setPadding(dp(12), dp(10), dp(12), dp(6));
        final EditText q = new EditText(this);
        q.setSingleLine(true);
        q.setText(svQuery);
        q.setHint(t("sv_search"));
        q.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        q.setTextColor(C_TEXT);
        q.setHintTextColor(0xFF7A6F76);
        q.setPadding(dp(10), dp(8), dp(10), dp(8));
        q.setBackground(round(SV_PANEL, 3, 0xFF3A3036));
        q.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
            public void onTextChanged(CharSequence a, int b, int c, int d) {}
            public void afterTextChanged(android.text.Editable e) {
                svQuery = e.toString().trim();
                fillServers();
            }
        });
        fbox.addView(q);
        android.widget.HorizontalScrollView fsv = new android.widget.HorizontalScrollView(this);
        fsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(0, dp(8), 0, 0);
        TextView cMap = svChip(t("sv_map") + ": " + (svMap.length() > 0 ? svMap : t("sv_all")) + "  ▾", svMap.length() > 0);
        cMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final java.util.List<String> maps = new java.util.ArrayList<String>();
                maps.add(t("sv_all"));
                for (JSONObject sv : svAll()) {
                    String m = sv.optString("map", "");
                    if (m.length() > 0 && !maps.contains(m)) maps.add(m);
                }
                new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog)
                        .setItems(maps.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int w) {
                                svMap = w == 0 ? "" : maps.get(w);
                                buildServers(root);
                            }
                        }).show();
            }
        });
        chips.addView(cMap);
        final String[][] FL = {{"ping", t("sv_ping")}, {"ne", t("sv_nempty")}, {"nf", t("sv_nfull")}};
        for (final String[] f : FL) {
            boolean on = f[0].equals("ping") ? svPing : f[0].equals("ne") ? svNotEmpty : svNotFull;
            TextView c = svChip(f[1], on);
            c.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (f[0].equals("ping")) svPing = !svPing;
                    else if (f[0].equals("ne")) svNotEmpty = !svNotEmpty;
                    else svNotFull = !svNotFull;
                    buildServers(root);
                }
            });
            chips.addView(c);
        }
        TextView quick = text("⚡ " + t("sv_quick"), 12, Color.WHITE, true);
        quick.setPadding(dp(14), dp(8), dp(14), dp(8));
        quick.setBackground(round(SV_RED, 3, 0));
        quick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject b = null;
                int bs = -99999;
                for (JSONObject sv : svAll()) {
                    if (!sv.optBoolean("online", false)) continue;
                    int pl = sv.optInt("players", 0), mx = sv.optInt("max", 32);
                    if (pl >= mx) continue;
                    Integer pg = pings.get(sv.optString("ip"));
                    int sc = Math.min(pl, 28) * 10 - (pg == null || pg < 0 ? 300 : pg) / 4;
                    if (sc > bs) { bs = sc; b = sv; }
                }
                if (b == null) toastLike(t("sv_none"));
                else { if (svDlg != null) svDlg.dismiss(); launchGame(b.optString("ip")); }
            }
        });
        chips.addView(quick);
        fsv.addView(chips);
        fbox.addView(fsv);
        root.addView(fbox);

        // ustun sarlavhalari
        LinearLayout ch = new LinearLayout(this);
        ch.setPadding(dp(12), dp(6), dp(12), dp(6));
        TextView h1 = text("★   " + t("sv_col_srv"), 10, SV_HEAD, true);
        ch.addView(h1, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView h2 = text(t("sv_col_pl"), 10, SV_HEAD, true);
        ch.addView(h2, new LinearLayout.LayoutParams(dp(78), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView h4 = text(t("sv_col_ping"), 10, SV_HEAD, true);
        h4.setGravity(Gravity.END);
        ch.addView(h4, new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(ch);

        ScrollView sc = new ScrollView(this);
        svList = new LinearLayout(this);
        svList.setOrientation(LinearLayout.VERTICAL);
        sc.addView(svList);
        root.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // pastki panel
        LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.VERTICAL);
        foot.setPadding(dp(12), dp(8), dp(12), dp(10));
        foot.setBackgroundColor(0xFF110D11);
        svCount = text("", 11, C_MUTED, false);
        foot.addView(svCount);
        LinearLayout fb = new LinearLayout(this);
        fb.setPadding(0, dp(8), 0, 0);
        String[][] BT = {{"add", "+ IP"}, {"ref", t("sv_refresh")}, {"join", t("sv_join")}};
        for (final String[] b : BT) {
            TextView bv = text(b[1], 13, Color.WHITE, b[0].equals("join"));
            bv.setGravity(Gravity.CENTER);
            bv.setPadding(dp(8), dp(11), dp(8), dp(11));
            bv.setBackground(round(b[0].equals("join") ? SV_RED : SV_PANEL, 3, b[0].equals("join") ? 0 : 0xFF3A3036));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, b[0].equals("join") ? 1.4f : 1f);
            lp.setMargins(0, 0, b[0].equals("join") ? 0 : dp(6), 0);
            bv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (b[0].equals("add")) showAddServer();
                    else if (b[0].equals("ref")) { loadServers(); if (svList != null) svList.postDelayed(new Runnable() { public void run() { fillServers(); } }, 2500); }
                    else if (svSel == null) toastLike(t("sv_pick"));
                    else { if (svDlg != null) svDlg.dismiss(); launchGame(svSel); }
                }
            });
            fb.addView(bv, lp);
        }
        foot.addView(fb);
        root.addView(foot);
        fillServers();
    }

    /** Hamma ma'lum serverlar (bo'limlardan, takrorlanmasdan). */
    private java.util.List<JSONObject> svAll() {
        java.util.List<JSONObject> out = new java.util.ArrayList<JSONObject>();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        JSONArray sections = lastData == null ? null : lastData.optJSONArray("sections");
        if (sections != null) for (int i = 0; i < sections.length(); i++) {
            JSONArray l = sections.optJSONObject(i) == null ? null : sections.optJSONObject(i).optJSONArray("servers");
            if (l != null) for (int k = 0; k < l.length(); k++) {
                JSONObject sv = l.optJSONObject(k);
                if (sv != null && seen.add(sv.optString("ip"))) out.add(sv);
            }
        }
        return out;
    }

    private boolean svPass(JSONObject sv) {
        String ip = sv.optString("ip");
        if (svQuery.length() > 0) {
            String qq = svQuery.toLowerCase(Locale.ROOT);
            if (!(cleanName(sv.optString("name")).toLowerCase(Locale.ROOT).contains(qq) || ip.contains(qq) || sv.optString("map").toLowerCase(Locale.ROOT).contains(qq))) return false;
        }
        if (svMap.length() > 0 && !svMap.equals(sv.optString("map"))) return false;
        if (svNotEmpty && sv.optInt("players", 0) == 0) return false;
        if (svNotFull && sv.optInt("players", 0) >= sv.optInt("max", 32)) return false;
        if (svPing) {
            Integer pg = pings.get(ip);
            if (pg == null || pg < 0 || pg >= 50) return false;
        }
        return true;
    }

    private void fillServers() {
        if (svList == null) return;
        svList.removeAllViews();
        svPingViews.clear();
        String flagBase = lastData == null ? "http://mmb.boost-top.com/flag.php?c=" : lastData.optString("flag_url", "http://mmb.boost-top.com/flag.php?c=");
        int nSrv = 0, nPl = 0;
        java.util.List<Object[]> groups = new java.util.ArrayList<Object[]>();   // {sarlavha, nishon rangi, ro'yxat}
        if (svTab.equals("net") || svTab.equals("top")) {
            JSONArray sections = lastData == null ? null : lastData.optJSONArray("sections");
            if (sections != null) for (int i = 0; i < sections.length(); i++) {
                JSONObject s = sections.optJSONObject(i);
                if (s == null) continue;
                String key = s.optString("key", "");
                if (svTab.equals("top") && !key.equals("top")) continue;
                JSONArray l = s.optJSONArray("servers");
                java.util.List<JSONObject> li = new java.util.ArrayList<JSONObject>();
                if (l != null) for (int k = 0; k < l.length(); k++) if (l.optJSONObject(k) != null) li.add(l.optJSONObject(k));
                String title = s.optString("title", "");
                JSONObject titles = s.optJSONObject("titles");
                if (titles != null && titles.has(lang)) title = titles.optString(lang, title);
                groups.add(new Object[]{title, key.equals("vip") ? 0xFFD4A20F : key.equals("top") ? SV_RED : 0xFF6A5A62, li});
            }
        } else {
            java.util.List<JSONObject> li = new java.util.ArrayList<JSONObject>();
            if (svTab.equals("fav")) {
                JSONObject fv = favs();
                java.util.Iterator<String> it = fv.keys();
                while (it.hasNext()) { String fip = it.next(); li.add(findServer(fip, fv.optString(fip, fip))); }
            } else {
                try {
                    JSONArray h = new JSONArray(prefs().getString("hist", "[]"));
                    for (int i = 0; i < h.length(); i++) li.add(findServer(h.optString(i), h.optString(i)));
                } catch (Exception ignored) {
                }
            }
            groups.add(new Object[]{null, 0, li});
        }
        java.util.HashSet<String> counted = new java.util.HashSet<String>();
        for (Object[] g : groups) {
            @SuppressWarnings("unchecked") java.util.List<JSONObject> li = (java.util.List<JSONObject>) g[2];
            java.util.List<JSONObject> ok = new java.util.ArrayList<JSONObject>();
            for (JSONObject sv : li) if (svPass(sv)) ok.add(sv);
            if (ok.isEmpty()) continue;
            if (g[0] != null) {
                LinearLayout sh = new LinearLayout(this);
                sh.setGravity(Gravity.CENTER_VERTICAL);
                sh.setPadding(dp(12), dp(10), dp(12), dp(4));
                TextView badge = text(String.valueOf(g[0]).toUpperCase(Locale.ROOT), 10, Color.WHITE, true);
                badge.setPadding(dp(6), dp(1), dp(6), dp(1));
                badge.setBackground(round((Integer) g[1], 2, 0));
                sh.addView(badge);
                svList.addView(sh);
            }
            for (JSONObject sv : ok) {
                svList.addView(svRow(sv, flagBase));
                if (counted.add(sv.optString("ip"))) { nSrv++; nPl += sv.optInt("players", 0); }
            }
        }
        if (svList.getChildCount() == 0) {
            TextView e = text(t("sv_none"), 13, C_MUTED, false);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(40), 0, dp(40));
            svList.addView(e);
        }
        if (svCount != null) svCount.setText(t("sv_count").replace("{s}", String.valueOf(nSrv)).replace("{p}", String.valueOf(nPl)));
    }

    private void svPingText(String ip, TextView pv) {
        Integer ms = pings.get(ip);
        if (ms == null) { pv.setText("…"); pv.setTextColor(C_MUTED); return; }
        if (ms < 0) { pv.setText("—"); pv.setTextColor(C_MUTED); return; }
        pv.setText("● " + ms);
        pv.setTextColor(ms < 50 ? 0xFF6FD45A : ms < 100 ? 0xFFFFC94A : 0xFFFF5A4E);
    }

    private View svRow(final JSONObject sv, String flagBase) {
        final String ip = sv.optString("ip");
        final String nm = cleanName(sv.optString("name", ip));
        boolean online = sv.optBoolean("online", false);
        boolean sel = ip.equals(svSel);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(9), dp(12), dp(9));
        row.setBackgroundColor(sel ? SV_ROW_SEL : 0xFF1E1418);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 0, 0, 1);
        row.setLayoutParams(rlp);
        LinearLayout l1 = new LinearLayout(this);
        l1.setGravity(Gravity.CENTER_VERTICAL);
        final TextView star = text(isFav(ip) ? "★" : "★", 15, isFav(ip) ? C_GOLD : 0xFF5A4E55, false);
        star.setPadding(0, 0, dp(8), 0);
        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavQuiet(ip, nm);
                star.setTextColor(isFav(ip) ? C_GOLD : 0xFF5A4E55);
            }
        });
        l1.addView(star);
        ImageView flag = new ImageView(this);
        flag.setAdjustViewBounds(true);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(11));
        flp.setMargins(0, 0, dp(6), 0);
        l1.addView(flag, flp);
        loadFlag(sv.optString("cc", ""), flagBase, flag);
        TextView name = text(nm, 13, online ? Color.WHITE : C_MUTED, false);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        l1.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        // o'yinchilar: chiziq + son
        LinearLayout plb = new LinearLayout(this);
        plb.setGravity(Gravity.CENTER_VERTICAL);
        int pl = sv.optInt("players", 0), mx = Math.max(1, sv.optInt("max", 32));
        android.widget.FrameLayout bar = new android.widget.FrameLayout(this);
        bar.setBackground(round(0xFF3A2C31, 2, 0));
        View fill = new View(this);
        fill.setBackground(round(SV_RED, 2, 0));
        bar.addView(fill, new android.widget.FrameLayout.LayoutParams(Math.max(dp(2), dp(30) * Math.min(pl, mx) / mx), dp(3)));
        plb.addView(bar, new LinearLayout.LayoutParams(dp(30), dp(3)));
        TextView pls = text(online ? pl + "/" + sv.optInt("max", 32) : t("offline"), 12, C_TEXT, false);
        pls.setPadding(dp(5), 0, 0, 0);
        plb.addView(pls);
        l1.addView(plb, new LinearLayout.LayoutParams(dp(78), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView pv = text("…", 12, C_MUTED, false);
        pv.setGravity(Gravity.END);
        svPingViews.put(ip, pv);
        svPingText(ip, pv);
        l1.addView(pv, new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(l1);
        TextView map = text(sv.optString("map", "") + "   ·   " + ip, 11, 0xFF8A7E86, false);
        map.setPadding(dp(23), dp(2), 0, 0);
        map.setSingleLine(true);
        row.addView(map);
        if (sel) {
            View acc = new View(this);
            acc.setBackgroundColor(SV_RED);
        }
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                svSel = ip;
                fillServers();
                showServerInfo(sv);
            }
        });
        return row;
    }

    private void toggleFavQuiet(String ip, String name) {
        try {
            JSONObject f = favs();
            if (f.has(ip)) f.remove(ip); else f.put(ip, name);
            prefs().edit().putString("favs", f.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** Server tafsiloti: xarita rasmi, IP (nusxa), o'yinchilar ro'yxati, Kirish. */
    private void showServerInfo(final JSONObject sv) {
        final String ip = sv.optString("ip");
        final String nm = cleanName(sv.optString("name", ip));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(SV_PANEL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        android.widget.FrameLayout mp = new android.widget.FrameLayout(this);
        mp.setBackground(round(0xFF2A2026, 4, 0));
        final ImageView mimg = new ImageView(this);
        mimg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mp.addView(mimg, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView mtag = text(sv.optString("map", "?"), 11, Color.WHITE, false);
        mtag.setPadding(dp(7), dp(3), dp(7), dp(3));
        mtag.setBackground(round(0xCC000000, 3, 0));
        android.widget.FrameLayout.LayoutParams tlp = new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        tlp.setMargins(dp(8), 0, 0, dp(8));
        mp.addView(mtag, tlp);
        box.addView(mp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)));
        final String mapn = sv.optString("map", "");
        if (mapn.matches("[\\w\\-.]{2,40}")) new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL("https://mmb.boost-top.com/map.php?m=" + enc(mapn.toLowerCase(Locale.ROOT))).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    final Bitmap b = BitmapFactory.decodeStream(c.getInputStream());
                    c.disconnect();
                    if (b != null) runOnUiThread(new Runnable() { public void run() { mimg.setImageBitmap(b); } });
                } catch (Exception ignored) {
                }
            }
        }).start();
        TextView title = text(nm, 17, Color.WHITE, true);
        title.setPadding(0, dp(10), 0, dp(6));
        box.addView(title);
        LinearLayout ipl = new LinearLayout(this);
        TextView ipv = text(ip, 13, C_TEXT, false);
        ipv.setPadding(dp(10), dp(8), dp(10), dp(8));
        ipv.setBackground(round(0xFF120E11, 2, 0xFF3A3036));
        ipv.setTextIsSelectable(true);
        ipl.addView(ipv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView cp = text(t("sv_copy"), 12, 0xFFFF6A5A, true);
        cp.setPadding(dp(12), dp(8), dp(12), dp(8));
        cp.setBackground(round(0xFF3A1A1E, 2, 0xFF5A2A2E));
        cp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ((android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("ip", ip));
                    toastLike(t("sv_copied"));
                } catch (Exception ignored) {
                }
            }
        });
        ipl.addView(cp);
        box.addView(ipl);
        LinearLayout ln = new LinearLayout(this);
        ln.setPadding(0, dp(12), 0, dp(6));
        ln.addView(text(t("sv_players") + "  " + sv.optInt("players", 0) + "/" + sv.optInt("max", 32), 13, Color.WHITE, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView pg = text("", 13, C_MUTED, false);
        svPingText(ip, pg);
        if (pings.get(ip) != null && pings.get(ip) >= 0) pg.setText("● " + pings.get(ip) + " ms");
        ln.addView(pg);
        box.addView(ln);
        final LinearLayout plist = new LinearLayout(this);
        plist.setOrientation(LinearLayout.VERTICAL);
        plist.addView(text(t("loading"), 12, C_MUTED, false));
        ScrollView psv = new ScrollView(this);
        psv.addView(plist);
        box.addView(psv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(170)));
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.util.List<PlayerInfo> ps = null;
                try { ps = queryPlayers(ip); } catch (Exception ignored) {}
                final java.util.List<PlayerInfo> fps = ps;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        plist.removeAllViews();
                        if (fps == null || fps.isEmpty()) { plist.addView(text(t("sv_noplayers"), 12, C_MUTED, false)); return; }
                        java.util.Collections.sort(fps, new java.util.Comparator<PlayerInfo>() {
                            public int compare(PlayerInfo a, PlayerInfo b) { return b.score - a.score; }
                        });
                        for (PlayerInfo p : fps) {
                            LinearLayout r = new LinearLayout(MainActivity.this);
                            r.setPadding(0, dp(4), 0, dp(4));
                            TextView a = text(cleanName(p.name).length() > 0 ? cleanName(p.name) : "...", 13, C_TEXT, false);
                            a.setSingleLine(true);
                            a.setEllipsize(TextUtils.TruncateAt.END);
                            r.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                            int min = (int) (p.time / 60);
                            r.addView(text(p.score + " · " + (min >= 60 ? (min / 60) + "h" : min + "'"), 12, C_MUTED, false));
                            plist.addView(r);
                        }
                    }
                });
            }
        }).start();
        final AlertDialog d = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                .setView(box)
                .setNegativeButton(isFav(ip) ? t("sv_unfav") : t("sv_tofav"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface di, int w) {
                        toggleFavQuiet(ip, nm);
                        fillServers();
                    }
                })
                .setPositiveButton(t("sv_join"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface di, int w) {
                        if (svDlg != null) svDlg.dismiss();
                        launchGame(ip);
                    }
                })
                .create();
        d.show();
    }

    /** Kompyuterdagidek: o'yinchi istalgan server IP sini qo'shadi (va xohlasa darrov kiradi). */
    private void showAddServer() {
        final EditText ed = new EditText(this);
        ed.setHint("51.89.39.107:27015");
        ed.setSingleLine(true);
        ed.setTextColor(C_TEXT);
        ed.setHintTextColor(C_MUTED);
        ed.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(6), dp(18), 0);
        box.addView(text(t("srv_add_msg"), 13, C_MUTED, false));
        box.addView(ed);
        final AlertDialog dlg = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                .setTitle(t("srv_add"))
                .setView(box)
                .setNegativeButton(t("srv_add_btn"), null)
                .setPositiveButton(t("srv_join_btn"), null)
                .create();
        dlg.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface di) {
                for (final int which : new int[]{AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE}) {
                    dlg.getButton(which).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String ip = normAddr(ed.getText().toString());
                            if (ip == null) {
                                toastLike(t("srv_bad"));
                                return;
                            }
                            try {
                                JSONObject f = favs();
                                if (!f.has(ip)) f.put(ip, ip);
                                prefs().edit().putString("favs", f.toString()).apply();
                            } catch (Exception ignored) {
                            }
                            dlg.dismiss();
                            toastLike(t("srv_added"));
                            render();
                            if (which == AlertDialog.BUTTON_POSITIVE) launchGame(ip);
                        }
                    });
                }
            }
        });
        dlg.show();
    }

    /** "1.2.3.4", "1.2.3.4:27016", "cs.example.uz" -> "host:port"; noto'g'ri bo'lsa null. */
    private static String normAddr(String in) {
        String v = in == null ? "" : in.trim().replace(" ", "");
        if (v.startsWith("connect")) v = v.substring(7);
        v = v.replaceAll("^[a-z]+://", "");
        if (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        if (!v.matches("[A-Za-z0-9.-]{3,253}(:[0-9]{1,5})?")) return null;
        if (!v.contains(":")) v += ":27015";
        int port = Integer.parseInt(v.substring(v.indexOf(':') + 1));
        if (port < 1 || port > 65535 || !v.substring(0, v.indexOf(':')).contains(".")) return null;
        return v;
    }

    private JSONObject favs() {
        try {
            return new JSONObject(prefs().getString("favs", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private boolean isFav(String ip) {
        return favs().has(ip);
    }

    private void toggleFav(String ip, String name) {
        try {
            JSONObject f = favs();
            if (f.has(ip)) f.remove(ip); else f.put(ip, name);
            prefs().edit().putString("favs", f.toString()).apply();
        } catch (Exception ignored) {
        }
        render();
    }

    /** Sevimlilar bo'limi uchun: saytdagi jonli ma'lumot, bo'lmasa saqlangan nom. */
    private static JSONObject fallbackServers() {
        try {
            JSONArray list = new JSONArray();
            for (String ip : FAV_FIXED) {
                JSONObject o = new JSONObject();
                o.put("ip", ip); o.put("name", ip); o.put("online", true);
                list.put(o);
            }
            JSONObject sec = new JSONObject();
            sec.put("title", "Serverlar"); sec.put("key", "top"); sec.put("servers", list);
            JSONObject d = new JSONObject();
            d.put("sections", new JSONArray().put(sec));
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject findServer(String ip, String savedName) {
        JSONArray sections = lastData == null ? null : lastData.optJSONArray("sections");
        if (sections != null) for (int i = 0; i < sections.length(); i++) {
            JSONObject s = sections.optJSONObject(i);
            JSONArray list = s == null ? null : s.optJSONArray("servers");
            if (list != null) for (int k = 0; k < list.length(); k++) {
                JSONObject sv = list.optJSONObject(k);
                if (sv != null && ip.equals(sv.optString("ip"))) return sv;
            }
        }
        JSONObject o = new JSONObject();
        try {
            o.put("ip", ip);
            o.put("name", savedName);
            o.put("online", true);
        } catch (Exception ignored) {
        }
        return o;
    }

    // ---------------- server ichidagi o'yinchilar (A2S_PLAYER) ----------------
    private static class PlayerInfo {
        String name;
        int score;
        float time;
    }

    private static java.util.List<PlayerInfo> queryPlayers(String addr) throws Exception {
        int c = addr.lastIndexOf(':');
        java.net.InetAddress host = java.net.InetAddress.getByName(addr.substring(0, c));
        int port = Integer.parseInt(addr.substring(c + 1));
        java.net.DatagramSocket sock = new java.net.DatagramSocket();
        try {
            sock.setSoTimeout(1500);
            byte[] req = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x55, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            byte[] buf = new byte[4096];
            java.net.DatagramPacket in = null;
            for (int attempt = 0; attempt < 4; attempt++) {
                sock.send(new java.net.DatagramPacket(req, req.length, host, port));
                in = new java.net.DatagramPacket(buf, buf.length);
                try {
                    sock.receive(in);
                } catch (java.net.SocketTimeoutException te) {
                    in = null;
                    continue;
                }
                if (in.getLength() >= 9 && buf[4] == 0x41) { // 'A' - challenge
                    req = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x55, buf[5], buf[6], buf[7], buf[8]};
                    in = null;
                    continue;
                }
                break;
            }
            if (in == null || in.getLength() < 6 || buf[4] != 0x44) throw new Exception("no answer"); // 'D'
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(buf, 0, in.getLength()).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            bb.position(5);
            int count = bb.get() & 0xff;
            java.util.List<PlayerInfo> out = new java.util.ArrayList<PlayerInfo>();
            for (int i = 0; i < count && bb.remaining() > 9; i++) {
                bb.get(); // index
                java.io.ByteArrayOutputStream nb = new java.io.ByteArrayOutputStream();
                byte b;
                while (bb.hasRemaining() && (b = bb.get()) != 0) nb.write(b);
                if (bb.remaining() < 8) break;
                PlayerInfo p = new PlayerInfo();
                byte[] raw = nb.toByteArray();
                String nm = new String(raw, "UTF-8");
                if (nm.indexOf('�') >= 0) nm = new String(raw, "windows-1251");
                p.name = nm;
                p.score = bb.getInt();
                p.time = bb.getFloat();
                out.add(p);
            }
            java.util.Collections.sort(out, new java.util.Comparator<PlayerInfo>() {
                @Override
                public int compare(PlayerInfo a, PlayerInfo b) {
                    return b.score - a.score;
                }
            });
            return out;
        } finally {
            sock.close();
        }
    }

    private void showPlayers(final String ip, final String serverName) {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(8));
        box.setBackgroundColor(C_CARD);
        final TextView st = text(t("loading").replace("Serverlar", "O'yinchilar"), 13, C_MUTED, false);
        box.addView(st);
        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                .setTitle(serverName)
                .setView(sv)
                .setPositiveButton(t("join"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        launchGame(ip);
                    }
                })
                .setNegativeButton(t("close"), null)
                .show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.util.List<PlayerInfo> list = null;
                try {
                    list = queryPlayers(ip);
                } catch (Exception ignored) {
                }
                final java.util.List<PlayerInfo> res = list;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        box.removeAllViews();
                        if (res == null) {
                            box.addView(text(t("pl_err"), 13, C_MUTED, false));
                            return;
                        }
                        if (res.isEmpty()) {
                            box.addView(text(t("pl_none"), 13, C_MUTED, false));
                            return;
                        }
                        box.addView(playerLine(t("pl_nick"), t("pl_score"), t("pl_time"), true));
                        for (PlayerInfo p : res) {
                            int m = (int) (p.time / 60);
                            String tm = (m / 60) + ":" + String.format(Locale.US, "%02d", m % 60);
                            box.addView(playerLine(p.name, String.valueOf(p.score), tm, false));
                        }
                    }
                });
            }
        }).start();
    }

    private View playerLine(String a, String b, String c, boolean head) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(5), 0, dp(5));
        int col = head ? C_GOLD : C_TEXT;
        TextView n = text(a, 13, col, head);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        r.addView(n, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView s = text(b, 13, col, head);
        s.setGravity(Gravity.END);
        r.addView(s, new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView t2 = text(c, 13, head ? C_GOLD : C_MUTED, head);
        t2.setGravity(Gravity.END);
        r.addView(t2, new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));
        return r;
    }

    private void showPing(String ip, TextView pv) {
        Integer ms = pings.get(ip);
        if (pv == null) return;
        if (ms == null) {
            pv.setText("… ms");
            pv.setBackground(round(C_LINE, 4, 0));
        } else if (ms < 0) {
            pv.setText("— ms");
            pv.setBackground(round(C_OFF, 4, 0));
        } else {
            pv.setText(ms + " ms");
            pv.setBackground(round(ms < 80 ? 0xFF22A355 : ms < 150 ? 0xFFC9A100 : 0xFFD9443A, 4, 0));
        }
    }

    /** Barcha serverlarga ping o'lchaydi (fon oqimida, bir vaqtda). */
    private void measurePings() {
        if (pinging || lastData == null) return;
        final java.util.LinkedHashSet<String> ips = new java.util.LinkedHashSet<String>();
        JSONArray sections = lastData.optJSONArray("sections");
        if (sections != null) for (int i = 0; i < sections.length(); i++) {
            JSONObject s = sections.optJSONObject(i);
            JSONArray list = s == null ? null : s.optJSONArray("servers");
            if (list != null) for (int k = 0; k < list.length(); k++) {
                JSONObject sv = list.optJSONObject(k);
                if (sv != null && sv.optBoolean("online", false)) ips.add(sv.optString("ip"));
            }
        }
        if (ips.isEmpty()) return;
        pinging = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.util.List<Thread> ts = new java.util.ArrayList<Thread>();
                for (final String ip : ips) {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final int ms = pingServer(ip);
                            pings.put(ip, ms);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showPing(ip, pingViews.get(ip));
                                }
                            });
                        }
                    });
                    ts.add(t);
                    t.start();
                }
                for (Thread t : ts) try { t.join(3000); } catch (InterruptedException ignored) { }
                pinging = false;
            }
        }).start();
    }

    /** GoldSrc A2S_INFO so'rovi; eng yaxshi javob vaqti (3 urinish), javob bo'lmasa -1. */
    private static int pingServer(String addr) {
        java.net.DatagramSocket sock = null;
        try {
            int c = addr.lastIndexOf(':');
            java.net.InetAddress host = java.net.InetAddress.getByName(addr.substring(0, c));
            int port = Integer.parseInt(addr.substring(c + 1));
            byte[] q = "\u00ff\u00ff\u00ff\u00ffTSource Engine Query\u0000".getBytes("ISO-8859-1");
            sock = new java.net.DatagramSocket();
            sock.setSoTimeout(1000);
            int best = -1;
            byte[] buf = new byte[1400];
            for (int i = 0; i < 3; i++) {
                long t0 = System.nanoTime();
                sock.send(new java.net.DatagramPacket(q, q.length, host, port));
                try {
                    sock.receive(new java.net.DatagramPacket(buf, buf.length));
                    int ms = (int) ((System.nanoTime() - t0) / 1000000L);
                    if (best < 0 || ms < best) best = ms;
                } catch (java.net.SocketTimeoutException ignored) {
                }
            }
            return best;
        } catch (Exception e) {
            return -1;
        } finally {
            if (sock != null) sock.close();
        }
    }

    private void loadServers() {
        if (status != null) status.setText(t("loading"));
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject data = null;
                boolean cached = false;
                try {
                    String body = httpGet(SERVERS_URL + "?t=" + System.currentTimeMillis());
                    data = new JSONObject(body);
                    prefs().edit().putString("srvcache", body).apply();   // sayt ishlamay qolsa - shu ro'yxat
                } catch (Exception ignored) {
                }
                /* sayt javob bermadi (internet bor, lekin boost-top serveri o'chiq bo'lishi mumkin): oxirgi saqlangan ro'yxat,
                 * u ham bo'lmasa - doimiy serverlar. O'yin serverlari saytdan alohida ishlaydi - o'ynash mumkin */
                if (data == null && lastData == null) {
                    try { data = new JSONObject(prefs().getString("srvcache", "")); } catch (Exception ignored) {}
                    if (data == null) data = fallbackServers();
                    cached = true;
                }
                final boolean failed = data == null || cached;
                netDown = failed;
                final JSONObject d = data;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (d != null) lastData = d;
                        render();
                        if (d != null) {
                            pings.clear();
                            measurePings();
                        }
                        if (failed && status != null) status.setText(t(lastData != null ? "sitedown" : "neterr"));
                        if (failed && !retryPending && content != null) {   // sayt qaytdimi - 45 soniyada qayta so'raymiz
                            retryPending = true;
                            content.postDelayed(new Runnable() {
                                @Override
                                public void run() { retryPending = false; if (!isFinishing() && !downloading) loadServers(); }
                            }, 45000);
                        }
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
        pingViews.clear();
        dlBar = null;
        dlText = null;

        if (apkBuild > myBuild() && apkUrl.length() > 0) content.addView(updateCard());
        if (!setupDone() || downloading) content.addView(setupCard());
        content.addView(accountCard());
        if (netDown && hasData()) content.addView(offlineCard());
        else if (hasData() && !downloading) content.addView(botsCard());
        addNews();

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
        Button openSv = button(t("sv_open"), C_ACCENT);
        openSv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        openSv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showServers();
            }
        });
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        olp.setMargins(0, dp(6), 0, 0);
        content.addView(openSv, olp);
        Button addSrv = button(t("srv_add"), C_LINE);
        addSrv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addSrv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddServer();
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        alp.setMargins(0, dp(6), 0, dp(2));
        content.addView(addSrv, alp);
        JSONObject fv = favs();
        if (fv.length() > 0) {
            content.addView(sectionHeader(t("favs"), "fav"));
            java.util.Iterator<String> it = fv.keys();
            int r = 1;
            while (it.hasNext()) {
                String fip = it.next();
                content.addView(serverRow(findServer(fip, fv.optString(fip, fip)), r++, flagBase));
                shown++;
            }
        }
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

    // ---------------- yangiliklar qatori ----------------
    private void addNews() {
        JSONArray arr = news;
        if (arr == null) return;
        String hidden = "," + prefs().getString("news_hidden", "") + ",";
        for (int i = 0; i < arr.length(); i++) {
            JSONObject n = arr.optJSONObject(i);
            if (n == null) continue;
            String id = n.optString("id", "");
            if (id.length() > 0 && hidden.contains("," + id + ",")) continue;
            String msg = n.optString(lang, "");
            if (msg.length() == 0) msg = n.optString("ru", n.optString("uz", n.optString("text", "")));
            if (msg.length() == 0) continue;
            content.addView(newsCard(n, id, msg));
        }
    }

    private View newsCard(JSONObject n, final String id, String msg) {
        int col = C_ACCENT;
        try {
            String c = n.optString("color", "");
            if (c.length() > 0) col = android.graphics.Color.parseColor(c);
        } catch (Exception ignored) {
        }
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(6), dp(10));
        card.setBackground(round(C_CARD, 12, col));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(6));
        card.setLayoutParams(lp);
        TextView tv = text(msg, 14, C_TEXT, true);
        card.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final String server = n.optString("server", "");
        final String url = n.optString("url", "");
        if (server.length() > 0 || url.length() > 0) {
            TextView go = text(server.length() > 0 ? t("join") : "›", server.length() > 0 ? 12 : 22, col, true);
            go.setPadding(dp(8), 0, dp(6), 0);
            card.addView(go);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (server.length() > 0) launchGame(server);
                    else openUrl(url);
                }
            });
        }
        if (id.length() > 0 && !n.optBoolean("pin", false)) {
            TextView x = text("×", 22, C_MUTED, false);
            x.setPadding(dp(10), 0, dp(8), 0);
            x.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String h = prefs().getString("news_hidden", "");
                    prefs().edit().putString("news_hidden", h.length() == 0 ? id : h + "," + id).apply();
                    render();
                }
            });
            card.addView(x);
        }
        return card;
    }

    /** Ilovaning build raqami (CI assets/bt_build.txt ga yozadi). */
    private int myBuild() {
        try {
            InputStream in = getAssets().open("bt_build.txt");
            byte[] b = new byte[32];
            int n = in.read(b);
            in.close();
            return Integer.parseInt(new String(b, 0, Math.max(0, n), "UTF-8").trim());
        } catch (Exception e) {
            return Integer.MAX_VALUE; // noma'lum bo'lsa yangilanish taklif qilinmaydi
        }
    }

    private View updateCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(0xFF0F3A1E, 12, 0xFF35C46A));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(8));
        card.setLayoutParams(lp);
        card.addView(text(t("upd"), 14, C_TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (updating) {
            updText = text(t("dling") + "0%", 13, C_GOLD, true);
            card.addView(updText);
            return card;
        }
        Button b = button(t("updb"), 0xFF22A355);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmSelfUpdate();
            }
        });
        card.addView(b);
        return card;
    }

    // ---------------- ilova ichida yangilash (PackageInstaller) ----------------
    private volatile boolean updating = false;
    private TextView updText;
    private static final String ACTION_INSTALL = "uz.boosttop.cs16.INSTALL_STATUS";
    private android.content.BroadcastReceiver installReceiver;

    /** Avval ogohlantiramiz: o'rnatishda ilova yopiladi (ko'pchilik buni "otib yubordi" deb o'ylaydi). */
    private void confirmSelfUpdate() {
        if (updating) return;
        try {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 77);
        } catch (Exception ignored) {
        }
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                .setTitle(t("upd"))
                .setMessage(t("updwarn"))
                .setPositiveButton(t("updok"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        startSelfUpdate();
                    }
                })
                .setNegativeButton(t("cancel"), null)
                .show();
    }

    /** Xato bo'lsa ilovadan chiqarib yubormaymiz - tanlov beramiz. */
    private void showUpdateError(String err) {
        sendStat("upderr", (err == null ? "?" : err) + " | " + Build.MANUFACTURER + " " + Build.MODEL + " | SDK " + Build.VERSION.SDK_INT);
        try {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                    .setTitle(t("upderr"))
                    .setMessage(String.valueOf(err))
                    .setPositiveButton(t("updretry"), new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            startSelfUpdate();
                        }
                    })
                    .setNeutralButton(t("updbrowser"), new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            openUrl(apkUrl);
                        }
                    })
                    .setNegativeButton(t("cancel"), null)
                    .show();
        } catch (Exception e) {
            toastLike(t("dlerr") + err);
        }
    }

    /** Yangi APK saqlanadigan fayl (Android 7+ da content:// orqali beriladi). */
    private File updateApkFile() {
        File dir = Build.VERSION.SDK_INT >= 24 ? new File(getCacheDir(), "upd") : getExternalCacheDir();
        if (dir == null) dir = getCacheDir();
        dir.mkdirs();
        return new File(dir, "update.apk");
    }

    /** Tizim o'rnatuvchisini ochadi: "Yangilash/O'rnatish" -> tugagach "Ochish". */
    private void openInstaller(File apk) {
        try {
            Uri uri;
            if (Build.VERSION.SDK_INT >= 24) uri = Uri.parse("content://" + getPackageName() + ".apk/update.apk");
            else {
                apk.setReadable(true, false);
                uri = Uri.fromFile(apk);
            }
            Intent i = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 1 /* FLAG_GRANT_READ_URI_PERMISSION */);
            startActivity(i);
        } catch (Exception e) {
            showUpdateError(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void startSelfUpdate() {
        if (updating) return;
        // Android 8+: "noma'lum manbalardan o'rnatish" ruxsati shu ilova uchun kerak
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            toastLike(t("updperm"));
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                openUrl(apkUrl);
            }
            return;
        }
        updating = true;
        render();
        final String url = apkUrl;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                HttpURLConnection c = null;
                try {
                    c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.setInstanceFollowRedirects(true);
                    // GitHub boshqa domenga (https) yo'naltiradi - qo'lda kuzatamiz
                    for (int i = 0; i < 5; i++) {
                        int code = c.getResponseCode();
                        if (code >= 300 && code < 400) {
                            String loc = c.getHeaderField("Location");
                            c.disconnect();
                            c = (HttpURLConnection) new URL(new URL(url), loc).openConnection();
                            c.setConnectTimeout(15000);
                            c.setReadTimeout(30000);
                            continue;
                        }
                        break;
                    }
                    if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                    final long total = Math.max(1, c.getContentLength());
                    // APK ni faylga yuklaymiz, keyin tizimning o'z o'rnatuvchisini ochamiz:
                    // u "O'rnatish" -> "Ochish" tugmalarini ko'rsatadi, o'yinchi nima bo'layotganini ko'radi.
                    final File apk = updateApkFile();
                    OutputStream out = new FileOutputStream(apk);
                    InputStream in = c.getInputStream();
                    byte[] buf = new byte[65536];
                    long done = 0, lastUi = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        done += n;
                        long now = System.currentTimeMillis();
                        if (now - lastUi > 300) {
                            lastUi = now;
                            final int pct = (int) Math.min(100, done * 100 / total);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (updText != null) updText.setText(t("dling") + pct + "%");
                                }
                            });
                        }
                    }
                    out.close();
                    in.close();
                    if (c.getContentLength() > 0 && done < c.getContentLength()) throw new Exception("yuklash uzildi (" + done + "/" + c.getContentLength() + ")");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updating = false;
                            render();
                            openInstaller(apk);
                        }
                    });
                } catch (Exception e) {
                    err = e.getClass().getSimpleName() + ": " + e.getMessage();
                } finally {
                    if (c != null) c.disconnect();
                }
                final String fe = err;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (fe != null) {
                            updating = false;
                            render();
                            showUpdateError(fe);
                        }
                    }
                });
            }
        }).start();
    }

    /** O'rnatish holati: tizim tasdiq oynasini so'rasa - ochamiz. */
    private void registerInstallReceiver() {
        installReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                int st = intent.getIntExtra(android.content.pm.PackageInstaller.EXTRA_STATUS, -999);
                if (st == android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirm != null) {
                        confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            startActivity(confirm);
                        } catch (Exception ignored) {
                        }
                    }
                } else if (st != android.content.pm.PackageInstaller.STATUS_SUCCESS) {
                    updating = false;
                    render();
                    String msg = intent.getStringExtra(android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE);
                    if (st != android.content.pm.PackageInstaller.STATUS_FAILURE_ABORTED)
                        showUpdateError((msg == null ? "status " + st : msg));
                }
            }
        };
        android.content.IntentFilter f = new android.content.IntentFilter(ACTION_INSTALL);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(installReceiver, f, 4 /* RECEIVER_NOT_EXPORTED */);
        else registerReceiver(installReceiver, f);
    }

    @Override
    protected void onDestroy() {
        try {
            if (installReceiver != null) unregisterReceiver(installReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
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
        final String nmFinal = sv.optString("name", ip);
        final TextView star = text(isFav(ip) ? "★" : "☆", 20, isFav(ip) ? C_GOLD : C_MUTED, true);
        star.setPadding(dp(8), 0, dp(4), 0);
        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFav(ip, nmFinal);
            }
        });
        nameLine.addView(star);
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
        if (online) {
            TextView pv = pill("…", C_LINE, Color.WHITE);
            pingViews.put(ip, pv);
            showPing(ip, pv);
            meta.addView(pv);
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
                showPlayers(ip, nmFinal);
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
