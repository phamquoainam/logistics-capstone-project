"""Fail closed on missing/skipped tests; publish only allowlisted test metadata."""
import argparse
import json
import os
import re
import stat
import subprocess
import time
from pathlib import Path
import xml.etree.ElementTree as ET

BASE = 'com.elogistic.backend.'
REQUIRED = {
    BASE + 'demo.PostgresTestcontainersTests': '''actualPostgres18UsesPinnedIsolatedContainer coldStartAppliesV1AndV2
validateAndRerunPreserveDataAndChecksums upgradeV1ToV2PreservesExistingData schemasAndObjectsBelongToOwner
runtimeCannotDdlAssumeOwnerOrReadHistory restartPreservesCommittedDataAndHistory'''.split(),
    BASE + 'testsupport.TestcontainersRuntimePolicyTests': '''pinsApprovedImagesAndRejectsUnknownImages
rejectsDisabledRyukPrivilegedChecksAndReuse rejectsPrivilegedAndHostNetworkCommands
bindsPublishedPortsToLoopbackWithRandomHostPorts rejectsOperationsOnForeignResources
policyDoesNotAddSocketMountsOrPrivilegeToDatabaseCommand'''.split(),
    BASE + 'demo.FoundationProfileTests': ['existingDemoHasNoRedisOrRabbitClientsOrContributors'],
    BASE + 'platform.internal.infrastructure.FoundationConfigurationTests': '''rejectsIncompleteProfileCombination
rejectsEachMissingOrBlankCredentialWithoutExposingValues acceptsExplicitProfilesWithRequiredCredentials'''.split(),
    BASE + 'demo.FoundationDependencyTests': '''cachedNativeRabbitProbeNeedsActiveCommunication
authenticatedRedisRejectsWrongPasswordDefaultUserAndWrites authenticatedRabbitRejectsWrongCredentialsVhostAndResourceCreation
activeProbeReleasesConnectionsAndLeavesApplicationPolicyUnchanged fullProfileProbesAreStatusOnlyAndManagementRemainsRestricted'''.split(),
    BASE + 'architecture.ArchitectureTests': '''productionScopeIsRealAndExcludesNegativeFixtures
packagesMatchApprovedModuleStructure modulesOnlyUsePublicApis publicApisDoNotLeakInternals
modulesFollowApprovedDependencyDirection domainIsIndependentOfOuterLayers applicationDependsInward
infrastructureDoesNotDependOnWeb webDoesNotUsePersistenceAdapters controllersDoNotOwnTransactions
moduleGraphHasNoCycles'''.split(),
    BASE + 'architecture.ArchitectureNegativeFixtureTests': '''financeCannotUseOrderInternalRepository
identityCannotCallMerchantEvenThroughPublicApi publicApiCannotLeakOrderDomain moduleCycleIsDetected
domainCannotUseJdbc applicationCannotDependOnItsAdapter webCannotCallRepositoryAdapter
infrastructureCannotDependOnHttpAdapter controllerCannotDeclareTransaction ordersCannotUseBroadAccountView
ordersDomainCannotUseIdentityActor minimalTrustedActorIsAllowedAtApplicationBoundary'''.split(),
    BASE + 'BackendApplicationTests': ['contextLoads'],
    BASE + 'demo.DemoAuthenticationTests': '''loginMeAndHashedPassword wrongPasswordAndDisabledLogin
anonymousUnauthorizedAndWrongRoleForbidden accountStatusAndRoleRecheckedAfterIssue
tamperedTokenAndClientRoleInjectionRejected rateLimitFailedLogins signedExpiredTokenRejected'''.split(),
    BASE + 'demo.DemoSecurityEvidenceTests': '''credentialsAndBearerTokenDoNotAppearInApplicationLogs
signingConfigurationFailsFastWhenMissingOrWeak'''.split(),
    BASE + 'demo.FoundationHealthTests': '''healthyProbesUseConfiguredDatabaseAndHideDetails
refusingTrafficIs503WhileLivenessStaysUp onlyExactAnonymousGetProbesAreAllowed
exhaustedRealPoolReturns503WithinAcquisitionBudgetAndRecovers'''.split(),
    BASE + 'demo.PostgresHealthSafetyTests': '''longRuntimeReadAndPhysicalReuseRemainSafeAfterSuccessfulProbe
concurrentLongReadIsNotInterruptedByExhaustedPoolProbe terminatedPhysicalConnectionIsDiscardedAndReplacementIsSafe
stalledSocketProbeIsBoundedEvictsAndRecoversWithoutTimeoutLeak'''.split(),
    BASE + 'demo.DemoShipmentTests': '''actorMerchantOwnershipIsRecheckedFromDatabase ownDraftAndCrossMerchantIsolation
createReplayConflictAndReferenceUniqueness validationOwnerAndStatusInjectionRejected
cancelGuardReplayAndImmutableEvents concurrentCreateHasOneEffect databaseConstraintAndAdminSummary'''.split(),
    BASE + 'demo.PostgresFoundationTests': '''actualPostgresAndSeparateMigrationPrincipal
nineSchemasAndTablesBelongToOwner runtimeCannotDdlAssumeOwnerOrReadHistory
eventsAndReceiptsCannotBeChangedByRuntime validateAndRerunPreserveDataAndChecksums
versionOneToTwoUpgradePreservesExistingTechnicalData failedForeignKeyRollsBackWholeTransaction
concurrentCancelHasOneEventAndReceipt'''.split(),
}
# Source-reviewed S2 inventory. Finite invocation indices come from the Java
# MethodSource fixtures, never from observed reports or arbitrary display names.
S2_REQUIRED = {
    BASE + 'identity.internal.application.TokenConfigurationEvidenceTests': [
        'missingEnvironmentPlaceholderRejectsContext(CapturedOutput)',
        *[f'shortOrBlankSecretRejectsContextWithoutEcho(String, String, CapturedOutput)[{i}]' for i in range(1, 6)],
        *[f'supportedByteBoundariesInstantiateService(String, String, int, CapturedOutput)[{i}]' for i in range(1, 7)],
        *[f'whitespaceOnlySecretMustRejectContext(String, String)[{i}]' for i in range(1, 4)],
        *[f'blankSecretFailsForValidationCauseWithoutValueDisclosure(String, String, CapturedOutput)[{i}]' for i in range(1, 4)],
        'directNullConstructorInputRetainsExistingRejection',
        'paddedSecretSignsAndVerifiesWithoutNormalization(CapturedOutput)',
    ],
    BASE + 'identity.internal.infrastructure.DemoSeedConfigurationEvidenceTests': [
        *[f'invalidAbsentAccountPasswordFailsBeforeThatAccountMutation(String, int, String)[{i}]' for i in range(1, 16)],
        *[f'sourceCharacterBoundariesControlFirstAccountMutation(String, int, boolean)[{i}]' for i in range(1, 5)],
        'multibyteSeedBoundaryUsesJavaCharactersNotTokenBytePolicy',
        *[f'existingAccountDoesNotRequireItsMissingPassword(String, int)[{i}]' for i in range(1, 6)],
        'disabledOrAbsentSeedFlagDoesNotCreateRunnerOrMutateFakePersistence',
        'creatingEnabledRunnerBeanAloneDoesNotExecuteApplicationRunner',
        *[f'whitespaceOnlySeedPasswordMustBeRejected(String, int)[{i}]' for i in range(1, 6)],
        *[f'blankPasswordValidationPrecedesEncodingAndMutationOfTheAbsentAccount(String, int)[{i}]' for i in range(1, 6)],
        'validPaddedPasswordReachesEncoderUnchangedAndSeedingRemainsIdempotent',
    ],
    BASE + 'platform.internal.infrastructure.FoundationContextEvidenceTests': [
        *[f'invalidCredentialPreventsContextRefresh(String, String, String, CapturedOutput)[{i}]' for i in range(1, 13)],
        *[f'incompleteProfilesRejectContext(String, String[])[{i}]' for i in range(1, 4)],
        'defaultProfileDoesNotCreateFoundationBeans',
        'validFoundationConfigurationRefreshesWithoutClientConnections(CapturedOutput)',
    ],
    BASE + 'platform.internal.infrastructure.PostgresConfigurationEvidenceTests': [
        *[f'missingPlaceholderFailsExplicitResolutionWithoutNetwork(String, String, String, String)[{i}]' for i in range(1, 15)],
        *[f'blankValuesAreNotRejectedAtPropertyResolutionBoundary(String, String, String, String)[{i}]' for i in range(1, 15)],
        'validPostgresPropertiesKeepRuntimeAndMigratorResponsibilitiesDistinct',
        'migrationOnlyNeedsGrantTargetButNotRuntimePasswordOrToken',
        'runtimeOnlyResolvesRuntimePropertiesWithoutUnusedMigrationVariables',
        'invalidMigrationBooleanHasConfigurationConversionFailure',
        'jdbcUrlSyntaxCheckDoesNotOpenConnection',
        'unrelatedDemoModeDoesNotRequirePostgresOrFoundationVariables',
    ],
}
REQUIRED.update(S2_REQUIRED)
# Source/MethodSource-reviewed seed preflight inventory: 65 invocations + 9
# singletons. These finite signatures/indices are not inferred from reports.
SEED_PREFLIGHT = BASE + 'identity.internal.infrastructure.DemoSeedPreflightTests'
SEED_PREFLIGHT_REQUIRED = [
    *[f'invalidMissingAccountFailsPreflightWithoutEncodingOrWriting(String, String, String)[{i}]' for i in range(1, 26)],
    *[f'pinnedBCryptCharacterization(String, String, int, int, boolean, boolean)[{i}]' for i in range(1, 16)],
    *[f'preflightKeepsCharacterRulesAndRejectsEncoderByteOverflow(String, String, int, int, boolean, boolean)[{i}]' for i in range(1, 16)],
    *[f'byteOverflowFailsBeforeEncodingForEveryMissingAccount(String)[{i}]' for i in range(1, 6)],
    *[f'existingAccountExemptionIgnoresUnusedInvalidPasswords(String, String)[{i}]' for i in range(1, 6)],
    'existingAccountsNeedNoPasswordsAndMixedStateChecksOnlyMissingAccounts',
    'externalDeletionAfterPreflightStillRejectsMissingPasswordInsteadOfUnsafeCreation',
    'invalidPreflightAbortsRefreshBeforeSmartLifecycleCanStart',
    'validReadOnlyPreflightAllowsLifecycleThenRunnerCreatesAccounts',
    'disabledAbsentOrNonDemoSeedDoesNotCreatePreflightOrQueryAccounts',
    'actualH2ProxyRollbackBCryptAndExistingIdentityArePreserved',
    'nullRequiredValueHasSanitizedConfigurationRejection',
    'pinnedEncoderNullBehaviorDoesNotReplaceApplicationNonNullPolicy',
    'malformedJavaUnicodeUsesSameUtf8ReplacementAsEncoder',
]
REQUIRED[SEED_PREFLIGHT] = SEED_PREFLIGHT_REQUIRED
IDENTITY_MIGRATION = BASE + 'identity.internal.infrastructure.IdentityMigrationTests'
# Finite source-reviewed T-002-03 inventory, not inferred from observed XML.
REQUIRED[IDENTITY_MIGRATION] = [
    'coldStartValidateAndRerunPreserveHistoryAndIdentityData',
    'upgradeV2PreservesAllDemoTablesWithoutImportingAccounts',
    'ownerOwnsEightTablesIndexesAndOnlyIdentityForeignKeys',
    'usernamesRemainExactCaseUnicodeAndRejectDuplicatesBlankAndNull',
    'accountLifecycleRequiresVerifiedActivatedCredentialAndPositiveVersions',
    'grantsAllowMultipleScopesButRejectDuplicatesInvalidScopesAndMissingAccounts',
    'sessionsRejectInvalidDeadlinesMissingAccountsAndNullExpiry',
    'refreshHashesMustBeUniqueCorrectSizeAndHaveExistingSession',
    'refreshLineageCannotCrossFamiliesForkOrHaveTwoLiveMembers',
    'proofPurposeLifetimeHashAndSingleOutstandingProofAreEnforced',
    'csrfSupportsPreauthAndSessionBindingButRejectsBadHashesAndReferences',
    'receiptsBindActorProofOrPreauthWithoutExistenceDisclosureOrDuplicateKey',
    'runtimeCanAppendAuditButCannotDdlDeleteAccountsOrMutateAuditAndReceipts',
    'realmRegistryStartsInactiveAndRuntimeCannotRegisterOrActivate',
    'realmForeignKeyRejectsUnknownIdentityButPreservesExternalScope',
    'realmUpgradePreservesV3DataAndChecksums',
    'unmatchedLegacyIdentityScopeStopsRealmUpgradeAtomically',
    'bootstrapCreatesPendingOnlyAndDuplicateCannotTakeOverAnAccount',
    'realmActivationRequiresVerifiedInvitationAndCannotBeReplayedOrReactivated',
    'bootstrapAuditFailureRollsBackPendingAccountGrantAndRealmBinding',
    'inactiveRealmIsNotEnoughToCreateActiveAdministrativeAuthority',
    'invitationV5UpgradePreservesLegacyProofsDataAndChecksums',
    'invitationOutboxEnforcesProofAssociationUniquenessAndProtectedShape',
    'invitationOutboxOwnershipAndRuntimePrivilegesRemainNarrow',
    'preexistingSchemaDriftFailsWithoutBaselineOrRepair',
]
# T-002-04 source-reviewed plain-Jupiter identities; required in H2 and PostgreSQL.
IDENTITY_DOMAIN_REQUIRED = {
    BASE + 'identity.internal.domain.IdentityAccountTests': [
        'provisioningRequiresExplicitAuthorityAndCreatesOnlyPendingAccount',
        'activationReturnsAccountAndConsumedInvitationWithoutMutatingOriginals',
        'activationRejectsWrongSubjectPurposeExpiredProofAndWeakPassword',
        'activationRejectsDisabledPendingAndAlreadyActiveAccounts',
        'disableAndReactivatePreserveActivationButNeverRestoreOldSessions',
        'repeatedDisableAndInvalidReactivationAreRejected',
        'passwordResetConsumesRecoveryAndInvalidatesPreviousAuthVersion',
        'recoveryRequiresActiveVerifiedAccountAndStoredEmailMatch',
        'restoreRejectsImpossibleActivationCombinations',
        'staleVersionPreventsLifecycleOrProofCommitResult',
    ],
    BASE + 'identity.internal.domain.IdentityAuthorizationTests': [
        'authorizedReplacementKeepsMultipleScopesAndInvalidatesSessions',
        'replacementRequiresAuthorityOverUnionIncludingRemovedGrants',
        'proposedGrantsNeedExplicitAssignabilityWithoutScopeCrossProduct',
        'missingProviderAndNonManagementActorDefaultDeny',
        'snapshotBindsActorAuthVersionTargetAndVersionEvenWhenGrantsEmpty',
        'selfDisableAndSelfGrantReplacementAreAlwaysRejected',
        'lastActiveAdminCannotBeDisabledOrLoseManagementRole',
        'emptyReplacementIsAllowedForNonAdminButDuplicatesAreRejected',
        'functionalAccessRequiresExactRoleAndScopeWithoutAdminFinanceFallback',
        'authorityCollectionsAreCopiedAndStaleExpectedVersionIsRejected',
    ],
    BASE + 'identity.internal.domain.IdentityProofAndRefreshTests': [
        'proofLifetimesAndExactExpiryBoundariesMatchAcceptedPolicy',
        'consumedOrRevokedProofCannotBeReusedOrResurrected',
        'proofRejectsWrongPurposeSubjectAndInvalidTime',
        'proofRestoreAndIssueRejectImpossibleStatesOrEligibility',
        'refreshRotationConsumesPreviousMemberAndPreservesSessionDeadlines',
        'consumedRefreshReplayRevokesFamilyAndSessionWithoutGrace',
        'revokedFamilyRejectsCurrentTokenRatherThanRotating',
        'unknownRefreshDoesNotRevokeLegitimateFamilyAndSuccessorCannotBeReused',
        'refreshRequiresMatchingFamilyAccountAndCurrentSession',
        'refreshExpiryAndRestoredMemberStateAreValidated',
    ],
    BASE + 'identity.internal.domain.IdentitySessionTests': [
        'openingSessionSetsAcceptedAbsoluteIdleAndAccessBounds',
        'idleAdmissionIsValidBeforeDeadlineButNotAtOrAfter',
        'explicitActivityExtendsIdleButPollingAndRefreshDoNot',
        'absoluteBoundaryCapsIdleAndJwtEvenAfterMeaningfulActivity',
        'jwtLifetimeAlsoCannotExceedIdleDeadline',
        'revokedSessionCannotBeAdmittedAndRevocationIsIdempotent',
        'accountBindingStatusAndAuthVersionAreRechecked',
        'invalidRestoredDeadlinesAreRejected',
        'expiredActivityCannotResurrectSessionAndClockRollbackIsDenied',
    ],
    BASE + 'identity.internal.domain.IdentityValueTests': [
        'usernamePreservesExactCaseUnicodeAndPadding',
        'usernameUsesCodePointsAndContractWhitespaceBoundaries',
        'newPasswordEnforcesCodePointMinimumAndUtf8Maximum',
        'loginAllowsLegacyMinimumWithoutRelaxingByteLimit',
        'passwordRejectsMalformedUnicodeAndDoesNotTransformInput',
        'valueObjectsRejectMissingScopeAndDoNotInventRoleCatalog',
        'versionsRejectInvalidStaleAndOverflowWithoutWrapping',
        'collectionsAreDefensiveAndDuplicateInputIsRejected',
        'sensitiveValuesAndExceptionsHaveRedactedRepresentations',
    ],
}
REQUIRED.update(IDENTITY_DOMAIN_REQUIRED)
IDENTITY_GRANTS = BASE + 'identity.internal.infrastructure.IdentityGrantApplicationTests'
# T-002-05 finite, reviewed PostgreSQL-only transaction suite; no valid skips.
REQUIRED[IDENTITY_GRANTS] = [
    'authorizedReplacementPersistsMultipleScopesVersionsAuditAndReceipt',
    'allTargetSessionsAndRefreshTokensAreRevokedWithoutTouchingActor',
    'authorizedReplayPrecedesStaleVersionAndDoesNotRepeatEffects',
    'reorderedGrantSetHasSameCanonicalReceiptFingerprint',
    'sameKeyChangedPayloadConflictsWithoutWrites',
    'sameKeyChangedTargetConflictsWithoutWrites',
    'sameKeyChangedExpectedVersionConflictsBeforeStaleCheck',
    'sameKeyChangedReasonConflictsWithoutWrites',
    'revokedActorSessionCannotReplayReceipt',
    'lostManagementAuthorityCannotReplayReceipt',
    'missingOwnerProviderDefaultsDenyEvenForEmptyTarget',
    'partialAuthorityCannotRemoveHiddenCurrentGrant',
    'requestedGrantOutsideAssignableScopeIsDenied',
    'nonManagementActorCannotUsePermissiveOwnerFixture',
    'selfGrantManagementIsForbidden',
    'lastAdministratorCannotRemoveItsOwnManagementGrant',
    'staleNonReplayVersionIsRejectedWithoutWrites',
    'duplicateGrantsAreRejectedWithoutWrites',
    'emptyReplacementIsRejectedByAcceptedContractBoundary',
    'malformedScopeIsRejectedWithoutWrites',
    'unknownResourceScopeIsNotGrantedImplicitly',
    'missingAndUnauthorizedTargetsHaveSameSafeClassification',
    'disabledActorCannotSubmitCommand',
    'expiredSessionAndChangedAuthVersionAreRejected',
    'invalidCommandMetadataDoesNotMutateState',
    'disabledTargetStaysDisabledAndDoesNotRegainSessions',
    'auditWriteFailureRollsBackAllSecurityEffects',
    'receiptWriteFailureRollsBackAuditAndAllSecurityEffects',
    'refreshRevocationFailureRollsBackSessionAndGrantWrites',
    'concurrentVersionsProduceOneMutationAndOneStaleResult',
    'concurrentSameKeyCommandsCreateOneReceiptAndOneAudit',
    'concurrentOppositeAdminRemovalCannotRemoveLastAdministrator',
    'resultContainsOnlyAcceptedAccountFieldsAndRedactsToString',
]
IDENTITY_AUTHORITY_UNIT = BASE + 'identity.internal.application.FailClosedGrantAuthorityTests'
REQUIRED[IDENTITY_AUTHORITY_UNIT] = [
    'missingProviderDeniesWithoutInventingIdentityScopeAuthority',
    'nullProviderResultDeniesWithoutFallback',
    'providerFailureIsDeniedWithoutExposingItsMessage',
    'disabledActorNeverReachesProvider',
    'unrelatedRoleNeverEstablishesManagementAuthority',
    'missingActorTargetOrRequestedContextDeniesBeforeProvider',
    'trustedDelegateReceivesExactCurrentSnapshotsAndImmutableRequestedSet',
    'vmErrorsAreNotSwallowedAsOrdinaryProviderUnavailability',
    'emptyTargetIsDeniedBeforeAnAllowingDelegate',
]
IDENTITY_AUTHORITY_PG_REQUIRED = [
    'productionDefaultCompositionDeniesWithoutResourceContracts',
    'productionProviderFailureIsSanitizedBeforeReceiptDisclosure',
    'productionIncompleteOwnerContextCannotRemoveHiddenGrants',
    'productionMissingProviderRejectsEvenAnEmptyTargetGrantSet',
    'productionDisabledActorIsRejectedBeforeAuthorityLookup',
    'productionRevokedOrExpiredSessionIsRejectedBeforeAuthorityLookup',
    'productionStaleAuthorizationVersionIsRejectedBeforeAuthorityLookup',
    'productionSnapshotSubstitutionAndUnassignableScopeAreRejected',
    'productionSelfManagementPreservesSpecificDomainDenial',
    'productionConcurrentAuthorizationVersionChangeIsObservedAfterLock',
    'productionConcurrentManagementGrantLossIsObservedAfterLock',
    'productionEmptyTargetIsDeniedBeforeAnAllowingDelegate',
]
REQUIRED[IDENTITY_GRANTS] += IDENTITY_AUTHORITY_PG_REQUIRED
IDENTITY_REALM_UNIT = BASE + 'identity.internal.application.IdentityRealmGrantPolicyTests'
REQUIRED[IDENTITY_REALM_UNIT] = [
    'exactCurrentSameRealmPairProducesBoundedAuthority',
    'missingRegisteredRealmNeverAuthorizes',
    'ordinaryGrantPossessionNeverDelegatesManagement',
    'targetMustHaveActualSameRealmManagementContext',
    'crossRealmActorCannotManageMatchingTarget',
    'unrelatedRequestedAndHiddenCurrentGrantDenyEntireReplacement',
    'disabledActorAndMissingInputsDeny',
    'existingDomainStillRejectsSelfManagement',
]
IDENTITY_REALM_PG_REQUIRED = [
    'realmProductionPerformsExactSameRealmReplacementAndAuthorizedReplay',
    'realmProductionRejectsInactiveAndMissingRegistryWithoutDisclosure',
    'realmProductionRejectsExternalOrHiddenCurrentGrants',
    'realmProductionPreservesSessionSelfAndVersionGuards',
    'realmProductionRollsBackOnAuditFailure',
    'realmProductionRequiresSameTransactionAndAdministrationLock',
    'realmDeactivationPrecedingAdmissionDeniesWaitingProductionCommand',
]
REQUIRED[IDENTITY_GRANTS] += IDENTITY_REALM_PG_REQUIRED
IDENTITY_PROVISION_UNIT = BASE + 'identity.internal.application.IdentityProvisionInputTests'
REQUIRED[IDENTITY_PROVISION_UNIT] = [
    'exactUsernameAndContactAreNotNormalized',
    'displayNameCountsUnicodeCodePointsAndRejectsMalformedText',
    'missingKeysOrRequiredFieldsAreRejectedWithoutEcho',
    'grantSetRejectsEmptyDuplicateAndMalformedMembers',
    'emailMatchesAcceptedFullFormatRatherThanTrimming',
    'fingerprintBindsEveryPayloadFieldAndIgnoresGrantOrder',
    'commandAndValidatedInputAreImmutableAndRedacted',
    'explicitInitialPairBindsActorAndNewPendingTarget',
    'unsupportedOrCrossRealmInitialGrantsCannotDelegate',
    'initialAuthorityRequiresActiveActorRealmAndFreshUnverifiedTarget',
    'provisioningDoesNotRelaxReplacementEmptyTargetGuard',
    'domainProvisioningRemainsPendingUnverifiedAndVersionOne',
]
IDENTITY_PROVISION_PG_REQUIRED = [
    'provisionPersistsPendingInitialPairProofAuditReceiptAndTestIntent',
    'provisionExactReplayDoesNotRepeatAccountProofAuditOrDelivery',
    'provisionConflictingPayloadKeyDoesNotDiscloseOrRepeatEffects',
    'provisionDuplicateUsernameCannotTakeOverExistingAccount',
    'provisionUnauthenticatedAndUnknownSessionsMakeNoWrites',
    'provisionRevokedSessionCannotUseCurrentAdministrativeGrant',
    'provisionExpiredSessionFailsBeforeContactLookup',
    'provisionDisabledActorAndStaleAuthVersionAreRejected',
    'provisionInactiveUnknownAndCrossRealmAuthorityFailClosed',
    'provisionExternalMixedAndOrdinaryRoleRequestsNeverAssignGrants',
    'provisionMissingDurableInvitationAdapterFailsBeforeWrites',
    'provisionInvalidPreparedProofClosesMaterialWithoutPartialAccount',
    'provisionGrantProofDeliveryAuditAndReceiptFaultsRollBackAtomically',
    'provisionConcurrentSameKeyHasOneAccountProofAndTestIntent',
    'provisionConcurrentUsernameConflictHasNoLoserEffects',
    'provisionReplayRechecksStoredTargetGrantUnionAndCurrentSession',
    'provisionCannotActivateRealmAccountOrIssueSessions',
    'provisionWaitsForConcurrentActorAndRealmStateBeforeAdmission',
]
REQUIRED[IDENTITY_GRANTS] += IDENTITY_PROVISION_PG_REQUIRED
IDENTITY_INVITATION_UNIT = BASE + 'identity.internal.infrastructure.InvitationProtectionTests'
REQUIRED[IDENTITY_INVITATION_UNIT] = [
    'freshTokensHaveFullUrlSafeEncodingAndDistinctProtectedValues',
    'verifierAcceptsOnlyExactTokenAndBoundProofAccountPurposeAndTimes',
    'exactExpiryAndConsumedRevokedOrFutureProofFailClosed',
    'missingMalformedAndSharedPurposeKeysAreRejectedWithoutValues',
    'keysAreDefensivelyCopiedAndOldKeysSupportRotation',
    'aeadRejectsCiphertextNonceAndContextTampering',
    'malformedNoncanonicalOrUnknownVerifierNeverAuthenticates',
    'protectedBuffersAreDefensiveRedactedAndDestroyedOnClose',
    'persistedMicrosecondPrecisionIsMandatoryForAuthenticatedContext',
    'deliveryPolicyRequiresExplicitBoundedAttemptsAndBudgets',
]
IDENTITY_INVITATION_PG_REQUIRED = [
    'secureProvisionQueuesEncryptedIntentWithMatchingHmac',
    'secureProvisionReplayAndConflictingKeyNeverRegenerateOutbox',
    'secureProvisionEveryMandatoryPersistenceFailureRollsBack',
    'secureProvisionConcurrentKeyAndUsernameHaveOneDurableEffect',
    'secureOutboxClaimLeaseRecoveryRejectsStaleOwner',
    'secureOutboxConcurrentClaimsHaveExactlyOneLease',
    'secureOutboxSentClearsPayloadWithoutConsumingOrActivating',
    'secureOutboxTransientRetriesAreBoundedAndExhaustionPurges',
    'secureOutboxPermanentFailurePurgesWithoutBusinessMutation',
    'secureOutboxExpiryRevocationAndConsumptionPurgeOnlyPayloads',
    'secureOutboxCurrentRealmAndGrantStateGateDelivery',
    'secureOutboxTamperingCannotProducePlaintextDelivery',
    'secureOutboxRotationReadsPersistedOldKeyIdentity',
    'secureOutboxMissingKeysOrRuntimeInsertPrivilegeCannotCommit',
    'secureOutboxRejectsIntentOutsideTheProvisionTransaction',
]
REQUIRED[IDENTITY_GRANTS] += IDENTITY_INVITATION_PG_REQUIRED

