# ADR-0005: Licensing and Entitlements

- Status: Accepted
- Date: 2026-09-29

## Context

Flexible Project Manager is a reusable Platform Core intended to support different future application domains and modules, such as Video QC, text editing, photo editing, and other integrations.

The Platform Core requires a licensing mechanism that can:

- operate in local/offline deployments;
- verify that a license was issued by a trusted authority;
- bind a license to the immutable local installation identity;
- enforce the general validity period of the Flexible Project Manager installation;
- enforce the maximum number of effective active users;
- carry extensible module-specific license feature keywords;
- expose those feature keywords without interpreting module-specific semantics;
- avoid embedding signing secrets in deployed installations;
- remain independent of future domain modules.

The local installation already has a canonical, server-controlled identifier:

`Installation.id`

This UUID is the installation identity used by Licensing.

The licensing system must not depend on unstable machine characteristics such as MAC addresses, hostnames, CPU identifiers, disk serial numbers, or operating-system machine identifiers.

The Platform Core must remain independent of future modules. Licensing may verify and expose module-specific feature keywords, but each module owns the meaning and enforcement of its own feature keywords.

---

## Decision

Flexible Project Manager licenses are cryptographically signed offline license documents represented as compact JWS values.

The signature algorithm is:

`Ed25519`

using the JWS algorithm identifier:

`EdDSA`

A license is bound to exactly one immutable:

`Installation.id`

The deployed Flexible Project Manager runtime contains only trusted public verification keys.

The private signing key never belongs to the deployed Platform Core runtime.

Normal license verification and use must work offline without contacting a remote licensing service.

The general Platform Core license controls:

- license validity/duration;
- maximum number of effective active users;
- a set of opaque `licenseFeatures` keywords.

The Platform Core does not interpret module-specific feature semantics.

---

## General License Scope

The general license applies to the Flexible Project Manager installation as a whole.

Its two general enforceable conditions are:

1. license validity;
2. `maxUsers`.

In addition, the same signed license carries an extensible array of `licenseFeatures`.

The presence of a module-specific feature keyword means that the license grants that capability if a module that understands the keyword is installed.

If a licensed feature belongs to a module that is not installed, it has no effect and does not cause an error.

Installing a module does not itself grant any license feature.

Therefore:

```text
installed != licensed
```

A module may be installed but not licensed.

A license may also contain feature keywords for a module that is not installed.

Both situations are valid.

---

## License Document

A license contains a versioned signed payload.

Conceptually:

```json
{
  "version": 1,
  "licenseId": "b8f0a1a5-0000-0000-0000-000000000001",
  "installationId": "83741d8b-0000-0000-0000-000000000001",
  "type": "SUBSCRIPTION",
  "issuedAt": "2026-09-29T08:00:00Z",
  "expiresAt": "2027-09-29T08:00:00Z",
  "maxUsers": 25,
  "licenseFeatures": [
    "module.video-qc",
    "video-qc.hdr",
    "video-qc.imf",
    "video-qc.automated-qc"
  ]
}
```

The exact serialized field names form part of the signed license format and are therefore versioned.

The payload must not be trusted before successful signature verification.

`maxUsers` is a general Platform Core license condition.

`licenseFeatures` is an extensible array of opaque keyword identifiers.

---

## JWS Protected Header

The JWS protected header contains at least:

```json
{
  "alg": "EdDSA",
  "kid": "fpm-2026-01"
}
```

Rules:

- `alg` must be exactly `EdDSA`;
- the verifier must not accept arbitrary algorithms from the token;
- `kid` identifies the trusted public verification key;
- unknown key identifiers are rejected.

The `kid` mechanism allows future key rotation while retaining verification support for licenses signed by older trusted keys.

---

## License Types

The initial supported license types are:

- `TRIAL`
- `SUBSCRIPTION`
- `PERPETUAL`
- `DEVELOPMENT`

Rules:

### TRIAL

`expiresAt` is required.

### SUBSCRIPTION

`expiresAt` is required.

### PERPETUAL

`expiresAt` must be absent or `null`.

### DEVELOPMENT

`expiresAt` is required.

Development licenses are deliberately time-limited so that internal/development licenses cannot circulate indefinitely.

No additional license types are introduced in MVP 0.1.

---

## Installation Binding

