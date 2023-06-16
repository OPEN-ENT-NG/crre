package fr.openent.crre.helpers;

import fr.openent.crre.model.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;

import static fr.openent.crre.core.constants.Field.UTF8_BOM;

@RunWith(VertxUnitRunner.class)
public class ExportHelperTest {

    @Test
    public void generateExportLineTest(TestContext ctx) throws Exception {
        OrderUniversalModel order = new OrderUniversalModel();
        StructureNeo4jModel structure = new StructureNeo4jModel();

        order.setOrderRegionId(156);
        order.setIdStructure("f83c66d4-6916-4491-8ac0-47ba8af38422");
        order.setValidatorValidationDate("2023-02-28 00:00:00.000000+0000");
        order.setProject(new ProjectModel().setTitle("projectTitle"));
        order.setCampaign(new Campaign().setName("campaignName"));
        order.setEquipmentKey("12345");
        order.setEquipmentName("Name");
        order.setEquipmentEditor("editor");
        order.setEquipmentDiffusor("diffusor");
        order.setEquipmentType("Camera");
        order.setEquipmentEanLibrary("eadLibrary");
        order.setEquipmentCatalogueType("catalogType");
        order.setReassort(false);
        order.setAmount(19);
        order.setEquipmentPriceht(59.65);
        order.setEquipmentTva5(0.5);
        order.setEquipmentTva20(45.5);
        order.setEquipmentPrice(480.5);
        order.setEquipmentTva5(0.5);
        order.setEquipmentTva5(0.5);
        order.setComment("comment");
        order.setStudents(new StudentsTableModel()
                .setGeneral(true)
                .setBma1(1)
                .setBma2(2)
                .setCap1(3)
                .setCap2(4)
                .setCap3(5)
                .setSeconde(6)
                .setPremiere(7)
                .setTerminale(8)
                .setSecondetechno(9)
                .setPremieretechno(10)
                .setTerminaletechno(11)
                .setSecondepro(12)
                .setPremierepro(13)
                .setTerminalepro(14));


        structure.setName("Test School");
        structure.setUai("0123456");
        structure.setAddress("123 Main St");
        order.setStructure(structure);


        String expectedResult = UTF8_BOM + UTF8_BOM + "ID unique;Date;Nom étab;UAI de l'étab;Adresse de livraison;Nom commande;Campagne;EAN de la ressource;Titre de la ressource;Editeur;Distributeur;Numerique;Id de l'offre choisie;Type;Reassort;Quantité;Prix HT de la ressource;Part prix 5,5%;Part prix 20%;Prix unitaire TTC;Montant total HT;Prix total TTC;Commentaire;2nde Generale;1ere Generale;Terminale Generale;2nde Technologique;1ere Technologique;Terminale Technologique;2nde Professionnelle;1ere Professionnelle;Terminale Professionnelle;BMA 1ere annee;BMA 2nde annee;CAP 1ere annee;CAP 2eme annee;CAP 3eme annee\n" +
                "156;2023-02-28;Test School;0123456;123 Main St;projectTitle;campaignName;12345;Name;editor;diffusor;Camera;eadLibrary;catalogType;Non;19;59.65;0.5;45.5;480.5;1133.35;9129.5;comment;6;7;8;9;10;11;12;13;14;1;2;3;4;5\n";

        JsonObject result = ExportHelper.generateExportRegion(Collections.singletonList(order));
        ctx.assertEquals(expectedResult, result.getString("csvFile"));
        ctx.assertEquals(1, result.getInteger("nbEtab"));
    }
}
