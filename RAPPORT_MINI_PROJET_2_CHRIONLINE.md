# Mini Projet 2 : Sécurisation d’une Application E-commerce Java avec AES et RSA

## 1. Page de garde

- **Application** : ChriOnline
- **Module** : Sécurité Informatique
- **Équipe** : 4 étudiants ingénieurs
- **Année universitaire** : 2025/2026
- **Technologies** : Java 17, JavaFX, TCP Sockets, SQLite, AES, RSA, PKCS12, Maven

---

## 2. Introduction générale

Le projet ChriOnline était initialement une application e-commerce client-serveur en Java, avec un client JavaFX, un serveur TCP, une base SQLite et un modèle de communication basé sur des objets `Request` et `Response`.

Le Mini Projet 2 demandait de sécuriser cette application avec un protocole inspiré de HTTPS. L’objectif principal était de protéger à la fois les communications réseau et les données sensibles stockées dans la base.

Après les modifications réalisées, le projet combine maintenant :

- RSA pour l’échange sécurisé de la clé de session AES
- AES pour chiffrer les communications client-serveur
- RSA challenge-response pour l’authentification administrateur
- chiffrement des champs sensibles en base
- amélioration de l’interface client et ajout d’une interface admin JavaFX

---

## 3. Rappel des exigences du Mini Projet 2

| Exigence | Implémentation dans ChriOnline |
|---|---|
| Application e-commerce Java client-serveur | Conservée avec un client JavaFX, un serveur TCP Java et SQLite |
| Client JavaFX/Swing | Client JavaFX conservé et modernisé dans `client.ui.*` |
| Serveur Java | Serveur Java conservé dans `server.main.ServerMain` et `server.network.ServerTCP` |
| Base de données sécurisée | SQLite conservée, avec chiffrement des champs sensibles via `server.security.DatabaseCryptoService` |
| Protocole sécurisé type HTTPS | Handshake RSA/AES ajouté dans `client.network.ClientTCP`, `adminclient.network.AdminClientHandler` et `server.network.ClientHandler` |
| RSA pour l’échange de clé | Implémenté via `shared.security.RSAUtil` et `server.security.ServerKeyManager` |
| AES pour chiffrer les données | Implémenté via `shared.security.AESUtil`, `shared.security.SecureMessage`, `shared.security.SecureSession` |
| Sécurisation des données stockées | Vérification email et adresse de livraison chiffrées avant stockage |

---

## 4. Architecture globale de l’application

L’architecture finale repose sur cinq blocs principaux :

- un client JavaFX pour les clients
- un client JavaFX pour les administrateurs
- un serveur Java TCP
- une base SQLite
- une couche de sécurité partagée

Diagramme simplifié :

```text
Client JavaFX / Admin JavaFX
   |
   | 1. Handshake sécurisé RSA/AES
   | 2. Requêtes/Réponses chiffrées en AES
   v
Serveur Java TCP
   |
   | DAO / Services
   v
Base SQLite sécurisée
```

Organisation des packages :

- `shared.security` : AES, RSA, `SecureMessage`, `SecureSession`
- `shared.network` : `Request`, `Response`
- `server.security` : gestion de clés serveur, chiffrement base
- `server.network` : acceptation des connexions et traitement client
- `server.service` : logique métier
- `server.dao` : accès SQLite
- `client.ui` : interface client JavaFX
- `adminclient.ui` : interface admin JavaFX

---

## 5. Protocole sécurisé inspiré de HTTPS

Le protocole implémenté est volontairement simplifié pour un cadre pédagogique, mais il reproduit l’idée essentielle d’HTTPS : utiliser RSA pour protéger une clé symétrique, puis utiliser AES pour toutes les données échangées.

Étapes du protocole :

1. Le client se connecte au serveur TCP.
2. Le serveur envoie sa clé publique RSA.
3. Le client génère une clé de session AES-256 aléatoire.
4. Le client chiffre cette clé AES avec la clé publique RSA du serveur.
5. Le serveur déchiffre la clé AES avec sa clé privée RSA.
6. Toutes les requêtes et réponses suivantes sont chiffrées en AES/GCM.