Each signed license contains:

`installationId`

During verification it must equal the immutable local:

`Installation.id`

Conceptually:

```text
verifiedLicense.installationId
        ==
currentInstallation.id
```

A mismatch makes the license unusable.

The application reports a stable application error such as:

`LICENSE_INSTALLATION_MISMATCH`

No alternative hardware identity is used.

In particular, Licensing does not depend on:

- MAC address;
- CPU identifier;
- disk identifier;
- hostname;
- operating-system machine identifier;
- `installationKey`.

---

## Signing-Key Ownership

There are two distinct trust domains.

### License issuer

Owns:

- the Ed25519 private signing key;
- the corresponding public key;
- the license issuance process.

### Flexible Project Manager runtime

Contains:

- trusted public verification key(s).

The runtime never contains the private signing key.

The private key must not be stored in:

- the FPM source repository;
- the backend runtime;
- frontend code;
- a customer installation;
- the normal FPM Docker image;
- customer environment variables.

License generation is performed by a separate issuer tool or service.

---

## Key Rotation

Trusted verification keys are identified by `kid`.

Conceptually:

```text
fpm-2026-01 -> public key A
fpm-2027-01 -> public key B
```

The runtime may trust more than one public key simultaneously.

Removing an old verification key invalidates the runtime's ability to verify licenses signed by that key.

Therefore key removal is an explicit compatibility decision and must not happen automatically.

Private-key rotation and license issuance are outside the Platform Core runtime.

---

## Verification Boundary

Licensing exposes an application-facing verification contract.

Conceptually:

```java
public interface LicenseVerifier {
    VerifiedLicense verify(String signedLicense);
}
```

The infrastructure implementation performs JWS/Ed25519 verification.

The application and domain layers do not depend on the cryptographic library or JWS implementation.

The verifier must validate at least:

- valid compact JWS syntax;
- supported license format version;
- `alg == EdDSA`;
- recognized `kid`;
- valid Ed25519 signature;
- valid `licenseId`;
- valid `installationId`;
- installation binding;
- valid license type;
- coherent `issuedAt`;
- coherent `expiresAt`;
- type-specific expiration rules;
- valid `maxUsers`;
- syntactically valid `licenseFeatures`.

Only after cryptographic and structural verification succeeds may the payload become a trusted:

`VerifiedLicense`

Application code must never make licensing decisions using an unverified payload.

---

## Activation

License activation is explicit.

Conceptually:

```text
signed compact JWS
        ↓
LicenseActivationUseCase
        ↓
LicenseVerifier
        ↓
VerifiedLicense
        ↓
installation-binding validation
        ↓
persistence transaction
```

The new license must be fully verified before replacing the currently activated license.

An invalid candidate license must never remove or corrupt a currently valid license.

Replacement occurs atomically.

---

## Persistence

The signed license is the authoritative persisted license artifact.

For the local MVP, one installation has at most one currently activated license.

Conceptually:

```text
installation_licenses
---------------------
installation_id
signed_license
activated_at
updated_at
```

The database must not become an independently editable copy of signed claims.

Fields such as:

- type;
- expiry;
- `maxUsers`;
- `licenseFeatures`;

must not be trusted from independently mutable database columns.

They are derived by verifying the signed license.

Additional indexed or projection fields may be introduced later if query requirements justify them, but the signed license remains authoritative.

---

## Effective License Status

License status is derived rather than stored as authoritative mutable state.

Initial effective statuses are:

- `UNLICENSED`
- `ACTIVE`
- `EXPIRED`

`INVALID` may be used internally or diagnostically when persisted license material cannot be verified.

Conceptually:

```text
no activated license
    -> UNLICENSED

valid signature
+ matching installation
+ non-expired license
    -> ACTIVE

valid signed time-limited license
+ now >= expiresAt
    -> EXPIRED
```

A perpetual license remains `ACTIVE` when all other verification rules pass.

No scheduled database mutation is required to move a license from `ACTIVE` to `EXPIRED`.

Effective status is calculated from verified signed data and the current clock.

If the existing `Installation.status` model contains a value such as `UNLICENSED`, it must not be treated as the authoritative licensing decision.

Effective licensing status comes from the verified license state defined by this ADR.

---

## General Validity Enforcement

