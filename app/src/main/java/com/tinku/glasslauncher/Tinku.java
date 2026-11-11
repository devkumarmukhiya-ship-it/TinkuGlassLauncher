package com.tinku.glasslauncher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/**
 * Thin wrapper around Android's SpeechRecognizer.
 *
 * The microphone is only open between start() and stop() / the end of one
 * utterance. Nothing here runs by itself: the activity decides when to call
 * start() and always calls stop() when the launcher leaves the screen.
 */
public class Tinku {

    public interface Listener {
        /** Live text while the person is still talking. */
        void onPartial(ArrayList<String> alts);

        /** Final text of one utterance (best guess first). */
        void onFinal(ArrayList<String> alts);

        /** true when the mic opened, false when it closed. */
        void onState(boolean listening);

        /** Something went wrong. fatal = Tinku gave up and the mic is closed. */
        void onProblem(String message, boolean fatal);
    }

    private final Context ctx;
    private final Listener listener;
    private final Handler h = new Handler(Looper.getMainLooper());

    private SpeechRecognizer sr;
    private boolean active = false;
    private boolean continuous = false;
    private String lang = "ne-NP";
    private int gen = 0;
    private int fails = 0;

    public Tinku(Context ctx, Listener listener) {
        this.ctx = ctx;
        this.listener = listener;
    }

    public static boolean available(Context c) {
        try {
            return SpeechRecognizer.isRecognitionAvailable(c);
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isContinuous() {
        return active && continuous;
    }

    /** Open the mic. continuous = keep listening utterance after utterance. */
    public void start(boolean continuous, String lang) {
        if (active) end(false);
        this.continuous = continuous;
        this.lang = lang;
        this.active = true;
        this.fails = 0;
        listener.onState(true);
        open();
    }

    /** Close the mic right now. */
    public void stop() {
        if (!active) {
            destroy();
            return;
        }
        end(true);
    }

    /** The current utterance was already acted on: drop it and carry on. */
    public void consumed() {
        if (!active) return;
        gen++;
        destroy();
        if (continuous) {
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    open();
                }
            }, 350);
        } else {
            end(true);
        }
    }

    private void end(boolean notify) {
        boolean was = active;
        active = false;
        gen++;
        h.removeCallbacksAndMessages(null);
        destroy();
        if (was && notify) listener.onState(false);
    }

    private void destroy() {
        if (sr != null) {
            try {
                sr.cancel();
            } catch (Throwable ignored) {
            }
            try {
                sr.destroy();
            } catch (Throwable ignored) {
            }
            sr = null;
        }
    }

    private void open() {
        if (!active) return;
        destroy();
        final int myGen = ++gen;
        try {
            sr = SpeechRecognizer.createSpeechRecognizer(ctx);
        } catch (Throwable t) {
            problem("Speech service not available", true);
            return;
        }
        sr.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (myGen != gen || !active) return;
                ArrayList<String> l = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                if (l != null && !l.isEmpty()) {
                    fails = 0;
                    listener.onPartial(l);
                }
            }

            @Override
            public void onResults(Bundle results) {
                if (myGen != gen || !active) return;
                fails = 0;
                ArrayList<String> l = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                if (l != null && !l.isEmpty()) listener.onFinal(l);
                if (myGen != gen || !active) return;
                if (continuous) {
                    restartSoon(250);
                } else {
                    end(true);
                }
            }

            @Override
            public void onError(int error) {
                if (myGen != gen || !active) return;
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        // Silence or noise. Normal when waiting for "Tinku".
                        if (continuous) {
                            restartSoon(300);
                        } else {
                            listener.onProblem("Didn't hear anything", false);
                            end(true);
                        }
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        problem("Microphone permission is off", true);
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    case SpeechRecognizer.ERROR_SERVER:
                        fails++;
                        if (fails >= 4 || !continuous) {
                            problem("Internet needed for voice. Tinku stopped", true);
                        } else {
                            restartSoon(2500);
                        }
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    case SpeechRecognizer.ERROR_CLIENT:
                    default:
                        fails++;
                        if (fails >= 6) {
                            problem("Voice service keeps failing. Tinku stopped", true);
                        } else {
                            restartSoon(900);
                        }
                        break;
                }
            }
        });

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.getPackageName());
        try {
            sr.startListening(i);
        } catch (Throwable t) {
            problem("Could not start the microphone", true);
        }
    }

    private void restartSoon(long ms) {
        final int g = gen;
        destroy();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (active && g == gen) open();
            }
        }, ms);
    }

    private void problem(String msg, boolean fatal) {
        if (fatal) end(true);
        listener.onProblem(msg, fatal);
    }
}