| Étape | Acteur | Opération | Algorithme |
|---|---|---|---|
| 1 | Serveur | Envoi de la clé publique | RSA |
| 2 | Client | Génération de clé de session | AES-256 |
| 3 | Client | Chiffrement de la clé AES | RSA/OAEP SHA-256 |
| 4 | Serveur | Déchiffrement de la clé AES | RSA |
| 5 | Client + Serveur | Échange de messages applicatifs | AES/GCM |

Pourquoi RSA n’est pas utilisé pour chiffrer toutes les données :

- RSA est beaucoup plus lent que AES
- RSA convient bien pour protéger une petite donnée comme une clé
- AES est plus rapide et adapté à un flux continu de requêtes/réponses

Dans le code, ce protocole est matérialisé par :

- `client.network.ClientTCP`
- `adminclient.network.AdminClientHandler`
- `server.network.ClientHandler`
- `shared.security.RSAUtil`
- `shared.security.AESUtil`
- `server.security.ServerKeyManager`

---

## 6. Implémentation AES

Le chiffrement symétrique est réalisé après le handshake sécurisé.

Caractéristiques techniques :

- mode : `AES/GCM/NoPadding`
- IV aléatoire de 12 octets pour chaque message
- tag d’authentification de 128 bits
- sérialisation des objets `Request` et `Response`, puis chiffrement

Le conteneur réseau utilisé est `SecureMessage`, qui contient :

- `byte[] iv`
- `byte[] ciphertext`

Rôle des classes :

- `AESUtil.java` : génération de clé AES, chiffrement et déchiffrement d’objets sérialisables
- `SecureMessage.java` : enveloppe réseau chiffrée
- `SecureSession.java` : stocke la clé AES d’une session socket
- `ClientTCP.java` : chiffre les requêtes et déchiffre les réponses côté client
- `ClientHandler.java` : déchiffre les requêtes et chiffre les réponses côté serveur

Ainsi, après le handshake, les objets `Request` et `Response` ne transitent plus en clair sur le socket.

---

## 7. Implémentation RSA

### A. RSA pour l’échange de clé AES

Le serveur possède une paire de clés RSA.

- chargée ou générée automatiquement via `ServerKeyManager`
- stockée dans `config/server-keystore.p12`

Le client récupère la clé publique, chiffre la clé AES, puis le serveur la déchiffre avec sa clé privée.

Classes concernées :

- `shared.security.RSAUtil`
- `server.security.ServerKeyManager`
- `client.network.ClientTCP`
- `adminclient.network.AdminClientHandler`
- `server.network.ClientHandler`

### B. RSA challenge-response pour l’authentification admin

Cette partie existait déjà dans le projet et a été conservée.

Fonctionnement :

1. l’admin saisit son email et son keystore PKCS12
2. le client admin envoie `ADMIN_AUTH_REQUEST`
3. le serveur génère un challenge aléatoire
4. l’admin signe ce challenge avec sa clé privée
5. le client envoie `ADMIN_AUTH_VERIFY`
6. le serveur vérifie la signature avec la clé publique stockée en base

Ainsi, aucun mot de passe administrateur n’est transmis au serveur.

Classes concernées :

- `server.service.AdminAuthService`
- `adminclient.SetupUtility`
- `adminclient.network.AdminClientHandler`
- `adminclient.ui.AdminAuthView`

Différence importante :

- **RSA key exchange** : sert à établir une clé AES de session pour chiffrer la communication
- **RSA authentication** : sert à prouver l’identité de l’administrateur par signature

Cette distinction est centrale pour expliquer le projet au professeur.

---

## 8. Sécurisation de la base de données

Toutes les données n’ont pas été chiffrées. Seuls les champs réellement sensibles ont été protégés.

Champs protégés :

- code de vérification email
- adresse de livraison ou de facturation si elle est stockée avec le paiement simulé

Champs non stockés ou masqués :

- numéro de carte : stocké uniquement sous forme masquée
- CVV : jamais stocké

