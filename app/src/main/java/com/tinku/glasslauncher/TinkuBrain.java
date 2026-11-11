package com.tinku.glasslauncher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns what the speech recognizer heard (Nepali in Devanagari, English, or a
 * mix) into a launcher command. No network, no AI: keyword and sound-alike
 * matching.
 *
 * Idea: Devanagari is first written in Latin letters, then every word is
 * reduced to a "skeleton" (vowels dropped, b/v/w merged, ph/f/p merged ...).
 * "व्हाट्सएप" and "WhatsApp" both become "btsp", so Nepali speech that the
 * recognizer wrote in Devanagari still finds the Latin app name.
 */
public class TinkuBrain {

    public static final int NONE = 0, OPEN_APP = 1, TOGGLE = 2, SET = 3, FAV_ADD = 4,
        FAV_REMOVE = 5, HIDE = 6, SEARCH = 7, NOTIF = 8, SETTINGS = 9, LOCK = 10,
        SYSTEM = 11, INFO = 12, STOP_LISTEN = 13, HIDDEN_LIST = 14, CLOSE = 15;

    public static class Cmd {
        public int type = NONE;
        public int app = -1;          // index into the labels list given to parse()
        public String key = "";
        public String value = "";
        public boolean on;            // for TOGGLE
        public boolean hasOnOff;      // false = flip the current value
        public boolean sure;          // safe to run before the sentence is finished
        public float score;
        public String say = "";
        public String heard = "";
        public boolean bareWake;      // only "Tinku" was said

        public String sig() {
            return type + "|" + app + "|" + key + "|" + value + "|" + on + "|" + hasOnOff;
        }
    }

    private static class Tok {
        String lat;
        String skel;
        boolean dev;
    }

    // ---------------------------------------------------------------- words

    private static final String[] OPEN = { "open", "launch", "run", "kholo", "khol*", "kholi*",
        "chalau*", "chalaid*", "khul*" };
    private static final String[] ON = { "on", "enable", "enabled", "show", "dekha*", "dikha*",
        "chalu", "chaalu", "chaaloo", "active", "activate" };
    private static final String[] OFF = { "off", "of", "disable", "disabled", "hide", "hatau*",
        "hata*", "band*", "bnd", "luka*" };
    private static final String[] FAVW = { "fav*", "favourite*", "favorite*", "pyaro*" };
    private static final String[] ADDW = { "add", "put", "rakh*", "raakh*", "hal*", "halau*" };
    private static final String[] REMW = { "remove", "delete", "hata*", "hatau*", "unfav*",
        "unfavourite", "unfavorite" };
    private static final String[] HIDEW = { "hide", "luka*", "lukau*" };
    private static final String[] SEARCHW = { "search", "find", "lookup", "khoj*", "khuj*" };
    private static final String[] FILLER = { "please", "pls", "the", "a", "an", "my", "to", "for",
        "me", "ma", "lai", "ko", "ka", "ki", "ho", "hai", "ta", "na", "gar*", "gara*", "garideu",
        "garnu", "dinu", "deu", "dehu", "hos", "hune", "hola", "from", "bata", "in", "all",
        "kripaya", "aba", "ani", "ra", "and", "app", "apps", "aap", "application", "ek", "now",
        "can", "you", "tinku", "mero", "tapai", "tapaile", "yo", "tyo", "ma" };

    private static final String[] STOPW = { "stop", "bye", "bas", "sleep", "enough", "band*", "bnd",
        "off", "of" };
    private static final String[] MICW = { "mic", "microphone", "listen*", "sun*", "suna*" };

