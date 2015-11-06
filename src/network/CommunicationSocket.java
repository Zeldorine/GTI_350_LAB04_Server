package network;

// ETS / Genie logiciel
// Annee 2015 - GTI610 Laboratoire 04
//
// Classe CommunicationSocket
//
//
//    + Version 1.0.0	: version initial
//			+ Création de la socket de communication
//
// Auteur : T. Cazorla 
import gti610_lab04_server_tcp.GTI610_LAB04_Server_TCP;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modélisation d'un noeud de communication TCP/IP. <br/>
 * Il est alors possible de modéliser un client ou un serveur.
 *
 * @author Tony CAZORLA
 * @version 1.0.0
 */
public class CommunicationSocket extends Observable {
    /*
     * Adresse du serveur auquel le client se connecte.
     */

    private String adresse;

    /*
     * Port sur lequel est connecté le client au serveur.
     */
    private int portConnecte;

    /*
     * Socket du serveur en liaison au client.
     */
    private Socket socketServer;

    /*
     * Socket du serveur pour accepter une nouvelle connexion d'un client.
     */
    private ServerSocket serveur;

    /*
     * Nom symbolique du serveur
     */
    private String nomSymbolique;

    /*
     * Liste des messages à envoyer au noeud cible via une socket.
     */
    private final BlockingQueue listeEmission;   // Liste des messages a envoyer

    /*
     * Liste des messages reçus au noeud cible via une socket.
     */
    private final BlockingQueue listeReception;  // Liste des messages a recevoir

    /*
     * Statut indiquant l'état de l'émission.
     */
    private final AtomicBoolean statusEmission;  // Status autorisationemettre

    /*
     * Statut indiquant l'état de la réception.
     */
    private final AtomicBoolean statusReception; // Status autorisationrecevoir

    /*
     * Buffer d'entrée en mode "bytes" pour la réception de message.
     */
    private ObjectInputStream bIn;

    /*
     * Buffer de sortie en mode "bytes" pour l'émission de message.
     */
    private ObjectOutputStream bOut;

    /*
     * Statut indiquant l'état de la connexion du noeud.
     */
    private final AtomicBoolean isConnecte;

    /*
     * Statut indiquant l'état du serveur du noeud.
     */
    private final AtomicBoolean isRunning;

    /*
     * Permet de vérifier si l'on est en attente d'un message d'un client.
     */
    private final AtomicBoolean attenteMessage;

    /*
     * Statut indiquant l'état du threads d'émission et du thread de réception.
     */
    private final AtomicBoolean quitterThreads;

    /**
     * Créer un nouveau socket de communication.
     *
     * @param host adresse IP de l'hôte, String.
     * @param port numero de port utilise, int.
     * @param nomSymbolique nomSymbolique du noeud, String.
     * @param observateur observateur du noeud, Observer.
     *
     * @version 1.0.0
     */
    public CommunicationSocket(String host, int port, String nomSymbolique, Observer observateur) {
        if (observateur != null) {
            addObserver(observateur);
        }

        listeEmission = new LinkedBlockingQueue();
        listeReception = new LinkedBlockingQueue();

        statusEmission = new AtomicBoolean(false);
        statusReception = new AtomicBoolean(false);
        isRunning = new AtomicBoolean(false);
        isConnecte = new AtomicBoolean(false);
        attenteMessage = new AtomicBoolean(false);
        quitterThreads = new AtomicBoolean(false);

        new ThreadEmission().start();
        new ThreadReception().start();

        ConnectionSocket noeudS = new CommunicationSocket.ConnectionSocket(nomSymbolique, port);

        noeudS.run();
    }

    /**
     * Permettre de débuter l'emission.
     *
     * @version 1.0.0
     */
    public void debuterEmission() {
        statusEmission.set(true);

        synchronized (statusEmission) {
            statusEmission.notifyAll();
        }
    }

    /**
     * Permettre de débuter la reception.
     *
     * @version 1.0.0
     */
    public void debuterReception() {
        this.statusReception.set(true);

        synchronized (statusReception) {
            statusReception.notifyAll();
        }
    }

    /**
     * Permettre de stopper l'emission.
     *
     * @version 1.0.0
     */
    public void stopperEmission() {
        this.statusEmission.set((boolean) false);
    }

    /**
     * Permettre de stopper la réception.
     *
     * @version 1.0.0
     */
    public void stopperReception() {
        statusReception.set((boolean) false);
    }

    /**
     * Permettre de connaître la socketSupport utilisée.
     *
     * @return socket Retourne la socket utilisée pour les échanges.
     *
     * @version 1.0.0
     */
    public Socket getServerSocket() {
        return socketServer;
    }