Champs laissés en clair pour des raisons fonctionnelles :

- email utilisateur pour la recherche de compte et la connexion
- noms de produits
- prix
- stock
- catégories

Classe responsable :

- `server.security.DatabaseCryptoService`

Caractéristiques :

- chiffrement en AES/GCM
- clé chargée depuis `DB_AES_MASTER_KEY_BASE64`
- aucune clé n’est codée en dur dans les classes Java
- utilisation des variables d’environnement ou du fichier `.env`

| Donnée | Protection |
|---|---|
| email | conservé en clair pour la connexion |
| code de vérification | chiffré avant stockage |
| numéro de carte | masqué uniquement |
| CVV | jamais stocké |
| adresse de livraison | chiffrée |
| données produit | non sensibles, donc non chiffrées |

---

## 9. Sécurité déjà existante conservée

Les mécanismes déjà présents dans le projet n’ont pas été supprimés.

- **Brute-force protection** : limite les tentatives de connexion répétées dans `UserService`
- **Protection anti-rejeu** : validation de `timestamp` et `nonce` dans `ClientHandler`
- **Session token management** : jetons de session conservés dans `activeSessions`
- **Thread pool** : limite les connexions simultanées via `ServerTCP`
- **IP rate limiting** : restriction du nombre de connexions par IP
- **Input validation** : validation email et username côté serveur
- **Email verification** : flux de vérification conservé
- **Order confirmation email** : service d’email conservé

---

## 10. Interface client améliorée

L’interface client a été entièrement modernisée en JavaFX sans utiliser FXML.

Améliorations principales :

- style inspiré d’une boutique électronique moderne
- palette bleu clair / blanc / accent vert
- écran d’authentification en split-screen
- badges de sécurité visibles
- tableau produit remplacé par une grille de cartes
- carte produit mise en avant
- filtre par catégories
- recherche
- panier modernisé avec panneau de résumé
- cartes de commandes avec statut visuel

Les catégories mises en avant sont :

- Smartphones
- Laptops
- Tablets
- Accessories

Les produits affichés sont filtrés sur :

`categorie_metier = 'Electronique'`

Classes principales :

- `client.ui.AuthView`
- `client.ui.MainDashboard`
- `client.ui.ProductView`
- `client.ui.CartView`
- `client.ui.OrderView`
- `src/main/resources/styles/app.css`

---

## 11. Interface admin améliorée

L’ancienne interface console a été conservée, mais une interface JavaFX a été ajoutée pour une démonstration plus professionnelle.

Flux d’authentification admin :

1. l’administrateur saisit email, chemin du keystore, mot de passe et alias
2. l’application demande un challenge au serveur
3. le challenge est signé avec la clé privée du keystore PKCS12
4. le serveur vérifie la signature
5. le dashboard s’ouvre si la signature est valide

Fonctionnalités ajoutées :

### 1. Product Management

- consultation des produits électroniques
- ajout d’un produit
- modification d’un produit
- suppression d’un produit
- mise à jour du stock et de la disponibilité
- recherche et filtrage

### 2. Order Management

- consultation de toutes les commandes
- filtre par statut
- consultation des lignes de commande
- mise à jour du statut

### 3. Dashboard Statistics

- nombre total de produits
- produits en stock faible
- nombre total de commandes
- chiffre d’affaires issu des paiements validés
- commandes récentes
- produits les mieux notés

Toutes les requêtes d’administration exigent :

- un `sessionToken` valide
- un utilisateur avec rôle `ADMIN`

Classes principales :

- `adminclient.main.AdminFxMain`
- `adminclient.ui.AdminAuthView`
- `adminclient.ui.AdminDashboardView`
- `adminclient.ui.AdminProductsView`
- `adminclient.ui.AdminOrdersView`
- `adminclient.ui.AdminStatsView`

---

## 12. Intégration du dataset électronique

Le modèle produit a été enrichi pour refléter la structure demandée :

