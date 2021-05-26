package com.philpy.utils;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListUtil {
    public static int getCount(List<String> list, String s) {
        int count = 0;
        for (String s1 : list) {
            if (s.equals(s1)) {
                count++;
            }
        }
        return count;
    }
}