    private static final String[] WEATHERW = { "weather", "wedar", "mausam*", "maausam*",
        "temperature", "tapman*", "taapman*" };
    private static final String[] BATTERYW = { "battery", "batteri*", "byatri*", "byaat*",
        "baitri*", "betri*", "batri*" };
    private static final String[] GESTW = { "gesture*", "jeschar*", "swipe*" };
    private static final String[] MONOW = { "mono*", "monochrome" };
    private static final String[] STACKW = { "stack*", "stacked" };
    private static final String[] CLOCKW = { "clock", "ghadi*", "ghadee*", "ghari*" };
    private static final String[] VERTW = { "vertical" };
    private static final String[] HORIZW = { "horizontal", "horijontal" };
    private static final String[] FREEW = { "free", "freely", "anywhere" };
    private static final String[] LOCKW = { "lock*", "tala", "taala", "lak" };
    private static final String[] SCREENW = { "screen", "skrin*", "sakrin*" };
    private static final String[] GLASSW = { "blur*", "blar*", "glass*", "glas*", "glaas*",
        "kanch*", "kaanch*" };
    private static final String[] SOFTW = { "soft", "low", "kam", "halka", "halko", "less" };
    private static final String[] MEDW = { "medium", "normal", "middle", "madhyam*" };
    private static final String[] STRONGW = { "strong", "stron*", "high", "more", "dherai", "badhi",
        "jyada", "max", "maximum" };
    private static final String[] CLEARW = { "clear", "transparent", "pardarshi", "safa", "saaf" };
    private static final String[] LIGHTW = { "light", "lait", "laait", "white", "seto", "safed" };
    private static final String[] DARKW = { "dark", "black", "kalo", "kaalo" };
    private static final String[] NOTIFW = { "notif*", "notice", "suchana*", "sochana*" };
    private static final String[] SETW = { "setting*", "setin*", "seting*", "setup" };
    private static final String[] PHONEW = { "phone", "fon*", "mobile", "mobail*", "system",
        "sistam*", "sistem*", "device" };
    private static final String[] HIDDENW = { "hidden" };
    private static final String[] TIMEW = { "time", "taim*", "taaim*", "samay*", "baj*", "date",
        "miti", "tarik*" };
    private static final String[] CLOSEW = { "home", "hom", "close", "back", "cancel", "dismiss" };

    private static final String[] WIFIW = { "wifi", "waifai", "waifi", "wai*" };
    private static final String[] BTW = { "bluetooth", "blutooth", "blu*", "bloo*" };
    private static final String[] DISPW = { "display", "brightness", "ujyal*", "ujjal*" };
    private static final String[] SOUNDW = { "sound", "volume", "awaj*", "aawaj*", "avaj*" };
    private static final String[] PLANEW = { "airplane", "aeroplane", "flight", "jahaj*" };
    private static final String[] LOCW = { "location", "gps", "lokeshan", "lokesan*" };
    private static final String[] HOTW = { "hotspot", "hotspots", "tethering" };

    // ------------------------------------------------------ transliteration

    private static String cons(char c) {
        switch (c) {
            case 'क': return "k";
            case 'ख': return "kh";
            case 'ग': return "g";
            case 'घ': return "gh";
            case 'ङ': return "n";
            case 'च': return "ch";
            case 'छ': return "chh";
            case 'ज': return "j";
            case 'झ': return "jh";
            case 'ञ': return "n";
            case 'ट': return "t";
            case 'ठ': return "th";
            case 'ड': return "d";
            case 'ढ': return "dh";
            case 'ण': return "n";
            case 'त': return "t";
            case 'थ': return "th";
            case 'द': return "d";
            case 'ध': return "dh";
            case 'न': return "n";
            case 'प': return "p";
            case 'फ': return "f";
            case 'ब': return "b";
            case 'भ': return "bh";
            case 'म': return "m";
            case 'य': return "y";
            case 'र': return "r";
            case 'ल': return "l";
            case 'ळ': return "l";
            case 'व': return "v";
            case 'श': return "sh";
            case 'ष': return "sh";
            case 'स': return "s";
            case 'ह': return "h";
            default: return null;
        }
    }

