package com.tinku.glasslauncher;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TinkuAnswer {
    private static final String[] EN_DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] NE_DAYS = {"आइतबार", "सोमबार", "मंगलबार", "बुधबार", "बिहीबार", "शुक्रबार", "शनिबार"};
    private static final String[] EN_MON = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static final String[] NE_MON = {"जनवरी", "फेब्रुअरी", "मार्च", "अप्रिल", "मे", "जुन", "जुलाई", "अगस्ट", "सेप्टेम्बर", "अक्टोबर", "नोभेम्बर", "डिसेम्बर"};

    private static final Pattern MATH = Pattern.compile(
        "(-?\\d+(?:\\.\\d+)?)\\s*(\\+|plus|jod\\S*|जोड\\S*|-|minus|ghata\\S*|घटा\\S*|x|\\*|×|times|into|gun\\S*|गुणा\\S*|/|÷|divided by|bhag\\S*|भाग\\S*)\\s*(-?\\d+(?:\\.\\d+)?)"
    );

    public static String clean(String heard) {
        if (heard == null) return "";
        return heard.trim();
    }

    public static String answer(String heard, boolean ne) {
        if (heard == null || heard.isEmpty()) return null;
        Matcher m = MATH.matcher(heard.toLowerCase(Locale.ROOT));
        if (m.find()) {
            double a = Double.parseDouble(m.group(1));
            double b = Double.parseDouble(m.group(3));
            String op = m.group(2);
            double r = 0;
            if (op.equals("+") || op.startsWith("plus") || op.startsWith("jod") || op.startsWith("जोड")) r = a + b;
            else if (op.equals("-") || op.startsWith("minus") || op.startsWith("ghata") || op.startsWith("घटा")) r = a - b;
            else if (op.equals("x") || op.equals("*") || op.equals("×") || op.startsWith("times") || op.startsWith("into") || op.startsWith("gun") || op.startsWith("गुणा")) r = a * b;
            else {
                if (b == 0) return ne ? "शून्यले भाग गर्न मिल्दैन" : "You cannot divide by zero";
                r = a / b;
            }
            return ne ? "उत्तर " + r + " हो" : "Result is " + r;
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
            return "अहिले " + h + " बजेर " + mi + " मिनेट भयो, " + NE_DAYS[dow];
        }
        return String.format(Locale.US, "It is %d:%02d, %s", h, mi, EN_DAYS[dow]);
    }
}
