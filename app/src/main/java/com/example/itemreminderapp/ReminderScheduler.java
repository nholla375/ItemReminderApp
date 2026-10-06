package com.example.itemreminderapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class ReminderScheduler {
    private ReminderScheduler() {
    }

    public static void scheduleReminder(Context context, String itemId, String itemName, long reminderTimeMillis) {
        if (context == null || itemId == null || itemId.isEmpty() || reminderTimeMillis <= System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildPendingIntent(context, itemId, itemName);
        alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
    }

    public static void cancelReminder(Context context, String itemId) {
        if (context == null || itemId == null || itemId.isEmpty()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildPendingIntent(context, itemId, "");
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private static PendingIntent buildPendingIntent(Context context, String itemId, String itemName) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("itemName", itemName);
        intent.putExtra("itemId", itemId);
        return PendingIntent.getBroadcast(
                context,
                itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
