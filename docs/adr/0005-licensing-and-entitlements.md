# ADR-0005: Licensing and Entitlements

- Status: Accepted
- Date: 2026-09-29

## Context

Flexible Project Manager is a reusable Platform Core intended to support different future application domains and modules, such as Video QC, text editing, photo editing, and other integrations.

The Platform Core needs a licensing mechanism that can:

- operate in local/offline deployments;
- verify that a license was issued by a trusted authority;
- bind a license to the immutable local installation identity;
- support time-limited and perpetual licenses;
- expose generic feature entitlements to future modules;
- enforce selected local capacity limits such as the number of active users;
- avoid embedding signing secrets in deployed installations;
- remain independent of future domain modules.

The local installation already has a canonical, server-controlled identifier:

`Installation.id`

This UUID is the installation identity used by Licensing.

The licensing system must not depend on unstable machine characteristics such as MAC addresses, hostnames, CPU identifiers, disk serial numbers, or operating-system machine identifiers.

The Platform Core must also remain independent of future modules. Licensing may expose generic entitlements that modules consume, but Licensing must not depend on those modules.

## Decision

Flexible Project Manager licenses are cryptographically signed offline license documents represented as compact JWS values.

The signature algorithm is:

`Ed25519`

using the JWS algorithm identifier:

`EdDSA`

A license is bound to exactly one immutable `Installation.id`.

The deployed Flexible Project Manager runtime contains only trusted public verification keys. The private signing key never belongs to the deployed Platform Core runtime.

Normal license verification and use must work offline without contacting a remote licensing service.

## Core Availability Without a License

The Platform Core remains usable without an activated license.

`UNLICENSED` does not make the application unusable.

Core capabilities remain available, including:

- setup;
- authentication;
- organization management;
- installation information;
- generic projects;
- user management.

Licensing is primarily responsible for:

- future commercial/domain modules;
- generic feature entitlements;
- local capacity limits such as `maxUsers`.

Core functionality is therefore not gated through feature identifiers such as `core.projects` or `core.users`.

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
  "limits": {
    "maxUsers": 25
  },
  "features": [
    "module.video-qc"
  ]
}
```

The exact serialized field names form part of the signed license format and are therefore versioned.

The payload must not be trusted before successful signature verification.

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

`expiresAt` must be absent.

### DEVELOPMENT

`expiresAt` is required.

Development licenses are deliberately time-limited so that internal/development licenses cannot circulate indefinitely.

No additional license types are introduced in MVP 0.1.

## Installation Binding

Each signed license contains `installationId`.

During verification it must equal the immutable local `Installation.id`.

A mismatch makes the license unusable and should result in a stable application error such as:

`LICENSE_INSTALLATION_MISMATCH`

Licensing does not depend on:

- MAC address;
- CPU identifier;
- disk identifier;
- hostname;
- operating-system machine identifier;
- `installationKey`.

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
- valid limits;
- syntactically valid feature identifiers.

Only after cryptographic and structural verification succeeds may the payload become a trusted `VerifiedLicense`.

Application code must never make licensing decisions using an unverified payload.

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

Fields such as expiry, type, features, and limits are derived from verification of the signed license.

Additional indexed or projection fields may be introduced later if query requirements justify them, but the signed license remains authoritative.

## Effective License Status

License status is derived rather than stored as authoritative mutable state.

Initial effective statuses are:

- `UNLICENSED`
- `ACTIVE`
- `EXPIRED`

`INVALID` may be used internally or diagnostically when persisted license material cannot be verified.

A perpetual license remains `ACTIVE` when all other verification rules pass.

No scheduled database mutation is required to move a license from `ACTIVE` to `EXPIRED`.

Effective status is calculated from verified signed data and the current clock.

If the existing `Installation.status` model contains `UNLICENSED`, it must not be treated as the authoritative licensing decision. Effective licensing status and entitlements come from the verified license state defined by this ADR.

## Clock Handling

Time comparisons use UTC `Instant`.

The initial offline model cannot completely prevent deliberate host-clock manipulation.

MVP 0.1 therefore does not implement:

- secure hardware clocks;
- remote time attestation;
- mandatory online time verification.

This limitation is accepted for the local/offline licensing model.

## Feature Entitlements

Licenses may contain generic feature identifiers for optional/future modules.

Examples:

```text
module.video-qc
module.text-editor
module.photo-editor
```

Feature identifiers are opaque strings from the perspective of Licensing.

Licensing must not depend on the implementation of those features or modules.

Dependency direction:

```text
future module -> entitlement abstraction
licensing     -> entitlement abstraction implementation

licensing -X-> future module
```

Core Platform functionality is not feature-gated in MVP 0.1.

## Entitlement Application Boundary

Licensing exposes a small application-facing entitlement abstraction.

Conceptually:

```java
public interface EntitlementProvider {
    boolean hasFeature(String feature);
    OptionalInt maxUsers();
}
```

The exact Java API may evolve when the first real domain module consumes it, but the dependency direction is fixed.

Do not introduce in MVP 0.1:

- plugin license managers;
- module registries;
- entitlement event buses;
- per-module licensing domain objects.

## User Limit

`maxUsers` is an enforceable local entitlement.

It counts effective active users, not historical/disabled users.

For the local MVP, a user consumes capacity when:

```text
User.status == ACTIVE
AND
OrganizationMember.status == ACTIVE
```

Disabled users do not consume a licensed seat.

Capacity must be checked when an operation can increase the number of active users, including:

- creating a new user that starts as `ACTIVE`;
- reactivating a `DISABLED` user.

Disabling a user releases capacity.

User Management must not depend directly on Licensing infrastructure.

Instead, a small policy/application boundary mediates the dependency.

Conceptually:

```java
public interface UserCapacityPolicy {
    void requireCapacityForNewActiveUser(UUID organizationId);
}
```

The exact method name may evolve to support both creation and reactivation, but the boundary must express capacity rather than expose Licensing infrastructure.

### Unlicensed behavior

Because the basic Platform Core remains usable without a license, the absence of an activated license does not impose a user limit in MVP 0.1 unless a later product decision explicitly defines an unlicensed capacity.

Therefore:

```text
UNLICENSED + no maxUsers entitlement
    -> Core user management remains available