- `sku`
- `nom_produit`
- `marque`
- `categorie_source`
- `categorie_metier`
- `prix_usd`
- `remise_pct`
- `prix_net_usd`
- `rating`
- `stock`
- `disponibilite`
- `description`
- `image_principale`
- `nb_images`
- `source_catalogue`

La classe `shared.model.Produit` et le DAO `server.dao.ProduitDAO` ont été mis à jour en conséquence.

Point important :

- le fichier SQL riche mentionné dans l’énoncé n’était pas présent dans le workspace au moment de l’implémentation
- pour garder une démonstration cohérente, le schéma a été migré et une sélection de produits électroniques représentatifs a été injectée lors de l’initialisation de la base

Cela améliore :

- le réalisme de la boutique
- la richesse visuelle des cartes produit
- la gestion produit côté admin

---

## 13. Scénario de démonstration devant le professeur

### Step 1 — Démarrer le serveur

- lancer le serveur
- montrer l’initialisation SQLite
- montrer les logs de handshake sécurisé

### Step 2 — Démarrer le client customer

- ouvrir l’écran d’authentification moderne
- se connecter ou créer un compte
- expliquer que le handshake RSA/AES a lieu avant les requêtes

### Step 3 — Parcourir les produits

- montrer la grille de produits
- faire une recherche
- appliquer un filtre catégorie
- ajouter un article au panier

### Step 4 — Créer une commande

- ouvrir le panier
- valider la commande
- rappeler que les objets `Request/Response` sont chiffrés en AES

### Step 5 — Simuler un paiement

- choisir un mode de paiement
- montrer que le numéro est masqué
- expliquer que le CVV n’est pas stocké
- observer le changement de statut

### Step 6 — Démarrer le client admin

- ouvrir l’application admin JavaFX
- s’authentifier avec le challenge-response RSA
- rappeler qu’aucun mot de passe admin n’est envoyé

### Step 7 — Utiliser le dashboard admin

- afficher les statistiques
- afficher les produits
- modifier le stock ou la disponibilité
- afficher les commandes
- changer un statut

### Step 8 — Expliquer la protection de la base

- montrer les colonnes sensibles chiffrées ou masquées
- expliquer `DB_AES_MASTER_KEY_BASE64`

---

## 14. Script oral pour la présentation

### Étudiant 1

Bonjour. Notre projet s’appelle ChriOnline. À l’origine, c’était une application e-commerce Java client-serveur avec JavaFX côté client, TCP sockets pour la communication et SQLite pour la base de données. Le Mini Projet 2 nous demandait de sécuriser cette application avec AES et RSA, tout en gardant l’architecture Java existante.

Nous avons donc conservé le serveur Java, le client JavaFX et la base SQLite, puis nous avons ajouté une vraie couche de sécurité réseau et une meilleure protection des données sensibles. L’architecture finale repose sur deux clients JavaFX, un pour le client normal et un pour l’administrateur, un serveur TCP Java, une base SQLite et une couche partagée `shared.security` pour la cryptographie.

### Étudiant 2

La partie la plus importante est le protocole sécurisé inspiré de HTTPS. Quand le client se connecte, le serveur envoie sa clé publique RSA. Ensuite, le client génère une clé AES-256 aléatoire. Cette clé AES est chiffrée avec RSA et envoyée au serveur. Le serveur la déchiffre avec sa clé privée. À partir de ce moment, toutes les requêtes et toutes les réponses sont chiffrées avec AES/GCM.

Nous n’avons pas utilisé RSA pour chiffrer tout le trafic, parce que RSA est lent et surtout adapté aux petites données comme une clé. AES est beaucoup plus rapide et plus réaliste pour les échanges continus. Dans le code, cela passe par `AESUtil`, `RSAUtil`, `SecureMessage`, `ClientTCP` et `ClientHandler`.

### Étudiant 3

Concernant la base de données, nous avons choisi de ne pas tout chiffrer aveuglément. Les données métier comme les noms de produits, les prix, le stock ou les catégories restent en clair car elles doivent être cherchées et affichées. En revanche, les données sensibles comme le code de vérification email et l’adresse de livraison sont chiffrées avant stockage avec `DatabaseCryptoService`.

