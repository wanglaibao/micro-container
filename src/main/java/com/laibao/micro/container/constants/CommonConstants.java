package com.laibao.micro.container.constants;

import java.util.regex.Pattern;

public interface CommonConstants {

    String EXTENSION_CONF_DIRECTORY = "META-INF/extensions/";

    String PREFIX_ADAPTIVE_CLASS = "*";

    String PREFIX_WRAPPER_CLASS = "+";

    Pattern NAME_SEPARATOR = Pattern.compile("\\s*,+\\s*");

    Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
}