ACTIVE license + maxUsers = N
    -> active-user capacity is N
```

An expired or invalid previously activated license must not silently continue granting its licensed capacity or module entitlements.

## Expired or Invalid License Behavior

An expired or invalid license does not disable the Platform Core.

Core functionality remains available.

However:

- licensed module feature entitlements are not granted;
- licensed capacity entitlements are not granted from the expired/invalid license.

Existing Core data is never deleted or made inaccessible merely because a license expires.

Licensing must not perform destructive behavior.

Future modules may choose to become unavailable or read-only when their entitlement is absent, but that behavior belongs to module/use-case policy and must not be hardcoded into the generic `Project` aggregate.

## Installation Limits

A per-license `maxInstallations` value is not enforced by the offline local runtime in MVP 0.1.

A signed license is already bound to one `Installation.id`.

An isolated installation cannot reliably know how many other installations have been activated under the same commercial agreement.

A global installation-count limit requires a central licensing/activation authority.

If such a service is introduced later, installation-count licensing requires a separate decision or extension to this ADR.

## Failure Behavior

Activation distinguishes stable application error categories such as:

- `LICENSE_INVALID`
- `LICENSE_SIGNATURE_INVALID`
- `LICENSE_KEY_UNKNOWN`
- `LICENSE_VERSION_UNSUPPORTED`
- `LICENSE_INSTALLATION_MISMATCH`
- `LICENSE_EXPIRED`

Cryptographic or internal exception details must not be exposed through the HTTP API.

## Security Rules

The implementation must:

- verify the signature before trusting payload data;
- pin the expected signature algorithm;
- reject unknown `kid` values;
- reject unsupported payload versions;
- never log private signing material;
- avoid logging complete signed licenses by default;
- never accept unsigned license JSON as an activated license;
- never allow the frontend to determine effective entitlements independently;
- enforce entitlements and capacity policies on the backend.

Frontend feature visibility is UX only.

Backend application policy is authoritative.

## Offline Operation

Once activated, a valid license can be verified locally without contacting a remote service.

MVP 0.1 does not implement:

- online activation;
- periodic license-server heartbeat;
- floating seats;
- network lease renewal;
- remote revocation;
- remote kill switch.

A cryptographically valid previously issued offline license cannot be remotely revoked without introducing an online or distributed revocation mechanism.

This limitation is accepted.

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
- assigning feature identifiers;
- assigning limits.

The issuer is not part of the normal Flexible Project Manager runtime trust boundary.

## Relationship to Future Modules

The Platform Core remains independent of future modules.

A future module may depend on generic Projects and entitlement abstractions.

For example:

```text
Video QC Module
    ↓
EntitlementProvider.hasFeature("module.video-qc")
    ↓
CreateProjectUseCase
    ↓
generic Project.id
    ↓
Video QC module-specific state
```

The Platform Core and Licensing modules never depend on domain-specific project subclasses.

Composition remains preferred over inheritance.

## Consequences

### Positive

- local installations work fully offline;
- no private signing key is distributed to customers;
- licenses cannot be modified without invalidating the signature;
- licenses are bound to stable Installation identity rather than hardware;
- effective expiry requires no background database mutation;
- key rotation is possible through `kid`;
- future modules can consume generic feature entitlements;
- Licensing remains independent of future application domains;
- the basic Platform Core remains usable without a license;
- historical disabled users do not unnecessarily consume licensed capacity.

### Negative

- offline licenses cannot be remotely revoked;
- deliberate system-clock manipulation cannot be completely prevented;
- global installation limits cannot be enforced by an isolated installation;
- public verification keys must be maintained for old licenses during key rotation;
- replacing an installation with a new `Installation.id` requires a newly issued license;
- active-user capacity enforcement introduces a dependency from User Management to a generic capacity-policy boundary.

These trade-offs are accepted for the local-first MVP.

## Alternatives Considered

### Symmetric HMAC license signatures

Rejected because every installation capable of verifying an HMAC would also contain the secret capable of generating valid licenses.

### Unsigned JSON license files

Rejected because they provide no authenticity or integrity guarantee.

### Hardware fingerprint binding

Rejected because hardware identifiers are brittle across VMs, hardware replacement, operating systems, network changes, and backups.

### Mandatory online license server

Rejected for MVP 0.1 because it conflicts with the local/offline-first deployment model.

### Blocking the entire Platform Core when unlicensed

Rejected for MVP 0.1. Licensing controls optional modules and explicit commercial limits without making core administration unusable.

### Licensing every Core capability as a feature

Rejected for MVP 0.1 because it would spread Licensing concerns throughout the basic Platform Core.

### Counting all historical users against `maxUsers`

Rejected. Disabled users do not consume an active licensed seat.

### Storing authoritative license claims as ordinary database columns

Rejected. Mutable database state must not be able to change signed licensing claims.

### Project-specific or module-specific license models

Rejected. Licensing must remain generic and independent of future modules.

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
- detailed UI behavior for every future licensed module.

These concerns require separate decisions if introduced.