    private static String matra(char c) {
        switch (c) {
            case 'ा': return "aa";
            case 'ि': return "i";
            case 'ी': return "ee";
            case 'ु': return "u";
            case 'ू': return "oo";
            case 'ृ': return "ri";
            case 'े': return "e";
            case 'ै': return "ai";
            case 'ो': return "o";
            case 'ौ': return "au";
            case 'ॉ': return "o";
            case 'ॅ': return "e";
            case 'ॆ': return "e";
            case 'ॊ': return "o";
            default: return null;
        }
    }

    private static String vowel(char c) {
        switch (c) {
            case 'अ': return "a";
            case 'आ': return "aa";
            case 'इ': return "i";
            case 'ई': return "ee";
            case 'उ': return "u";
            case 'ऊ': return "oo";
            case 'ऋ': return "ri";
            case 'ए': return "e";
            case 'ऐ': return "ai";
            case 'ओ': return "o";
            case 'औ': return "au";
            case 'ऑ': return "o";
            case 'ऍ': return "e";
            case 'ं': return "n";
            case 'ँ': return "n";
            case 'ः': return "h";
            default: return null;
        }
    }

    private static boolean isDev(char c) {
        return c >= 'ऀ' && c <= 'ॿ';
    }

    static String translit(String w) {
        StringBuilder b = new StringBuilder();
        int n = w.length();
        for (int i = 0; i < n; i++) {
            char c = w.charAt(i);
            String con = cons(c);
            if (con != null) {
                b.append(con);
                int j = i + 1;
                if (j < n && w.charAt(j) == '़') j++;   // nukta
                if (j >= n) {
                    i = j - 1;                               // no schwa at word end
                    continue;
                }
                char m = w.charAt(j);
                if (m == '्') {                         // virama: no vowel
                    i = j;
                    continue;
                }
                String v = matra(m);
                if (v != null) {
                    b.append(v);
                    i = j;
                } else {
                    b.append('a');
                    i = j - 1;
                }
                continue;
            }
            String v = vowel(c);
            if (v != null) {
                b.append(v);
            } else if (!isDev(c)) {
                b.append(c);
            }
        }
        return b.toString();
    }

