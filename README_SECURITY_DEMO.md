# ChriOnline Security Demo

## 1. RSA/AES handshake
- The TCP connection starts with a server RSA key exchange.
- `server.network.ClientHandler` sends the server public key loaded by `server.security.ServerKeyManager`.
- `client.network.ClientTCP` and `adminclient.network.AdminClientHandler` generate an AES-256 session key.
- The AES key is encrypted with RSA/OAEP and sent to the server.
- The server decrypts it with its RSA private key.
- After that, all `Request` and `Response` objects are wrapped in `shared.security.SecureMessage`.

## 2. AES protection of Request/Response
- AES mode: `AES/GCM/NoPadding`
- IV size: 12 bytes per message
- Authentication tag: 128 bits
- Classes involved:
  - `shared.security.AESUtil`
  - `shared.security.SecureMessage`
  - `shared.security.SecureSession`
  - `client.network.ClientTCP`
  - `adminclient.network.AdminClientHandler`
  - `server.network.ClientHandler`

## 3. Secured stored data
- Sensitive database fields are protected by `server.security.DatabaseCryptoService`.
- Encrypted at rest:
  - email verification code
  - delivery or billing address when payment simulation stores one
- Stored in masked form only:
  - card number as `**** **** **** 1234`
- Never stored:
  - CVV

## 4. Admin RSA challenge-response
- Admin login remains separate from the AES transport handshake.
- Flow:
  1. `ADMIN_AUTH_REQUEST`
  2. server sends challenge
  3. admin signs challenge with PKCS12 private key
  4. `ADMIN_AUTH_VERIFY`
  5. server verifies with stored admin public key
- Classes involved:
  - `server.service.AdminAuthService`
  - `adminclient.SetupUtility`
  - `adminclient.ui.AdminAuthView`

## 5. Run the server
```powershell
mvn exec:java "-Dexec.mainClass=server.main.ServerMain"
```

## 6. Run the customer client
```powershell
mvn javafx:run "-DmainClass=client.main.ClientMain"
```

## 7. Run the admin JavaFX client
```powershell
mvn javafx:run "-DmainClass=adminclient.main.AdminFxMain"
```

## 8. Generate `DB_AES_MASTER_KEY_BASE64`
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

## 9. Generate or use an admin PKCS12 keystore
```powershell
keytool -genkeypair -alias admin -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore admin_keystore.p12 -validity 365 -dname "CN=Admin, OU=IT, O=ChriOnline, L=Casablanca, S=Casablanca, C=MA"
```

## Environment variables
- `GMAIL_EMAIL`
- `GMAIL_APP_PASSWORD`
- `DB_AES_MASTER_KEY_BASE64`
- `SERVER_KEYSTORE_PASSWORD`

## Notes
- The uploaded standalone electronics SQL dataset file was not present in the workspace during implementation.
- To keep the migration demonstrable, the database schema was upgraded and seeded with curated electronics examples matching the requested product structure and categories.
