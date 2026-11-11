package com.tinku.glasslauncher;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline answers Tinku can speak (no internet, no AI). Returns null when it has no answer. */
public class TinkuAnswer {

    private static final String[] EN_DAYS = { "Sunday", "Monday", "Tuesday", "Wednesday",
        "Thursday", "Friday", "Saturday" };
    private static final String[] NE_DAYS = { "आइतबार", "सोमबार", "मंगलबार", "बुधबार",
        "बिहीबार", "शुक्रबार", "शनिबार" };
    private static final String[] EN_MON = { "January", "February", "March", "April", "May",
        "June", "July", "August", "September", "October", "November", "December" };
    private static final String[] NE_MON = { "जनवरी", "फेब्रुअरी", "मार्च", "अप्रिल", "मे",
        "जुन", "जुलाई", "अगस्ट", "सेप्टेम्बर", "अक्टोबर", "नोभेम्बर", "डिसेम्बर" };

    private static final Pattern MATH = Pattern.compile(
        "(-?\\d+(?:\\.\\d+)?)\\s*(\\+|plus|jod\\S*|जोड\\S*|-|minus|ghata\\S*|घटा\\S*|"
        + "x|\\*|×|times|into|gun\\S*|गुणा\\S*|/|÷|divided by|bhag\\S*|भाग\\S*)\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern WAKE_LAT = Pattern.compile("^(t|d|p)h?[ie]+n[gk]h?[aeiouy]+s?$");
    private static final Pattern WAKE_DEV = Pattern.compile("^[टतदप][िीे][ंनङ]?[्]?[कग][ुूोाी]?$");

    /** The sentence without the wake word "Tinku". */
    public static String clean(String heard) {
        if (heard == null) return "";
        StringBuilder b = new StringBuilder();
        String[] w = heard.trim().split("\\s+");
        for (int i = 0; i < w.length; i++) {
            String t = w[i];
            String low = t.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            if (low.length() > 0 && !low.equals("think") && !low.equals("thing")
                && !low.equals("thank") && !low.equals("thanks") && WAKE_LAT.matcher(low).matches()) continue;
            if (WAKE_DEV.matcher(t).matches()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(t);
        }
        return b.toString().trim();
    }

    private static String digits(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '०' && c <= '९') b.append((char) ('0' + (c - '०'))); else b.append(c);
        }
        return b.toString();
    }

    private static boolean any(String s, String... keys) {
        for (int i = 0; i < keys.length; i++) if (s.contains(keys[i])) return true;
        return false;
    }

    static String fmt(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e12) return String.valueOf((long) d);
        String r = String.format(Locale.US, "%.2f", d);
        if (r.indexOf('.') >= 0) r = r.replaceAll("0+$", "").replaceAll("\\.$", "");
        return r;
    }

    public static String answer(String heard, boolean ne) {
        String s = digits(clean(heard)).toLowerCase(Locale.ROOT);
        if (s.length() == 0) return null;

        Matcher m = MATH.matcher(s);
        if (m.find()) {
            double a = Double.parseDouble(m.group(1));
            double b = Double.parseDouble(m.group(3));
            String op = m.group(2);
            double r;
            String word;
            if (op.equals("+") || op.startsWith("plus") || op.startsWith("jod") || op.startsWith("जोड")) {
                r = a + b; word = "plus";
            } else if (op.equals("-") || op.startsWith("minus") || op.startsWith("ghata") || op.startsWith("घटा")) {
                r = a - b; word = "minus";
            } else if (op.equals("x") || op.equals("*") || op.equals("×") || op.startsWith("times")
                || op.startsWith("into") || op.startsWith("gun") || op.startsWith("गुणा")) {
                r = a * b; word = "times";
            } else {
                if (b == 0) return ne ? "शून्यले भाग गर्न मिल्दैन" : "You can't divide by zero";
                r = a / b; word = "divided by";
            }
            return ne ? "उत्तर " + fmt(r) + " हो"
                : fmt(a) + " " + word + " " + fmt(b) + " is " + fmt(r);
        }
        if (any(s, "thank", "dhanyabad", "dhanyawad", "धन्यवाद", "शुक्रिया")) {
            return ne ? "स्वागत छ" : "You're welcome";
        }
        if (any(s, "your name", "who are you", "timro naam", "timro nam", "तिम्रो नाम",
            "तपाईंको नाम", "तिमी को", "तिमी कोहो")) {
            return ne ? "म टिन्कु हुँ, तपाईंको फोनको सहायक" : "I'm Tinku, your phone assistant";
        }
        if (any(s, "how are you", "kasto cha", "kasto chha", "कस्तो छ", "के छ", "सन्चै")) {
            return ne ? "म ठीक छु, धन्यवाद" : "I'm good, thanks";
        }
        if (any(s, "hello", "namaste", "namaskar", "नमस्ते", "नमस्कार", "hey", "hi")
            && s.split("\\s+").length <= 2) {
            return ne ? "नमस्ते! के गरिदिऊँ?" : "Hello! What can I do?";
        }
        return null;
    }

    public static String timeSpoken(boolean ne) {
        Calendar c = Calendar.getInstance();
        int h = c.get(Calendar.HOUR);
        if (h == 0) h = 12;
        int mi = c.get(Calendar.MINUTE);
        int dow = c.get(Calendar.DAY_OF_WEEK) - 1;
        int dom = c.get(Calendar.DAY_OF_MONTH);
        int mon = c.get(Calendar.MONTH);
        if (ne) {
            String t = mi == 0 ? "अहिले " + h + " बज्यो" : "अहिले " + h + " बजेर " + mi + " मिनेट भएको छ";
            return t + ", आज " + NE_DAYS[dow] + ", " + dom + " " + NE_MON[mon];
        }
        String ap = c.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        return String.format(Locale.US, "It's %d:%02d %s, %s, %d %s", h, mi, ap,
            EN_DAYS[dow], dom, EN_MON[mon]);
    }
}
