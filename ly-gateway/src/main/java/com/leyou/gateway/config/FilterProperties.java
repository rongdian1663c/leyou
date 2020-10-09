package com.leyou.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ly.filter")
@Setter
@Getter
public class FilterProperties {

    private List<String> allowPaths;
}
