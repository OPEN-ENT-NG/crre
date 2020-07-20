package fr.openent.crre.service;

public interface NotificationService {

    /**
     * Send message to notification system
     * @param text Text to send
     */
    void sendMessage(String text);
}
