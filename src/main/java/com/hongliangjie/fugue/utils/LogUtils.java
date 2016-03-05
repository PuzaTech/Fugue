package com.hongliangjie.fugue.utils;

/**
 * Created by hongliangjie on 3/4/2016.
 */
public class LogUtils {
    public static Double logAddExp(Double x1, Double x2){
        Double maximum = Math.max(x1, x2);
        Double minimum = Math.min(x1, x2);

        if (Math.abs(maximum - minimum) > 30){
            return maximum;
        }

        return maximum + Math.log(1 + Math.exp(minimum - maximum));
    }
}
