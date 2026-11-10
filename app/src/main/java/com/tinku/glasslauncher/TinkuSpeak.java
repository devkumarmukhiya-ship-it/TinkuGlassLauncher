package com.tinku.glasslauncher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class TinkuSpeak {
    private TextToSpeech tts;
    private boolean ready = false;

    public TinkuSpeak(Context c) {
        tts = new TextToSpeech(c.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                ready = true;
            }
        });
    }

    public void speak(String text, String lang, Runnable done) {
        if (!ready || tts == null) {
            if (done != null) done.run();
            return;
        }
        tts.setLanguage("ne".equals(lang) ? new Locale("ne", "NP") : Locale.US);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tinku_speak");
        if (done != null) new Handler(Looper.getMainLooper()).postDelayed(done, 2000);
    }

    public void stop() {
        if (tts != null) tts.stop();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
