package com.u91porn.utils;

import android.os.Environment;

/**
 * 常量
 *
 * @author flymegoc
 * @date 2017/11/19
 * @describe
 */

public class Constants {
    public static final String DB_NAME = "91porn";
    public static final int APK_DOWNLOAD_NOTIFICATION_ID = 1;
    public static final int VIDEO_DOWNLOAD_NOTIFICATION_ID = 2;
    public static final int PROXY_MAX_PORT=65535;
    /**
     * 永久访问地址
     */
    public final static String NEVER_GO_ADDRESS = "http://91porn.com/";
    /**
     * 临时可访问地址
     */
    public static final String BASE_URL = "http://91.91p23.space/";
    public static final String ROOT_FOLDER = Environment.getExternalStorageDirectory() + "/91porn/";
    public static final String DOWNLOAD_PATH = ROOT_FOLDER + "video/";
    public static final String DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "yyyy-MM-dd HH:mm:ss";
    public static final String EXPORT_FILE = ROOT_FOLDER + "export.txt";

    public static final int USER_LOGIN_REQUEST_CODE = 1;
}
