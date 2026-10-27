package com.abcde.hdbresale.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * When the built frontend is bundled into the jar (classpath:/static), browser routes such as /flats/12 or
 * /bookmarks are served index.html so the single-page app can handle them.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String path : new String[]{"/results", "/flats/{id}", "/bookmarks", "/compare", "/profile", "/login",
                "/signup", "/help"}) {
            registry.addViewController(path).setViewName("forward:/index.html");
        }
    }
}
