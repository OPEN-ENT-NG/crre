package fr.openent.lystore.export.instructions.iris;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;

public class IrisTab extends TabHelper {
    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     */

    private final String IDDOS = "iddos";//
    private final String LBDDOS = "lbdoss";//
    private final String OBJDDOS = "objdos";//
    private final String IDTIERS = "idtiers";//
    private final String NATURE = "nature";//
    private final String PROGRAMME = "programme";//
    private final String ACTION = "action";//
    private final String ENVILIG = "envilig";//
    private final String MTPROP = "mtprop";//
    private final String IDCOM = "idcom";//
    private final String IDLOCAL = "idlocal";//
    private final String IDCPER = "idcper";
    private final String IDCPRD = "idcprd";
    private final String QPVIDENT = "qpvident";
    private final String CPVIDENT = "cvident";

    public IrisTab(Workbook wb, JsonObject instruction) {
        super(wb, instruction, "IRIS");
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }




    @Override
    public void initDatas(Handler<Either<String, Boolean>> handler) {

        ArrayList structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            if(!structuresId.contains(data.getString("id_structure")))
                structuresId.add(structuresId.size(), data.getString("id_structure"));
        }

        try {
            getStructures(new JsonArray(structuresId), getStructureHandler(structuresId,handler));
        }catch (Exception e){
            initDatas(handler);
        }
    }

    @Override
    protected void fillPage(JsonArray structures){
        setStructuresFromDatas(structures);
        setArray(datas);
    }
    @Override
    protected void setArray(JsonArray datas){
        excel.insertStandardText(0, 0, IDDOS);
        excel.insertStandardText(1, 0, LBDDOS);
        excel.insertStandardText(2, 0, OBJDDOS);//: Objet du dossier sur 200 caractères Max. :  Libellé de l'équipement + " / " + Commentaire Région de la demande
        //(exemple : RIDEAUX COMPLEMENT RENOUVELLEMENT / LYSTORE / PRIORITE 3 / EQUIPEMENT DE RIDEAUX)
        excel.insertStandardText(3, 0, IDTIERS);  //    Code Tiers Coriolis  - Cf. Nomemclature
        //           (exemple : R3230 pour l'établissement 0750558Z)
        excel.insertStandardText(4, 0, NATURE);//Code Nature Comptable du marché
        //  (exemple 236)
        excel.insertStandardText(5, 0, PROGRAMME); /* Colonne F : programme : Code contrat du programme Cf. éléments comptables de la demande  "type de contract "
        (exemple 122008)*/
        excel.insertStandardText(6, 0, ACTION);/* action : Code action Cf. éléments comptables de la demande
                            (exemple 12200801)*/
        excel.insertStandardText(7, 0, ENVILIG);/*Colonne H : envilig : Année d'exercice sur 4 caractères + "-" +
        Code programme Cf. éléments comptables de la demande "programme" + "-" + Numéro (1 pour les CMD , 2 pour les CMR , 3 pour les EPLE hors CMD et CMR )
                            (exemple 2019-HP222-008-3 : 2019=année d'exercice, HP222-008=programme, 3=EPLE hors CMR et CMD)*/
        excel.insertStandardText(8, 0, MTPROP);//Colonne I : mtprop : Montant proposé en TTC de l'équipement
        excel.insertStandardText(9, 0, IDCOM);//Colonne J : idcom : Codification de la commission IRIS : "AD000195"
        excel.insertStandardText(10, 0, IDLOCAL);//Colonne K : idlocal : Codification Local : Champ Vide
        excel.insertStandardText(11, 0, IDCPER);//Colonne L : idcper : codification CPER : "E0060728"
        excel.insertStandardText(12, 0, IDCPRD);/*Colonne M : idcprd : codification CPRD : "L0003954"
Colonne N : qpvident : Indicateur lycées en QPV : "QPVNON"
Colonne O : cvident : Indicateur Contrat de Ville : "CVNON"*/
        excel.insertStandardText(13, 0, QPVIDENT);
        excel.insertStandardText(14, 0, CPVIDENT);
        for(int i=0;i<datas.size();i++){
            JsonObject data =datas.getJsonObject(i);

            String EnviligString = data.getString("school_year").substring(0,4)+  "-" +data.getString("program");
            switch(data.getString("cite_mixte")){
                case CMR:
                    EnviligString+="-2";
                    break;
                case CMD:
                    EnviligString+="-1";
                    break;
                default:
                    EnviligString+="-3";
                    break;
            }

            String OBJDDOS = data.getString("name_equipment") + " / ";
            if(data.getBoolean("isregion"))
                OBJDDOS += makeCellWithoutNull(data.getString("comment"));
            excel.insertStandardText(0,1 + i,"");
            excel.insertStandardText(1,1 + i,stringWithMaxCharacter("ETAB. "+data.getString("uai") +" Opération "+ data.getString("operation"),80));
            excel.insertStandardText(2,1 + i,stringWithMaxCharacter(OBJDDOS,200));
            excel.insertStandardText(3,1 + i,data.getString("code_coriolis"));
            excel.insertStandardText(4,1 + i,data.getString("code"));
            excel.insertStandardText(5,1 + i,data.getString("program_type"));
            excel.insertStandardText(6,1 + i,data.getString("program_action"));
            excel.insertStandardText(7,1 + i,EnviligString);
            excel.insertStandardText(8,1 + i,data.getString("total").replace(".",","));
            excel.insertStandardText(9,1 + i,"AD000195");
            excel.insertStandardText(10,1 + i,"");
            excel.insertStandardText(11,1 + i,"E0060728");
            excel.insertStandardText(12,1 + i,"L0003954");
            excel.insertStandardText(13,i + 1,"QPVNON");
            excel.insertStandardText(14,i + 1,"CVNON");

        }
    }


    private String stringWithMaxCharacter(String s,int maxLength) {
        if(s.length()<=maxLength){
            return s;
        }else{
            return s.substring(0,maxLength);
        }
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "       With values as  (             " +
                "     SELECT  orders.id ,orders.\"price TTC\",  " +
                "             ROUND((( SELECT CASE          " +
                "            WHEN orders.price_proposal IS NOT NULL THEN 0     " +
                "            WHEN orders.override_region IS NULL THEN 0 " +
                "            WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) IS NULL THEN 0         " +
                "            ELSE SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount)         " +
                "            END           " +
                "             FROM   " + Lystore.lystoreSchema + ".order_client_options oco  " +
                "              where oco.id_order_client_equipment = orders.id " +
                "             ) + orders.\"price TTC\" " +
                "              ) * orders.amount   ,2 ) " +
                "             as Total, contract.name as market, contract_type.code as code,  program.program_type,   " +
                "             program.name as program,         CASE WHEN orders.id_order_client_equipment is not null  " +
                "             THEN  (select oce.name FROM " + Lystore.lystoreSchema + ".order_client_equipment oce    " +
                "              where oce.id = orders.id_order_client_equipment limit 1)     " +
                "             ELSE ''      " +
                "             END as old_name,     " +
                "             orders.id_structure,orders.id_operation as id_operation, label.label as operation ,     " +
                "             orders.equipment_key as key, orders.name as name_equipment, true as region,  orders.id as id,  " +
                "             program_action.id_program,program_action.action as program_action, orders.amount ,contract.id as market_id,   campaign.name as campaign, orders.comment, project.room, orders.isregion, " +
                "             project.stair,project.building,  program.functional_code, exercise.year as school_year, specific_structures.code_coriolis ," +
                "             case when specific_structures.type is null      " +
                "             then '" + LYCEE + "'          " +
                "             ELSE specific_structures.type     " +
                "             END as cite_mixte     " +
                "             FROM (      " +
                "             (select ore.id,  true as isregion, ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary, " +
                "             ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure, " +
                "             ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  " +
                "             ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation ," +
                "             null as override_region          from " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )      " +
                "             UNION      " +
                "             (select oce.id ,  false as isregion," +
                "             CASE WHEN price_proposal is null then  price + (price*tax_amount/100)  else price_proposal end as \"price TTC\", " +
                "             amount, creation_date, null as modification_date, name,  " +
                "             summary, description, image,  status, id_contract, equipment_key, id_campaign, id_structure, cause_status, number_validation, " +
                "             id_order, comment, rank as \"prio\", price_proposal, id_project, null as id_order_client_equipment,  program, action,  " +
                "             id_operation, override_region           from " + Lystore.lystoreSchema + ".order_client_equipment  oce) " +
                "             ) as orders       " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id   and (orders.override_region != true OR orders.override_region is NULL))               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id)                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id" +
                " AND   contract_type.code = '236')      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".project ON orders.id_project = project.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         ";
        query += "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                "     OR  (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "') " +
                "      OR (spa.structure_type = '" + LYCEE + "' AND ( specific_structures.type is null OR specific_structures.type = '"+ LYCEE+"' )))    ";
        query += "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id       " +
                "      INNER JOIN  " + Lystore.lystoreSchema+".exercise on instruction.id_exercise = exercise.id "+
                "             Group by program.name,contract_type.code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region , orders.comment,campaign.name , orders.id,exercise.year," +
                "               orders.isregion,  program.functional_code , program.program_type ," +
                "              project.room,project.stair, project.building,program_action.action ,specific_structures.code_coriolis" +
                "             order by campaign,code,market_id, id_structure,program,code " +
                "  )    SELECT  values.* " +
                "  from  values      " ;


        sqlHandler(handler);
    }
}
