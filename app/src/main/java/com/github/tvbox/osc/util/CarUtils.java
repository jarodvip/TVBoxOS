package com.github.tvbox.osc.util;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.orhanobut.hawk.Hawk;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 车机（DiLink / 方程豹 / 比亚迪）识别与适配工具。
 *
 * <p>说明：不同车型的 Build 字段差异较大，无法保证 100% 自动识别，因此提供三态开关：</p>
 * <ul>
 *     <li>{@link HawkConfig#CAR_MODE} = -1：自动识别（默认）</li>
 *     <li>{@link HawkConfig#CAR_MODE} = 0：强制关闭（按普通电视处理）</li>
 *     <li>{@link HawkConfig#CAR_MODE} = 1：强制开启（按车机触屏处理）</li>
 * </ul>
 */
public final class CarUtils {

    private static final String KEY_CAR_MODE = HawkConfig.CAR_MODE;

    /** 自动识别时用于匹配的厂商/型号关键字 */
    private static final String[] CAR_KEYWORDS = {
            "byd", "fangchengbao", "fangcheng", "方程豹", "dilink",
            "bao5", "bao8", "tai3", "tai7", "豹5", "豹8", "钛3", "钛7"
    };

    private static Boolean sCarCached;

    private CarUtils() {
    }

    /**
     * 是否按车机模式处理。结果会缓存，切换模式开关后请调用 {@link #reset()}。
     */
    public static boolean isCar() {
        if (sCarCached != null) return sCarCached;
        int mode = -1;
        try {
            mode = Hawk.get(KEY_CAR_MODE, -1);
        } catch (Throwable ignored) {
        }
        if (mode == 1) {
            sCarCached = true;
        } else if (mode == 0) {
            sCarCached = false;
        } else {
            sCarCached = detectCar();
        }
        return sCarCached;
    }

    public static void reset() {
        sCarCached = null;
    }

    private static boolean detectCar() {
        String[] fields = {
                Build.MANUFACTURER, Build.BRAND, Build.MODEL,
                Build.PRODUCT, Build.DEVICE, Build.BOARD,
                getSystemProperty("ro.product.brand"),
                getSystemProperty("ro.product.manufacturer"),
                getSystemProperty("ro.product.model"),
                getSystemProperty("ro.byd.product.model")
        };
        for (String field : fields) {
            if (TextUtils.isEmpty(field)) continue;
            String value = field.toLowerCase(Locale.ROOT);
            for (String keyword : CAR_KEYWORDS) {
                if (value.contains(keyword)) return true;
            }
        }
        return false;
    }

    /**
     * 车机是固定横屏 + 触屏操作，不应按电视遥控器逻辑隐藏返回键/锁屏键。
     */
    public static boolean isTouchCar(Context context) {
        return isCar() && context != null;
    }

    private static String getSystemProperty(String key) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class);
            return (String) get.invoke(null, key);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
