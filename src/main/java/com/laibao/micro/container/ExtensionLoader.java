package com.laibao.micro.container;

import com.laibao.micro.container.annotation.SPI;
import com.laibao.micro.container.utils.Holder;
import com.laibao.micro.container.helper.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.laibao.micro.container.constants.CommonConstants.*;
import static com.laibao.micro.container.helper.ExtensionHelper.*;

/**
 * 加载和管理扩展。
 * <p/>
 * <ul>
 * <li>管理的扩展实例是<b>单例</b>。
 * <li>Wrapper实例每次获得扩展实例重新创建，并Wrap到扩展实例上。
 * </ul>
 *
 * @see com.laibao.micro.container.annotation.SPI
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Service implementation of JDK5</a>
 * @since 1.0
 */
public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap();

    /**
     * {@link ExtensionLoader}的工厂方法。
     *
     * @param type 扩展点接口类型
     * @param <T>  扩展点类型
     * @return {@link ExtensionLoader}实例
     * @throws IllegalArgumentException 参数为<code>null</code>；
     *                                  或是扩展点接口上没有{@link com.laibao.micro.container.annotation.SPI}注解。
     * @since 1.0
     */
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        checkExtensionType(type);
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }


    public T getExtension(String name) {
        if (StringHelper.isEmpty(name)){
            throw new IllegalArgumentException("Extension name == null");
        }
        return getExtension(name, new HashMap<>(), new ArrayList<>());
    }

    public T getExtension(String name, Map<String, String> properties) {
        if (StringHelper.isEmpty(name)){
            throw new IllegalArgumentException("Extension name == null");
        }
        return getExtension(name, properties, new ArrayList<>());
    }

    public T getExtension(Map<String, String> properties) {
        String name = properties.get(type.getName());
        if (StringHelper.isEmpty(name)) {
            name = defaultExtension;
        }
        return getExtension(name, properties, new ArrayList<String>());
    }

    public T getExtension(String name, List<String> wrappers) {
        if (wrappers == null) {
            throw new IllegalArgumentException("wrappers == null");
        }
        return getExtension(name, new HashMap<>(), wrappers);
    }

    public T getExtension(String name, Map<String, String> properties, List<String> wrappers) {
        if (StringHelper.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        T extension = createExtension(name, properties);
        injectExtension(extension, properties);
        return createWrapper(extension, properties, wrappers);
    }

    /**
     * 返回缺省的扩展。
     *
     * @throws IllegalStateException 指定的扩展没有设置缺省扩展点
     * @since 1.0
     */
    public T getDefaultExtension() {
        if (null == defaultExtension || defaultExtension.length() == 0) {
            throw new IllegalStateException("No default extension on extension " + type.getName());
        }
        return getExtension(defaultExtension);
    }

    /**
     * 返回缺省的扩展。
     *
     * @param wrappers 返回的实例上，要启用的Wrapper。
     * @throws IllegalStateException 指定的扩展没有设置缺省扩展点
     * @since 1.0
     */
    public T getDefaultExtension(List<String> wrappers) {
        if (null == defaultExtension || defaultExtension.length() == 0) {
            throw new IllegalStateException("No default extension on extension " + type.getName());
        }
        return getExtension(defaultExtension, wrappers);
    }

    /**
     * 检查是否有指定名字的扩展。
     *
     * @param name 扩展名
     * @return 有指定名字的扩展，则<code>true</code>，否则<code>false</code>。
     * @throws IllegalArgumentException 参数为<code>null</code>或是空字符串。
     * @since 1.0
     */
    public boolean hasExtension(String name) {
        if (name == null || name.length() == 0){
            throw new IllegalArgumentException("Extension name == null");
        }
        return getExtensionClasses().get(name) != null;
    }

    /**
     * 检查是否有指定缺省的扩展。
     *
     * @return 有指定缺省的扩展，则<code>true</code>，否则<code>false</code>。
     * @since 1.0
     */
    public boolean hasDefaultExtension() {
        return !(null == defaultExtension || defaultExtension.length() == 0);

    }

    /**
     * 获取扩展点实现的所有扩展点名。
     *
     * @since 1.0
     */
    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> classes = getExtensionClasses();
        return Collections.unmodifiableSet(new HashSet<>(classes.keySet()));
    }

    /**
     * 返回缺省的扩展点名，如果没有设置缺省则返回<code>null</code>。
     *
     * @since 1.0
     */
    public String getDefaultExtensionName() {
        return defaultExtension;
    }

    public Map<String, Map<String, String>> getExtensionAttribute() {
        // 先一下加载扩展点类
        getExtensionClasses();
        return name2Attributes;
    }

    public Map<String, String> getExtensionAttribute(String name) {
        if (name == null || name.length() == 0){
            throw new IllegalArgumentException("Extension name == null");
        }
        // 先一下加载扩展点类，如果没有这个名字的扩展点类，会抛异常，
        // 这样不用创建不必要的Holder。
        getExtensionClass(name);
        return name2Attributes.get(name);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "<" + type.getName() + ">";
    }

    // ==============================
    // internal methods
    // ==============================

    private final Class<T> type;

    private final String defaultExtension;

    private ExtensionLoader(Class<T> type) {
        this.type = type;
        String defaultExt = null;
        final SPI annotation = type.getAnnotation(SPI.class);
        if (annotation != null) {
            String value = annotation.value();
            if (value != null && (value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " +
                            type.getName() + ": " + Arrays.toString(names));
                }
                if (names.length == 1 && names[0].trim().length() > 0) {
                    defaultExt = names[0].trim();
                }
                if (!isValidExtName(defaultExt)) {
                    throw new IllegalStateException("default name(" + defaultExt +
                            ") of extension " + type.getName() + " is invalid!");
                }
            }
        }
        defaultExtension = defaultExt;
    }

    private T createExtension(String name, Map<String, String> properties) {
        Class<?> clazz = getExtensionClass(name);
        try {
            return injectExtension((T) clazz.newInstance(), properties);
        } catch (Throwable t) {
            String msg = "Fail to create extension " + name +
                    " of extension point " + type.getName() + ", cause: " + t.getMessage();
            logger.warn(msg);
            throw new IllegalStateException(msg, t);
        }
    }

    private T createWrapper(T instance, Map<String, String> properties, List<String> wrappers) {
        if (wrappers != null) {
            for (String name : wrappers) {
                try {
                    instance = injectExtension(name2Wrapper.get(name).getConstructor(type).newInstance(instance), properties);
                } catch (Throwable e) {
                    throw new IllegalStateException("Fail to create wrapper(" + name + ") for extension point " + type);
                }
            }
        }
        return instance;
    }

    private T injectExtension(T instance, Map<String, String> properties) {
        for (Method method : instance.getClass().getMethods()) {
            if (method.getName().startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())) {
                Class<?> pt = method.getParameterTypes()[0];
                if (pt.isInterface() && withExtensionAnnotation(pt)) {
                    if (pt.equals(type)) {
                        logger.warn("Ignore self set(" + method + ") for class(" +
                                instance.getClass() + ") when inject.");
                        continue;
                    }
                    try {
                        Object prototype = getExtensionLoader(pt).getExtension(properties);
                        method.invoke(instance, prototype);
                    } catch (Throwable t) {
                        String errMsg = "Fail to inject via method " + method.getName()
                                + " of interface to extension implementation " + instance.getClass() +
                                " for extension point " + type.getName() + ", cause: " + t.getMessage();
                        logger.warn(errMsg, t);
                        throw new IllegalStateException(errMsg, t);
                    }
                }
            }
        }
        return instance;
    }

    // ====================================
    // get & load Extension Class
    // ====================================

    // Holder<Map<ext-name, ext-class>>
    private final Holder<Map<String, Class<?>>> extClassesHolder = new Holder();

    private volatile Map<String, Map<String, String>> name2Attributes;

    private final ConcurrentMap<Class<?>, String> extClass2Name = new ConcurrentHashMap();

    private volatile Class<?> adaptiveClass = null;

    private volatile Map<String, Class<? extends T>> name2Wrapper;

    private final Map<String, IllegalStateException> extClassLoadExceptions = new ConcurrentHashMap();

    private Class<?> getExtensionClass(String name) {
        if (name == null){
            throw new IllegalArgumentException("Extension name == null");
        }
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null){
            throw findExtensionClassLoadException(name);
        }
        return clazz;
    }

    /**
     * Thread-safe.
     */
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = extClassesHolder.get();
        if (classes == null) {
            synchronized (extClassesHolder) {
                classes = extClassesHolder.get();
                if (classes == null) {
                    loadExtensionClasses0();
                    classes = extClassesHolder.get();
                }
            }
        }
        return classes;
    }

    private IllegalStateException findExtensionClassLoadException(String name) {
        String msg = "No such extension " + type.getName() + " by name " + name;

        for (Map.Entry<String, IllegalStateException> entry : extClassLoadExceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                IllegalStateException e = entry.getValue();
                return new IllegalStateException(msg + ", cause: " + e.getMessage(), e);
            }
        }

        StringBuilder buf = new StringBuilder(msg);
        if (!extClassLoadExceptions.isEmpty()) {
            buf.append(", possible causes: ");
            int i = 1;
            for (Map.Entry<String, IllegalStateException> entry : extClassLoadExceptions.entrySet()) {
                buf.append("\r\n(");
                buf.append(i++);
                buf.append(") ");
                buf.append(entry.getKey());
                buf.append(":\r\n");
                buf.append(StringHelper.toString(entry.getValue()));
            }
        }
        return new IllegalStateException(buf.toString());
    }

    private void loadExtensionClasses0() {
        Map<String, Class<?>> extName2Class = new HashMap();
        Map<String, Class<? extends T>> tmpName2Wrapper = new LinkedHashMap();
        Map<String, Map<String, String>> tmpName2Attributes = new LinkedHashMap();
        String fileName = null;
        try {
            ClassLoader classLoader = getClassLoader();
            fileName = EXTENSION_CONF_DIRECTORY + type.getName();
            Enumeration<URL> urls;
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }

            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    readExtension0(extName2Class, tmpName2Attributes, tmpName2Wrapper, classLoader, url);
                }
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension point(interface: " +
                    type.getName() + ", description file: " + fileName + ").", t);
        }

        extClassesHolder.set(extName2Class);
        name2Attributes = tmpName2Attributes;
        name2Wrapper = tmpName2Wrapper;
    }

    private void readExtension0(Map<String, Class<?>> extName2Class, Map<String, Map<String, String>> name2Attributes, Map<String, Class<? extends T>> name2Wrapper, ClassLoader classLoader, URL url) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String config = line;

                // delete comments
                final int ci = config.indexOf('#');
                if (ci >= 0) config = config.substring(0, ci);
                config = config.trim();
                if (config.length() == 0) continue;

                try {
                    String name = null;
                    String body = null;
                    String attribute = null;
                    int i = config.indexOf('=');
                    if (i > 0) {
                        name = config.substring(0, i).trim();
                        body = config.substring(i + 1).trim();
                    }
                    // 没有配置文件中没有扩展点名，从实现类的Extension注解上读取。
                    if (name == null || name.length() == 0) {
                        throw new IllegalStateException(
                                "missing extension name, config value: " + config);
                    }
                    int j = config.indexOf("(", i);
                    if (j > 0) {
                        if (config.charAt(config.length() - 1) != ')') {
                            throw new IllegalStateException(
                                    "missing ')' of extension attribute!");
                        }
                        body = config.substring(i + 1, j).trim();
                        attribute = config.substring(j + 1, config.length() - 1);
                    }

                    Class<? extends T> clazz = Class.forName(body, true, classLoader).asSubclass(type);
                    if (!type.isAssignableFrom(clazz)) {
                        throw new IllegalStateException("Error when load extension class(interface: " +
                                type.getName() + ", class line: " + clazz.getName() + "), class "
                                + clazz.getName() + "is not subtype of interface.");
                    }

                    if (name.startsWith(PREFIX_ADAPTIVE_CLASS)) {
                        if (adaptiveClass == null) {
                            adaptiveClass = clazz;
                        } else if (!adaptiveClass.equals(clazz)) {
                            throw new IllegalStateException("More than 1 adaptive class found: "
                                    + adaptiveClass.getClass().getName()
                                    + ", " + clazz.getClass().getName());
                        }
                    } else {
                        final boolean isWrapper = name.startsWith(PREFIX_WRAPPER_CLASS);
                        if (isWrapper)
                            name = name.substring(PREFIX_WRAPPER_CLASS.length());

                        String[] nameList = NAME_SEPARATOR.split(name);
                        for (String n : nameList) {
                            if (!isValidExtName(n)) {
                                throw new IllegalStateException("name(" + n +
                                        ") of extension " + type.getName() + "is invalid!");
                            }

                            if (isWrapper) {
                                try {
                                    clazz.getConstructor(type);
                                    name2Wrapper.put(name, clazz);
                                } catch (NoSuchMethodException e) {
                                    throw new IllegalStateException("wrapper class(" + clazz +
                                            ") has NO copy constructor!", e);
                                }
                            } else {
                                try {
                                    clazz.getConstructor();
                                } catch (NoSuchMethodException e) {
                                    throw new IllegalStateException("extension class(" + clazz +
                                            ") has NO default constructor!", e);
                                }
                                if (extName2Class.containsKey(n)) {
                                    if (extName2Class.get(n) != clazz) {
                                        throw new IllegalStateException("Duplicate extension " +
                                                type.getName() + " name " + n +
                                                " on " + clazz.getName() + " and " + clazz.getName());
                                    }
                                } else {
                                    extName2Class.put(n, clazz);
                                }
                                name2Attributes.put(n, parseExtAttribute(attribute));

                                if (!extClass2Name.containsKey(clazz)) {
                                    extClass2Name.put(clazz, n); // 实现类到扩展点名的Map中，记录了一个就可以了
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    IllegalStateException e = new IllegalStateException("Failed to load config line(" + line +
                            ") of config file(" + url + ") for extension(" + type.getName() +
                            "), cause: " + t.getMessage(), t);
                    logger.warn("", e);
                    extClassLoadExceptions.put(line, e);
                }
            } // end of while read lines
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type.getName() + ", class file: " + url + ") in " + url, t);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
    }


}
