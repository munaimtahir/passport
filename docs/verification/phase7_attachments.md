# Phase 7 Verification: Secure Attachments

## 1. Local Encryption Vault
- Implemented `UtilityAttachmentVault` for secure local file storage under the app-private `utility_vault/` directory.
- Leverages Keystore-backed cryptography (`KeystoreCryptoService`) to encrypt the files.
- Computes SHA-256 file hashes to prevent uploading duplicate attachment files under the same record.

## 2. Attachment List and Preview UI
- Integrates the attachments panel directly into the Payment Details card inside `MonthlyOccurrenceDetailsDialog`.
- Leverages `rememberLauncherForActivityResult` with `GetContent` contract to launch the native system document picker.
- Supports secure deletion: removes the local encrypted file and drops the record from the database.
- Implemented a complete `PreviewAttachmentDialog` (using `renderAttachmentPreview` and Compose `Image` decoding) that decrypts files on the fly and renders previews internally.

## 3. Host-Side Verification Output
- `./gradlew test` -> BUILD SUCCESSFUL (exited with code 0)
- `./gradlew lint` -> BUILD SUCCESSFUL (exited with code 0)