Nous avons aussi conservé les protections déjà présentes dans le projet initial : la protection contre le brute-force, la protection anti-rejeu avec timestamp et nonce, les tokens de session, le thread pool, la limitation par IP, la validation d’entrée et la vérification email. Enfin, la boutique a été convertie en magasin électronique avec un modèle produit enrichi.

### Étudiant 4

Pour l’interface, nous avons modernisé le client avec une interface JavaFX plus présentable : écran d’authentification en split-screen, grille de produits, filtres par catégorie, cartes produit avec image, prix, note et état du stock, ainsi qu’un panier et une vue commandes plus modernes.

Nous avons aussi ajouté une vraie interface admin JavaFX. L’authentification admin se fait toujours par challenge-response RSA avec un keystore PKCS12. Après authentification, l’admin peut gérer les produits, consulter les commandes et voir les statistiques. En conclusion, nous avons conservé les fonctionnalités e-commerce initiales, ajouté AES et RSA, sécurisé les données sensibles et amélioré la présentation générale du projet.

---

## 15. Questions possibles du professeur + réponses

1. **Pourquoi utiliser RSA et AES ?**  
Parce que RSA sert à protéger l’échange initial de la clé AES, et AES sert ensuite à chiffrer efficacement toutes les communications.

2. **Pourquoi ne pas tout chiffrer avec RSA ?**  
Parce que RSA est trop lent pour un trafic applicatif continu et n’est pas conçu pour de gros volumes de données.

3. **Quel est le rôle de AES dans ce projet ?**  
AES chiffre toutes les requêtes et réponses après le handshake.

4. **Quel est le rôle de RSA dans ce projet ?**  
RSA protège la clé AES de session et sert aussi à l’authentification admin par signature.

5. **Quelle différence entre échange de clé RSA et authentification admin RSA ?**  
L’échange de clé RSA sert à établir la confidentialité de session. L’authentification admin sert à prouver l’identité de l’administrateur.

6. **Le mot de passe admin est-il envoyé au serveur ?**  
Non. L’admin signe un challenge avec sa clé privée. Le serveur ne reçoit qu’une signature.

7. **Comment protégez-vous contre le rejeu ?**  
Chaque requête porte un `timestamp` et un `nonce`, vérifiés côté serveur.

8. **Que se passe-t-il si quelqu’un capture un paquet TCP ?**  
Après le handshake, le contenu intercepté est chiffré en AES/GCM, donc il n’est pas lisible sans la clé de session.

9. **Pourquoi AES/GCM et pas AES/ECB ?**  
Parce que GCM apporte confidentialité et intégrité. ECB est faible et ne doit pas être utilisé pour ce cas.

10. **Pourquoi le CVV n’est-il pas stocké ?**  
Parce que c’est une donnée extrêmement sensible et inutile pour un paiement simulé.

11. **Pourquoi les noms et prix produits ne sont pas chiffrés ?**  
Parce qu’ils ne sont pas des données sensibles et doivent rester facilement recherchables et affichables.

12. **Comment la clé de chiffrement de la base est-elle protégée ?**  
Elle est chargée depuis `DB_AES_MASTER_KEY_BASE64` et n’est pas codée en dur dans les classes Java.

13. **Que se passe-t-il si la clé AES de session est compromise ?**  
La session concernée peut être lue, mais cela ne donne pas accès à la clé maître de base ni aux autres sessions.

14. **Comment prouvez-vous que la communication est chiffrée ?**  
Les objets échangés après handshake sont des `SecureMessage` contenant `iv` et `ciphertext`, pas des `Request/Response` en clair.

15. **Quelles sont les limites par rapport à HTTPS réel ?**  
Il n’y a pas d’autorité de certification, pas de vérification de certificat, pas de rotation complète de clés ni toute la robustesse d’un vrai TLS.

---

## 16. Limites du projet

Nous avons volontairement gardé une approche honnête sur les limites :

