package fr.openent.crre.controllers;

import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.service.QuoteService;
import fr.openent.crre.service.impl.DefaultQuoteService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static java.lang.Integer.parseInt;

public class QuoteController extends BaseController {


    private final QuoteService quoteService;


    public QuoteController() {
        this.quoteService = new DefaultQuoteService("equipment");
    }

    @Get("/quote")
    @ApiDoc("get all quotes ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getAllQuotations(HttpServerRequest request) {
        Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
        quoteService.getAllQuote(page, arrayResponseHandler(request));
    }

    @Get("/quote/csv/:id")
    @ApiDoc("generate csv attachment ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getCSVQuote(HttpServerRequest request) {
        Integer id = request.params().contains("id")
                ? parseInt(request.params().get("id"))
                : null;
        if(id != null){
            quoteService.getQuote(id, quote -> {
                if(quote.isRight()) {
                    JsonObject quoteResult = quote.right().getValue();
                    String attachment = quoteResult.getString("attachment");
                    String title = quoteResult.getString("title");
                    request.response()
                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=" + title + ".csv")
                            .end(attachment);
                }else{
                    log.error("An error occured during SQL Quote request",quote.left());
                    renderError(request);
                }
            });
        }else{
            log.error("Unable to get the id of the quote");
            renderError(request);
        }
  }

   @Get("/quote/search")
   @ApiDoc("Search in quotes")
   @SecuredAction(value = "", type = ActionType.RESOURCE)
   @ResourceFilter(AdministratorRight.class)
   public void search(HttpServerRequest request) throws UnsupportedEncodingException {
         String query = "";
         Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
         if (request.params().contains("q")) {
           query = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
         }
         quoteService.search(query, page, arrayResponseHandler(request));
  }

}



