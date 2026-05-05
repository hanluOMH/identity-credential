# Basic Utopia Issuer Implementation Plan

## Summary

Build the Basic Utopia issuer as a thin OpenID4VCI backend that registers narrow
interfaces through `ServerEnvironmentInitializer`. Do not introduce an abstract
`UtopiaIssuer` superclass.

## Key Changes

- Add a backend module at `multipaz-utopia/organizations/basic_issuer/backend`.
- Reuse existing OpenID4VCI routing from `multipaz-openid4vci-server`.
- Keep issuer setup as composition data:
  - `UtopiaIssuerProfile`
  - `ServerEnvironmentInitializer.addUtopiaIssuer(profile)`
  - optional `IssuanceObserver`
- Register `CredentialFactoryRegistry`, `DocumentTypeRepository`, and optional
  issuer-specific interfaces through the environment initializer.
- Expose `CredentialFactoryDigitalPaymentCredential()` and
  `CredentialFactoryUtopiaLoyalty()` by default.

## Implementation Details

- The backend `Main.kt` calls `runServer`, registers
  `addUtopiaIssuer(BasicUtopiaIssuerProfile.profile)`, and calls existing
  OpenID4VCI `configureRouting`.
- `UtopiaIssuerProfile` is configuration data only. It must not own routing,
  trust loading, resource serving, nonce policy, or System-of-Record behavior.
- `IssuanceObserver` is a separate optional OpenID4VCI customization interface.
  The credential handler invokes it after successful credential persistence; if
  no implementation is registered, issuance behavior is unchanged.
- Issuer display metadata remains compatible with existing `issuer_name` and
  `issuer_locale` configuration.
- Resources use the default classpath `Resources` behavior for v1.

## Test Plan

- Verify Basic issuer metadata exposes only the configured Basic credential
  factories.
- Verify `DocumentTypeRepository` contains both generic and Utopia doctypes
  after initialization.
- Verify absence of `IssuanceObserver` does not change initialization behavior.
- Verify a registered `IssuanceObserver` is invoked once per successfully
  minted credential.

## Non-Goals

- No abstract `UtopiaIssuer` base class.
- No broad migration of protocol code into Utopia.
- No bundling of doctypes, trust loading, nonce policy, resources, and
  System-of-Record behavior into one monolithic package object.
