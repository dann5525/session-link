# Session_Link - Notarization API for Sessions  

**Session_Link** is a project designed to **notarize sessions** that can be accessed by different services. It ensures that sessions are validated, securely stored, and accessible across multiple platforms. This project is based on the **voting-poll example** as a starting point, leveraging its foundational principles while extending functionality for session management and notarization.

---

## Overview

The **Session_Link** system works as follows:

- **Session Creation:** Authorized users can create sessions that are timestamped and notarized.
- **Session Access:** Services can securely retrieve session information to ensure its validity.
- **Validation:** Only verified sessions are accepted into the system, ensuring integrity and preventing duplicates.
- **Security:** Session ownership and access rely on cryptographic signatures.
- **Cross-Service Compatibility:** Sessions are designed to be accessed by multiple external services without compromising data security.

---

## Template Structure  

The core code for the application resides in the following directories:

```
modules/l0/src/main/scala/com/sessionlink/l0/*
modules/l1/src/main/scala/com/sessionlink/l1/*
modules/data_l1/src/main/scala/com/sessionlink/data_l1/*
modules/shared_data/src/main/scala/com/sessionlink/shared_data/*
```

---

## Application Lifecycle  

The lifecycle of the **Session_Link** system follows these key functions:

1. **Session Creation**
   - New sessions are validated and notarized.

2. **Session Validation**
   - Ensures the session's origin and integrity.

3. **State Management**
   - Combines sessions into the snapshot state to maintain a consistent record.

---

### Key Methods  

The following methods govern the lifecycle and validation of the system:

#### 1. `validateUpdate`
- Performs preliminary validation for new session updates. If the update fails validation, the system returns a `500` error.

#### 2. `validateData`
- Contextual validation on the L0 layer. This step ensures:
   - The session origin matches the signing user.
   - Duplicate or outdated sessions are rejected.

#### 3. `combine`
- Integrates validated session data into the current state.

#### 4. `dataEncoder` / `dataDecoder`
- Handles encoding and decoding of session updates.

#### 5. `calculatedStateEncoder`
- Manages the serialization of the notarized session state.

#### 6. `serializeBlock` / `deserializeBlock`
- Converts blocks to and from byte arrays for snapshot storage.

#### 7. `serializeState` / `deserializeState`
- Manages state serialization for storage.

#### 8. `setCalculatedState` / `getCalculatedState`
- Provides methods to set and retrieve the current notarized state.

#### 9. `routes`
- Defines custom API routes for accessing session data.

---

## API Endpoints  

The following endpoints are provided for interacting with the notarized sessions:

- **GET** `<base_url>/data-application/sessions`  
   Returns all active notarized sessions.

- **GET** `<base_url>/data-application/sessions/:session_id`  
   Returns details of a specific session by its ID.

---

## Sample UI Integration  

Session_Link includes a sample UI web app for interacting with the API. The UI allows users to create and validate sessions while demonstrating integration with wallets and external services.

Instructions for setting up the sample UI can be found in the [sample-ui](./sample-ui/README.md) folder.

---

## Scripts  

The project includes a script to generate, sign, and send session updates to the system:

### **Script Location**  
`scripts/send_data_transaction.js`

### **Usage**

1. **Install dependencies**:  
   ```bash
   npm i
   ```

2. **Update the script with your configuration**:  
   Replace the following variables in the script:  
   - `globalL0Url`  
   - `metagraphL1DataUrl`  
   - `privateKey`  

3. **Run the script**:  
   ```bash
   node send_data_transaction.js
   ```

4. **Verify the State**:  
   Query the updated session state using:  
   ```plaintext
   GET <your metagraph L0 base url>/data-application/sessions
   ```

---

## Documentation  

For additional details, refer to the official [Data API Documentation](https://docs.constellationnetwork.io/sdk/frameworks/currency/data-api).

---

This README provides an overview of how **Session_Link** enables secure, notarized sessions across services, building upon the foundational principles of the voting-poll example. Let us know if you need further clarification or assistance! 🚀