IDENTITY_ACTIVATION_UNIT = BASE + 'identity.internal.application.IdentityInvitationActivationTests'
IDENTITY_ACTIVATION_UNIT_REQUIRED = [
    'validActivationInvokesAllWritesInsideTheTransaction',
    'missingCommandIdentifiersFailBeforeTheStore',
    'invalidNewPasswordsNeverReachPersistence',
    'passwordUnicodeAndUtf8BoundsReuseTheDomainPolicy',
    'invalidTokenCannotReachReceiptOrPasswordHashing',
    'unavailableRealmCannotReachPasswordHashing',
    'consumedProofWithoutExactReceiptCannotActivate',
    'exactConsumedProofReceiptReplayHasNoWrites',
    'changedReceiptFingerprintConflictsBeforeAnyWrite',
    'replayRequiresCurrentActiveAccountAndRetainedVersion',
    'receiptRetentionCannotBeShortenedBelowTwentyFourHours',
    'commandBuffersAreCopiedRedactedAndClosed',
]
REQUIRED[IDENTITY_ACTIVATION_UNIT] = IDENTITY_ACTIVATION_UNIT_REQUIRED
IDENTITY_ACTIVATION_PG_REQUIRED = [
    'activationCommitsVerifierProofVersionsAuditReceiptWithoutSession',
    'activationExactReceiptReplayHasNoSecondEffect',
    'activationChangedPasswordConflictsOnTheBoundReceipt',
    'activationWrongTokenAndUnknownLocatorCannotAuthenticate',
    'activationExactExpiryAndFutureProofFailClosed',
    'activationConsumedAndRevokedProofCannotBeUsedAgain',
    'activationWrongPurposeAndAccountBindingAreRejected',
    'activationDisabledPendingAccountIsNotReactivated',
    'activationInactiveRealmAndRemovedGrantDeny',
    'activationPasswordBoundsPrecedeEveryPersistentWrite',
    'activationPasswordWriteFailureRollsBackAllEffects',
    'activationAuditFailureRollsBackAllEffects',
    'activationReceiptFailureRollsBackAllEffects',
    'activationProofConsumptionFailureRollsBackPassword',
    'activationPayloadClearFailureRollsBackConsumption',
    'activationConcurrentDifferentKeysConsumeExactlyOnce',
    'activationConcurrentSameKeyHasOneDurableEffect',
    'activationRetiredKeyRotationSupportsReplayAndMissingKeyDenies',
    'activationOptimisticVersionMismatchRollsBackTheTransaction',
    'activationRedactsSecretsRevokesOldSessionsAndDeniesStaleDelivery',
]
REQUIRED[IDENTITY_GRANTS] += IDENTITY_ACTIVATION_PG_REQUIRED
IDENTITY_SESSION_UNIT = BASE + 'identity.internal.application.IdentitySessionApplicationTests'
IDENTITY_SESSION_UNIT_REQUIRED = ['loginCreatesSessionRefreshAndAuditInOneTransaction', 'wrongPasswordHasNoWrites', 'unknownAccountStillPerformsCredentialComparison', 'pendingAndDisabledAccountsCannotLogin', 'inactiveRealmDeniesLogin', 'missingAdmissionCannotCompose', 'deniedAdmissionPrecedesCredentialLookup', 'invalidUsernameAndOversizedLoginPasswordFailWithoutStore', 'refreshRotatesWithoutExtendingSessionDeadlines', 'consumedRefreshCommitsRevocationBeforeReplayDenial', 'unknownRefreshDoesNotResolveAnAccount', 'staleAuthorizationVersionDeniesRefresh', 'expiredAndRevokedSessionsCannotRefresh', 'sessionAccountSubstitutionCannotRefresh', 'logoutWithoutCookieStillRequiresAdmission', 'logoutRevokesOnlyResolvedSessionAndIsIdempotent', 'expiredLogoutCanRevokeWithoutRestoringSession', 'tokenBuffersAndResultsAreCopiedRedactedAndClosed', 'refreshMaterialIsRandomCanonicalAndDigested', 'legacyPasswordUsesBcryptWithoutNewPasswordMinimum', 'storeRejectsActualAmbientTransactionBeforeJdbcAccess', 'storeRejectsAmbientSynchronizationBeforeJdbcAccess', 'storeRejectsBoundDataSourceBeforeJdbcAccess', 'storeRejectsUnknownBoundResourceBeforeJdbcAccess']
REQUIRED[IDENTITY_SESSION_UNIT] = IDENTITY_SESSION_UNIT_REQUIRED
IDENTITY_SESSION_PG_REQUIRED = ['sessionLoginPersistsOnlyDigestSessionAndAudit', 'sessionInvalidAndUnknownCredentialsHaveSameNoWriteOutcome', 'sessionPendingAndDisabledAccountsCannotLogin', 'sessionInactiveRealmAndUnsupportedScopeFailClosed', 'sessionRefreshRotatesPredecessorAndPreservesDeadlines', 'sessionReplayCommitsFamilyRevocationBeforeDenial', 'sessionConcurrentRefreshHasOneRotationThenReplayRevocation', 'sessionExactIdleAndAbsoluteExpiryRejectRefresh', 'sessionStaleAuthVersionAndDisabledAccountRejectRefresh', 'sessionRealmDeactivationRejectsRefreshWithoutMutation', 'sessionLogoutPreservesOtherAccountsAndIsIdempotent', 'sessionExpiredLogoutAndMissingCookieDoNotRestoreSessions', 'sessionLoginMandatoryWriteFailuresRollBackEverything', 'sessionRefreshSuccessorFailureRollsBackConsumption', 'sessionReplayAuditFailureRollsBackRevocation', 'sessionAdmissionMustBindTheAuthoritativeFamily', 'sessionPasswordsAndRawRefreshNeverEnterPersistedRows', 'sessionWaitingLoginObservesConcurrentRealmDeactivation', 'sessionAmbientRefreshRejectedBeforeRotationAndCallerCommit', 'sessionAmbientReplayRejectedBeforeCallerRollback', 'sessionDirectReplayRevocationAndAuditAreIndependentlyCommitted', 'sessionAmbientLoginRejectedBeforeCallerCommit', 'sessionAmbientLogoutRejectedBeforeCallerRollback']
REQUIRED[IDENTITY_GRANTS] += IDENTITY_SESSION_PG_REQUIRED
IDENTITY_CURRENT_SESSION_UNIT = BASE + 'identity.internal.application.IdentityCurrentSessionApplicationTests'
IDENTITY_CURRENT_SESSION_UNIT_REQUIRED = ['currentMeReturnsApprovedAccountWithoutActivityMutation', 'meaningfulActivityUsesDistinctTrustedAdmissionAndDomainTransition', 'repeatedActivityAtSameInstantDoesNotWrite', 'missingAdmissionCannotCompose', 'missingRequestIdCannotReachAdmissionOrStore', 'rejectedTrustedAdmissionNeverReachesStore', 'authenticatedReadDoesNotAuthorizeMeaningfulActivity', 'malformedTrustedContextsFailWithoutDatabaseAccess', 'expiredFutureOrOverlongAccessFactsFailClosed', 'accessExpiryIsRecheckedAfterWaitingForLocks', 'crossAccountSessionSubstitutionFailsBeforeAccountDisclosure', 'missingPersistedSessionFailsClosed', 'staleTrustedOrPersistedAuthorizationVersionFailsClosed', 'revokedSessionFailsClosed', 'exactIdleOrAbsoluteExpiryCannotBeExtended', 'disabledPendingAndUnverifiedAccountsFailClosed', 'inactiveRealmOrUnsupportedGrantContextFailsClosed', 'meaningfulActivityIsCappedByAbsoluteDeadline', 'backwardClockCannotAdvanceOrRewindActivity', 'activityPersistenceFailureIsPropagatedForTransactionRollback', 'currentSessionBoundaryExposesNoClientIdentityActivityFlagOrSecrets']
REQUIRED[IDENTITY_CURRENT_SESSION_UNIT] = IDENTITY_CURRENT_SESSION_UNIT_REQUIRED
IDENTITY_CURRENT_SESSION_PG_REQUIRED = ['currentSessionMeReturnsApprovedAccountWithoutExtendingIdle', 'currentSessionActivityUpdatesOnlyOwnedIdleFields', 'currentSessionAccountAndRealmSubstitutionFailClosed', 'currentSessionExpiredAccessCannotReadOrRecordActivity', 'currentSessionStaleVersionAndDisabledAccountDenyWithoutMutation', 'currentSessionPendingAccountCannotUseOldSession', 'currentSessionExactIdleAbsoluteAndRevocationDenyWithoutMutation', 'currentSessionInactiveRealmAndExternalGrantsFailClosed', 'currentSessionActivityNeverExtendsAbsoluteDeadline', 'currentSessionActivityWriteFailureRollsBackAllState', 'currentSessionOwnedBoundaryRejectsAmbientReadAndActivity', 'currentSessionWaitingActivityObservesConcurrentRevocation', 'currentSessionWaitingMeObservesConcurrentRealmDeactivation', 'currentSessionConcurrentActivityCannotRewindIdle']
REQUIRED[IDENTITY_GRANTS] += IDENTITY_CURRENT_SESSION_PG_REQUIRED
IDENTITY_ADMIN_UNIT = BASE + 'identity.internal.application.IdentityAdministrationTests'
IDENTITY_ADMIN_UNIT_REQUIRED = ['accountReadReturnsOnlyAcceptedViewWithoutActivityWrites', 'unknownAndUnmanagedTargetHaveSameSafeDenial', 'trustedSubjectMustMatchAuthoritativeSessionOwner', 'expiredAccessFactsCannotReachPersistence', 'waitingForLocksRechecksAccessExpiry', 'disabledActorCannotUseAdministrativeOperations', 'staleAuthorizationVersionAndRevocationDeny', 'inactiveRealmHasOperationAppropriateSafeDenial', 'mixedActorScopesNeverAcquireNewAuthority', 'missingAdmissionAndShortRetentionCannotCompose', 'authorizationPrecedesMalformedMutationMetadata', 'roleAndScopeCatalogExposeOnlyApprovedPair', 'paginationRequestsFilteredLimitPlusOneAndNoTotal', 'pageDefaultsAreBoundedAndEmptyHasNoCursor', 'invalidPageLimitsFailWithoutListing', 'invalidCursorCannotBypassAuthorization', 'cursorBindsActorRealmButNeverGrantsAuthority', 'disableUsesDomainTransitionAndAtomicRevocationPorts', 'pendingDisableDoesNotActivateAccount', 'reactivateVerifiedDisabledAccountWithoutRestoringSessions', 'disabledPendingCannotReactivate', 'selfDisableRemainsForbidden', 'lastAdministratorDomainGuardIsRetained', 'staleVersionFailsBeforeMutation', 'exactReceiptReplayPrecedesStaleVersionWithoutRepeatWrites', 'changedPayloadConflictsOnSameOperationKey', 'auditFailurePropagatesForOwnedTransactionRollback', 'missingIssuerStopsResendBeforeAllWrites', 'activeOrDisabledPendingCannotReceiveResend', 'resendInvalidatesOldProofBeforeProtectedReplacement', 'resendReplayDoesNotGenerateAnotherProof', 'accessExpiryDuringCryptoPreparationAbortsBeforeWrites', 'rejectedTrustedAdmissionCannotListOrMutate', 'publicBoundaryHasNoCredentialOrClientPrincipalFields', 'expiryDuringEmptyQueryCannotReturnAnAuthorizedPage', 'listingAuthorityLookupFailureHasAcceptedSafeCode']
REQUIRED[IDENTITY_ADMIN_UNIT] = IDENTITY_ADMIN_UNIT_REQUIRED
IDENTITY_ADMIN_PG_REQUIRED = ['adminReadFiltersWholeTargetAndDoesNotWriteActivity', 'adminCatalogsExposeOnlyCurrentRegisteredIdentityPair', 'adminPaginationFiltersUnauthorizedRowsBeforeLimit', 'adminCursorCannotBeTransferredBetweenActors', 'adminRevokedStaleAndExpiredSessionsCannotReadOrMutate', 'adminInactiveRealmAndMixedActorRemainDefaultDeny', 'adminDisableAtomicallyRevokesTargetAndPreservesActor', 'adminPendingDisablePreservesUnverifiedCredentialState', 'adminReactivationNeverRestoresOldSessionsOrProofs', 'adminDisabledPendingCannotBeReactivated', 'adminSelfDisableDeniedWithoutAnyMutation', 'adminCurrentAuthorityIsRequiredBeforeReceiptReplay', 'adminExactLifecycleReplayHasNoSecondEffect', 'adminLifecycleChangedPayloadConflictsBeforeStaleVersion', 'adminOperationsUseSeparateReceiptNamespaces', 'adminDisableAuditFailureRollsBackAllState', 'adminDisableReceiptFailureRollsBackAllState', 'adminDisableStateWriteFailureRollsBackAllState', 'adminDisableRevocationFailureRollsBackAllState', 'adminAmbientCallerTransactionsCannotOwnResults', 'adminOppositeDisableConcurrencyKeepsOneActiveAdministrator', 'adminConcurrentSameKeyHasOneLifecycleEffect', 'adminWaitingMutationObservesConcurrentRealmDeactivation', 'adminResendQueuesProtectedReplacementAndKeepsPending', 'adminResendReplayAndConflictNeverRegenerateMaterial', 'adminResendProofFailureRollsBackPriorRevocation', 'adminResendOutboxFailureRollsBackAllState', 'adminResendAuditFailureRollsBackAllState', 'adminResendReceiptFailureRollsBackAllState', 'adminConcurrentResendKeyCreatesOneReplacement', 'adminResendRejectsActiveAndDisabledPendingAccounts', 'adminMissingCryptoFailsBeforeProofOrAccountMutation', 'adminUnsupportedTargetScopesRemainDeniedForAllCommands', 'adminConcurrentConflictingKeysNeverRepeatLifecycleEffect']
REQUIRED[IDENTITY_GRANTS] += IDENTITY_ADMIN_PG_REQUIRED
IDENTITY_HTTP_UNIT = BASE + 'identity.internal.web.ProductionIdentitySecurityTests'
IDENTITY_HTTP_UNIT_REQUIRED = ['validRs256CarriesExactProductionClaims', 'issuerIsSeparateFromDemo', 'audienceIsExactAndSingle', 'noneAlgorithmRejected', 'symmetricAlgorithmConfusionRejected', 'differentRsaAlgorithmRejected', 'unknownKidRejected', 'missingKidRejected', 'wrongSignatureRejected', 'unexpectedHeaderRejected', 'nonJwtTypeRejected', 'exactExpiryIsExclusive', 'futureIssuanceAndNotBeforeRejected', 'lifetimeAboveFifteenMinutesRejected', 'allRequiredClaimsEnforced', 'staleRoleClaimsCannotBecomeAuthority', 'invalidSubjectSessionAndVersionRejected', 'duplicateClaimsAndMalformedEncodingRejected', 'retiredVerificationKeyWorksWithNewSigningKey', 'emergencyKeyRemovalDeniesCompromisedKey', 'mismatchedPrivateKeyFailsBeforeUse', 'missingAndWeakKeysHaveSafeConfigurationFailure', 'signingNeverExtendsApplicationDeadlines', 'admissionWithoutVerifiedRequestFailsClosed', 'numericDateUsesOnlyIdenticalPersistedIssuingSecond', 'requestIdAndActivityFlagAreNeverAuthority', 'admissionIsClearedOnFailureAndNeverInheritedByThread', 'bearerReadReturnsNoStoreAndEtagWithoutTokenEcho', 'unknownRevokedOrStaleAuthoritativeSessionDenied', 'unsignedAccountAndCookieCannotAuthenticate', 'forwardedHeadersNeverTurnHttpIntoTrustedTls', 'nullCrossOriginAndDuplicateOriginDenied', 'duplicateBearerAndQueryTokensDenied', 'mutatingRoutesCannotBypassDeferredCsrfAndLimiter', 'objectAuthorityDenialMasksExistenceAndEtag', 'malformedIdentifiersHaveOnlySafeValidationErrors', 'configurationRejectsDemoProfileAndMissingKeyMaterial', 'mutationAuthorityPrecedesPreconditionDisclosure', 'mutationPreconditionsAndPayloadsRemainStrict', 'selfManagementCannotReachPreconditionsOrMutation', 'completedMutationAttestsActivityAfterApplicationSuccess', 'receiptReplayDoesNotQualifyAsMeaningfulActivity', 'grantWireUsesVerifiedSessionAndAcceptedFieldsOnly']
REQUIRED[IDENTITY_HTTP_UNIT] = IDENTITY_HTTP_UNIT_REQUIRED
IDENTITY_HTTP_PG_REQUIRED = ['httpReadValidRs256UsesRealCurrentStateWithoutIdleWrites', 'httpReadIntegerJwtDatesResolveOnlyOwnedMicrosecondSession', 'httpReadAlreadySignedTokenRejectsRevocation', 'httpReadAlreadySignedTokenRejectsChangedAuthVersion', 'httpReadDisabledAndUnverifiedAccountsReject', 'httpReadInactiveRealmAndMixedActorReject', 'httpReadSessionIdleAndAbsoluteExpiryRejectBeforeDisclosure', 'httpReadForeignAndUnknownTargetsHaveSameMaskedContract', 'httpReadListingsRolesAndScopesKeepWholeTargetIsolation', 'httpReadWrongSessionOwnerFailsBeforeAuthoritativeView', 'httpReadFullChainRejectsPostAndAmbientTransaction', 'httpReadWaitingForLockObservesConcurrentRevocation', 'httpReadBearerDisableAndReplayKeepAtomicApplicationEffects', 'httpReadBearerReactivationDoesNotRestoreTargetSessions', 'httpReadGrantReplacementRequiresAuthoritativeWholeTarget', 'httpReadMutationFailuresNeverRecordActivityOrPartialCommit', 'httpReadMutationConflictAndStaleVersionHaveAcceptedCodes', 'httpReadConcurrentSameKeyMutationHasOneEffect']
REQUIRED[IDENTITY_GRANTS] += IDENTITY_HTTP_PG_REQUIRED
IDENTITY_AUTH_UNIT = BASE + "identity.internal.web.ProductionAuthenticationHttpTests"
IDENTITY_AUTH_UNIT_REQUIRED = ['csrfUsesOnlyChallengeAndSecureCookie', 'loginIssuesAcceptedAccessBodyAndTwoHostOnlyCookies', 'insecureTransportCannotBeUpgradedByForwardedHeaders', 'missingPostOriginRejectedBeforeCredentials', 'crossOriginAndDuplicateOriginsRejected', 'csrfGetWithoutOriginIsSameOriginReadableOnly', 'missingAndDuplicateCsrfHeadersReject', 'duplicateCookiesCannotSelectAnAlternativeCredential', 'csrfStoreDenialPreventsQuotaAndAuthentication', 'invalidJsonAndUnknownFieldsHaveSafeValidationError', 'loginDoesNotNormalizeUsernameOrPassword', 'oversizeAndWrongMediaTypeRejectedBeforeAuthentication', 'invalidCredentialsFinalizeOnlyFailedAccountReservation', 'successfulLoginReleasesOnlyItsOwnAccountReservation', 'exhaustedQuotaIs429WithRetryAfter', 'unavailableQuotaIs503Never429OrAuthentication', 'finalizationFailureNeverPublishesCredentials', 'refreshReplacesCookieWithoutRebindingCsrfOrIdle', 'refreshReplayHasStable401WithoutSuccessCookies', 'logoutClearsRefreshOnlyAfterConfirmedRevocation', 'failedLogoutIs503WithoutClaimingRevocation', 'csrfStorageFailureIs503', 'admissionWithoutVerifiedScopeFailsClosed', 'admissionChecksRequestAndFamilyBeforeRefreshQuota', 'forwardedIpCannotChangeLimiterSubject', 'missingAndZeroLimiterKeyFailConfiguration', 'limiterIdentifiersAreDedicatedHmacAndPurposeSeparated', 'allSixPoliciesKeepExactApprovedLimits', 'refreshStorageFailureIs503WithoutCredentialReplacement']
REQUIRED[IDENTITY_AUTH_UNIT] = IDENTITY_AUTH_UNIT_REQUIRED
IDENTITY_AUTH_PG_REQUIRED = ['authRateAllSixThresholdsAreEnforcedByPostgres', 'authRateSuccessfulLoginDoesNotConsumeFailedAccountBudget', 'authRateSuccessNeverErasesConcurrentFailedReservations', 'authRateMultiDimensionExhaustionIsAtomic', 'authRateIndependentInstancesAndConcurrentReservationsCannotExceedFive', 'authRateReversedDimensionOrderHasOneDeterministicLockOrder', 'authRateExpiredLeaseRemainsConservativelyCounted', 'authRateExpiredLeaseCannotReleaseQuotaOrIssueSuccess', 'authRateRollingWindowExpiryRestoresAdmissionWithoutFixedBoundaryBurst', 'authRateBusinessRollbackCannotUndoCommittedAccounting', 'authRateRuntimeHasOnlyIntendedDmlAndNoOwnerDdl', 'authRateStorageFailureNeverProducesReservation', 'authRateRetentionRemovesOnlyExpiredOwnedRows', 'authRateStoredIdentifiersAreHmacNotPlainSubjects', 'authRateForwardV6PreservesEarlierMigrationChecksumsAndData', 'authBrowserBindingRejectsWrongFamilyRevocationAndExpiry', 'authBrowserPreauthCannotBecomeSessionFamilyAuthority', 'authHttpLoginRefreshAndLogoutUseRealApplicationAndFilterChain', 'authHttpRefreshReplayCommitsFamilyRevocation', 'authHttpRepeatedLogoutUsesFreshPreauthWithoutRestoringSessions', 'authHttpWrongPasswordExhaustsFiveFailuresWithGenericResponses', 'authHttpDisabledAccountAndInactiveRealmNeverIssueCredentials', 'authHttpCsrfCannotCrossFamiliesOrBypassOrigin', 'authHttpPollingAndRefreshNeverExtendSessionIdle', 'authHttpChangedAuthVersionRejectsRefreshAndAccess', 'authHttpAuditFailureRollsBackLoginButKeepsLimiterReservation', 'authHttpConcurrentRefreshHasOneRotationAndDurableReplay', 'authHttpLimiterFailureIs503BeforeSessionCreation', 'authRateRowContentionTimesOutWithoutPermissiveFallback', 'authHttpRefreshEnforcesThirtyPerSessionWithoutIdleExtension', 'authRateDatabaseConstraintsRejectBypassStatesAndInvalidDurations']
REQUIRED[IDENTITY_GRANTS] += IDENTITY_AUTH_PG_REQUIRED
PG = BASE + 'demo.PostgresFoundationTests'
PG_ONLY = {PG, BASE + 'demo.PostgresHealthSafetyTests'}
FOUNDATION_ONLY = {BASE + 'demo.FoundationDependencyTests'}
# Spec003 source-reviewed exact inventory. Existing methods/skips/cleanup rules retained.
CUSTOMER_ADMISSION_UNIT_REQUIRED = {'identity.internal.application.CustomerSessionApplicationTests': ['verifiedCustomerWithNoSystemOrMerchantGrantCanOpenOwnSession', 'emailLongerThanLegacyUsernameLimitUsesCustomerContract', 'systemLoginRetainsItsExactUsernameLimit', 'phoneAndWhitespaceCannotEstablishCustomerIdentifier', 'exactEmailDoesNotCaseFoldTrimOrNormalize', 'identifierAndCredentialDiagnosticsAreRedacted', 'staleOrMissingDerivedIdentifierDeniesEvenActiveAccount', 'unknownCustomerStillComparesPasswordWithoutCreatingAccount', 'disabledIdentityAlwaysDeniesCustomerSession', 'customerStoreRejectsAmbientPhysicalTransaction', 'customerStoreRejectsAmbientSynchronization', 'customerStoreRejectsPreboundCallerResources'], 'identity.internal.application.CustomerAdmissionTests': ['callbackGetsOnlyCurrentSubjectInsideOwnedTransaction', 'suppliedOrCapturedContextCannotBecomeAuthority', 'callbackCannotReenterOrReuseContextOnAnotherThread', 'missingRequestEvidenceDeniesBeforeDatabaseOrBusinessWork', 'invalidSystemOrDemoTokenCannotReachDatabase', 'staleIdentifierAndRevokedSessionDenyBeforeBusinessWork', 'staleAccountVersionDeniesEvenSignedUnexpiredToken', 'substitutedSessionAccountCannotEstablishAuthority', 'expiryDuringCallbackRejectsTransactionBeforeCommit', 'csrfRevocationDuringCallbackRejectsFinalDecision', 'readOrReceiptDoesNotExtendIdle', 'meaningfulNewCommandUpdatesActivityOnlyInSecondOwnedTransaction', 'businessFailureClearsContextAndNeverUpdatesIdle', 'nullBindingHasNoPermissiveDefault', 'subjectAndClaimDiagnosticsDoNotExposeEmailOrAccessValue']}
CUSTOMER_ADMISSION_UNIT_REQUIRED.update({'identity.internal.application.CustomerEnrollmentTests': ['enrollmentCannotHoldPlaintextPasswordOrInvalidPendingState', 'consumingEnrollmentDestroysProtectedMaterialAndCannotReplay', 'exactDeadlineAndFutureTimeDenyActivation', 'resendPreservesIdentityAndCredentialButRenewsProofWindow', 'expiryDestroysProtectedMaterialAndPreservesHistoricalEnrollment', 'enrollmentAndCommandsNeverExposeCredentialsOrContactInDiagnostics', 'applicationHasNoPermissiveAdmissionOrProtectionDefault'], 'identity.internal.infrastructure.CustomerProofProtectionTests': ['purposeDeadlinesAndRequiredBindingsAreExact', 'invalidPartialSessionAndUnscopedOrganizationAreRejected', 'consumptionRequiresEveryCurrentBindingAndCannotReplay', 'futureExpiryConsumedAndRevokedProofsAreUnavailable', 'issuanceEncryptsDistinctCanonicalHighEntropyPossessionTokens', 'verifierBindsLocatorSubjectPurposeTimeSessionAndVersion', 'tamperingDestinationAndEitherKeyIdCannotDecrypt', 'exactExpiryAndConsumedProofDenyDecryptionAndMatching', 'retainedRotationKeysWorkWithoutReinterpretingTheirIdentifiers', 'malformedMissingAndNonCanonicalPossessionInputsFailClosed', 'consentAndReauthenticationCannotBecomeEmailPossessionProofs', 'invalidWeakMissingAndCrossPurposeSharedKeysDenyStartup', 'requestFingerprintsAreKeyedOperationBoundAndLengthDelimited', 'protectedBuffersAreDefensiveClosedAndDiagnosticsRedacted', 'passwordMaterialUsesAcceptedPolicyAndBcryptWithoutRawPersistence', 'deliveryRecipientIsSeparatelyEncryptedAndBoundToProofAndKeys']})
CUSTOMER_ADMISSION_UNIT_REQUIRED.update({'identity.internal.ProductionCustomerConfigurationTests': ['customerProfileCannotActivateWithoutIdentityPostgresAndSandbox', 'sandboxProfileDoesNotNeedOrGenerateCredentialsUntilExplicitRuntimeAssembly', 'sharedIssuerAudienceOrAnyRetainedSystemKeyCannotEnterCustomerTrustRing', 'missingCurrentPrivateKeyOrSystemTrustContextFailsClosed', 'actualRs256TokensCannotCrossPurposeEvenForTheSameAccountAndSession'], 'identity.internal.application.CustomerDeliveryTests': ['leaseIsBoundedAndCannotBeClaimedTwiceWhileLive', 'exactLeaseDeadlineRejectsOldWorkerAndAllowsFencedReclaim', 'leaseNeverOutlivesProofAndExactProofExpiryDeniesCompletion', 'providerAcceptanceIsTerminalWithoutBecomingDelivered', 'transientRetryBackoffStopsAtFiveAttempts', 'retryCannotCrossProofExpiryOrBecomeAnUnboundedLease', 'permanentFailureAndKeyOutageRemainDifferentAuditableStates', 'fifthCrashedAttemptCannotBeReclaimedForever'], 'identity.internal.infrastructure.SmtpCustomerDeliveryTests': ['loopbackSmtpActuallyReceivesPurposeBoundFragmentWithoutClaimingDelivery', 'realStarttlsChecksApprovedCertificateAndAuthenticatesBeforeSending', 'realImplicitTlsUsesApprovedCertificateAndHasNoPlaintextFallback', 'missingStarttlsIsBlockedBeforeAuthEnvelopeOrProofBody', 'untrustedCertificateFailsBeforeAuthAndRecipientDisclosure', 'permanentAndTransientSmtpRejectionsAreDistinctAndDoNotClaimAcceptance', 'retryRetainsStableMessageIdRatherThanIssuingAnotherProof', 'unsafeWireLoggingBlocksBeforeConnectingOrDecryptedBodyEmission', 'insecureOrUnapprovedSmtpConfigurationCannotEnableProductionFallback'], 'identity.internal.web.CustomerHttpSecurityTests': ['customerCsrfHasExactChallengeAndOnlyCustomerCookie', 'emailLoginUsesExactCustomerBodyCookiesAndIndependentQuotas', 'legacyUsernameBodyAndSystemCookiesDoNotBecomeCustomerCredentials', 'refreshUsesCustomerSessionQuotaAndRetainsAcceptedRotationProtocol', 'enrollmentUsesServerCsrfBindingAndIgnoresForwardedClientAddress', 'insecureMissingDuplicateAndForeignOriginsDenyBeforeApplication', 'missingDuplicateCsrfAndCookiesCannotChooseAlternativeBinding', 'malformedUnknownDuplicateAndOversizedBodiesNeverReachEnrollment', 'missingAndMalformedIdempotencyKeysDoNotCreateEffect', 'quotaOutageAndExhaustionDoNotPublishAcceptance', 'preauthScopeCannotBeForgedCapturedReenteredOrUsedOnAnotherThread', 'finalPreauthRecheckRejectsRevokedOrSubstitutedCsrfBinding', 'protectedRequestRequiresLiveSameRequestSessionAndMutationCsrf', 'customerPoliciesHaveExactLimitsAndDistinctLoginReservationSemantics', 'customerOriginConfigurationHasNoHttpWildcardOrPathFallback']})
CUSTOMER_ADMISSION_UNIT_REQUIRED['identity.internal.application.CustomerRecoveryTests'] = ['liveRecoveryHasNoSessionButMustBindCurrentSecurityVersion', 'resetPreservesExactUsernameGrantsAndEligibilityWhileAdvancingBothVersions', 'staleProofSecurityVersionAndWrongAccountVersionDenyReset', 'enrollmentAnotherSubjectAndConsumedProofCannotResetCredential', 'disabledOrUnverifiedAccountCannotRecoverEvenWithProof', 'exactFifteenMinuteDeadlineAndFutureIssuanceDenyReset', 'acceptedPasswordPolicyAndVersionExhaustionHaveNoMutablePartialState', 'closedUnboundHistoricalProofStaysReadableButNeverUsable']
CUSTOMER_ADMISSION_UNIT_REQUIRED['identity.internal.web.CustomerHttpSecurityTests'] += ['recoveryHttpBindsOnlyEmailAndDedicatedRequestQuotas', 'recoveryConfirmationUsesExactInputAndClearsRefreshOnlyAfterCommit', 'recoveryRejectsActorDestinationAndDuplicateProofFieldInjection', 'recoveryFailureNeverClearsCookiesOrFabricatesCredentialChange']
CUSTOMER_ADMISSION_UNIT_REQUIRED.update({'merchant.internal.application.CustomerProfileTests': ['uninitializedSelfProfileIsReadOnlyAndDoesNotCreateTenantOrActivity', 'businessEditDoesNotMutateSecuritySubjectAndUsesVersionedReceipt', 'replayReturnsOriginalSnapshotAfterLaterEditWithoutAuditOrIdleExtension', 'changedBodyOrOriginalVersionCannotReuseCommittedKey', 'staleVersionCannotWriteHistoryAuditOrReceipt', 'invalidContactLanguageAndDisplayCannotPersistOrBecomeAuthority', 'revokedOrConstructedContextCannotReadOrMutateBusinessProfile', 'profileAndCommandDiagnosticsDoNotExposePersonalContact', 'optionalContactsCanBeRemovedWithoutChangingImmutableAccountBinding'], 'merchant.internal.infrastructure.MerchantFingerprintTests': ['exactFieldsNullAndOperationVersionTargetAreCryptographicallyBound', 'retainedKeyRotationPreservesExactRetryAndClonesCallerMaterial', 'missingWeakZeroAndUnknownReceiptKeysFailClosed', 'profileStoreCannotOpenAnIndependentOrUnboundBusinessTransaction']})
REQUIRED.update({BASE+name:methods for name,methods in CUSTOMER_ADMISSION_UNIT_REQUIRED.items()})
CUSTOMER_ADMISSION_PG_REQUIRED = ['customerLoginUsesIndependentVerifiedBindingWithoutSystemGrant', 'customerExactEmailAndCurrentSecurityMasterPreventStaleIndexAdmission', 'dualPurposeAccountRefreshCannotCrossCustomerAndSystemTables', 'customerRefreshReplayCommitsOnlyCustomerFamilyRevocation', 'identityVersionAndDisableInvalidateCustomerAdmissionImmediately', 'customerCsrfHashesAndFamilyBindingCannotCrossSystemStore', 'customerFinalDeadlineRollsBackStagedWritesInOwnedTransaction', 'customerMeaningfulActivityCommitsAfterBusinessAndContextCannotEscape']
CUSTOMER_ADMISSION_PG_REQUIRED += ['enrollmentQueuesOnlyProtectedMaterialWithoutAccountOrGrant', 'enrollmentUniformReceiptAndChangedBindingPreventEnumerationAndDuplicates', 'verificationAtomicallyCreatesVerifiedPersonalAccountWithoutSystemAuthority', 'resendRevokesOldProofAndCiphertextBeforeCreatingFreshIntent', 'wrongFutureAndExpiredEnrollmentProofsCannotCreateAccount', 'verificationAuditFailureRollsBackAccountIndexProofAndReceipt', 'concurrentVerificationHasOneAccountAndOneSingleUseOutcome', 'enrollmentStoreRejectsCallerOwnedTransactionAndResourceBindings']
CUSTOMER_ADMISSION_PG_REQUIRED += ['customerHttpEnrollmentMailVerificationLoginRefreshLogoutUseActualChains', 'customerHttpSystemTokenAndRevokedAccountCannotBecomeCustomerAuthority', 'customerHttpRegistrationReplayAndDurableQuotaAreNotReleasedByRollback', 'customerHttpQuotaStorageOutageDeniesBeforePendingAccountOrDelivery', 'deliveryLeaseFencingKeepsExpiredWorkerFromRecordingNewLeaseOutcome', 'deliveryBoundedFailuresAndDeadlineScrubPendingSecretsWithoutDeletingHistory', 'deliveryAuditFailureRollsBackLeaseAndRealSmtpRunsOutsidePhysicalTransaction', 'missingDeliveryKeyBlocksWithoutSendingOrClaimingProviderAcceptance', 'customerPasswordQuotaReleasesOnlyItsOwnSuccessAndCannotConsumeSystemBudget', 'customerPreauthCsrfMustBeUnboundCurrentAndNeverSystemPurpose', 'enrollmentAuditRollbackCannotReleaseDurableCustomerQuota']
CUSTOMER_ADMISSION_PG_REQUIRED += ['recoveryUniformRequestsNeverUseUnverifiedDisabledStaleOrLegacyOnlyEmail', 'recoveryHttpActualMailResetsCredentialRevokesBothFamiliesAndPreservesGrants', 'recoveryWrongExpiredAndReplayedProofCannotMutateCredentialOrSessions', 'recoveryReplacementRevokesOnlyPreviousRecoveryAndNotPrivilegedInvitation', 'recoveryMandatoryAuditFailureRollsBackCredentialBothFamiliesAndProofReceipt', 'concurrentRecoveryConfirmationHasOneSecurityVersionChangeAndOneReceipt', 'recoverySecurityVersionChangeOrDisableBeforeConsumptionAlwaysDenies', 'enrollmentProofReceiptSurvivesCsrfRotationWithoutRepeatingAccountActivation', 'recoveryPersistenceRejectsConstructedDomainChangeWithoutActualHmacPossession']
CUSTOMER_ADMISSION_PG_REQUIRED += ['profileHttpReadIsSelfScopedReadOnlyWithoutMembershipOrIdleExtension', 'profileHttpUpdatePreservesIdentityAndReturnsOriginalVersionOnReceiptReplay', 'profileHttpRejectsActorAuthorityDuplicateFieldAndPreconditionInjection', 'profileAuditFailureRollsBackAggregateHistoryReceiptAndActivity', 'concurrentProfileFirstUpdateHasOneCommittedVersionAndStaleLoser', 'revokedCustomerAndSystemOnlyPrincipalCannotReadOrChangePersonalProfile']
REQUIRED[IDENTITY_GRANTS] += CUSTOMER_ADMISSION_PG_REQUIRED
CUSTOMER_MIGRATION_PG_REQUIRED = ['customerV7UpgradePreservesV6DataChecksumsAndCreatesNoBindings', 'customerIdentifierHasExactUniqueActiveAccountBinding', 'customerSessionsRefreshAndCsrfEnforceOwnPurposeForeignKeys', 'customerTablesRemainOwnerOwnedWithoutRuntimeDdlOrAuditEscalation']
CUSTOMER_MIGRATION_PG_REQUIRED += ['enrollmentV8UpgradePreservesPopulatedV7AndEarlierChecksums', 'enrollmentAndProofConstraintsDenyPlaintextWrongSubjectAndDeadline', 'enrollmentPersistenceHasLeastPrivilegeAndImmutableReceipts']
CUSTOMER_MIGRATION_PG_REQUIRED += ['customerQuotaV9PreservesV8ChecksumsAndExistingSystemReservations', 'customerQuotaConstraintsPreserveCountedNonPasswordAndRejectUnknownPolicies']
CUSTOMER_MIGRATION_PG_REQUIRED += ['recoveryV10UpgradePreservesV9AndRevokesOnlyUnsupportedUnboundRecovery', 'recoveryProofDatabaseRequiresSecurityVersionWithoutSession', 'proofReceiptDatabaseEnforcesPurposeRetentionAndNarrowPrivileges']
CUSTOMER_MIGRATION_PG_REQUIRED += ['profileV11UpgradePreservesV10IdentityDataChecksumsAndOldSystemWrites', 'profileDatabaseRequiresHistoryAndPreservesImmutableAccountVersionBinding', 'profileRuntimeCannotRewriteHistoryAuditReceiptOrDeleteEvidence', 'profileReceiptRequiresExactSubjectVersionRetentionAndContainsNoRawContact']
REQUIRED[IDENTITY_MIGRATION] += CUSTOMER_MIGRATION_PG_REQUIRED

