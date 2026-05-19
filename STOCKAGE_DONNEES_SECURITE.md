# Stockage des donnees et mesures de securite

Projet : ChriOnline - Application E-Commerce securisee

Ce document explique ou sont stockees les informations sensibles du projet, quelles classes les manipulent, et quelles mesures de securite protegent leur acces.

## Vue generale

| Information | Emplacement de stockage | Fichiers principaux | Mesures de securite |
|---|---|---|---|
| Comptes utilisateurs | Table `users` dans `chri_online.db` | `UserDAO.java`, `UserService.java` | Validation serveur, session token, verification email, protection brute force |
| Emails utilisateurs | Table `users.email` | `UserDAO.java` | Unicite SQL, validation format email, acces via DAO |
| Mots de passe utilisateurs | Hash PBKDF2 dans `users.password` | `UserDAO.java`, `UserService.java`, `PasswordHashService.java` | Sel aleatoire, PBKDF2-HMAC-SHA256, authentification serveur, brute force lock, logs IDS |
| Codes OTP email | `users.verification_code` et `users.encrypted_verification_code` | `UserService.java`, `UserDAO.java`, `DatabaseCryptoService.java` | Code genere aleatoirement, chiffrement en base, suppression apres validation |
| Sessions | Memoire serveur `activeSessions` | `ClientHandler.java` | Token aleatoire UUID, verification avant routes protegees |
| Produits | Table `products` | `ProduitDAO.java`, `ProduitService.java` | Requetes preparees, actions modification reservees admin |
| Paniers | Tables `carts`, `cart_items` et memoire service | `PanierDAO.java`, `PanierService.java` | Session obligatoire pour consulter/modifier son panier |
| Commandes | Tables `orders`, `order_items` | `CommandeDAO.java`, `CommandeService.java` | Session obligatoire, routes admin protegees par role |
| Paiements | Table `payments` | `PaiementDAO.java`, `PaiementService.java` | Carte masquee, adresse chiffree, session obligatoire |
| Carte bancaire | Non stockee complete ; uniquement `masked_card` | `ClientHandler.java`, `PaiementService.java`, `PaiementDAO.java` | Masquage avant stockage |
| Adresse livraison/facturation | `payments.encrypted_billing_or_delivery_address` | `DatabaseCryptoService.java`, `PaiementService.java` | Chiffrement AES avant stockage |
| Cle publique admin | `users.public_key` | `SetupUtility.java`, `UserDAO.java`, `AdminAuthService.java` | Utilisee uniquement pour verifier signature RSA |
| Cle privee admin | Fichier keystore admin local `admin_keystore.p12` | `SetupUtility.java`, `AdminClientHandler.java` | Reste cote admin, protegee par mot de passe keystore |
| Cle publique serveur | Keystore serveur + envoyee au client au handshake | `ServerKeyManager.java`, `ClientHandler.java`, `ClientTCP.java` | Sert a chiffrer la cle AES de session |
| Cle privee serveur | `config/server-keystore.p12` | `ServerKeyManager.java` | Stockee dans keystore PKCS12, mot de passe via env/.env |
| Cle AES de session | Memoire client et serveur | `SecureSession.java`, `AESUtil.java` | Generee par connexion, jamais stockee en base |
| Cle AES master base de donnees | Variable `DB_AES_MASTER_KEY_BASE64` dans `.env` ou environnement systeme | `DatabaseCryptoService.java` | Hors Git, requise pour chiffrer/dechiffrer certains champs |
| Logs securite | `logs/security-events.log` | `SecurityEventLogger.java`, `IntrusionDetectionService.java` | Fichier local ignore par Git, contient traces d'audit |

## 1. Comptes utilisateurs

Les comptes sont stockes dans la base SQLite :

```text
chri_online.db
table users
```

Colonnes importantes :

```text
id
username
email
password
role
is_verified
verification_code
encrypted_verification_code
public_key
```

Fichiers :
- `src/main/java/server/dao/UserDAO.java`
- `src/main/java/server/service/UserService.java`
- `database/schema.sql`

Mesures de securite :
- l'inscription verifie que `username`, `email` et `password` existent ;
- le format email est valide cote serveur ;
- le username est limite a un format controle ;
- le mot de passe est hashe avant stockage ;
- l'email est unique en base ;
- l'utilisateur doit valider son email avant de se connecter ;
- les echecs de login sont comptes ;
- apres trop d'echecs, le compte est temporairement bloque ;
- les tentatives suspectes sont journalisees par l'IDS.