    /**
     * Obtenir la liste des messages envoyés.
     *
     * @return BlockingQueue Retourne la liste des messages à envoyer.
     *
     * @version 1.0.0
     */
    public BlockingQueue obtenirMessages() {
        return listeEmission;
    }

    /**
     * Obtenir la liste des messages recus.
     *
     * @return BlockingQueue Retourne la liste des messages recus.
     *
     * @version 1.0.0
     */
    public BlockingQueue obtenirReponses() {
        return listeReception;
    }

    /**
     * Connaître l'état concernant l'émission.
     *
     * @return boolean Retourne le status à propos de l'émission.
     *
     * @version 1.0.0
     */
    public boolean obtenirStatusEmission() {
        return statusEmission.get();
    }

    /**
     * Connaître l'état concernant la réception.
     *
     * @return boolean Retourne le status à propos de la réception.
     *
     * @version 1.0.0
     */
    public boolean obtenirStatusReception() {
        return statusReception.get();
    }

    /**
     * Initialiser les flux de communication.
     *
     * @param Socket La socket sur laquelle l'initialisation des flux entrant et
     * sortant aura lieu.
     *
     * @return boolean La réussite ou l'échec de l'opération
     *
     * @version 1.0.0
     */
    private boolean initFlux(Socket s) {

        // Creer le flux de sortie
        //
        OutputStream streamOut;
        try {
            streamOut = s.getOutputStream();
        } catch (Exception e) {
            return false;
        }
        if (streamOut == null) {
            return false;
        }

        // Creer le buffer de sortie
        //
        try {
            bOut = new ObjectOutputStream(streamOut);
        } catch (Exception e) {
            return false;
        }
        if (bOut == null) {
            return false;
        }

        // Creer le flux d'entree
        //
        InputStream streamIn = null;
        try {
            streamIn = s.getInputStream();
        } catch (Exception e) {
            return false;
        }
        if (streamIn == null) {
            return false;
        }

        // Creer le buffer d'entree
        //
        try {
            bIn = new ObjectInputStream(streamIn);
        } catch (Exception e) {
            return false;
        }
        
        return bIn != null;
    }

    /**
     * Ajouter un message à la liste d'émission.
     *
     * @param msg Message à envoyer
     *
     * @return boolean
     *
     * @version 1.0.0
     */
    public boolean envoyerMessage(Object msg) {
        if (msg == null) {
            return false;
        }

        listeEmission.offer(msg);
        return true;
    }

