package com.laibao.micro.container.annotation;

import java.lang.annotation.*;

/**
 * 把一个接口标识成扩展点。
 * <p/>
 * 没有此注释的接口{@link com.laibao.micro.container.ExtensionLoader}会拒绝接管。
 *
 * @see com.laibao.micro.container.ExtensionLoader
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {
    /**
     * the default extension name.
     *
     * @since 1.0
     */
    String value() default "";
}