- c’est un protocole pédagogique inspiré de HTTPS, pas un remplacement de TLS
- SQLite est acceptable pour un prototype universitaire, mais pas pour une forte montée en charge
- le mot de passe client est hashé côté client, mais une stratégie serveur plus robuste avec salage dédié serait préférable en production
- la rotation de clés n’est pas entièrement gérée
- il n’y a pas de vérification par autorité de certification
- le dataset électronique complet mentionné dans l’énoncé n’était pas disponible dans le workspace, donc une base de démonstration cohérente a été constituée à partir de produits électroniques représentatifs

---

## 17. Conclusion

Le projet ChriOnline satisfait désormais les objectifs du Mini Projet 2 :

- il conserve les fonctionnalités e-commerce initiales
- il ajoute une communication sécurisée RSA/AES
- il protège les données sensibles stockées
- il conserve et améliore l’authentification admin RSA
- il améliore fortement l’interface client et ajoute une interface admin JavaFX
- il exploite un catalogue électronique réaliste au niveau du schéma et des données de démonstration

---

## 18. Annexe technique

### A. Fichiers modifiés

- `.gitignore` : ajout des règles pour `.db`
- `database/schema.sql` : schéma enrichi produits/paiements/utilisateurs
- `pom.xml` : configuration de lancement client/admin
- `client.network.ClientTCP` : handshake RSA/AES + messages AES
- `server.network.ClientHandler` : handshake serveur + chiffrement AES + routes admin
- `server.network.ServerTCP` : injection du gestionnaire de clés serveur
- `server.database.DBConnection` : migrations et seed électronique
- `server.dao.UserDAO` : chiffrement du code de vérification
- `server.dao.ProduitDAO` : nouveau modèle produit électronique
- `server.dao.CommandeDAO` : statistiques et détails de commande
- `server.dao.PaiementDAO` : stockage du numéro masqué et de l’adresse chiffrée
- `server.service.ProduitService` : recherche, filtre, CRUD admin
- `server.service.CommandeService` : gestion admin des commandes et stats
- `server.service.PaiementService` : protection des données de paiement simulé
- `shared.model.Produit` : modèle enrichi
- `shared.model.Paiement` : masked card + adresse chiffrée
- `shared.model.Commande` : email/username pour l’admin
- `shared.model.LignePanier` : calcul basé sur `prixNetUsd`
- `client.ui.*` : redesign complet du frontend client

### B. Nouveaux fichiers

- `shared.security.AESUtil`
- `shared.security.RSAUtil`
- `shared.security.SecureMessage`
- `shared.security.SecureSession`
- `server.security.ServerKeyManager`
- `server.security.DatabaseCryptoService`
- `shared.model.DashboardStats`
- `shared.model.OrderItemDetail`
- `adminclient.main.AdminFxMain`
- `adminclient.ui.AdminAuthView`
- `adminclient.ui.AdminDashboardView`
- `adminclient.ui.AdminProductsView`
- `adminclient.ui.AdminOrdersView`
- `adminclient.ui.AdminStatsView`
- `src/main/resources/styles/app.css`
- `README_SECURITY_DEMO.md`

### C. Commandes de lancement

Serveur :

```powershell
mvn exec:java "-Dexec.mainClass=server.main.ServerMain"
```

Client customer :

```powershell
mvn javafx:run "-DmainClass=client.main.ClientMain"
```

Client admin JavaFX :

```powershell
mvn javafx:run "-DmainClass=adminclient.main.AdminFxMain"
```

Setup admin :

```powershell
mvn exec:java "-Dexec.mainClass=adminclient.SetupUtility"
```

### D. Variables d’environnement

- `GMAIL_EMAIL`
- `GMAIL_APP_PASSWORD`
- `DB_AES_MASTER_KEY_BASE64`
- `SERVER_KEYSTORE_PASSWORD`

### E. Génération de `DB_AES_MASTER_KEY_BASE64`

Méthode PowerShell :

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

### F. Génération du keystore admin PKCS12

```powershell
keytool -genkeypair -alias admin -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore admin_keystore.p12 -validity 365 -dname "CN=Admin, OU=IT, O=ChriOnline, L=Casablanca, S=Casablanca, C=MA"
```