License validity is a general Platform Core condition.

Normal protected application operations require an effective `ACTIVE` license.

The exact technical enforcement mechanism should be centralized as much as possible and must not require every use case to understand JWS, cryptography, or persistence details.

The following remain accessible when the installation is `UNLICENSED`, `EXPIRED`, or otherwise not effectively licensed:

- first-run setup where applicable;
- authentication needed to administer the installation;
- license status retrieval;
- license activation/replacement;
- minimum administration required to restore a valid license.

The application must not delete or mutate user/project data merely because a license is absent, expired, or invalid.

Existing data remains persisted.

The exact read-only/degraded UX may evolve, but the backend remains authoritative about which operations require an active license.

---

## Clock Handling

Time comparisons use UTC `Instant`.

The initial offline model cannot completely prevent deliberate host-clock manipulation.

MVP 0.1 therefore does not implement:

- secure hardware clocks;
- remote time attestation;
- mandatory online time verification.

This limitation is accepted for the local/offline licensing model.

More advanced clock-tampering protection requires a separate architectural decision.

---

## User Limit

`maxUsers` is a general Platform Core license condition.

It counts effective active users, not historical/disabled users.

For the local MVP, a user consumes capacity when:

```text
User.status == ACTIVE
AND
OrganizationMember.status == ACTIVE
```

Disabled users do not consume a licensed user slot.

Example:

```text
maxUsers = 10

8 effective ACTIVE users
15 DISABLED users

licensed usage = 8 / 10
```

Capacity must be checked when an operation can increase the number of effective active users, including:

- creating a new user that starts as `ACTIVE`;
- reactivating a `DISABLED` user;
- reactivating an inactive organization membership if that makes the user effective-active.

Disabling a user or membership releases capacity.

User Management must not depend directly on:

- JWS;
- cryptographic libraries;
- license persistence entities;
- `LicenseVerifier`.

Instead, a small application policy boundary mediates the dependency.

Conceptually:

```java
public interface UserCapacityPolicy {
    void requireCapacityForAdditionalActiveUser(UUID organizationId);
}
```

The exact method shape may evolve, but the boundary must express capacity rather than expose Licensing infrastructure.

The Licensing implementation enforces the current effective `maxUsers` behind this boundary.

---

## Behavior When License Is Not Active

Because license validity is itself a general Platform Core condition, an expired or invalid license must not silently behave like an unrestricted installation.

Therefore:

```text
ACTIVE
-> normal licensed operation
-> maxUsers enforced
-> licenseFeatures available

UNLICENSED
-> normal licensed operations unavailable
-> activation/admin recovery paths remain available
-> module licenseFeatures are not granted

EXPIRED
-> normal licensed operations unavailable
-> activation/admin recovery paths remain available
-> module licenseFeatures are not granted

INVALID
-> treated as not effectively licensed
-> activation/admin recovery paths remain available
-> module licenseFeatures are not granted
```

Existing data is never deleted because of licensing state.

Replacing an invalid/expired license with a valid license restores normal operation.

---

## License Features

The signed Platform Core license contains an extensible array:

`licenseFeatures`

Each entry is an opaque keyword string.

Examples:

```text
module.video-qc
video-qc.hdr
video-qc.imf
video-qc.automated-qc

module.photo-editor
photo-editor.raw
photo-editor.ai-filters

module.text-editor
text-editor.ai
text-editor.collaboration
```

Keywords should be namespaced to reduce collisions.

A root feature may represent entitlement to a module:

```text
module.video-qc
```

Additional features may represent capabilities inside that module:

```text
video-qc.hdr
video-qc.imf
```

The Platform Core Licensing module:

- verifies that the signed array is structurally valid;
- exposes feature presence;
- does not interpret module-specific meaning;
- does not know whether the corresponding module is installed;
- does not activate module behavior itself.

Each module:

- owns the names/keywords it understands;
- owns constants for those keywords if desired;
- queries the Core license feature provider;
- decides how to enforce its own licensed capabilities.

The Platform Core must not define a global enum containing all current/future module features.

For example, this is deliberately avoided:

```java
enum LicenseFeature {
    VIDEO_QC,
    VIDEO_QC_HDR,
    PHOTO_EDITOR,
    PHOTO_AI
}
```

because adding a module would then require modifying the Platform Core.

