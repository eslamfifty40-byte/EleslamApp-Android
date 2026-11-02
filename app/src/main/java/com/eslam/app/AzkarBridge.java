package com.eslam.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import java.util.Calendar;

public class AzkarBridge {
    private final Context ctx;

        public AzkarBridge(Context ctx) {
                this.ctx = ctx;
                    }

                        @JavascriptInterface
                            public void scheduleDaily(String id, int hour, int minute, String title, String body) {
                                    try {
                                                AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

                                                            Intent intent = new Intent(ctx, ReminderReceiver.class);
                                                                        intent.putExtra("id", id);
                                                                                    intent.putExtra("hour", hour);
                                                                                                intent.putExtra("minute", minute);
                                                                                                            intent.putExtra("title", title);
                                                                                                                        intent.putExtra("body", body);

                                                                                                                                    PendingIntent pi = PendingIntent.getBroadcast(
                                                                                                                                                        ctx, id.hashCode(), intent,
                                                                                                                                                                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                                                                                                                                                                                        Calendar cal = Calendar.getInstance();
                                                                                                                                                                                                    cal.set(Calendar.HOUR_OF_DAY, hour);
                                                                                                                                                                                                                cal.set(Calendar.MINUTE, minute);
                                                                                                                                                                                                                            cal.set(Calendar.SECOND, 0);
                                                                                                                                                                                                                                        cal.set(Calendar.MILLISECOND, 0);
                                                                                                                                                                                                                                                    if (cal.before(Calendar.getInstance())) {
                                                                                                                                                                                                                                                                    cal.add(Calendar.DAY_OF_YEAR, 1);
                                                                                                                                                                                                                                                                                }

                                                                                                                                                                                                                                                                                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                                                                                                                                                                                                                                                                                                  
                                                                                                                                                                                                                                                                                    } catch (SecurityException ignored) {
                                                                                                                                                                                                                                                                                                                // المستخدم ماسمحش بالمنبهات الدقيقة
                                                                                                                                                                                                                                                                                                                        }
                                                                                                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                                                                                                            }