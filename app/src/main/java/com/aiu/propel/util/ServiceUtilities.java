package com.aiu.propel.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Rohan on 11/2/2015.
 */
public class ServiceUtilities {

    public String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault() );
        Date date = new Date();
        return dateFormat.format( date );
    }

    public String getTimeText( int seconds ) {

        int minutes = seconds / 60;
        int secs = seconds % 60;
        int hrs = minutes / 60;
        minutes = minutes % 60;
        DecimalFormat decFormat = new DecimalFormat( "##" );
        return decFormat.format( hrs ) + ":" + decFormat.format( minutes ) + ":" + decFormat.format( secs );
    }
}
