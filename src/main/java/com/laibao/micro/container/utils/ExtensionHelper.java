package com.laibao.micro.container.utils;

import com.laibao.micro.container.ExtensionLoader;
import com.laibao.micro.container.annotation.SPI;

import java.util.HashMap;
import java.util.Map;

import static com.laibao.micro.container.constants.CommonConstants.NAME_PATTERN;

public interface ExtensionHelper {

    static <T> void checkExtensionType(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }

        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type.getName() + ") is not interface!");
        }

        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("type(" + type.getName() + ") is not a extension, because WITHOUT @Extension Annotation!");
        }
    }


    static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }


    static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        classLoader = ExtensionLoader.class.getClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return classLoader;
    }


    static boolean isValidExtName(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }


    /**
     * <code>
     * "attrib1=value1,attrib2=value2,isProvider,order=3" =>
     * {"attrib1"="value1", "attrib2"="value2", "isProvider"="", "order"="3"}
     * </code>
     */
    static Map<String, String> parseExtAttribute(String attribute) {
        Map<String, String> ret = new HashMap();
        if (attribute == null || attribute.length() == 0) {
            return ret;
        }

        String[] parts = attribute.split(",");
        for (String part : parts) {
            part = part.trim();
            int idx = part.indexOf('=');
            if (idx > 0) {
                ret.put(part.substring(0, idx).trim(),
                        part.substring(idx + 1).trim());
            } else {
                ret.put(part, "");
            }
        }

        return ret;
    }
}