## 2. Emails

Les emails sont stockes dans :

```text
users.email
```

Ils sont utilises pour :
- l'identification utilisateur ;
- l'envoi du code OTP ;
- l'authentification admin ;
- l'envoi de confirmation de commande.

Fichiers :
- `UserDAO.java`
- `UserService.java`
- `EmailService.java`
- `AdminAuthService.java`

Mesures de securite :
- validation du format email ;
- contrainte d'unicite dans la base ;
- l'email seul ne suffit pas pour acceder au compte ;
- pour l'admin, l'email doit correspondre a un utilisateur avec role `ADMIN`.

## 3. Mots de passe

Les mots de passe utilisateurs ne sont plus stockes en clair. Ils sont transformes en hash avant insertion dans la base.

Stockage :

```text
users.password
```

Format stocke :

```text
pbkdf2_sha256$iterations$salt_base64$hash_base64
```

Exemple :

```text
pbkdf2_sha256$210000$...$...
```

Les mots de passe sont utilises dans :

```text
UserDAO.authenticate(email, password)
```

Fichiers :
- `src/main/java/server/dao/UserDAO.java`
- `src/main/java/server/service/UserService.java`
- `src/main/java/server/security/PasswordHashService.java`

Mesures de securite appliquees :
- hash PBKDF2-HMAC-SHA256 ;
- 210 000 iterations ;
- sel aleatoire de 16 octets pour chaque mot de passe ;
- hash final de 256 bits ;
- comparaison securisee avec `MessageDigest.isEqual` ;
- authentification uniquement cote serveur ;
- protection brute force dans `UserService` ;
- blocage temporaire apres plusieurs echecs ;
- logs d'echecs de connexion ;
- alertes IDS apres plusieurs echecs rapides.

Migration :
- les nouveaux comptes sont directement stockes avec un hash PBKDF2 ;
- les anciens comptes qui avaient encore un mot de passe en clair restent compatibles ;
- au premier login reussi d'un ancien compte, le serveur remplace automatiquement le mot de passe clair par un hash PBKDF2.

Scenario :
- l'utilisateur cree un compte avec le mot de passe `secret123` ;
- la base ne stocke pas `secret123` ;
- elle stocke une chaine du type `pbkdf2_sha256$210000$sel$hash` ;
- lors du login, le serveur recalcule le hash avec le sel stocke et compare le resultat.

## 4. OTP et verification email

Lors de l'inscription, le serveur genere un code de verification a 6 chiffres.

Stockage :

```text
users.verification_code
users.encrypted_verification_code
```

Fichiers :
- `UserService.java`
- `UserDAO.java`
- `DatabaseCryptoService.java`

Mesures de securite :
- le code est genere cote serveur ;
- le compte reste non verifie tant que le bon code n'est pas saisi ;
- une version chiffree du code est stockee dans `encrypted_verification_code` ;
- apres validation, le code est supprime ;
- les OTP invalides repetes sont detectes par l'IDS.

Scenario :
- un utilisateur cree un compte ;
- il recoit un code par email ;
- si le code est faux, le compte reste bloque ;
- apres plusieurs faux codes, une alerte `OTP_ABUSE` est ajoutee aux logs.

## 5. Sessions utilisateur

Apres connexion, le serveur cree un token de session :

```text
UUID.randomUUID().toString()
```

Stockage :

```text
Memoire serveur : ClientHandler.activeSessions
Memoire client : ClientTCP.sessionToken ou AdminClientHandler.sessionToken
```

Fichiers :
- `ClientHandler.java`
- `ClientTCP.java`
- `AdminClientHandler.java`

Mesures de securite :
- le mot de passe n'est pas renvoye a chaque requete ;
- les routes protegees verifient l'existence du token ;
- `LOGOUT` supprime le token cote serveur ;
- les requetes admin verifient aussi le role.

Limite :
- les sessions sont en memoire ; elles disparaissent si le serveur redemarre.

## 6. Produits

Les produits sont stockes dans :

```text
table products
```

Fichiers :
- `ProduitDAO.java`
- `ProduitService.java`
- `Produit.java`

Mesures de securite :
- la consultation est publique ;
- la creation, modification et suppression sont reservees aux administrateurs ;
- la plupart des requetes SQL utilisent `PreparedStatement`.

Routes sensibles :

```text
ADMIN_CREATE_PRODUCT
ADMIN_UPDATE_PRODUCT
ADMIN_DELETE_PRODUCT
```

