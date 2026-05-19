# Repartition des taches de l'equipe

Projet : ChriOnline - Application E-Commerce securisee

Equipe :
- Ali
- Iyad
- Oumayma
- Hiba

## Tableau de repartition

| Module | Tache | Estimation (h) | Responsable | Realite (h) |
|---|---|---:|---|---:|
| Mini projet 1 | Conception + UML | 5 | Hiba | 4 |
| Architecture | Architecture client/serveur globale | 3 | Hiba | 4 |
| Server | ServerTCP + gestion connexions | 3 | Hiba | 4 |
| Server | Threads + thread pool | 2 | Hiba | 3 |
| Client | ClientHandler | 3 | Iyad | 4 |
| Security | Login protection / brute force | 3 | Iyad | 4 |
| UI | AuthMenu + AuthView | 3.5 | Iyad | 3 |
| Feature | Inscription utilisateur | 2 | Iyad | 2 |
| Feature | Connexion utilisateur | 2 | Iyad | 2 |
| Feature | Verification email / OTP | 3 | Iyad | 4 |
| DAO | UserDAO | 2 | Hiba | 3 |
| Model | User | 1 | Hiba | 2 |
| Database | Schema + JDBC | 3 | Hiba | 4 |
| Model | Produit + LignePanier | 2 | Oumayma | 2 |
| DAO | ProduitDAO | 2 | Oumayma | 3 |
| Feature | Consultation produits | 2 | Oumayma | 3 |
| Feature | Recherche / filtre produits | 2 | Oumayma | 2 |
| Panier | PanierService logique | 3 | Oumayma | 2 |
| UI | ProductMenu + CartMenu | 4 | Oumayma | 5 |
| Model | Commande + Paiement | 3.5 | Ali | 4 |
| DAO | CommandeDAO + PaiementDAO | 3.5 | Ali | 4 |
| Feature | Commande + paiement flow | 5 | Ali | 6 |
| UI | OrderMenu | 2 | Ali | 2 |
| Admin | Interface admin JavaFX | 4 | Ali/Hiba | 5 |
| Admin | Dashboard admin produits/commandes | 4 | Ali/Hiba | 5 |
| Tests | Tests & Debug | 6 | Ali | 9 |
| Final | Soutenance prep | 2 | Ali | 2 |
| TPs Attaques | Attaque brute force | 2 | Iyad | 3 |
| EX2 | Replay attack | 2 | Iyad | 3 |
| EX3 | SYN / connection flood | 2 | Iyad | 3 |
| EX4 | UDP flood, etude theorique | 1.5 | Iyad | 2 |
| EX5 | Session hijacking, scenario | 2 | Iyad | 2 |
| EX6 | IP spoofing, scenario | 1.5 | Iyad | 2 |
| Mini projet 2 | Generation cles RSA serveur | 3 | Iyad/Hiba | 4 |
| Security | Gestion keystore serveur | 3 | Iyad/Hiba | 4 |
| Security | Echange cle publique RSA | 3 | Iyad/Hiba | 4 |
| Security | Chiffrement cle AES avec RSA | 3 | Iyad/Hiba | 4 |
| Security | Generation cle AES cote client | 2 | Oumayma | 3 |
| Security | Dechiffrement cle AES cote serveur | 2 | Iyad/Hiba | 3 |
| Security | Chiffrement AES/GCM des requetes | 3 | Oumayma | 3 |
| Security | Nonce + timestamp anti-replay | 3 | Iyad | 4 |
| Admin Security | Auth admin challenge-response RSA | 4 | Ali | 5 |
| Admin Security | SetupUtility compte admin | 3 | Ali | 4 |
| Data Security | Chiffrement code OTP en base | 2 | Hiba | 3 |
| Data Security | Masquage carte bancaire | 2 | Ali | 2 |
| Data Security | Chiffrement adresse livraison | 2 | Ali | 3 |
| Mini projet 3 | Systeme de logs securite | 3 | Iyad | 4 |
| IDS | Detection brute force login | 3 | Iyad | 4 |
| IDS | Detection OTP invalides | 2 | Hiba | 3 |
| IDS | Detection acces admin anormal | 2 | Iyad | 3 |
| IDS | Detection flood de requetes | 3 | Iyad | 4 |
| IPS | Blocage temporaire IP | 3 | Iyad | 4 |
| IPS | Limitation connexions simultanees | 2 | Iyad | 3 |
| Audit | Journalisation login/OTP/admin/paiement | 3 | Iyad | 4 |
| Audit | Fichier logs/security-events.log | 1.5 | Iyad | 2 |
| Demo | Scenarios de test IDS/IPS | 3 | All | 4 |
| Final | Preparation rapport securite | 3 | All | 4 |

## Roles de presentation

### Iyad

Iyad presente la partie client, authentification et securite avancee :
- ClientHandler ;
- interface d'authentification ;
- inscription, connexion et verification email / OTP ;
- login protection et brute force ;
- RSA, keystore serveur et echange de cle avec Hiba ;
- protection anti-replay ;
- logs securite ;
- IDS/IPS et blocage IP.

### Hiba

Hiba presente la partie architecture, serveur de base et donnees utilisateur :
- conception + UML ;
- architecture client/serveur globale ;
- ServerTCP, threads et gestion des connexions ;
- UserDAO et modele User ;
- schema de base de donnees + JDBC ;
- participation a la partie RSA cote serveur avec Iyad ;
- participation a l'interface admin avec Ali.

### Oumayma

Oumayma presente la partie produits, panier et communication client :
- modele Produit ;
- ProduitDAO et ProduitService ;
- consultation, recherche et filtrage produits ;
- panier et PanierService ;
- interface produits et panier ;
- generation de la cle AES cote client et envoi chiffre des requetes.

### Ali

Ali presente la partie commandes, paiement et administration :
- modele Commande et Paiement ;
- creation de commandes ;
- paiement et validation de commande ;
- masquage de carte bancaire ;
- chiffrement de l'adresse de livraison ;
- setup admin ;
- interface et dashboard admin avec Hiba ;
- tests finaux et preparation de demonstration.