    /** Sound-alike skeleton. */
    static String skel(String s) {
        s = s.toLowerCase(Locale.ROOT);
        s = s.replace("ph", "f").replace("ch", "#").replace("sh", "s").replace("ck", "k")
            .replace("kh", "k").replace("gh", "g").replace("th", "t").replace("dh", "d")
            .replace("bh", "b").replace("jh", "j");
        StringBuilder b = new StringBuilder();
        char last = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String out;
            switch (c) {
                case '#': case 'c': case 'q': case 'k': out = "k"; break;
                case 'v': case 'w': case 'b': out = "b"; break;
                case 'f': case 'p': out = "p"; break;
                case 'z': case 'j': out = "j"; break;
                case 'x': out = "ks"; break;
                case 'a': case 'e': case 'i': case 'o': case 'u': case 'y': case 'h':
                    out = ""; break;
                default:
                    if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) out = String.valueOf(c);
                    else out = "";
            }
            for (int k = 0; k < out.length(); k++) {
                char o = out.charAt(k);
                if (o != last) b.append(o);
                last = o;
            }
        }
        return b.toString();
    }

    static int lev(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    // --------------------------------------------------------------- tokens

    private static List<Tok> tokenize(String text) {
        List<Tok> out = new ArrayList<Tok>();
        String t = text.toLowerCase(Locale.ROOT);
        StringBuilder w = new StringBuilder();
        for (int i = 0; i <= t.length(); i++) {
            char c = i < t.length() ? t.charAt(i) : ' ';
            boolean letter = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || isDev(c);
            if (c == '-' || c == '\'') continue;       // wi-fi -> wifi
            if (letter) {
                w.append(c);
            } else if (w.length() > 0) {
                out.add(makeTok(w.toString()));
                w.setLength(0);
            }
        }
        return out;
    }

    private static Tok makeTok(String raw) {
        Tok k = new Tok();
        boolean dev = false;
        for (int i = 0; i < raw.length(); i++) if (isDev(raw.charAt(i))) dev = true;
        k.dev = dev;
        k.lat = dev ? translit(raw) : raw;
        k.skel = skel(k.lat);
        return k;
    }

    /** Does this token mean the keyword? A trailing * means "starts with". */
    private static boolean m(Tok t, String kw) {
        if (kw.endsWith("*")) {
            return t.lat.startsWith(kw.substring(0, kw.length() - 1));
        }
        if (t.lat.equals(kw)) return true;
        if (t.dev) {
            String ks = skel(kw);
            if (ks.length() >= 2) {
                if (t.skel.equals(ks)) return true;
                if (ks.length() >= 5 && lev(t.skel, ks) <= 1) return true;
            }
        } else if (kw.length() >= 6 && t.lat.length() >= 5 && lev(t.lat, kw) <= 1) {
            return true;
        }
        return false;
    }

    private static boolean has(List<Tok> toks, String[] kws) {
        for (int i = 0; i < toks.size(); i++) {
            for (int k = 0; k < kws.length; k++) {
                if (m(toks.get(i), kws[k])) return true;
            }
        }
        return false;
    }

    private static boolean isAny(Tok t, String[]... lists) {
        for (int a = 0; a < lists.length; a++) {
            for (int k = 0; k < lists[a].length; k++) {
                if (m(t, lists[a][k])) return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------- wake word

    private static boolean wakeWord(String s) {
        if (s.equals("think") || s.equals("thing") || s.equals("thank") || s.equals("thanks")) return false;
        return s.matches("^(t|d|p)h?[ie]+n[gk]h?[aeiouy]+s?$");
    }

    /** Index of the wake word token (or -1). A second index in out[1] if it spans two tokens. */
    private static int[] findWake(List<Tok> toks) {
        for (int i = 0; i < toks.size(); i++) {
            if (wakeWord(toks.get(i).lat)) return new int[] { i, -1 };
            if (i + 1 < toks.size()) {
                String a = toks.get(i).lat;
                String b = toks.get(i + 1).lat;
                if (a.matches("^(t|d|p)h?i?n$") && b.matches("^[gk]h?[aeiouy]+$")) {
                    return new int[] { i, i + 1 };
                }
            }
        }
        return null;
    }

    // --------------------------------------------------------- app matching

    private static float sim(String h, String l) {
        if (h.length() == 0 || l.length() == 0) return 0f;
        if (h.equals(l)) return 1f;
        int mx = Math.max(h.length(), l.length());
        if (mx <= 3) return 0f;
        if (h.length() >= 3 && l.length() >= 3) {
            if (l.startsWith(h)) return 0.80f + 0.15f * h.length() / l.length();
            if (h.startsWith(l)) return 0.85f;
        }
        float s = 1f - (float) lev(h, l) / mx;
        return s >= 0.7f ? s : 0f;
    }

    private static class Hit {
        int idx = -1;
        float score = 0f;
        int others = 0;     // other apps that also scored >= 0.8
    }

    private static Hit bestApp(List<Tok> rest, String[] plain, String[] lskel) {
        Hit hit = new Hit();
        if (rest.isEmpty()) return hit;
        float[] sc = new float[lskel.length];
        int[] cov = new int[lskel.length];
        int n = rest.size();
        for (int s = 0; s < n; s++) {
            StringBuilder lat = new StringBuilder();
            for (int len = 1; len <= 4 && s + len <= n; len++) {
                lat.append(rest.get(s + len - 1).lat);
                String hs = skel(lat.toString());
                if (hs.length() < 2) continue;
                String hp = lat.toString();
                for (int i = 0; i < lskel.length; i++) {
                    float v = sim(hs, lskel[i]);
                    if (plain[i].equals(hp)) v = 1f;
                    if (v > sc[i] || (v == sc[i] && v > 0f && hs.length() > cov[i])) {
                        sc[i] = v;
                        cov[i] = hs.length();
                    }
                }
            }
        }
        float bestKey = 0f;
        for (int i = 0; i < sc.length; i++) {
            float key = sc[i] + 0.0001f * cov[i];
            if (sc[i] > 0f && key > bestKey) {
                bestKey = key;
                hit.score = sc[i];
                hit.idx = i;
            }
        }
        for (int i = 0; i < sc.length; i++) {
            if (i != hit.idx && sc[i] >= 0.8f) hit.others++;
        }
        return hit;
    }

    // ---------------------------------------------------------------- parse

    /**
     * @param alts     what the recognizer heard, best guess first
     * @param labels   names of the apps that can be opened
     * @param needWake true when "Tinku" must be in the sentence (always-listening mode)
     * @return null when nothing was meant for Tinku
     */
    public static Cmd parse(List<String> alts, List<String> labels, boolean needWake) {
        String[] plain = new String[labels.size()];
        String[] lskel = new String[labels.size()];
        for (int i = 0; i < plain.length; i++) {
            String lb = labels.get(i);
            boolean dev = false;
            for (int k = 0; k < lb.length(); k++) if (isDev(lb.charAt(k))) dev = true;
            String lat = dev ? translit(lb) : lb;
            StringBuilder p = new StringBuilder();
            String low = lat.toLowerCase(Locale.ROOT);
            for (int k = 0; k < low.length(); k++) {
                char c = low.charAt(k);
                if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) p.append(c);
            }
            plain[i] = p.toString();
            lskel[i] = skel(lat);
        }

        Cmd best = null;
        float bestScore = -1f;
        boolean anyWake = false;
        for (int a = 0; a < alts.size() && a < 5; a++) {
            Cmd c = parseOne(alts.get(a), labels, plain, lskel, needWake);
            if (c == null) continue;
            anyWake = true;
            float s = c.score - 0.03f * a;
            if (s > bestScore) {
                bestScore = s;
                best = c;
            }
        }
        if (!anyWake) return null;
        return best;
    }

    private static Cmd parseOne(String heard, List<String> labels, String[] plain, String[] lskel,
                                boolean needWake) {
        List<Tok> all = tokenize(heard);
        List<Tok> toks = new ArrayList<Tok>();
        int[] wake = findWake(all);
        if (needWake && wake == null) return null;
        for (int i = 0; i < all.size(); i++) {
            if (wake != null && (i == wake[0] || i == wake[1])) continue;
            toks.add(all.get(i));
        }

        Cmd c = new Cmd();
        c.heard = heard;

        if (toks.isEmpty()) {
            c.say = "Yes? Say a command";
            c.bareWake = wake != null;
            return c;
        }

        boolean on = has(toks, ON);
        boolean off = has(toks, OFF);
        boolean onOff = on || off;
        boolean openVerb = has(toks, OPEN);

        // Stop listening: "Tinku band gar", "mic off", "stop listening".
        boolean onlyStop = toks.size() <= 2 && has(toks, STOPW);
        for (int i = 0; i < toks.size() && onlyStop; i++) {
            if (!isAny(toks.get(i), STOPW, FILLER, MICW)) onlyStop = false;
        }
        if ((has(toks, MICW) && (off || has(toks, STOPW))) || onlyStop) {
            c.type = STOP_LISTEN;
            c.score = 0.9f;
            c.sure = true;
            c.say = "Tinku mic off";
            return c;
        }

        // App name candidates: everything that is not a command or filler word.
        List<Tok> rest = new ArrayList<Tok>();
        for (int i = 0; i < toks.size(); i++) {
            Tok t = toks.get(i);
            if (isAny(t, OPEN, ON, OFF, FAVW, ADDW, REMW, HIDEW, SEARCHW, FILLER)) continue;
            rest.add(t);
        }
        Hit hit = bestApp(rest, plain, lskel);

        // "open X": explicit, so apps win over keywords ("open weather" opens the Weather app).
        if (openVerb && hit.idx >= 0 && hit.score >= 0.85f) {
            return openCmd(c, labels, hit, true);
        }

        // ---- settings of the launcher
        if (has(toks, BATTERYW)) {
            if (onOff) return toggle(c, "showbat", "Battery %", true, on && !off);
            c.type = INFO;
            c.key = "battery";
            c.score = 0.9f;
            c.sure = true;
            return c;
        }
        if (has(toks, WEATHERW)) {
            if (onOff) return toggle(c, "weather", "Weather", true, on && !off);
            c.type = INFO;
            c.key = "weather";
            c.score = 0.9f;
            c.sure = true;
            return c;
        }
        if (has(toks, GESTW)) {
            return toggle(c, "gestures", "Swipe gestures", onOff, on && !off);
        }
        boolean blackWhite = has(toks, new String[] { "black" }) && has(toks, new String[] { "white" });
        if (has(toks, MONOW) || blackWhite) {
            return toggle(c, "mono", "Monochrome icons", onOff, on && !off);
        }
        boolean vert = has(toks, VERTW);
        boolean horiz = has(toks, HORIZW);
        boolean fav = has(toks, FAVW);
        if (has(toks, STACKW) || (has(toks, CLOCKW) && (vert || horiz))) {
            c.type = TOGGLE;
            c.key = "stacked";
            c.hasOnOff = onOff || vert || horiz;
            c.on = off ? false : (horiz ? false : true);
            c.say = "Stacked clock";
            c.score = 0.9f;
            c.sure = c.hasOnOff;
            return c;
        }
        if (fav && (vert || horiz || has(toks, FREEW))) {
            c.type = SET;
            c.key = "favlayout";
            c.value = vert ? "vertical" : (horiz ? "horizontal" : "free");
            c.say = "Favourites: " + c.value;
            c.score = 0.9f;
            c.sure = true;
            return c;
        }
        if (has(toks, GLASSW)) {
            c.type = SET;
            c.score = 0.9f;
            c.sure = true;
            if (has(toks, SOFTW) || has(toks, MEDW) || has(toks, STRONGW)) {
                c.key = "blurstr";
                c.value = has(toks, STRONGW) ? "strong" : (has(toks, SOFTW) ? "soft" : "medium");
                c.say = "Blur: " + c.value;
                return c;
            }
            if (has(toks, CLEARW) || has(toks, LIGHTW) || has(toks, DARKW)) {
                c.key = "glass";
                c.value = has(toks, DARKW) ? "dark" : (has(toks, LIGHTW) ? "light" : "clear");
                c.say = "Glass: " + c.value;
                return c;
            }
            c.type = NONE;
            c.score = 0f;
            c.say = "Blur: say soft, medium or strong. Glass: clear, light or dark";
            return c;
        }
        if (has(toks, LOCKW)) {
            if (has(toks, SCREENW) && onOff) return toggle(c, "lockscreen", "Lock screen", true, on && !off);
            c.type = LOCK;
            c.score = 0.9f;
            c.sure = true;
            c.say = "Locking";
            return c;
        }

        // ---- phone pages
        String sys = null;
        if (has(toks, WIFIW) || wifiPair(toks)) sys = "wifi";
        else if (has(toks, BTW)) sys = "bluetooth";
        else if (has(toks, PLANEW)) sys = "airplane";
        else if (has(toks, HOTW)) sys = "hotspot";
        else if (has(toks, LOCW)) sys = "location";
        else if (has(toks, DISPW)) sys = "display";
        else if (has(toks, SOUNDW)) sys = "sound";
        if (sys != null) {
            c.type = SYSTEM;
            c.key = sys;
            c.score = 0.9f;
            c.sure = true;
            c.say = "Opening " + sys;
            return c;
        }

        if (has(toks, NOTIFW)) {
            c.type = NOTIF;
            c.score = 0.9f;
            c.sure = true;
            c.say = "Notifications";
            return c;
        }
        if (has(toks, HIDDENW)) {
            c.type = HIDDEN_LIST;
            c.score = 0.9f;
            c.sure = true;
            c.say = "Hidden apps";
            return c;
        }
        if (has(toks, SETW)) {
            if (has(toks, PHONEW)) {
                c.type = SYSTEM;
                c.key = "settings";
                c.say = "Phone settings";
            } else {
                c.type = SETTINGS;
                c.say = "Launcher settings";
            }
            c.score = 0.9f;
            c.sure = true;
            return c;
        }
        if (has(toks, SEARCHW)) {
            c.type = SEARCH;
            c.score = 0.85f;
            if (hit.idx >= 0 && hit.score >= 0.8f) {
                c.value = labels.get(hit.idx);
            } else {
                StringBuilder q = new StringBuilder();
                for (int i = 0; i < rest.size(); i++) {
                    if (q.length() > 0) q.append(' ');
                    q.append(rest.get(i).lat);
                }
                c.value = q.toString();
            }
            c.say = "Search " + c.value;
            return c;
        }

        // ---- favourites / hide need an app
        if (fav && hit.idx >= 0 && hit.score >= 0.75f) {
            boolean remove = has(toks, REMW);
            c.type = remove ? FAV_REMOVE : FAV_ADD;
            c.app = hit.idx;
            c.score = hit.score;
            c.sure = hit.score >= 0.9f && hit.others == 0;
            c.say = (remove ? "Removed from favourites: " : "Favourite: ") + labels.get(hit.idx);
            return c;
        }
        if (has(toks, HIDEW) && hit.idx >= 0 && hit.score >= 0.75f) {
            c.type = HIDE;
            c.app = hit.idx;
            c.score = hit.score;
            c.sure = hit.score >= 0.9f && hit.others == 0;
            c.say = "Hidden: " + labels.get(hit.idx);
            return c;
        }

        // ---- plain app name ("WhatsApp", "Tinku YouTube")
        if (hit.idx >= 0 && hit.score >= 0.85f) {
            return openCmd(c, labels, hit, openVerb);
        }

        // ---- small talk commands
        if (has(toks, TIMEW)) {
            c.type = INFO;
            c.key = "time";
            c.score = 0.8f;
            c.sure = true;
            return c;
        }
        if (toks.size() <= 2 && has(toks, CLOSEW)) {
            c.type = CLOSE;
            c.score = 0.8f;
            c.sure = true;
            c.say = "Home";
            return c;
        }
        if (hit.idx >= 0 && hit.score >= 0.75f) {
            return openCmd(c, labels, hit, false);
        }

        c.say = "Didn't understand: " + heard;
        return c;
    }

    private static boolean wifiPair(List<Tok> toks) {
        for (int i = 0; i + 1 < toks.size(); i++) {
            String a = toks.get(i).lat;
            String b = toks.get(i + 1).lat;
            if ((a.equals("wi") || a.equals("wai")) && (b.equals("fi") || b.equals("fai"))) return true;
        }
        return false;
    }

    private static Cmd toggle(Cmd c, String key, String name, boolean hasOnOff, boolean on) {
        c.type = TOGGLE;
        c.key = key;
        c.say = name;
        c.on = on;
        c.hasOnOff = hasOnOff;
        c.score = 0.9f;
        c.sure = hasOnOff;
        return c;
    }

    private static Cmd openCmd(Cmd c, List<String> labels, Hit hit, boolean openVerb) {
        c.type = OPEN_APP;
        c.app = hit.idx;
        c.score = hit.score;
        c.sure = hit.score >= 0.9f && hit.others == 0 && openVerb;
        c.say = "Opening " + labels.get(hit.idx);
        return c;
    }
}