TESTCONTAINERS_ONLY = {BASE + 'demo.PostgresTestcontainersTests', BASE + 'testsupport.TestcontainersRuntimePolicyTests', IDENTITY_MIGRATION, IDENTITY_GRANTS}
SUREFIRE_NAMES = {
    (BASE + 'demo.DemoSecurityEvidenceTests',
     'credentialsAndBearerTokenDoNotAppearInApplicationLogs(CapturedOutput)'):
        'credentialsAndBearerTokenDoNotAppearInApplicationLogs',
}


def expected_suites(mode):
    return {k: v for k, v in REQUIRED.items()
            if (k in FOUNDATION_ONLY if mode == 'foundation' else mode != 'architecture' or '.architecture.' in k)
            and (k not in TESTCONTAINERS_ONLY or mode == 'postgres')}


def audit_task_c_credentials(credential_file, diagnostics):
    """Fail closed; return only a boolean, never credential or diagnostic text."""
    try:
        path = Path(credential_file)
        if path.is_symlink():
            return False
        secrets = path.read_text(encoding='utf-8').splitlines()
        if not secrets or any(len(value) != 43 or not all(c.isascii() and (c.isalnum() or c in '-_') for c in value) for value in secrets):
            return False
        paths = list(map(Path, diagnostics))
        if not paths or any(not p.is_file() or p.resolve() == path.resolve() for p in paths):
            return False
        return not any(value.encode() in p.read_bytes() for p in paths for value in secrets)
    except (OSError, ValueError, UnicodeError):
        return False


