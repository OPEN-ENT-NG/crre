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
import org.entcore.common.storage.Storage;

public class ServiceFactory {
    private final Vertx vertx;
    private final ConfigModel config;
    private final WebClient webClient;
    private final EmailSendService emailSender;
    private final DefaultOrderRegionService orderRegionService;
    private final DefaultPurseService purseService;
    private final DefaultQuoteService quoteService;
    private final DefaultStructureService structureService;
    private final DefaultBasketService basketService;
    private final DefaultCampaignService campaignService;
    private final DefaultEquipmentService equipmentService;
    private final DefaultOrderService orderService;
    private final DefaultStatisticsService statisticsService;
    private final DefaultStructureGroupService structureGroupService;
    private final DefaultUserService userService;
    private final DefaultLogService logService;
    private final DefaultStorageService storageService;


    public ServiceFactory(Vertx vertx, ConfigModel config, EmailFactory emailFactory, Storage storage) {
        this.vertx = vertx;
        this.config = config;
        this.webClient = initWebClient();
        if (emailFactory != null) {
            this.emailSender = new EmailSendService(emailFactory.getSender(), config);
        } else {
            this.emailSender = null;
        }
        this.orderRegionService = new DefaultOrderRegionService(Field.EQUIPEMENT);
        this.purseService = new DefaultPurseService();
        this.quoteService = new DefaultQuoteService(Field.EQUIPEMENT);
        this.structureService = new DefaultStructureService(Crre.crreSchema, vertx.eventBus());
        this.basketService = new DefaultBasketService();
        this.campaignService = new DefaultCampaignService(Crre.crreSchema, Field.CAMPAIGN);
        this.equipmentService = new DefaultEquipmentService(Crre.crreSchema, Field.EQUIPEMENT);
        this.orderService = new DefaultOrderService(Crre.crreSchema, Field.ORDER_CLIENT_EQUIPMENT);
        this.statisticsService = new DefaultStatisticsService(Crre.crreSchema);
        this.structureGroupService = new DefaultStructureGroupService(this);
        this.userService = new DefaultUserService();
        this.logService = new DefaultLogService();
        this.storageService = new DefaultStorageService(storage);
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

    public DefaultBasketService getBasketService() {
        return basketService;
    }

    public DefaultCampaignService getCampaignService() {
        return campaignService;
    }

    public DefaultEquipmentService getEquipmentService() {
        return equipmentService;
    }

    public DefaultOrderService getOrderService() {
        return orderService;
    }

    public DefaultStatisticsService getStatisticsService() {
        return statisticsService;
    }

    public DefaultStructureGroupService getStructureGroupService() {
        return structureGroupService;
    }

    public DefaultUserService getUserService() {
        return userService;
    }

    public DefaultLogService getLogService() {
        return logService;
    }

    public DefaultStorageService getStorageService() {
        return storageService;
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
