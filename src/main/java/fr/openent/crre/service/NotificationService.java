package fr.openent.crre.service;

import java.util.List;

public interface NotificationService {
    /**
     * Send a notification to the prescriber in relation to the list of orderClientEquipment provided in parameter
     * @param orderClientEquipmentIdList list of order client equipment id
     */
    void sendNotificationPrescriber(List<Integer> orderClientEquipmentIdList);

    /**
     * Send a notification to all validators of structures related to the list of orderRegionEquipment provided in parameter
     * @param orderRegionEquipmentIdList list of order region equipment id
     */
    void sendNotificationValidator(List<Integer> orderRegionEquipmentIdList);
}