    /**
     * Attendre un message (blocant pour le thread l'utilisant)
     *
     * @return HashMap Le message recu aprés attente de celui-ci
     *
     * @version 1.0.0
     *
     */
    public Object attendreMessage() {
        // Modifier le boolean d'attenteClient pour préciser que l'on attend un message
        //
        attenteMessage.set(true);
        Object msg = null;

        // Attendre la réception d'un nouveau message (bloquant)q
        //
        try {
            msg = (Object) listeReception.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Modifier le boolean d'attenteClient pour préciser que l'on a recu le message attendu
        //
        attenteMessage.set(false);

        return msg;
    }

    /**
     * Permettre de forcer la fermeture.
     *
     * @return boolean Echec ou réussite de la fermeture
     *
     * @version 1.0.0
     */
    public boolean fermerForce() {

        try {
            // Modifire l'état de la connection et/ou le fonctionnement du serveur/client
            //
            isConnecte.set(false);
            isRunning.set(false);

            if (serveur != null) {
                serveur.close();
            }

            // Fermer les buffers
            //
            if (bIn != null) {
                bIn.close();
            }

            if (bOut != null) {
                bOut.close();
            }

        } catch (Exception e) {
            return false;
        }

        // Stopper les threads d'émission et de réception
        //
        quitterThreads.set(true);

        synchronized (quitterThreads) {
            quitterThreads.notifyAll();
        }

        return true;
    }

    /**
     * Permettre de fermer le noeud. Echoue si un message est en attente.
     *
     * @return HashMap Echec ou réussite de la fermeture
     *
     * @version 1.0.0
     */
    public boolean fermer() {
        // Verifier que l'on ne soit pas en attente d'un message
        //
        if (attenteMessage.equals(new AtomicBoolean(false))) {
            return fermerForce();
        } else {
            return false;
        }
    }

    /**
     * Modélisation de la socket de reception de la connexion d'un client.
     *
     * @author Tony CAZORLA
     * @version 1.0.0
     */
    private class ConnectionSocket extends Thread {
        /*
         * Port d'écoute pour la réception d'un nouveau client.
         */

        private final int portReception;

        /**
         * Créer une socket de connexion recevant des connexions sur le port
         * indiqué.
         *
         * @param nomThread Nom alloue au thread d'attente de connexion d'un client.
         * @param port Port de connexion.
         *
         * @version 1.0.0
         */
        public ConnectionSocket(String nomThread, int port) {
            super(nomThread);
            portReception = port;
        }

        /**
         * Accepter un Client.
         *
         * @return boolean Echec ou réussite de l'opération.
         *
         * @version 1.0.0
         */
        private boolean accepter() {
            try {
                serveur = new ServerSocket(portReception);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Erreur lors de la creation de la socket serveur");
                return false;
            }

            try {
                socketServer = serveur.accept();
                isRunning.set(true);
                adresse = socketServer.getLocalAddress().getHostAddress();
                portConnecte = portReception;

                System.out.println("Client connecté ! Adresse : " + socketServer.getLocalAddress().getHostAddress() + ", port : " + socketServer.getLocalPort());
            } catch (SocketTimeoutException s) {
            } catch (Exception e) {
                return false;
            }

            return initFlux(socketServer);
        }

        /**
         * Etablir la connexion avec un client cible.
         *
         * @return void
         *
         * @version 1.0.0
         */
        @Override
        public void run() {
            if (accepter()) {
                debuterEmission();
                debuterReception();
            }
        }
    }

    /**
     * Savoir si le serveur est en execution.
     *
     * @return boolean True si le serveur tourne, False s'il est arrété.
     *
     * @version 1.0.0
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Savoir si le noeud est connecté.
     *
     * @return boolean True si le noeud est connecté, False s'il ne l'est pas.
     *
     * @version 1.0.0
     */
    public boolean estConnecte() {
        return isConnecte.get();
    }

    /**
     * Recupérer le nom symbolique du noeud.
     *
     * @return String
     *
     * @version 1.0.0
     */
    public String getNomSymbolique() {
        return nomSymbolique;
    }

    /**
     * Représenter la valeur courante de l'object.
     *
     * @override @see java.lang.String de la classe @see java.lang.Object.
     *
     * @return Le string représentant la valeur courante.
     *
     * @version 1.0.0
     */
    @Override
    public String toString() {
        return "| Server - Adresse : " + adresse + " | Port : " + portConnecte + " -" + " | Pseudo : " + nomSymbolique + " |";
    }
    
    public String getClientIP(){
    return socketServer.getRemoteSocketAddress().toString();
    }

    /**
     * Effectuer les émissions de messages.
     *
     * @author Tony CAZORLA
     * @version 1.0.0
     */
    private class ThreadEmission extends Thread {

        /**
         * Permettre l'émission de message.
         *
         * @override Méthode run de la classe Thread.
         *
         * @return void
         *
         * @version 1.0.0
         */
        @Override
        public void run() {
            Object msg;

            while (!quitterThreads.get()) {
                if (!quitterThreads.get()) {
                    synchronized (quitterThreads) {
                        if (quitterThreads.get() == true) {
                            break;
                        }
                    }
                }

                if (!statusEmission.get()) {
                    synchronized (statusEmission) {
                        try {
                            statusEmission.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                // Envoyer le message
                //
                try {
                    msg = listeEmission.poll();

                    if (bOut != null) {
                        if (msg != null) {
                            bOut.writeObject(msg);
                        }

                        bOut.flush();
                    }
                } catch (IOException e) {
                    setChanged();
                    notifyObservers(GTI610_LAB04_Server_TCP.ERROR_CODE_CLIENT);
                    close();
                }
            }
        }
    }

    /**
     * Permettre la réception de message.
     *
     * @author Tony CAZORLA
     *
     * @version 1.0.0
     */
    private class ThreadReception extends Thread {

        /**
         * Permettre une réception éventuelle de message en continue.
         *
         * @override Méthode run de la classe Thread.
         *
         * @return void
         *
         * @version 1.0.0
         */
        @Override
        public void run() {
            Object msg = null;

            while (!quitterThreads.get()) {
                if (!quitterThreads.get()) {
                    synchronized (quitterThreads) {
                        if (quitterThreads.get() == true) {
                            break;
                        }
                    }
                }

                if (!statusReception.get()) {
                    synchronized (statusReception) {
                        try {
                            statusReception.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                try {
                    msg = bIn.readObject();
                } catch (SocketException e) {
                    setChanged();
                    notifyObservers(GTI610_LAB04_Server_TCP.ERROR_CODE_CLIENT);
                    close();
                } catch (EOFException e) {
                } catch (IOException e) {
                } catch (ClassNotFoundException e) {
                } catch (Exception e) {
                }

                if (msg != null) {
                    setChanged();
                    notifyObservers(msg);
                    listeReception.add(msg);
                }
            }
        }
    }
    
    private void close() {
        stopperEmission();
        stopperReception();
        listeEmission.clear();
        listeReception.clear();
        fermerForce();
    }
}