Ces routes passent par `ClientHandler` et exigent un utilisateur admin.

## 7. Panier

Le panier est stocke dans :

```text
tables carts et cart_items
```

Fichiers :
- `PanierDAO.java`
- `PanierService.java`
- `ClientHandler.java`

Mesures de securite :
- l'utilisateur doit etre connecte ;
- le serveur utilise l'ID de l'utilisateur connecte depuis la session ;
- un client ne choisit pas directement l'ID utilisateur a manipuler.

Routes protegees :

```text
ADD_TO_CART
VIEW_CART
REMOVE_FROM_CART
CREATE_ORDER
```

## 8. Commandes

Les commandes sont stockees dans :

```text
orders
order_items
```

Fichiers :
- `CommandeDAO.java`
- `CommandeService.java`
- `ClientHandler.java`

Mesures de securite :
- consultation des commandes personnelles seulement avec session valide ;
- consultation globale des commandes reservee admin ;
- mise a jour du statut reservee admin ;
- les acces commandes sont journalises comme acces sensibles.

Routes :

```text
GET_MY_ORDERS
ADMIN_GET_ORDERS
ADMIN_GET_ORDER_DETAILS
ADMIN_UPDATE_ORDER_STATUS
```

## 9. Paiement et carte bancaire

Les paiements sont stockes dans :

```text
payments
```

Colonnes importantes :

```text
order_id
amount
method
status
masked_card
encrypted_billing_or_delivery_address
```

Fichiers :
- `PaiementService.java`
- `PaiementDAO.java`
- `ClientHandler.java`

Mesures de securite :
- la carte bancaire complete n'est pas stockee ;
- le serveur masque le numero avant stockage ;
- exemple : `**** **** **** 1234` ;
- l'adresse de livraison/facturation est chiffree avant stockage ;
- le paiement necessite une session valide ;
- les paiements sont journalises comme acces sensibles.

Limite :
- le paiement est simule, il n'y a pas d'integration reelle avec une passerelle de paiement.

## 10. Informations administrateur

Un administrateur est un utilisateur avec :

```text
users.role = ADMIN
```

Sa cle publique est stockee dans :

```text
users.public_key
```

Fichiers :
- `SetupUtility.java`
- `AdminAuthService.java`
- `UserDAO.java`
- `AdminClientHandler.java`

Mesures de securite :
- le setup admin est separe du client admin ;
- le serveur verifie que l'utilisateur existe et a le role `ADMIN` ;
- l'authentification admin utilise un challenge signe ;
- la cle privee admin reste cote admin ;
- seule la cle publique est stockee en base ;
- les routes admin exigent un token de session admin.

Scenario :
- le serveur genere un challenge ;
- le client admin signe ce challenge avec sa cle privee ;
- le serveur verifie la signature avec la cle publique stockee ;
- si la signature est valide, une session admin est creee.

## 11. Cle privee admin

La cle privee admin n'est pas stockee dans la base.

Elle se trouve dans un keystore local :

```text
admin_keystore.p12
```

Fichiers :
- `SetupUtility.java`
- `AdminClientHandler.java`
- `Signer.java`

Mesures de securite :
- la cle privee reste hors base de donnees ;
- elle est protegee par le mot de passe du keystore ;
- elle sert uniquement a signer le challenge admin ;
- le fichier `.p12` est ignore par Git.

## 12. Cles serveur RSA

Le serveur possede une paire de cles RSA.

Stockage :

```text
config/server-keystore.p12
```

Fichiers :
- `ServerKeyManager.java`
- `ClientHandler.java`
- `RSAUtil.java`

Mesures de securite :
- le serveur charge ou genere un keystore PKCS12 ;
- la cle privee serveur reste cote serveur ;
- la cle publique serveur est envoyee au client au debut de la connexion ;
- la cle publique sert a chiffrer la cle AES de session ;
- le keystore est ignore par Git.

Mot de passe :
- priorite a la variable d'environnement `SERVER_KEYSTORE_PASSWORD` ;
- sinon lecture possible depuis `.env` ;
- sinon mot de passe de demonstration `changeit123`.

Limite :
- pour une vraie production, il ne faut pas utiliser le mot de passe par defaut.

## 13. Cle AES de session

Chaque client genere une cle AES temporaire apres reception de la cle publique serveur.

Stockage :

```text
Memoire client : SecureSession
Memoire serveur : SecureSession
```