---

## License Feature Boundary

The Core exposes a small application-facing abstraction.

Conceptually:

```java
public interface LicenseFeatureProvider {
    boolean hasFeature(String feature);
}
```

A module may use it as:

```java
if (!licenseFeatures.hasFeature("module.video-qc")) {
    // module not licensed
}
```

and:

```java
if (licenseFeatures.hasFeature("video-qc.hdr")) {
    // HDR capability licensed
}
```

A module does not need to know:

- how JWS works;
- which public key verified the license;
- where the signed license is stored;
- how expiration is calculated.

Dependency direction remains:

```text
Future Module
      |
      v
LicenseFeatureProvider
      |
      v
Core Licensing
```

Core Licensing does not depend on the future module.

---

## Feature Availability Requires an Active License

Feature keywords are only effective when the general license is `ACTIVE`.

Conceptually:

```text
license contains "video-qc.hdr"
+
effective license status == ACTIVE
    -> hasFeature("video-qc.hdr") == true
```

If the effective status is:

- `UNLICENSED`;
- `EXPIRED`;
- `INVALID`;

then:

```text
hasFeature("video-qc.hdr") == false
```

even if an old or invalid signed payload contains that keyword.

This keeps module logic simple and prevents expired licenses from continuing to grant module capabilities.

---

## Module Installation and Feature Presence

Module installation and module licensing are independent concepts.

### Installed and licensed

```text
Video QC installed
+
license contains "module.video-qc"
-> module may be enabled
```

### Installed but not licensed

```text
Video QC installed
+
license does not contain "module.video-qc"
-> module is not licensed
```

### Licensed but not installed

```text
license contains "module.video-qc"
+
Video QC not installed
-> no effect
```

The Platform Core does not treat unused feature keywords as errors.

---

## Module-Specific Licensing Responsibility

Although feature keywords are carried by the general Platform Core license, the semantics of module-specific features belong to the module.

For example:

```text
video-qc.hdr
```

is merely an opaque string to Core Licensing.

Only the Video QC module determines:

- what HDR means;
- which use cases require it;
- which UI elements depend on it;
- whether lack of the feature disables, hides, or limits a particular capability.

This keeps domain-specific licensing outside the generic Project and Platform Core domains.

The generic `Project` aggregate must never contain module license state.

---

## Installation Limits

`maxInstallations` is not part of local enforcement in MVP 0.1.

A signed license is already bound to one `Installation.id`.

An isolated installation cannot reliably know how many other installations have been activated under the same commercial agreement.

A global installation-count limit requires a central licensing/activation authority.

If such a service is introduced later, installation-count licensing requires a separate decision or extension to this ADR.

MVP 0.1 does not pretend to enforce a global installation count locally.

---

## Failure Behavior

Activation distinguishes stable application error categories such as:

- `LICENSE_INVALID`
- `LICENSE_SIGNATURE_INVALID`
- `LICENSE_KEY_UNKNOWN`
- `LICENSE_VERSION_UNSUPPORTED`
- `LICENSE_INSTALLATION_MISMATCH`
- `LICENSE_EXPIRED`
- `LICENSE_USER_LIMIT_EXCEEDED`

Exact public error exposure may be reduced where security considerations make a more generic error preferable.

Cryptographic or internal exception details must not be exposed through the HTTP API.

---

## Security Rules

The implementation must:

- verify the signature before trusting payload data;
- pin the expected signature algorithm;
- reject unknown `kid` values;
- reject unsupported payload versions;
- never log private signing material;
- avoid logging complete signed licenses by default;
- never accept unsigned license JSON as an activated license;
- never allow the frontend to determine effective license status independently;
- never allow the frontend to authoritatively enforce `maxUsers`;
- never allow the frontend to authoritatively grant module features;
- enforce general license validity on the backend;
- enforce user capacity on the backend;
- expose only verified effective feature availability.

Frontend visibility/disablement is UX only.

Backend application policy is authoritative.

---

## Offline Operation

Once activated, a valid license can be verified locally without contacting a remote service.

This is an intentional property of the local deployment model.

MVP 0.1 does not implement:

- online activation;
- periodic license-server heartbeat;
- floating seats;
- network lease renewal;
- remote revocation;
- remote kill switch.

