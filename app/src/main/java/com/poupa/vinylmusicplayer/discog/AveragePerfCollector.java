package com.poupa.vinylmusicplayer.discog;

import android.content.Context;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author SC (soncaokim)
 */
public class AveragePerfCollector {
    final static boolean ENABLED = false;

    // TODO Take the map key type as template param, requiring Comparable
    final static SortedMap<String, Long> elapsedPerMarker = new TreeMap<>();
    final static List<String> markerByAppearanceOrder = new ArrayList<>();
    private static Long lastTick = System.nanoTime();

    public static void reset() {
        synchronized (elapsedPerMarker) {
            lastTick = System.nanoTime();
            markerByAppearanceOrder.clear();
            elapsedPerMarker.clear();
        }
    }

    public static void addMark() {
        if (!ENABLED) return;

        final StackTraceElement frame = Thread.currentThread().getStackTrace()[3];
        final String caller =
                frame.getClassName().replaceFirst("^com\\.poupa\\.vinylmusicplayer\\.", "") + "." + frame.getMethodName() + ":" + frame.getLineNumber();

        addMark(caller);
    }

    private static void addMark(@NonNull final String marker) {
        final long now = System.nanoTime();
        synchronized (markerByAppearanceOrder) {
            if (!markerByAppearanceOrder.contains(marker)) {markerByAppearanceOrder.add(marker);}

            Long elapsed = elapsedPerMarker.get(marker);
            if (elapsed == null) {elapsed = 0L;}

            elapsed = saturatedAdd(elapsed, now - lastTick);
            elapsedPerMarker.put(marker, elapsed);
        }
        lastTick = now;
    }

    public static String getHtmlInfoString() {
        synchronized (markerByAppearanceOrder) {
            long totalElapsed = 0;
            for (long elapsed : elapsedPerMarker.values()) {totalElapsed = saturatedAdd(totalElapsed, elapsed);}

            final StringBuilder infoString = new StringBuilder();
            infoString.append("<pre>");
            for (final String marker : markerByAppearanceOrder) {
                long elapsed = elapsedPerMarker.get(marker);
                infoString.append(String.format(
                        Locale.getDefault(),
                        "%3d%%: %s<br/>",
                        elapsed * 100 / totalElapsed,
                        marker
                ));
            }
            infoString.append("</pre>");

            return infoString.toString();
        }
    }


    public static void showInfoDialog(@NonNull final Context context) {
        WebView webView = new WebView(context);
        webView.loadData(getHtmlInfoString(), "text/html", "utf-8");

        new AlertDialog.Builder(context)
                .setTitle(AveragePerfCollector.class.getSimpleName())
                .setView(webView)
                .setPositiveButton("OK", null)
                .create()
                .show();
    }

    private static long saturatedAdd(long a, long b) {
        return a + b; // TODO Handle overflow
    }
}
