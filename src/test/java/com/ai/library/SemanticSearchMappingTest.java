package com.ai.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SemanticSearchMappingTest {

    @Autowired
    ApplicationContext context;

    @Test
    void onlyOneSemanticSearchMapping() {
        RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
    // Some Spring versions expose patterns differently; inspect the toString() output instead.
    var infos = mapping.getHandlerMethods().keySet();
    Set<String> infoStrings = infos.stream().map(Object::toString).collect(Collectors.toSet());

    long matchCount = infos.stream()
        .map(Object::toString)
        .filter(s -> s.contains("/api/semantic/search"))
        .count();

    assertThat(matchCount)
        .withFailMessage("Expected one RequestMappingInfo containing '/api/semantic/search'. Entries: %s", infoStrings)
        .isEqualTo(1);
    }
}
