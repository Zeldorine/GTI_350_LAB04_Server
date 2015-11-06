package gti610_lab04_server_tcp;

import java.util.Observable;
import java.util.Observer;
import network.CommunicationSocket;

/**
 *
 * @author Tony CAZORLA
 */
public class GTI610_LAB04_Server_TCP extends Thread implements Observer {

    private static CommunicationSocket communicationSocket;
    public static final String ERROR_CODE_CLIENT = "1";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        GTI610_LAB04_Server_TCP server = new GTI610_LAB04_Server_TCP();
        server.start();
    }

    public GTI610_LAB04_Server_TCP() {
        System.out.println("\t Lancement du serveur");
        System.out.print("* Creation et demarrage du serveur TCP/IP ");
        System.out.println("(V 1.0.0)\n");

        communicationSocket = new CommunicationSocket("127.0.0.1", 9980, "Socket de communication avec le client pour GTI610", this);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String) {
            traiterMesssage((String) arg);
        }
    }

    private void traiterMesssage(String message) {
        if (message.equalsIgnoreCase("exit") || message.equalsIgnoreCase(ERROR_CODE_CLIENT)) {
            finCommunication();
        } else {
            System.out.println(message + " - " + communicationSocket.getClientIP());
            communicationSocket.envoyerMessage(message.toUpperCase());
        }
    }

    private void finCommunication() {
        communicationSocket.stopperEmission();
        communicationSocket.stopperReception();
        communicationSocket.fermerForce();
        System.out.println("client deconnecte\n");
        System.exit(0);
    }
}
