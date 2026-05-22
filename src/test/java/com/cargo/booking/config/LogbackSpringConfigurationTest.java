package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class LogbackSpringConfigurationTest {

    @Test
    void shouldDefineReadableLocalConsoleLogging() throws Exception {
        Document document = logbackConfig();
        Element localProfile = springProfile(document, "local");

        assertThat(appender(localProfile, "LOCAL_CONSOLE").getAttribute("class"))
                .isEqualTo("ch.qos.logback.core.ConsoleAppender");
        assertThat(text(encoder(appender(localProfile, "LOCAL_CONSOLE")), "pattern"))
                .isEqualTo("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        assertThat(logger(localProfile, "com.cargo.booking").getAttribute("level")).isEqualTo("DEBUG");
        assertThat(root(localProfile).getAttribute("level")).isEqualTo("INFO");
        assertThat(appenderRef(root(localProfile))).isEqualTo("LOCAL_CONSOLE");
    }

    @Test
    void shouldDefineJsonLoggingForDevAndProdWithMdcProvider() throws Exception {
        assertThat(Class.forName("net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder"))
                .isNotNull();

        Document document = logbackConfig();
        Element jsonProfile = springProfile(document, "dev | prod");
        Element jsonAppender = appender(jsonProfile, "JSON_CONSOLE");

        assertThat(jsonAppender.getAttribute("class")).isEqualTo("ch.qos.logback.core.ConsoleAppender");
        assertThat(encoder(jsonAppender).getAttribute("class"))
                .isEqualTo("net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder");
        assertThat(providerNames(jsonAppender))
                .containsExactly("timestamp", "logLevel", "loggerName", "message", "threadName", "mdc");
        assertThat(providerFieldNames(jsonAppender))
                .containsExactly("timestamp", "level", "logger", "message", "thread", "mdc");
        assertThat(logger(jsonProfile, "com.cargo.booking").getAttribute("level")).isEqualTo("INFO");
    }

    @Test
    void shouldUseProfileSpecificJsonRootLevels() throws Exception {
        Document document = logbackConfig();

        Element devProfile = springProfile(document, "dev");
        assertThat(root(devProfile).getAttribute("level")).isEqualTo("INFO");
        assertThat(appenderRef(root(devProfile))).isEqualTo("JSON_CONSOLE");

        Element prodProfile = springProfile(document, "prod");
        assertThat(root(prodProfile).getAttribute("level")).isEqualTo("WARN");
        assertThat(appenderRef(root(prodProfile))).isEqualTo("JSON_CONSOLE");
    }

    private Document logbackConfig() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        try (InputStream inputStream = new ClassPathResource("logback-spring.xml").getInputStream()) {
            return factory.newDocumentBuilder().parse(inputStream);
        }
    }

    private Element springProfile(Document document, String name) {
        return elements(document.getDocumentElement(), "springProfile").stream()
                .filter(element -> name.equals(element.getAttribute("name")))
                .findFirst()
                .orElseThrow();
    }

    private Element appender(Element parent, String name) {
        return elements(parent, "appender").stream()
                .filter(element -> name.equals(element.getAttribute("name")))
                .findFirst()
                .orElseThrow();
    }

    private Element encoder(Element appender) {
        return elements(appender, "encoder").getFirst();
    }

    private Element logger(Element parent, String name) {
        return elements(parent, "logger").stream()
                .filter(element -> name.equals(element.getAttribute("name")))
                .findFirst()
                .orElseThrow();
    }

    private Element root(Element parent) {
        return elements(parent, "root").getFirst();
    }

    private String appenderRef(Element root) {
        return elements(root, "appender-ref").getFirst().getAttribute("ref");
    }

    private String text(Element parent, String tagName) {
        return elements(parent, tagName).getFirst().getTextContent();
    }

    private List<String> providerNames(Element jsonAppender) {
        Element providers = elements(encoder(jsonAppender), "providers").getFirst();
        return elements(providers).stream()
                .map(Element::getTagName)
                .toList();
    }

    private List<String> providerFieldNames(Element jsonAppender) {
        Element providers = elements(encoder(jsonAppender), "providers").getFirst();
        return elements(providers).stream()
                .map(provider -> text(provider, "fieldName"))
                .toList();
    }

    private List<Element> elements(Element parent, String tagName) {
        return elements(parent).stream()
                .filter(element -> tagName.equals(element.getTagName()))
                .toList();
    }

    private List<Element> elements(Element parent) {
        return java.util.stream.IntStream.range(0, parent.getChildNodes().getLength())
                .mapToObj(parent.getChildNodes()::item)
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                .map(Element.class::cast)
                .toList();
    }
}
