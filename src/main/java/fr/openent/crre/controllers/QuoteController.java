package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.security.ValidatorRight;
import fr.openent.crre.service.PurseService;
import fr.openent.crre.service.QuoteService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.impl.DefaultOrderService;
import fr.openent.crre.service.impl.DefaultPurseService;
import fr.openent.crre.service.impl.DefaultQuoteService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.filter.ResourceFilter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class QuoteController extends BaseController {


    private final QuoteService quoteService;
    private final PurseService purseService;
    private final StructureService structureService;
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);


    public QuoteController() {
        this.quoteService = new DefaultQuoteService("equipment");
        this.purseService = new DefaultPurseService();
        this.structureService = new DefaultStructureService(Crre.crreSchema);
    }

    @Get("/quote")
    @ApiDoc("get all quotes ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getAllQuotations(HttpServerRequest request) {
        Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
        quoteService.getAllQuote(page, arrayResponseHandler(request));
    }

    @Get("/quote/csv")
    @ApiDoc("generate csv attachment ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getCSVQuote(HttpServerRequest request) {
        String b64_attachment = request.getParam("attachment");
        String attachment = new String(Base64.getDecoder().decode(b64_attachment));
        request.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                .end(attachment);
  }

   @Get("/quote/search")
   @ApiDoc("Search in quotes")
   @SecuredAction(value = "", type = ActionType.RESOURCE)
   @ResourceFilter(AdministratorRight.class)
   public void search(HttpServerRequest request) throws UnsupportedEncodingException {
         String query = "";
         Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
         if (request.params().contains("q")) {
           query = URLDecoder.decode(request.getParam("q"), "UTF-8");
         }
         quoteService.search(query, page, arrayResponseHandler(request));
  }

}