TASK_C_SUITES = {
    'demo-postgres': BASE + 'demo.PostgresTestcontainersTests',
    'identity-migration': IDENTITY_MIGRATION,
    'identity-grants': IDENTITY_GRANTS,
}
TASK_C_POSTGRES = 'postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c'
TASK_C_RYUK = 'testcontainers/ryuk:0.14.0@sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0'
TASK_C_STAGES = ('reserved', 'observed', 'created', 'verified', 'stopped')
TASK_C_UUID = r'[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}'


def task_c_execution_identity():
    """Bind evidence to this job/attempt; no self-declared artifact-only freshness."""
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        run, attempt, job = (os.environ.get(k, '') for k in ('GITHUB_RUN_ID', 'GITHUB_RUN_ATTEMPT', 'GITHUB_JOB'))
        if not re.fullmatch('[1-9][0-9]*', run) or not re.fullmatch('[1-9][0-9]*', attempt) or not re.fullmatch('[A-Za-z0-9_-]{1,80}', job):
            raise ValueError('Invalid execution identity')
        return f'gha:{run}:{attempt}:{job}'
    local = os.environ.get('TASK_C_EXECUTION_ID', '')
    if not re.fullmatch(TASK_C_UUID, local):
        raise ValueError('Missing execution identity')
    return 'local:' + local


