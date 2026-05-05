# Bank of Utopia Issuer Implementation Plan

## Summary

Build `BankOfUtopiaIssuer` as a thin organization-specific OpenID4VCI backend
that reuses the Basic Utopia issuer composition model. This issuer exists to
verify that the Basic issuer wiring can support a second organization with its
own credential selection, issuer metadata, and bank-specific document type
without introducing an abstract `UtopiaIssuer` superclass.

## Key Changes

- Add a backend module at `multipaz-utopia/organizations/bank_of_utopia_issuer/backend`.
- Reuse the existing OpenID4VCI server routing and the Basic issuer helper:
  - `UtopiaIssuerProfile`
  - `ServerEnvironmentInitializer.addUtopiaIssuer(profile)`
  - optional `IssuanceObserver`
- Define a bank-specific credential factory for a new `BankAccountCredential`.
- Register generic doctypes, Utopia doctypes, and the new bank doctype in the
  issuer `DocumentTypeRepository`.
- Configure issuer metadata through existing configuration keys:
  - `issuer_name=Bank of Utopia`
  - `issuer_locale=en-US`

## Bank Credential Shape

- Add a new Utopia document type named `BankAccountCredential`.
- Use mdoc format for the first version.
- Suggested constants:
  - doctype: `org.multipaz.utopia.bank.account.1`
  - namespace: `org.multipaz.utopia.bank.account.1`
  - credential configuration id: `bank_account_mdoc`
  - scope: `bank_account`
- Include only simple demo fields needed to validate issuance:
  - `issuer_name`
  - `account_holder_name`
  - `account_id`
  - `masked_account_reference`
  - `account_type`
  - `issue_date`
  - `expiry_date`
- Default demo values may be generated from `core.given_name`,
  `core.family_name`, and the `bank_account` System-of-Record record. If the
  record is absent in local/dev mode, use deterministic sample values so the
  issuer can be tested without a real System of Record.

## Implementation Details

- Create `BankOfUtopiaIssuerProfile.profile` using `UtopiaIssuerProfile`.
- The backend `Main.kt` should match the Basic issuer pattern:
  - call `runServer(args, needAdminPassword = true, checkConfiguration = ...)`
  - register `addUtopiaIssuer(BankOfUtopiaIssuerProfile.profile)`
  - call existing OpenID4VCI `configureRouting(environment)`
- The bank profile should register exactly one factory initially:
  - `CredentialFactoryBankAccountCredential()`
- The profile should customize `configureDocumentTypes` to call:
  - `addKnownTypes()`
  - `addUtopiaTypes()`
  - `addDocumentType(BankAccountCredential.getDocumentType(locale))`
- Keep bank behavior isolated to the bank organization backend or clearly named
  Utopia bank package. Do not add bank-specific logic to generic OpenID4VCI
  routes.
- Do not add custom `NonceManager`, resource overlay, or observer behavior in
  v1. Add them only after the Basic issuer composition path is verified.

## Test Plan

- Verify `/.well-known/openid-credential-issuer` exposes only
  `bank_account_mdoc`.
- Verify the bank backend registers generic doctypes, shared Utopia doctypes,
  and `BankAccountCredential`.
- Verify the bank credential factory can create display metadata from demo
  System-of-Record data.
- Verify issuer metadata uses `Bank of Utopia` through the existing
  `issuer_name` configuration path.
- Run:
  - `./gradlew :multipaz-utopia:organizations:bank_of_utopia_issuer:backend:test`
  - `./gradlew :multipaz-utopia:organizations:basic_issuer:backend:test`

## Acceptance Criteria

- The bank issuer backend starts with the same OpenID4VCI routing as the Basic
  issuer.
- The bank issuer uses composition and environment registration only; no
  inheritance-based issuer base class is introduced.
- Bank issuer metadata lists only the bank account credential.
- Basic issuer tests still pass unchanged, proving the helper supports multiple
  issuers.

## Non-Goals

- No production banking semantics.
- No payment network integration.
- No real account lookup, KYC, AML, or risk checks.
- No migration of OpenID4VCI protocol code into Utopia organization modules.
- No broad shared issuer superclass.
