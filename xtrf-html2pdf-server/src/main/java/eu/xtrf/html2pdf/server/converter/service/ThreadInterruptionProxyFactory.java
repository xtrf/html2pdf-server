package eu.xtrf.html2pdf.server.converter.service;

import org.apache.commons.lang3.Validate;

import java.lang.reflect.Proxy;

public class ThreadInterruptionProxyFactory {

    public static <T> T wrap(Class<T> clazz, T target) {
        Validate.isTrue(clazz.isInterface(), "Only interface may be wrapped into proxy");
        ClassLoader classLoader = target.getClass().getClassLoader();
        Class<?>[] interfaces = {clazz};
        ThreadInterruptionProxyHandler proxyHandler = new ThreadInterruptionProxyHandler(target);
        Object proxy = Proxy.newProxyInstance(classLoader, interfaces, proxyHandler);
        return clazz.cast(proxy);
    }

}
