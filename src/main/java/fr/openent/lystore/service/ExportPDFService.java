package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ExportPDFService {
    /**
     * Generation of a PDF from a XHTML template
     * @param request Http request
     * @param templateProps  JSON object
     * @param templateName  template's name
     * @param prefixPdfName prefix the PDF's name (it will be completed by the date)
     * @param handler Function handler returning data
     */
    void generatePDF(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                     final String prefixPdfName, Handler<Buffer> handler );
}
