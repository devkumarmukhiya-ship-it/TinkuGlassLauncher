package com.tinku.glasslauncher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

/** Text-to-speech for Tinku. Calls done on the main thread when speech ends (or can't happen). */
public class TinkuSpeak {

    private final Handler h = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ready = false;
    private boolean failed = false;
    private String pText;
    private String pLang;
    private Runnable pDone;
    private Runnable curDone;
    private String curId = "";
    private int seq = 0;

    public TinkuSpeak(Context c) {
        try {
            tts = new TextToSpeech(c.getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.SUCCESS) {
                        failed = true;
                    } else {
                        ready = true;
                        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String id) {
                            }

                            @Override
                            public void onDone(String id) {
                                finishLater(id);
                            }

                            @Override
                            public void onError(String id) {
                                finishLater(id);
                            }
                        });
                    }
                    if (pText != null) {
                        String t = pText;
                        String l = pLang;
                        Runnable d = pDone;
                        pText = null;
                        pDone = null;
                        speak(t, l, d);
                    }
                }
            });
        } catch (Throwable t) {
            failed = true;
        }
    }

    private void finishLater(final String id) {
        h.post(new Runnable() {
            @Override
            public void run() {
                finish(id);
            }
        });
    }

    private void finish(String id) {
        if (!id.equals(curId)) return;
        Runnable d = curDone;
        curDone = null;
        curId = "";
        if (d != null) d.run();
    }

    private boolean pick(String lang) {
        int ok = TextToSpeech.LANG_AVAILABLE;
        if ("ne".equals(lang)) {
            Locale[] tries = { new Locale("ne", "NP"), new Locale("ne"), new Locale("hi", "IN") };
            for (int i = 0; i < tries.length; i++) {
                if (tts.isLanguageAvailable(tries[i]) >= ok) {
                    tts.setLanguage(tries[i]);
                    return true;
                }
            }
            return false;           // no voice that can read Devanagari
        }
        Locale[] tries = { new Locale("en", "IN"), Locale.US };
        for (int i = 0; i < tries.length; i++) {
            if (tts.isLanguageAvailable(tries[i]) >= ok) {
                tts.setLanguage(tries[i]);
                return true;
            }
        }
        return true;
    }

    /** lang = "ne" or "en". */
    public void speak(String text, String lang, final Runnable done) {
        if (failed || tts == null) {
            if (done != null) h.post(done);
            return;
        }
        if (!ready) {
            pText = text;
            pLang = lang;
            pDone = done;
            return;
        }
        try {
            if (!pick(lang)) {
                if (done != null) h.post(done);
                return;
            }
            final String id = "t" + (++seq);
            curId = id;
            curDone = done;
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish(id);     // safety net if the engine never reports back
                }
            }, 15000);
        } catch (Throwable t) {
            if (done != null) h.post(done);
        }
    }

    public void stop() {
        pText = null;
        pDone = null;
        curDone = null;
        curId = "";
        try {
            if (tts != null) tts.stop();
        } catch (Throwable ignored) {
        }
    }

    public void shutdown() {
        stop();
        try {
            if (tts != null) tts.shutdown();
        } catch (Throwable ignored) {
        }
        tts = null;
    }
}
