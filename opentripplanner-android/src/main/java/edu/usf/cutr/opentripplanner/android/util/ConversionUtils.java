/*
 * Copyright 2012 University of South Florida
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package edu.usf.cutr.opentripplanner.android.util;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.v092snapshot.api.model.Itinerary;
import org.opentripplanner.v092snapshot.api.model.Leg;

import android.content.Context;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import edu.usf.cutr.opentripplanner.android.OTPApp;
import edu.usf.cutr.opentripplanner.android.R;

/**
 * @author Khoa Tran
 */

public class ConversionUtils {

    public static double getDuration(String startTimeText, String endTimeText) {
        double duration = 0;
        DateFormat formatter;
        Date startTime = null, endTime = null;
//		2012-03-09T22:46:00-05:00
        formatter = new SimpleDateFormat(OTPApp.FORMAT_OTP_SERVER_DATE_RESPONSE);
        try {
            startTime = (Date) formatter.parse(startTimeText);
            endTime = (Date) formatter.parse(endTimeText);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        duration = 0;
        if (startTime != null && endTime != null) {
            duration = (endTime.getTime() - startTime.getTime()) / 1000;
        }
        return duration;
    }

    /**
     *
     * @param meters
     * @param applicationContext
     * @return
     */
    public static String getFormattedDistance(Double meters, Context applicationContext) {
        String text = "";

        if (meters < 1000) {
            text += String.format(OTPApp.FORMAT_DISTANCE_METERS, meters) + " " + applicationContext
                    .getResources().getString(R.string.distance_meters);
        } else {
            meters = meters / 1000;
            text += String.format(OTPApp.FORMAT_DISTANCE_KILOMETERS, meters) + " "
                    + applicationContext.getResources().getString(R.string.distance_kilometers);
        }
        return text;
    }

    /**
     *
     * @param sec
     * @param applicationContext
     * @return
     */
    public static String getFormattedDurationText(long sec, Context applicationContext) {
        String text = "";
        long h = sec / 3600;
        if (h >= 24) {
            return null;
        }
        long m = (sec % 3600) / 60;
        long s = (sec % 3600) % 60;
        if (h > 0) {
            text += Long.toString(h) + applicationContext.getResources()
                    .getString(R.string.short_hours);
        }
        text += Long.toString(m) + applicationContext.getResources()
                .getString(R.string.short_minutes);
        text += Long.toString(s) + applicationContext.getResources()
                .getString(R.string.short_seconds);
        return text;
    }

    /**
     *
     * @param sec
     * @return
     */
    public static String getFormattedDurationTextNoSeconds(long sec, Context applicationContext) {
        String text = "";
        long h = sec / 3600;
        if (h >= 24) {
            return null;
        }
        long m = (sec % 3600) / 60;
        if (h > 0) {
            text += Long.toString(h) + applicationContext.getResources()
                    .getString(R.string.short_hours);
            text += " " + Long.toString(m) + applicationContext.getResources()
                    .getString(R.string.short_minutes);
        } else {
            if (m == 0) {
                text += Long.toString(m) + " " + applicationContext.getResources()
                        .getString(R.string.long_minutes);
            } else {
                text += Long.toString(m) + " " + applicationContext.getResources()
                        .getString(R.string.long_minutes);
            }
        }
        return text;
    }

    public static List<Itinerary> fixTimezoneOffsets(List<Itinerary> itineraries,
            boolean useDeviceTimezone) {
        int agencyTimeZoneOffset = 0;
        boolean containsTransitLegs = false;

        if ((itineraries != null) && !itineraries.isEmpty()) {
            ArrayList<Itinerary> itinerariesFixed = new ArrayList<Itinerary>(itineraries);

            for (Itinerary it : itinerariesFixed) {
                for (Leg leg : it.legs) {
                    if ((TraverseMode.valueOf((String) leg.mode)).isTransit()
                            && !containsTransitLegs) {
                        containsTransitLegs = true;
                    }
                    if (leg.getAgencyTimeZoneOffset() != 0) {
                        agencyTimeZoneOffset = leg.getAgencyTimeZoneOffset();
                        //If agencyTimeZoneOffset is different from 0, route contains transit legs
                        containsTransitLegs = true;
                        break;
                    }
                }
            }

            if (useDeviceTimezone || !containsTransitLegs) {
                agencyTimeZoneOffset = TimeZone.getDefault()
                        .getOffset(itinerariesFixed.get(0).startTime.getTimeInMillis());
            }

            if (agencyTimeZoneOffset != 0) {
                for (Itinerary it : itinerariesFixed) {
                    for (Leg leg : it.legs) {
                        leg.agencyTimeZoneOffset = agencyTimeZoneOffset;
                    }
                }
            }

            return itinerariesFixed;
        } else {
            return itineraries;
        }
    }

    public static String getTimeWithContext(Context applicationContext, int offsetGMT, long time,
            boolean inLine) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(applicationContext);
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(applicationContext);
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        cal.setTimeInMillis(time);

        String noDeviceTimezoneNote = "";
        if (offsetGMT != TimeZone.getDefault().getOffset(time)) {
            noDeviceTimezoneNote = "GMT";
            if (offsetGMT != 0) {
                noDeviceTimezoneNote += offsetGMT / 3600000;
            }
        }

        cal.add(Calendar.MILLISECOND, offsetGMT);

        if (inLine) {
            if (ConversionUtils.isToday(cal)) {
                return (" " + applicationContext.getResources().getString(R.string.connector_time)
                        + " " + timeFormat.format(cal.getTime()) + " " + noDeviceTimezoneNote);
            } else if (ConversionUtils.isTomorrow(cal)) {
                return (" " + applicationContext.getResources().getString(R.string.next_day) + " "
                        + applicationContext.getResources().getString(R.string.connector_time) + " "
                        + timeFormat.format(cal.getTime()) + " " + noDeviceTimezoneNote);
            } else {
                return (" " + applicationContext.getResources()
                        .getString(R.string.connector_full_date) + " " + dateFormat
                        .format(cal.getTime()) + " " + applicationContext.getResources()
                        .getString(R.string.connector_time) + " " + timeFormat.format(cal.getTime())
                        + " " + noDeviceTimezoneNote);
            }
        } else {
            if (ConversionUtils.isToday(cal)) {
                return (timeFormat.format(cal.getTime()) + " " + noDeviceTimezoneNote);
            } else if (ConversionUtils.isTomorrow(cal)) {
                return (" " + timeFormat.format(cal.getTime()) + ", "
                        + applicationContext.getResources().getString(R.string.next_day) + " "
                        + noDeviceTimezoneNote);
            } else {
                return (timeFormat.format(cal.getTime()) + ", " + dateFormat.format(cal.getTime())
                        + " " + noDeviceTimezoneNote);
            }
        }

    }

    public static boolean isToday(Calendar cal) {
        Calendar actualTime = Calendar.getInstance();

        return (actualTime.get(Calendar.ERA) == cal.get(Calendar.ERA) &&
                actualTime.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                actualTime.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR));
    }

    public static boolean isTomorrow(Calendar cal) {
        Calendar tomorrowTime = Calendar.getInstance();
        tomorrowTime.add(Calendar.DAY_OF_YEAR, 1);

        return (tomorrowTime.get(Calendar.ERA) == cal.get(Calendar.ERA) &&
                tomorrowTime.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                tomorrowTime.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR));
    }

}