def task_c_record(path, keys):
    """Bounded strict metadata, no aliases/duplicates/partial publications or secret output."""
    info = path.lstat()
    if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1 or info.st_size > 4096:
        raise ValueError('Unsafe evidence file')
    raw = path.read_text(encoding='ascii')
    if not raw.endswith('\n'):
        raise ValueError('Incomplete evidence')
    result = {}
    for line in raw.splitlines():
        key, separator, value = line.partition('=')
        if not separator or key in result or not value or key not in keys:
            raise ValueError('Invalid evidence field')
        result[key] = value
    if set(result) != keys:
        raise ValueError('Missing evidence field')
    return result


def task_c_resource_inventory(evidence):
    """Exactly two known suites; shared Ryuk is explicit, never generic deduplication."""
    path = Path(evidence)
    if path.name != 'task-c-runtime.txt' or path.parent.is_symlink():
        raise ValueError('Invalid evidence location')
    base_keys = {'format', 'execution', 'taskRun', 'session'}
    manifest = task_c_record(path, base_keys | {'suite.' + k for k in TASK_C_SUITES})
    if manifest['format'] != 'task-c-lifecycle-v2' or manifest['execution'] != task_c_execution_identity():
        raise ValueError('Stale or unsupported execution')
    if not all(re.fullmatch(TASK_C_UUID, manifest[k]) for k in ('taskRun', 'session')):
        raise ValueError('Invalid ownership identity')
    if any(manifest['suite.' + k] != v for k, v in TASK_C_SUITES.items()):
        raise ValueError('Unknown or duplicate suite')
    directory = path.with_suffix('.d')
    if directory.is_symlink() or not directory.is_dir():
        raise ValueError('Missing evidence directory')
    expected_files = {suite + '.' + stage for suite in TASK_C_SUITES for stage in TASK_C_STAGES}
    if {p.name for p in directory.iterdir()} != expected_files:
        raise ValueError('Missing, failed, duplicate or unexpected lifecycle record')
    resource_keys = {'pgContainer', 'pgVolume', 'volumeContainer', 'ryukContainer', 'pgImage', 'ryukImage',
                     'pgLabelRun', 'pgLabelSession', 'ryukLabelRun', 'ryukName'}
    containers, volumes, shared_ryuk = set(), set(), None
    for suite, classname in TASK_C_SUITES.items():
        facts = None
        for stage in TASK_C_STAGES:
            keys = base_keys | {'suite', 'stage'}
            if stage == 'observed': keys |= {'pgContainer'}
            elif stage in ('created', 'verified', 'stopped'): keys |= resource_keys
            record = task_c_record(directory / (suite + '.' + stage), keys)
            if any(record[k] != manifest[k] for k in base_keys) or record['suite'] != classname or record['stage'] != stage:
                raise ValueError('Lifecycle identity mismatch')
            if stage == 'observed':
                observed = record['pgContainer']
                if not re.fullmatch('[a-f0-9]{64}', observed): raise ValueError('Invalid observed resource')
            if stage not in ('created', 'verified', 'stopped'): continue
            current = {k: record[k] for k in resource_keys}
            if facts is not None and current != facts:
                raise ValueError('Resource substitution')
            facts = current
        if facts['pgImage'] != TASK_C_POSTGRES or facts['ryukImage'] != TASK_C_RYUK:
            raise ValueError('Unapproved image')
        if not all(re.fullmatch('[a-f0-9]{64}', facts[k]) for k in ('pgContainer', 'pgVolume', 'ryukContainer')):
            raise ValueError('Invalid resource identity')
        if (facts['pgLabelRun'] != manifest['taskRun'] or facts['ryukLabelRun'] != manifest['taskRun'] or
                facts['pgLabelSession'] != manifest['session'] or facts['ryukName'] != '/testcontainers-ryuk-' + manifest['session']):
            raise ValueError('Resource ownership mismatch')
        if facts['volumeContainer'] != facts['pgContainer'] or facts['pgContainer'] != observed:
            raise ValueError('Volume/observation binding mismatch')
        if facts['pgContainer'] in containers or facts['pgVolume'] in volumes or facts['pgContainer'] == facts['ryukContainer']:
            raise ValueError('Duplicate suite resource')
        ryuk = tuple(facts[k] for k in ('ryukContainer', 'ryukImage', 'ryukLabelRun', 'ryukName'))
        if shared_ryuk is not None and shared_ryuk != ryuk:
            raise ValueError('Shared reaper identity mismatch')
        shared_ryuk = ryuk
        containers.add(facts['pgContainer']); volumes.add(facts['pgVolume'])
    if shared_ryuk[0] in containers:
        raise ValueError('Resource type collision')
    containers.add(shared_ryuk[0])
    return containers, volumes, manifest['taskRun']


