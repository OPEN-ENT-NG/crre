package fr.openent.lystore.service.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserInfos;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class EmailSendService {

    private Neo4j neo4j;

    private static final io.vertx.core.logging.Logger LOGGER = LoggerFactory.getLogger (EmailSendService.class);
    private final EmailSender emailSender;
    public EmailSendService(EmailSender emailSender){
        this.emailSender = emailSender;
        this.neo4j = Neo4j.getInstance();
    }

    public void sendMail(HttpServerRequest request, String eMail, String object, String body) {

        emailSender.sendEmail(request,
                eMail,
                null,
                null,
                object,
                body,
                null,
                true,
                null);

    }

    public void sendMails(HttpServerRequest request, JsonObject result, JsonArray rows, UserInfos user, String url,
                          JsonArray structureRows){
        final int contractNameIndex = 1;
        final int agentEmailIndex = 3;
        JsonObject structRow;
        JsonArray row;
        String oldIdStruct = "",currentIdStruct, nameEtab = "";
        String number_validation = result.getString("number_validation");
        ArrayList<Integer> idsCampaign =  new ArrayList<>();
        JsonArray line = rows.getJsonArray(0);
        String agentMailObject = "[LyStore] Commandes " + line.getString(contractNameIndex);
        String agentMailBody = getAgentBodyMail(line, user, number_validation, url);
        JsonArray mailsRow = new JsonArray();
        sendMail(request, line.getString(agentEmailIndex),
                agentMailObject,
                agentMailBody);

    for(int i = 0 ; i < rows.size(); i++){
        row = rows.getJsonArray(i);
        currentIdStruct = row.getString(4);
        Integer idCampaign = row.getInteger(5);

        if(!oldIdStruct.equals(currentIdStruct)){
            oldIdStruct = currentIdStruct;
            if(i != 0){
                mailsToClient(request, result, user, url, nameEtab, idsCampaign, mailsRow);
                idsCampaign =  new ArrayList<>();
            }
            for(int j =0; j < structureRows.size(); j++){
                structRow = structureRows.getJsonObject(j);
                if(structRow.getString("id").equals(currentIdStruct)){
                    nameEtab = structRow.getString("name");
                    mailsRow = structRow.getJsonArray("mails");
                }
            }
        }
        if(!idsCampaign.contains(idCampaign)){
            idsCampaign.add(idCampaign);
        }
    }

        mailsToClient(request, result, user, url, nameEtab, idsCampaign, mailsRow);


    }

    private void mailsToClient(HttpServerRequest request,JsonObject result, UserInfos user, String url, String nameEtab, ArrayList<Integer> idsCampaign, JsonArray mailsRow) {
        for (int k = 0; k < mailsRow.size(); k++) {
            JsonObject userMail = mailsRow.getJsonObject(k);
            String mailObject = "[LyStore] Commandes ";
            if (userMail.getString("mail") != null) {
                String mailBody = getStructureBodyMail(mailsRow.getJsonObject(k), user,
                        result.getString("number_validation"), url, nameEtab,idsCampaign);
                            sendMail(request, userMail.getString("mail"),
                                    mailObject,
                                    mailBody);
            }
        }
    }


    public void getPersonnelMailStructure(JsonArray structureIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (w:WorkflowAction {displayName: 'lystore.access'})--(r:Role) with r , count((r)-->(w)) as NbrRows " +
                " Match p = ((r)<--(mg:ManualGroup)-->(s:Structure)), (mg)<-[IN]-(u:User)  " +
                "where NbrRows=1 AND s.id IN {ids} return s.id as id, s.name as name, " +
                "collect(DISTINCT {mail : u.email, name: u.displayName} ) as mails ";
        neo4j.execute(query, new JsonObject().put("ids", structureIds),
                Neo4jResult.validResultHandler(handler));

    }

    private static String getStructureBodyMail(JsonObject row, UserInfos user, String numberOrder, String url,
                                               String name, ArrayList<Integer> idsCampaign){
        StringBuilder listOrders= new StringBuilder();
        for(int i = 0;i < idsCampaign.size(); i++){
          listOrders.append("<br />").append(url).append("#/campaign/").append(idsCampaign.get(i)).append("/order <br />");
        }
        String body = "Bonjour " + row.getString("name") + ", <br/> <br/>"
                + "Une commande sous le numéro \"" + numberOrder + "\" vient d'être validée."
                + " Une partie de la commande concerne l'établissement " + name + ". "
                + "Cette confirmation est visible sur l'interface de LyStore en vous rendant ici :  <br />"
                + listOrders
                + "<br /> Bien Cordialement, "
                + "<br /> L'équipe LyStore. ";
        return formatAccentedString(body);

    }
    private static String getAgentBodyMail(JsonArray row, UserInfos user, String numberOrder, String url){
        final int contractName = 2 ;
        String body = null;
        body = "Bonjour " + row.getString(contractName) + ", <br/> <br/>"
                + user.getFirstName() + " " + user.getLastName() + " vient de valider une commande sous le numéro \""
                + numberOrder + "\"."
                + " Une partie de la commande concerne le marché " + row.getString(1) + ". "
                + "<br /> Pour générer le bon de commande et les CSF associés, il suffit de se rendre ici : <br />"
                + "<br />" + url  + "#/order/valid" + "<br />"
                + "<br /> Bien Cordialement, "
                + "<br /> L'équipe LyStore. ";
        return formatAccentedString(body);
    }

    private static String getEncodedRedirectUri(String callback) {
        try {
            return "/auth/login?callback=" + URLEncoder.encode(callback, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String formatAccentedString (String body){
        return  body.replace("&","&amp;").replace("€","&euro;")
                .replace("à","&agrave;").replace("â","&acirc;")
                .replace("é","&eacute;").replace("è","&egrave;")
                .replace("ê","&ecirc;").replace("î","&icirc;")
                .replace("ï","&iuml;") .replace("œ","&oelig;")
                .replace("ù","&ugrave;").replace("û","&ucirc;")
                .replace("ç","&ccedil;").replace("À","&Agrave;")
                .replace("Â","&Acirc;").replace("É","&Eacute;")
                .replace("È","&Egrave;").replace("Ê","&Ecirc;")
                .replace("Î","&Icirc;").replace("Ï","&Iuml;")
                .replace("Œ","&OElig;").replace("Ù","&Ugrave;")
                .replace("Û","&Ucirc;").replace("Ç","&Ccedil;");

    }
}