Fichiers :
- `AESUtil.java`
- `SecureSession.java`
- `ClientTCP.java`
- `AdminClientHandler.java`
- `ClientHandler.java`

Mesures de securite :
- cle generee par `KeyGenerator` ;
- taille AES 256 bits ;
- chiffrement `AES/GCM/NoPadding` ;
- IV aleatoire par message ;
- tag d'authentification GCM 128 bits ;
- la cle AES n'est pas stockee en base.

## 14. Cle master de chiffrement base

Certaines donnees stockees en base sont chiffrees avec une cle AES master.

Nom :

```text
DB_AES_MASTER_KEY_BASE64
```

Emplacement :

```text
variable d'environnement systeme
ou fichier .env
```

Fichier :
- `DatabaseCryptoService.java`

Mesures de securite :
- la cle doit faire 32 octets apres decodage Base64 ;
- `.env` est ignore par Git ;
- sans cette cle, le service refuse de fonctionner ;
- elle sert a chiffrer les champs sensibles comme OTP et adresse.

## 15. Logs securite

Les logs sont stockes dans :

```text
logs/security-events.log
```

Fichiers :
- `SecurityEventLogger.java`
- `IntrusionDetectionService.java`
- `ClientHandler.java`
- `ServerTCP.java`

Evenements journalises :
- connexion TCP ;
- deconnexion ;
- login reussi/echec ;
- OTP reussi/echec ;
- authentification admin ;
- acces commandes ;
- paiement ;
- action admin ;
- replay attack ;
- flood ;
- IP bloquee.

Mesures de securite :
- les logs sont ecrits cote serveur ;
- les logs sont ignores par Git ;
- ils permettent l'audit et la demonstration IDS ;
- les alertes importantes sont aussi affichees dans la console serveur.

Exemple de ligne :

```text
2026-05-19 14:30:20 | user=test@mail.com | ip=127.0.0.1 | type=ALERT | action=BRUTE_FORCE_LOGIN | status=3 failed login attempts
```

## 16. IDS/IPS

Le module IDS/IPS est en memoire serveur.

Fichier :
- `IntrusionDetectionService.java`

Regles :
- plusieurs echecs login rapides -> alerte brute force ;
- plusieurs OTP invalides rapides -> alerte OTP ;
- trop de requetes par minute -> blocage IP ;
- trop de connexions simultanees -> blocage IP ;
- acces admin refuse -> alerte.

Stockage :

```text
Memoire serveur :
- failedLoginsByKey
- failedOtpByKey
- requestsByIp
- blockedIps
```

Mesures de securite :
- blocage temporaire IP pendant 5 minutes ;
- journalisation des alertes ;
- rejet des connexions venant d'une IP bloquee.

Limite :
- comme le stockage est en memoire, les compteurs disparaissent au redemarrage du serveur.

## 17. Fichiers ignores par Git

Le fichier `.gitignore` protege les fichiers sensibles ou locaux.

Fichier :
- `.gitignore`

Elements ignores :

```text
.env
*.db
chri_online.db
*.p12
*.jks
*.log
target/
.idea/
```

Objectif :
- eviter de partager les secrets ;
- eviter de versionner les bases locales ;
- eviter de versionner les keystores ;
- eviter de versionner les logs.

## 18. Resume par niveau de sensibilite

| Niveau | Donnees | Protection |
|---|---|---|
| Tres sensible | Cle privee serveur, cle privee admin, cle master DB | Keystore, `.env`, ignore Git, acces local |
| Sensible | Mot de passe, OTP, adresse livraison, paiement | Hash PBKDF2, auth serveur, chiffrement partiel, masquage carte, IDS |
| Interne | Sessions, commandes, paniers | Token session, controle role, routes protegees |
| Public controle | Produits | Lecture publique, modification admin uniquement |
| Audit | Logs securite | Fichier local serveur, ignore Git |

## Conclusion

Le projet applique plusieurs couches de securite :
- chiffrement des communications ;
- verification anti-replay ;
- sessions serveur ;
- verification email par OTP ;
- authentification admin RSA ;
- controle d'acces par role ;
- hash PBKDF2 des mots de passe utilisateurs ;
- chiffrement partiel des donnees sensibles ;
- masquage de carte bancaire ;
- journalisation securite ;
- IDS/IPS basique avec blocage IP.

Les principales limites restantes sont :
- remplacer les mots de passe de demonstration par des secrets forts ;
- persister les alertes IDS dans une base ou un dashboard si le projet devient plus avance.