def audit_task_c_cleanup(evidence, docker='docker', timeout=30):
    """Read-only absence audit of ALL suites. No flag/record can authorize deletion."""
    try:
        ids, volumes, run = task_c_resource_inventory(evidence)
        deadline = time.monotonic() + timeout
        while True:
            def query(*args):
                p = subprocess.run([docker, *args], text=True, capture_output=True, timeout=15)
                if p.returncode:
                    raise OSError('Docker read-only audit failed')
                return set(p.stdout.splitlines())
            containers = query('ps', '-aq', '--no-trunc')
            volumes_now = query('volume', 'ls', '-q')
            owned = query('ps', '-aq', '--filter', 'label=com.elogistic.task-c.run=' + run)
            if not ids.intersection(containers) and not volumes.intersection(volumes_now) and not owned:
                return True
            if time.monotonic() >= deadline:
                return False
            time.sleep(1)
    except (OSError, ValueError, subprocess.SubprocessError):
        return False


def verify(source, output, mode, maven_exit):
    problems, seen, rows = [], {}, []
    expected = expected_suites(mode)
    for path in sorted(Path(source).glob('TEST-*.xml')):
        try:
            suite = ET.parse(path).getroot()
            name = suite.attrib['name']
            if name not in expected or name in seen or suite.tag != 'testsuite':
                raise ValueError('Unexpected/duplicate suite')
            cases = suite.findall('testcase')
            names, counts = {}, dict(tests=len(cases), failures=0, errors=0, skipped=0)
            for case in cases:
                method = SUREFIRE_NAMES.get((name, case.attrib['name']), case.attrib['name'])
                # Do not publish arbitrary report strings (including secrets in dynamic names).
                if method not in REQUIRED[name] or method in names or case.attrib['classname'] != name:
                    raise ValueError('Unexpected/duplicate test')
                status = 'passed'
                if sum(case.find(tag) is not None for tag in ('failure', 'error', 'skipped')) > 1:
                    raise ValueError('Conflicting test outcomes')
                for tag in ('failure', 'error', 'skipped'):
                    if case.find(tag) is not None:
                        key = {'failure': 'failures', 'error': 'errors', 'skipped': 'skipped'}[tag]
                        counts[key] += 1
                        status = tag
                names[method] = status
                rows.append((name, method, status))
            if any(int(suite.attrib[k]) != value for k, value in counts.items()):
                raise ValueError('Inconsistent counts')
            seen[name] = names
        except (ET.ParseError, KeyError, ValueError, OSError):
            problems.append('Malformed, unexpected or inconsistent Surefire report')
    for name, methods in expected.items():
        actual = seen.get(name, {})
        for method in methods:
            wanted = 'skipped' if (mode == 'h2' and name in PG_ONLY) or (mode in ('h2', 'postgres') and name in FOUNDATION_ONLY) else 'passed'
            if actual.get(method) != wanted:
                problems.append(f'{name}.{method}: required {wanted}')
    if not rows:
        problems.append('No test cases discovered')
    if maven_exit:
        problems.append('Maven returned a nonzero exit code')
    counts = {status: sum(row[2] == status for row in rows)
              for status in ('passed', 'failure', 'error', 'skipped')}
    summary = dict(mode=mode, status='FAIL' if problems else 'PASS', discovered=len(rows),
                   executed=len(rows) - counts['skipped'], **counts, problems=problems)
    destination = Path(output)
    destination.mkdir(parents=True, exist_ok=True)
    (destination / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    safe = ET.Element('testsuite', name=f'CI-{mode}', tests=str(len(rows)),
                      failures=str(counts['failure']), errors=str(counts['error']), skipped=str(counts['skipped']))
    for name, method, status in rows:
        test = ET.SubElement(safe, 'testcase', classname=name, name=method)
        if status != 'passed':
            ET.SubElement(test, status).text = 'See local private diagnostics; raw output is not published.'
    ET.ElementTree(safe).write(destination / 'tests.xml', encoding='utf-8', xml_declaration=True)
    print(f"{mode}: {summary['status']}; discovered={len(rows)}, executed={summary['executed']}, "
          f"passed={counts['passed']}, skipped={counts['skipped']}")
    for problem in problems:
        print(problem)
    return not problems


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--mode', choices=['architecture', 'h2', 'postgres', 'foundation'], required=True)
    parser.add_argument('--maven-exit', type=int, required=True)
    args = parser.parse_args()
    raise SystemExit(0 if verify(args.source, args.output, args.mode, args.maven_exit) else 1)
