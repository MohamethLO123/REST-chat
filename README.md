# REST Chat

Application de messagerie en temps réel basée sur une architecture **REST** en Java pur, sans framework externe. Les clients communiquent via un serveur HTTP qui expose une API JSON.

---

## Table des matières

1. [Architecture](#architecture)
2. [Prérequis](#prérequis)
3. [Structure du projet](#structure-du-projet)
4. [Installation et compilation](#installation-et-compilation)
5. [Lancer l'application](#lancer-lapplication)
6. [API REST](#api-rest)
7. [Clients disponibles](#clients-disponibles)
8. [Technologies utilisées](#technologies-utilisées)

---

## Architecture

```
┌──────────────┐   POST /messages   ┌─────────────────────┐
│   Client A   │ ─────────────────► │                     │
│  (GUI / CLI) │                    │   ChatServer        │
│              │ ◄───────────────── │   localhost:8080    │
└──────────────┘   GET  /messages   │                     │
                                    └─────────────────────┘
┌──────────────┐   POST /messages          ▲
│   Client B   │ ─────────────────────────►│
│  (GUI / CLI) │ ◄─────────────────────────┘
└──────────────┘   GET  /messages
```

- Le **serveur** expose un endpoint unique `/messages` qui accepte `GET` et `POST`.
- Chaque **client** envoie ses messages via `POST` et récupère les messages des autres via `GET` (polling toutes les 2 secondes).
- Les messages sont stockés **en mémoire** sur le serveur (réinitialisés au redémarrage).

---

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java (JDK) | 11 ou supérieur |
| Git | toute version récente |

Vérifiez votre installation :
```bash
java -version
javac -version
```

---

## Structure du projet

```
REST-Chat/
├── server/
│   ├── ChatServer.java      # Point d'entrée du serveur HTTP (port 8080)
│   ├── ChatService.java     # Gestionnaire des requêtes GET et POST
│   └── Message.java         # Modèle de données (pseudo + contenu)
├── client/
│   ├── ChatClient.java      # Client en ligne de commande (terminal)
│   └── ChatClientGUI.java   # Client avec interface graphique (Swing)
├── lib/
│   └── gson-2.10.1.jar      # Bibliothèque de sérialisation JSON
├── bin/                     # Bytecode compilé (ignoré par Git)
├── .gitignore
└── README.md
```

---

## Installation et compilation

### 1. Cloner le dépôt

```bash
git clone <url-du-depot>
cd REST-Chat
```

### 2. Créer le dossier de sortie

```bash
# Windows (PowerShell)
mkdir bin

# Linux / macOS
mkdir bin
```

### 3. Compiler les sources

**Windows (PowerShell) :**
```powershell
javac -cp "lib/*" -d bin server\Message.java server\ChatService.java server\ChatServer.java client\ChatClient.java client\ChatClientGUI.java
```

**Linux / macOS :**
```bash
javac -cp "lib/*" -d bin server/Message.java server/ChatService.java server/ChatServer.java client/ChatClient.java client/ChatClientGUI.java
```

> Si la compilation réussit, aucun message n'est affiché. Le dossier `bin/` contiendra tous les fichiers `.class`.

---

## Lancer l'application

> **Important :** le serveur doit toujours être démarré **avant** les clients.

### Étape 1 — Démarrer le serveur

**Windows :**
```powershell
java -cp "lib/*;bin" server.ChatServer
```

**Linux / macOS :**
```bash
java -cp "lib/*:bin" server.ChatServer
```

Vous devriez voir :
```
✅ Serveur REST démarré sur http://localhost:8080/messages
```

---

### Étape 2 — Lancer un ou plusieurs clients

Ouvrez un **nouveau terminal** pour chaque participant.

#### Client graphique (recommandé)

**Windows :**
```powershell
java -cp "lib/*;bin" client.ChatClientGUI
```

**Linux / macOS :**
```bash
java -cp "lib/*:bin" client.ChatClientGUI
```

Une fenêtre de connexion s'ouvre — entrez votre pseudo puis cliquez sur **Rejoindre**.

#### Client terminal

**Windows :**
```powershell
java -cp "lib/*;bin" client.ChatClient
```

**Linux / macOS :**
```bash
java -cp "lib/*:bin" client.ChatClient
```

Saisissez votre pseudo, puis tapez vos messages. Appuyez sur **Entrée** pour envoyer.

---

## API REST

Le serveur expose un seul endpoint :

### `POST /messages` — Envoyer un message

**URL :** `http://localhost:8080/messages`

**Corps de la requête (JSON) :**
```json
{
  "pseudo": "Mohameth",
  "contenu": "Bonjour tout le monde !"
}
```

**Réponse :**
```
200 OK
Message reçu
```

**Exemple avec curl :**
```bash
curl -X POST http://localhost:8080/messages \
     -H "Content-Type: application/json" \
     -d '{"pseudo":"Mohameth","contenu":"Hello !"}'
```

---

### `GET /messages` — Récupérer tous les messages

**URL :** `http://localhost:8080/messages`

**Réponse (JSON formaté) :**
```json
[
  {
    "pseudo": "I.Fall",
    "contenu": "Bonjour la classe"
  },
  {
    "pseudo": "Mohameth",
    "contenu": "Bonjour !"
  }
]
```

**Exemple avec curl :**
```bash
curl http://localhost:8080/messages
```

Vous pouvez aussi consulter les messages directement dans votre navigateur à l'adresse :
```
http://localhost:8080/messages
```

---

## Clients disponibles

### Client graphique (`ChatClientGUI`)

| Fonctionnalité | Description |
|---|---|
| Bulles de messages | Vos messages à droite (bleu), ceux des autres à gauche (gris) |
| Avatars | Cercle coloré avec l'initiale du pseudo, couleur unique par utilisateur |
| Horodatage | Heure d'envoi affichée sous chaque message |
| Actualisation automatique | Nouveaux messages récupérés toutes les **2 secondes** |
| Indicateur de connexion | Point vert **En ligne** / rouge **Connexion perdue** |
| Notification | La barre de titre clignote lors d'un nouveau message si la fenêtre est inactive |
| Raccourci clavier | **Entrée** = envoyer, **Shift + Entrée** = saut de ligne |

### Client terminal (`ChatClient`)

- Saisie du pseudo au démarrage
- Envoi de messages ligne par ligne
- Affichage des messages des autres participants au format `[Pseudo] message`
- Actualisation automatique toutes les **2 secondes** en arrière-plan

---

## Technologies utilisées

| Composant | Technologie |
|---|---|
| Serveur HTTP | `com.sun.net.httpserver.HttpServer` (JDK intégré) |
| Sérialisation JSON | [Gson 2.10.1](https://github.com/google/gson) |
| Interface graphique | Java Swing |
| Communication réseau | `java.net.HttpURLConnection` |
| Langage | Java 11+ |
