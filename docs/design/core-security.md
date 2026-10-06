# core:security

The platform token ciphers: an AES-GCM key in the Android Keystore, and an ECIES key pair kept in
the iOS Keychain. No module dependencies. Used by `core:database`'s `RoomTokenStore`; `shared`
picks the cipher per platform.

## Classes

```mermaid
classDiagram
    direction TB
    class TokenCipher {
        <<interface>>
        +encrypt(plaintext: String) String
        +decrypt(ciphertext: String) String?
    }
    class TokenCipherException {
        +message: String
        +cause: Throwable?
    }
    class KeystoreTokenCipher {
        <<androidMain>>
        -alias: String
        -keyStore: KeyStore
        +encrypt(plaintext: String) String
        +decrypt(ciphertext: String) String?
        -key() SecretKey
        -generateKey() SecretKey
        -newKey() SecretKey
    }
    class KeychainTokenCipher {
        <<iosMain>>
        -service: String
        -privateKey: SecKeyRef
        +encrypt(plaintext: String) String
        +decrypt(ciphertext: String) String?
        -loadKey() SecKeyRef?
        -createAndStoreKey() SecKeyRef
        -store(keyData: CFDataRef)
        -keyFromBytes(data: CFDataRef) SecKeyRef?
    }
    class CoreFoundationSupport {
        <<iosMain file>>
        ~cfDictionaryOf(entries) CFMutableDictionaryRef
        ~cfNumberOf(value: Int) CFNumberRef
        ~ByteArray.toCFData() CFDataRef
        ~CFDataRef.toByteArray() ByteArray
        ~String.toCFString() CFStringRef
    }

    KeystoreTokenCipher ..|> TokenCipher : implements
    KeychainTokenCipher ..|> TokenCipher : implements
    KeystoreTokenCipher ..> TokenCipherException : throws
    KeychainTokenCipher ..> TokenCipherException : throws
    KeychainTokenCipher --> CoreFoundationSupport : uses
```

## Sequences

### Encrypt and decrypt (Android)

`encrypt` throws `TokenCipherException`; `decrypt` returns null for anything it can't read,
including a plaintext row from an older build.

```mermaid
sequenceDiagram
    participant C as Caller (RoomTokenStore)
    participant K as KeystoreTokenCipher
    participant KS as AndroidKeyStore
    C->>K: encrypt(plaintext)
    K->>KS: getEntry(alias)
    alt no key yet
        K->>KS: generate AES-256 GCM key
        Note over K,KS: on failure delete the alias and try once more, then throw TokenCipherException
    end
    K->>K: AES/GCM encrypt, Base64(iv + ciphertext)
    K-->>C: String
    C->>K: decrypt(ciphertext)
    K->>K: Base64 decode, split iv, AES/GCM decrypt
    alt readable
        K-->>C: plaintext
    else bad Base64, wrong key, tampered
        K-->>C: null
    end
```

### Encrypt and decrypt (iOS)

```mermaid
sequenceDiagram
    participant C as Caller (RoomTokenStore)
    participant K as KeychainTokenCipher
    participant KC as Keychain
    C->>K: encrypt(plaintext)
    Note over K: privateKey is lazy: loadKey() ?: createAndStoreKey()
    alt first use
        K->>KC: SecItemCopyMatching(service, account)
        alt stored
            KC-->>K: key bytes
            K->>K: SecKeyCreateWithData
        else none
            K->>K: SecKeyCreateRandomKey (P-256)
            K->>KC: SecItemDelete then SecItemAdd (AfterFirstUnlockThisDeviceOnly)
        end
    end
    K->>K: SecKeyCreateEncryptedData(public key, ECIES X963 SHA256 AES-GCM)
    K-->>C: Base64 String
    C->>K: decrypt(ciphertext)
    K->>K: SecKeyCreateDecryptedData(private key)
    K-->>C: plaintext or null
```
