package com.hongliangjie.fugue.utils;

/**
 * Created by hongliangjie on 3/4/2016.
 */
public class LogUtils {
    public static Double logSumTwo(Double x1, Double x2){
        Double maximum = Math.max(x1, x2);
        Double minimum = Math.min(x1, x2);

        if (Math.abs(maximum - minimum) > 30){
            return maximum;
        }

        return maximum + Math.log(1 + Math.exp(minimum - maximum));
    }

    public static Double logSumAll(Double[] x){
        Double maximum = null;
        for (int i = 0; i < x.length; i++){
            if (maximum == null) {
                maximum = x[i];
            }
            else{
                if (x[i] > maximum)
                    maximum = x[i];
            }
        }

        Double result = 0.0;

        for (int i =  0; i < x.length; i++){
            result += Math.exp(x[i] - maximum);
        }
        return maximum + Math.log(result);
    }
}
