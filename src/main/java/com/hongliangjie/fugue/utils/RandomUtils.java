package com.hongliangjie.fugue.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by liangjie on 3/3/16.
 */
public class RandomUtils {
    public static Double NativeRandom(){
        return ThreadLocalRandom.current().nextDouble();
    }
}