A cryptographically valid previously issued offline license cannot be remotely revoked without introducing an online or distributed revocation mechanism.

This limitation is accepted.

---

## License Issuer

License issuance is explicitly outside the deployed Platform Core.

A future separate component may be implemented, for example:

```text
fpm-license
```

It may be a CLI or service capable of:

- reading a private signing key;
- producing versioned signed licenses;
- assigning installation IDs;
- assigning license type;
- assigning expiration;
- assigning `maxUsers`;
- assigning `licenseFeatures`.

The issuer is not part of the normal Flexible Project Manager runtime trust boundary.

---

## Relationship to Future Modules

The Platform Core remains independent of future modules.

A future module may depend on:

- generic Projects;
- `LicenseFeatureProvider`;
- other stable Platform Core contracts.

For example:

```text
Video QC Module
    |
    +--> LicenseFeatureProvider.hasFeature("module.video-qc")
    |
    +--> LicenseFeatureProvider.hasFeature("video-qc.hdr")
    |
    +--> CreateProjectUseCase
              |
              v
        generic Project.id
              |
              v
        Video QC module-specific state
```

The Platform Core and Licensing modules never depend on:

- `VideoQcProject`;
- `PhotoProject`;
- `TextProject`;
- equivalent domain-specific project subclasses.

Composition remains preferred over inheritance.

---

## Consequences

### Positive

- local installations work fully offline;
- no private signing key is distributed to customers;
- licenses cannot be modified without invalidating the signature;
- licenses are bound to stable Installation identity rather than hardware;
- license duration is enforced as a general Platform Core condition;
- maximum active users is enforced centrally;
- module feature keywords can evolve without modifying the Platform Core;
- unused licensed features do not affect installations that lack the corresponding module;
- effective expiry requires no background database mutation;
- key rotation is possible through `kid`;
- modules remain responsible for their own feature semantics;
- Licensing remains independent of future application domains.

### Negative

- offline licenses cannot be remotely revoked;
- deliberate system-clock manipulation cannot be completely prevented;
- global installation limits cannot be enforced by an isolated installation;
- public verification keys must be maintained for old licenses during key rotation;
- replacing an installation with a new `Installation.id` requires a newly issued license;
- User Management needs a generic capacity-policy integration point;
- modules must consistently enforce their own feature keywords.

These trade-offs are accepted for the local-first MVP.

---

## Alternatives Considered

### Symmetric HMAC license signatures

Rejected.

Every installation capable of verifying an HMAC would also contain the secret capable of generating valid licenses.

### Unsigned JSON license files

Rejected.

They provide no authenticity or integrity guarantee.

### Hardware fingerprint binding

Rejected.

Hardware identifiers are brittle across VMs, hardware replacement, operating systems, network changes, and backups.

### Mandatory online license server

Rejected for MVP 0.1.

It conflicts with the local/offline-first deployment model.

### Treating an expired license as unrestricted/unlicensed operation

Rejected.

License duration is a general Platform Core condition. Expiration must not remove the restrictions that existed while the license was valid.

### Counting all historical users against `maxUsers`

Rejected.

Disabled users and inactive memberships do not consume an active licensed user slot.

### Storing authoritative license claims as ordinary database columns

Rejected.

Mutable database state must not be able to change signed licensing claims.

### Global enum of module features in Platform Core

Rejected.

Every new module or feature would require modifying the Platform Core and would create an unnecessary dependency from Core Licensing to domain-specific concepts.

### Platform Core interpreting module-specific feature semantics

Rejected.

The Core verifies and exposes opaque feature keywords. Each module owns the meaning and enforcement of its own keywords.

### Project-specific subclasses for licensed modules

Rejected.

Module licensing must not change the generic Project aggregate. Modules extend the platform by composition using `Project.id`.

---

## Out of Scope

ADR-0005 does not define:

- payment processing;
- billing;
- customer accounts;
- subscription purchasing;
- online activation;
- global installation-count enforcement;
- remote revocation;
- floating licenses;
- license-server high availability;
- hardware fingerprints;
- plugin discovery;
- module loading;
- marketplace functionality;
- cryptographic private-key infrastructure for the external issuer;
- exact module-specific license feature semantics;
- exact UX for every future licensed module.

These concerns require separate decisions if introduced.
