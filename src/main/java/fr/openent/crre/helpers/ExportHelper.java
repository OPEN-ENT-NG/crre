package fr.openent.crre.helpers;

import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.OrderUniversalOfferModel;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.openent.crre.controllers.OrderController.exportPriceComment;
import static fr.openent.crre.controllers.OrderController.exportStudents;
import static fr.openent.crre.core.constants.Field.UTF8_BOM;

public class ExportHelper {

    private ExportHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonObject generateExportRegion(List<OrderUniversalModel> orderUniversalModelList) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(UTF8_BOM).append(getExportHeaderRegion());

        // Generate commands first
        orderUniversalModelList.forEach(orderUniversalModel -> {
            report.append(generateExportLineRegion(orderUniversalModel));
            if (orderUniversalModel.getOffers() != null && !orderUniversalModel.getOffers().isEmpty()) {
                orderUniversalModel.getOffers().forEach(orderUniversalOfferModel -> report.append(generateExportLineRegionOffer(orderUniversalOfferModel)));
            }
        });

        long structureCount = orderUniversalModelList.stream()
                .map(OrderUniversalModel::getIdStructure)
                .filter(idStructure -> !StringHelper.isNullOrEmpty(idStructure))
                .distinct()
                .count();

        return new JsonObject()
                .put("csvFile", report.toString())
                .put("nbEtab", (int) structureCount);
    }

    private static String getExportHeaderRegion() {
        return "ID unique" + ";" +
                "Date;" +
                "Nom étab;" +
                "UAI de l'étab;" +
                "Adresse de livraison;" +
                "Nom commande;" +
                "Campagne;" +
                "EAN de la ressource;" +
                "Titre de la ressource;" +
                "Editeur;" +
                "Distributeur;" +
                "Numerique;" +
                "Id de l'offre choisie;" +
                "Type;" +
                "Reassort;" +
                "Quantité;" +
                "Prix HT de la ressource;" +
                "Part prix 5,5%;" +
                "Part prix 20%;" +
                "Prix unitaire TTC;" +
                "Montant total HT;" +
                "Prix total TTC;" +
                "Commentaire;" +
                "2nde Generale;" +
                "1ere Generale;" +
                "Terminale Generale;" +
                "2nde Technologique;" +
                "1ere Technologique;" +
                "Terminale Technologique;" +
                "2nde Professionnelle;" +
                "1ere Professionnelle;" +
                "Terminale Professionnelle;" +
                "BMA 1ere annee;" +
                "BMA 2nde annee;" +
                "CAP 1ere annee;" +
                "CAP 2eme annee;" +
                "CAP 3eme annee" +
                "\n";
    }

    private static String generateExportLineRegion(OrderUniversalModel orderUniversalModel) {
        return orderUniversalModel.getOrderRegionId() + ";" +
                (orderUniversalModel.getValidatorValidationDate() != null ?
                        DateHelper.convertStringDateToOtherFormat(orderUniversalModel.getValidatorValidationDate(), DateHelper.SQL_FULL_FORMAT, DateHelper.SQL_FORMAT) : "") + ";" +
                (orderUniversalModel.getStructure().getName() != null ? orderUniversalModel.getStructure().getName() : "") + ";" +
                (orderUniversalModel.getStructure().getUai() != null ? orderUniversalModel.getStructure().getUai() : "") + ";" +
                (orderUniversalModel.getStructure().getAddress() != null ? orderUniversalModel.getStructure().getAddress() : "") + ";" +
                (orderUniversalModel.getProject().getTitle() != null ? orderUniversalModel.getProject().getTitle() : "") + ";" +
                (orderUniversalModel.getCampaign().getName() != null ? orderUniversalModel.getCampaign().getName() : "") + ";" +
                (orderUniversalModel.getEquipmentKey() != null ? orderUniversalModel.getEquipmentKey() : "") + ";" +
                (orderUniversalModel.getEquipmentName() != null ? orderUniversalModel.getEquipmentName() : "") + ";" +
                (orderUniversalModel.getEquipmentEditor() != null ? orderUniversalModel.getEquipmentEditor() : "") + ";" +
                (orderUniversalModel.getEquipmentDiffusor() != null ? orderUniversalModel.getEquipmentDiffusor() : "") + ";" +
                (orderUniversalModel.getEquipmentType() != null ? orderUniversalModel.getEquipmentType() : "") + ";" +
                (orderUniversalModel.getEquipmentEanLibrary() != null ? orderUniversalModel.getEquipmentEanLibrary() : "") + ";" +
                (orderUniversalModel.getEquipmentCatalogueType() != null ? orderUniversalModel.getEquipmentCatalogueType() : "") + ";" +
                (orderUniversalModel.getReassort() != null ? (orderUniversalModel.getReassort() ? "Oui" : "Non") : "") + ";" +
                exportPriceComment(orderUniversalModel) +
                exportStudents(orderUniversalModel) +
                "\n";
    }

    private static String generateExportLineRegionOffer(OrderUniversalOfferModel orderUniversalOfferModel) {
        OrderUniversalModel orderUniversalModel = orderUniversalOfferModel.getOrderUniversalModel();
        return orderUniversalOfferModel.getIdOffer() + ";" +
                (orderUniversalOfferModel.getOrderUniversalModel().getValidatorValidationDate() != null ?
                        DateHelper.convertStringDateToOtherFormat(
                                orderUniversalOfferModel.getOrderUniversalModel().getValidatorValidationDate(), DateHelper.SQL_FULL_FORMAT, DateHelper.SQL_FORMAT
                        ) : "") + ";" +
                (orderUniversalModel.getStructure().getName() != null ? orderUniversalModel.getStructure().getName() : "") + ";" +
                (orderUniversalModel.getStructure().getUai() != null ? orderUniversalModel.getStructure().getUai() : "") + ";" +
                (orderUniversalModel.getStructure().getAddress() != null ? orderUniversalModel.getStructure().getAddress() : "") + ";" +
                (orderUniversalModel.getProject().getTitle() != null ? orderUniversalModel.getProject().getTitle() : "") + ";" +
                (orderUniversalModel.getCampaign().getName() != null ? orderUniversalModel.getCampaign().getName() : "") + ";" +
                (orderUniversalOfferModel.getEan() != null ? orderUniversalOfferModel.getEan() : "") + ";" +
                (orderUniversalOfferModel.getName() != null ? orderUniversalOfferModel.getName() : "") + ";" +
                ";" +
                ";" +
                ";" +
                ";" +
                (orderUniversalOfferModel.getTypeCatalogue() != null ? orderUniversalOfferModel.getTypeCatalogue() : "") + ";" +
                ";" +
                exportPriceComment(orderUniversalOfferModel) +
                exportStudents(orderUniversalModel) +
                "\n";
    }
}

