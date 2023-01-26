package fr.openent.crre.service;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.model.config.ConfigModel;
import fr.openent.crre.service.impl.*;
import io.vertx.core.Vertx;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.entcore.common.email.EmailFactory;

public class ServiceFactory {
    private final Vertx vertx;
    private final ConfigModel config;
    private final WebClient webClient;
    private final EmailSendService emailSender;
    private final DefaultOrderRegionService orderRegionService;
    private final DefaultPurseService purseService;
    private final DefaultQuoteService quoteService;
    private final DefaultStructureService structureService;

    public ServiceFactory(Vertx vertx, ConfigModel config, EmailFactory emailFactory) {
        this.vertx = vertx;
        this.config = config;
        this.webClient = initWebClient();
        this.emailSender = new EmailSendService(emailFactory.getSender(), config);
        this.orderRegionService = new DefaultOrderRegionService(Field.EQUIPEMENT);
        this.purseService = new DefaultPurseService();
        this.quoteService = new DefaultQuoteService(Field.EQUIPEMENT);
        this.structureService = new DefaultStructureService(Crre.crreSchema, vertx.eventBus());
    }

    public Vertx getVertx() {
        return vertx;
    }

    public ConfigModel getConfig() {
        return config;
    }

    public WebClient getWebClient() {
        return webClient;
    }

    public EmailSendService getEmailSender() {
        return emailSender;
    }

    public DefaultOrderRegionService getOrderRegionService() {
        return orderRegionService;
    }

    public DefaultPurseService getPurseService() {
        return purseService;
    }

    public DefaultQuoteService getQuoteService() {
        return quoteService;
    }

    public DefaultStructureService getStructureService() {
        return structureService;
    }

    private WebClient initWebClient() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);
        if (System.getProperty("httpclient.proxyHost") != null) {
            ProxyOptions proxyOptions = new ProxyOptions();
            proxyOptions.setHost(System.getProperty("httpclient.proxyHost"));
            proxyOptions.setPort(Integer.parseInt(System.getProperty("httpclient.proxyPort")));
            proxyOptions.setUsername(System.getProperty("httpclient.proxyUsername"));
            proxyOptions.setPassword(System.getProperty("httpclient.proxyPassword"));
            proxyOptions.setType(ProxyType.HTTP);
            options.setProxyOptions(proxyOptions);
        }
        return WebClient.create(this.vertx, options);
    }
}
