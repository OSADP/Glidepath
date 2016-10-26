package com.leidos.glidepath.appcommon.utils;

import com.leidos.glidepath.dvi.AppConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Wrapper to always return a reference to the Spring Application Context from
 * within non-Spring enabled beans. Unlike Spring MVC's WebApplicationContextUtils
 * we do not need a reference to the Servlet context for this. All we need is
 * for this bean to be initialized during application startup.
 */
public class GlidepathApplicationContext implements ApplicationContextAware {

    private static ApplicationContext CONTEXT;

    private GlidepathApplicationContext() {
    }

    private static class GlidepathApplicationContextHolder  {
        private static final GlidepathApplicationContext _instance = new GlidepathApplicationContext();
    }

    public static GlidepathApplicationContext getInstance()
    {
        return GlidepathApplicationContextHolder._instance;
    }


    /**
     * This method is called from within the SpeedControl once it is
     * done starting up, it will stick a reference to the app context into this bean.
     * @param context a reference to the ApplicationContext.
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        CONTEXT = context;
    }

    /**
     * get the ApplicationContext
     *
     * @return
     */
    public ApplicationContext getApplicationContext()   {
        return CONTEXT;
    }

    /**
     * Provide access to spring beans through non-spring managed classes
     *
     * @param tClass
     * @param <T>
     * @return
     * @throws org.springframework.beans.BeansException
     */
    public <T>  T getBean(Class<T> tClass) throws BeansException   {
        return CONTEXT.getBean(tClass);
    }

    /**
     * Directly acquire AppConfig
     *
     * @return
     */
    public AppConfig getAppConfig()   {
        return getBean(AppConfig.class);
    }
}