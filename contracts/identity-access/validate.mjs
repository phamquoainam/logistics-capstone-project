// Process-free contract checks: local reads/Ajv only. No HTTP, child processes or writes.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const require = createRequire(path.join(root, 'packages/api-client/package.json'));
for (const [name, version] of [['ajv','8.20.0'],['ajv-formats','3.0.1']]) {
  assert.equal(require(name+'/package.json').version, version, 'Pinned tool mismatch: '+name);
}
const Ajv = require('ajv/dist/2020.js').default;
const addFormats = require('ajv-formats');
const doc = JSON.parse(fs.readFileSync(path.join(root,'contracts/openapi/identity-access.yaml'),'utf8'));
const fixtures = JSON.parse(fs.readFileSync(path.join(root,'contracts/identity-access/fixtures.json'),'utf8'));
// Independent source constants: NEVER infer expected inventory from the document being tested.
const EXPECTED = [
  [
    "getIdentityCsrf",
    "get",
    "/auth/csrf",
    null,
    "CsrfChallenge",
    200,
    "public",
    false,
    false,
    {
      "403": [
        "ORIGIN_REJECTED"
      ],
      "429": [
        "RATE_LIMITED"
      ]
    }
  ],
  [
    "loginIdentity",
    "post",
    "/auth/login",
    "LoginRequest",
    "AccessSession",
    200,
    "preauth",
    false,
    false,
    {
      "400": [
        "VALIDATION_FAILED"
      ],
      "401": [
        "INVALID_CREDENTIALS"
      ],
      "403": [
        "CSRF_REJECTED",
        "ORIGIN_REJECTED"
      ],
      "429": [
        "RATE_LIMITED"
      ]
    }
  ],
  [
    "refreshIdentity",
    "post",
    "/auth/refresh",
    null,
    "AccessSession",
    200,
    "session",
    false,
    false,
    {
      "401": [
        "SESSION_INVALID",
        "REFRESH_REPLAY"
      ],
      "403": [
        "CSRF_REJECTED",
        "ORIGIN_REJECTED"
      ],
      "429": [
        "RATE_LIMITED"
      ]
    }
  ],
  [
    "logoutIdentity",
    "post",
    "/auth/logout",
    null,
    null,
    204,
    "preauth",
    false,
    false,
    {
      "403": [
        "CSRF_REJECTED",
        "ORIGIN_REJECTED"
      ]
    }
  ],
  [
    "getIdentityMe",
    "get",
    "/auth/me",
    null,
    "Account",
    200,
    "bearer",
    false,
    false,
    {
      "401": [
        "SESSION_INVALID"
      ]
    }
  ],
  [
    "requestIdentityRecovery",
    "post",
    "/auth/recovery",
    "RecoveryRequest",
    "Accepted",
    202,
    "preauth",
    true,
    false,
    {
      "400": [
        "VALIDATION_FAILED"
      ],
      "403": [
        "CSRF_REJECTED",
        "ORIGIN_REJECTED"
      ],
      "409": [
        "IDEMPOTENCY_CONFLICT"
      ],
      "429": [
        "RATE_LIMITED"
      ]
    }
  ],
  [
    "confirmIdentityRecovery",
    "post",
    "/auth/recovery/confirm",
    "RecoveryConfirmRequest",
    null,
    204,
    "preauth",
    true,
    false,
    {
      "400": [
        "VALIDATION_FAILED"
      ],
      "403": [
        "CSRF_REJECTED",
        "ORIGIN_REJECTED"
      ],
      "409": [
        "IDEMPOTENCY_CONFLICT",
        "TOKEN_INVALID_OR_EXPIRED"
      ],
      "429": [
        "RATE_LIMITED"
      ]
    }
  ],
  [
    "acceptIdentityInvitation",
    "post",
    "/auth/invitations/accept",
    "InvitationAcceptRequest",
    null,
    204,
    "preauth",
    true,
    false,
    {
      "400": [
        "VALIDATION_FAILED"
      ],
      "403": [
        "CSRF_REJECTED",
        "ORIGIN_REJECTED"
      ],
      "409": [
        "IDEMPOTENCY_CONFLICT",
        "TOKEN_INVALID_OR_EXPIRED",
        "INVALID_ACCOUNT_STATE"
      ],
      "429": [
        "RATE_LIMITED"
      ]
    }
  ],
  [
    "listIdentityAccounts",
    "get",
    "/admin/users",
    null,
    "AccountPage",
    200,
    "bearer",
    false,
    false,
    {
      "400": [
        "VALIDATION_FAILED"
      ],
      "401": [
        "SESSION_INVALID"
      ],
      "403": [
        "AUTHORITY_REQUIRED"
      ]
    }
  ],
  [
    "provisionIdentityAccount",
    "post",
    "/admin/users",
    "ProvisionRequest",
    "Account",
    201,
    "bearer",
    true,
    false,
    {
      "400": [
        "VALIDATION_FAILED"
      ],
      "401": [
        "SESSION_INVALID"
      ],
      "403": [
        "AUTHORITY_REQUIRED"
      ],
      "409": [
        "USERNAME_CONFLICT",
        "IDEMPOTENCY_CONFLICT"
      ]
    }
  ],
  [
    "getIdentityAccount",
    "get",
    "/admin/users/{accountId}",
    null,
    "Account",
    200,
    "bearer",
    false,
    false,
    {
      "400": [
        "VALIDATION_FAILED"
      ],
      "401": [
        "SESSION_INVALID"
      ],
      "404": [
        "ACCOUNT_NOT_ACCESSIBLE"
      ]
    }
  ],
  [
    "replaceIdentityGrants",
    "put",
    "/admin/users/{accountId}/grants",
    "ReplaceGrantsRequest",
    "Account",
    200,
    "bearer",
    true,
    true,
    {
      "400": [
        "VALIDATION_FAILED",
        "PRECONDITION_MALFORMED"
      ],
      "401": [
        "SESSION_INVALID"
      ],
      "403": [
        "SELF_MANAGEMENT_FORBIDDEN"
      ],
      "404": [
        "ACCOUNT_NOT_ACCESSIBLE"
      ],
      "409": [
        "IDEMPOTENCY_CONFLICT",
        "LAST_ADMIN_PROTECTED"
      ],
      "412": [
        "PRECONDITION_STALE"
      ],
      "428": [
        "PRECONDITION_REQUIRED"
      ]
    }
  ],
  [
    "disableIdentityAccount",
    "post",
    "/admin/users/{accountId}/disable",
    "LifecycleRequest",
    "Account",
    200,
    "bearer",
    true,
    true,
    {
      "400": [
        "VALIDATION_FAILED",
        "PRECONDITION_MALFORMED"
      ],
      "401": [
        "SESSION_INVALID"
      ],
      "403": [
        "SELF_MANAGEMENT_FORBIDDEN"
      ],
      "404": [
        "ACCOUNT_NOT_ACCESSIBLE"
      ],
      "409": [
        "IDEMPOTENCY_CONFLICT",
        "LAST_ADMIN_PROTECTED",
        "INVALID_ACCOUNT_STATE"
      ],
      "412": [
        "PRECONDITION_STALE"
      ],
      "428": [
        "PRECONDITION_REQUIRED"
      ]
    }
  ],
  [
    "reactivateIdentityAccount",
    "post",
    "/admin/users/{accountId}/reactivate",
    "LifecycleRequest",
    "Account",
    200,
    "bearer",
    true,
    true,
    {
      "400": [
        "VALIDATION_FAILED",
        "PRECONDITION_MALFORMED"
      ],
      "401": [
        "SESSION_INVALID"
      ],
      "404": [
        "ACCOUNT_NOT_ACCESSIBLE"
      ],
      "409": [
        "IDEMPOTENCY_CONFLICT",
        "INVALID_ACCOUNT_STATE"
      ],
      "412": [
        "PRECONDITION_STALE"
      ],
      "428": [
        "PRECONDITION_REQUIRED"
      ]
    }
  ],
  [
    "resendIdentityInvitation",
    "post",
    "/admin/users/{accountId}/invitation",
    "LifecycleRequest",
    "Accepted",
    202,
    "bearer",
    true,
    true,
    {
      "400": [
        "VALIDATION_FAILED",
        "PRECONDITION_MALFORMED"
      ],
      "401": [
        "SESSION_INVALID"
      ],
      "404": [
        "ACCOUNT_NOT_ACCESSIBLE"
      ],
      "409": [
        "IDEMPOTENCY_CONFLICT",
        "INVALID_ACCOUNT_STATE"
      ],
      "412": [
        "PRECONDITION_STALE"
      ],
      "428": [
        "PRECONDITION_REQUIRED"
      ],
      "429": [
        "RATE_LIMITED"
      ]
    }
  ],
  [
    "listIdentityRoles",
    "get",
    "/admin/roles",
    null,
    "RoleCatalog",
    200,
    "bearer",
    false,
    false,
    {
      "401": [
        "SESSION_INVALID"
      ],
      "403": [
        "AUTHORITY_REQUIRED"
      ]
    }
  ],
  [
    "listIdentityScopes",
    "get",
    "/admin/scopes",
    null,
    "ScopeCatalog",
    200,
    "bearer",
    false,
    false,
    {
      "401": [
        "SESSION_INVALID"
      ],
      "403": [
        "AUTHORITY_REQUIRED"
      ]
    }
  ]
];
const SHAPES = {
  "Username": {
    "type": "string",
    "minLength": 1,
    "maxLength": 80,
    "pattern": "\\S"
  },
  "NewPassword": {
    "type": "string",
    "minLength": 15,
    "maxLength": 72,
    "writeOnly": true
  },
  "LoginPassword": {
    "type": "string",
    "minLength": 1,
    "maxLength": 72,
    "writeOnly": true
  },
  "Proof": {
    "type": "string",
    "minLength": 32,
    "maxLength": 512,
    "writeOnly": true
  },
  "Grant": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "role": {
        "type": "string",
        "minLength": 1,
        "maxLength": 80
      },
      "scopeType": {
        "type": "string",
        "enum": [
          "IDENTITY",
          "MERCHANT",
          "WAREHOUSE",
          "HUB",
          "ASSIGNMENT"
        ]
      },
      "scopeId": {
        "type": "string",
        "format": "uuid"
      }
    },
    "required": [
      "role",
      "scopeType",
      "scopeId"
    ]
  },
  "Account": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "id": {
        "type": "string",
        "format": "uuid"
      },
      "username": {
        "$ref": "#/components/schemas/Username"
      },
      "displayName": {
        "type": "string",
        "minLength": 1,
        "maxLength": 160
      },
      "email": {
        "type": "string",
        "format": "email"
      },
      "status": {
        "type": "string",
        "enum": [
          "PENDING_ACTIVATION",
          "ACTIVE",
          "DISABLED"
        ]
      },
      "grants": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/Grant"
        },
        "uniqueItems": true
      },
      "version": {
        "type": "integer",
        "minimum": 1
      },
      "emailVerified": {
        "type": "boolean"
      }
    },
    "required": [
      "id",
      "username",
      "displayName",
      "email",
      "status",
      "grants",
      "version",
      "emailVerified"
    ]
  },
  "AccountPage": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/Account"
        },
        "maxItems": 100
      },
      "nextCursor": {
        "type": [
          "string",
          "null"
        ],
        "maxLength": 512
      }
    },
    "required": [
      "items",
      "nextCursor"
    ]
  },
  "LoginRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "username": {
        "$ref": "#/components/schemas/Username"
      },
      "password": {
        "$ref": "#/components/schemas/LoginPassword"
      }
    },
    "required": [
      "username",
      "password"
    ]
  },
  "AccessSession": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "accessToken": {
        "type": "string",
        "minLength": 1
      },
      "tokenType": {
        "const": "Bearer",
        "type": "string"
      },
      "expiresIn": {
        "type": "integer",
        "minimum": 1,
        "maximum": 900
      },
      "sessionExpiresAt": {
        "type": "string",
        "format": "date-time"
      },
      "user": {
        "$ref": "#/components/schemas/Account"
      }
    },
    "required": [
      "accessToken",
      "tokenType",
      "expiresIn",
      "sessionExpiresAt",
      "user"
    ]
  },
  "CsrfChallenge": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "csrfToken": {
        "type": "string",
        "minLength": 32,
        "maxLength": 512
      }
    },
    "required": [
      "csrfToken"
    ]
  },
  "ProvisionRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "username": {
        "$ref": "#/components/schemas/Username"
      },
      "displayName": {
        "type": "string",
        "minLength": 1,
        "maxLength": 160
      },
      "email": {
        "type": "string",
        "format": "email"
      },
      "grants": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/Grant"
        },
        "minItems": 1,
        "uniqueItems": true
      }
    },
    "required": [
      "username",
      "displayName",
      "email",
      "grants"
    ]
  },
  "ReplaceGrantsRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "grants": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/Grant"
        },
        "minItems": 1,
        "uniqueItems": true
      },
      "reason": {
        "type": "string",
        "minLength": 1,
        "maxLength": 300
      }
    },
    "required": [
      "grants",
      "reason"
    ]
  },
  "LifecycleRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "reason": {
        "type": "string",
        "minLength": 1,
        "maxLength": 300
      }
    },
    "required": [
      "reason"
    ]
  },
  "InvitationAcceptRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "token": {
        "$ref": "#/components/schemas/Proof"
      },
      "password": {
        "$ref": "#/components/schemas/NewPassword"
      }
    },
    "required": [
      "token",
      "password"
    ]
  },
  "RecoveryRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "username": {
        "$ref": "#/components/schemas/Username"
      },
      "email": {
        "type": "string",
        "format": "email"
      }
    },
    "required": [
      "username",
      "email"
    ]
  },
  "RecoveryConfirmRequest": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "token": {
        "$ref": "#/components/schemas/Proof"
      },
      "password": {
        "$ref": "#/components/schemas/NewPassword"
      }
    },
    "required": [
      "token",
      "password"
    ]
  },
  "Accepted": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "message": {
        "type": "string",
        "const": "If eligible, instructions will be sent."
      }
    },
    "required": [
      "message"
    ]
  },
  "Role": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "code": {
        "type": "string",
        "minLength": 1,
        "maxLength": 80
      }
    },
    "required": [
      "code",
      "description"
    ]
  },
  "Scope": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "id": {
        "type": "string",
        "format": "uuid"
      },
      "type": {
        "type": "string",
        "enum": [
          "IDENTITY",
          "MERCHANT",
          "WAREHOUSE",
          "HUB",
          "ASSIGNMENT"
        ]
      },
      "displayName": {
        "type": "string",
        "minLength": 1,
        "maxLength": 160
      }
    },
    "required": [
      "id",
      "type",
      "displayName"
    ]
  },
  "RoleCatalog": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/Role"
        }
      }
    },
    "required": [
      "items"
    ]
  },
  "ScopeCatalog": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "items": {
        "type": "array",
        "items": {
          "$ref": "#/components/schemas/Scope"
        }
      }
    },
    "required": [
      "items"
    ]
  },
  "Error": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "code": {
        "type": "string",
        "pattern": "^[A-Z][A-Z0-9_]+$"
      },
      "message": {
        "type": "string",
        "minLength": 1
      },
      "requestId": {
        "type": "string",
        "format": "uuid"
      },
      "fields": {
        "type": "array",
        "items": {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "field": {
              "type": "string",
              "minLength": 1
            },
            "code": {
              "type": "string",
              "minLength": 1
            }
          },
          "required": [
            "field",
            "code"
          ]
        }
      }
    },
    "required": [
      "code",
      "message",
      "requestId"
    ]
  }
};
const POLICY_STRUCTURES = {
  "authorization": {
    "targetRead": "ALL_CURRENT_GRANTS",
    "fullReplacement": "UNION_CURRENT_REQUESTED_GRANTS",
    "lifecycle": "ALL_CURRENT_GRANTS",
    "authoritySource": "RESOURCE_OWNER_PUBLIC_API_FAIL_CLOSED",
    "revocation": "ALL_TARGET_SESSIONS_IN_IDENTITY_TRANSACTION"
  },
  "csrf": {
    "binding": "SERVER_BOUND_PREAUTH_OR_SESSION",
    "authPostOrigin": "EXACT_REQUIRED",
    "challengeGetRotatesRefresh": false,
    "loginLogoutRebind": true,
    "refreshCookie": {
      "hostOnly": true,
      "secure": true,
      "httpOnly": true,
      "sameSite": "Strict",
      "path": "/"
    }
  },
  "reactivation": {
    "previouslyActivated": true,
    "verifiedEmail": true,
    "disabledPendingEligible": false,
    "restoreOldSessions": false
  },
  "preconditions": {
    "malformed": 400,
    "missing": 428,
    "staleNonReplay": 412,
    "businessConflict": 409,
    "authorizedReceiptReplayBeforeStale": true
  },
  "logout": {
    "refreshCookieRequired": false,
    "csrfRequired": true,
    "originRequired": true,
    "missingCookieResult": 204,
    "presentCookieRequiresFamilyBinding": true
  },
  "recovery": {
    "receiptScope": "PREAUTH_BINDING_OPERATION_KEY",
    "requestFingerprintIncludes": [
      "username",
      "email"
    ],
    "receiptForEligibleAndIneligible": true,
    "deliveryAddress": "STORED_VERIFIED_EMAIL_ONLY",
    "submittedEmailMustMatchStored": true,
    "uniformAcceptedResponse": true,
    "receiptStoresPlaintextSecrets": false
  }
};
const CLAIMS = ['iss','aud','sub','jti','sid','iat','nbf','exp','authVersion'];
const SECS = {public:[],preauth:[{csrfState:[]}],session:[{csrfState:[],refreshCookie:[]}],bearer:[{productionBearer:[]}]};
class ContractError extends Error { constructor(category) { super(category); this.category=category; } }
function demand(ok, category) { if (!ok) throw new ContractError(category); }
const canonical=v=>Array.isArray(v)?v.map(canonical):v&&typeof v==='object'?Object.fromEntries(Object.keys(v).sort().map(k=>[k,canonical(v[k])])):v;
const equal=(a,b)=>JSON.stringify(canonical(a))===JSON.stringify(canonical(b));
function wireShape(v) { if(Array.isArray(v))return v.map(wireShape);if(!v||typeof v!=='object')return v;return Object.fromEntries(Object.entries(v).filter(([k])=>!k.startsWith('x-')&&!['description','example'].includes(k)).map(([k,x])=>[k,wireShape(x)])); }
function walk(v, fn) { if (!v || typeof v!=='object') return; fn(v); Object.values(v).forEach(x=>walk(x,fn)); }
function inspect(d) {
  demand(d.openapi==='3.1.0' && equal(d.servers.map(x=>x.url),['/api/v1']), 'VERSION_PREFIX');
  const operations=[];
  for (const [route,item] of Object.entries(d.paths ?? {})) for (const [method,o] of Object.entries(item)) {
    demand(['get','post','put'].includes(method), 'INVENTORY');
    operations.push([route,method,o.operationId]);
  }
  demand(operations.length===EXPECTED.length,'INVENTORY');
  for (const [id,method,route,req,res,status,auth,key,etag,errors] of EXPECTED) {
    const o=d.paths?.[route]?.[method];
    demand(o?.operationId===id,'INVENTORY');
    demand(equal(o.security,SECS[auth]),'SECURITY');
    const headers=o.parameters?.filter(x=>x.in==='header'&&x.required).map(x=>x.name).sort();
    const wanted=[...(key?['Idempotency-Key']:[]),...(etag?['If-Match']:[]),...(['preauth','session'].includes(auth)?['Origin','X-CSRF-Token']:[])].sort();
    demand(equal(headers,wanted),'HEADERS');
    for (const name of wanted) {
      const schema=o.parameters.find(x=>x.in==='header'&&x.name===name).schema;
      const expected=name==='If-Match'?{type:'string',pattern:'^"v[1-9][0-9]*"$'}:name==='Idempotency-Key'?{type:'string',format:'uuid'}:name==='Origin'?{type:'string',format:'uri'}:{type:'string',minLength:32,maxLength:512};
      demand(equal(schema,expected),'HEADER_SCHEMA');
    }
    if(route.includes('{accountId}')) demand(o.parameters.some(x=>x.in==='path'&&x.name==='accountId'&&x.required&&x.schema.format==='uuid'),'TARGET_PARAMETER');
    if(req) demand(o.requestBody?.required===true && o.requestBody.content?.['application/json']?.schema?.$ref==='#/components/schemas/'+req,'REQUEST_SCHEMA');
    else demand(!o.requestBody,'REQUEST_SCHEMA');
    const success=o.responses?.[status];
    demand(success,'STATUSES');
    if(res) demand(success.content?.['application/json']?.schema?.$ref==='#/components/schemas/'+res,'RESPONSE_SCHEMA');
    else demand(!success.content,'RESPONSE_SCHEMA');
    const statuses=[String(status),...Object.keys(errors)].sort();
    demand(equal(Object.keys(o.responses).sort(),statuses),'STATUSES');
    for(const r of Object.values(o.responses)) {
      demand(r.headers?.['Cache-Control']?.required && r.headers['Cache-Control'].schema?.const==='no-store' && r.headers?.['X-Request-Id']?.required,'RESPONSE_HEADERS');
    }
    if(res==='Account') demand(success.headers?.ETag?.required===true,'RESPONSE_HEADERS');
    if(status===201) demand(success.headers?.Location?.required===true,'RESPONSE_HEADERS');
    if(['getIdentityCsrf','loginIdentity','refreshIdentity','logoutIdentity'].includes(id)) demand(success.headers?.['Set-Cookie']?.required===true,'COOKIE_BINDING');
    for(const [s,codes] of Object.entries(errors)) {
      const errorRef='#/components/schemas/'+id+'Error'+s;
      demand(o.responses[s].content?.['application/json']?.schema?.$ref===errorRef,'ERROR_BINDING');
      const schema=d.components.schemas[id+'Error'+s];
      demand(schema?.allOf?.[0]?.$ref==='#/components/schemas/Error' && equal(schema.allOf[1]?.properties?.code?.enum,codes),'ERROR_BINDING');
      if(s==='429') demand(o.responses[s].headers?.['Retry-After']?.required===true,'RESPONSE_HEADERS');
    }
  }
  const p=d['x-identity-policy'];
  demand(p && p.accessSeconds===900 && p.absoluteSeconds===28800 && p.idleSeconds===1800 && p.invitationSeconds===86400 && p.recoverySeconds===900,'LIFETIMES');
  demand(p.refreshReplay==='REVOKE_FAMILY_NO_GRACE' && equal(p.idleExcluded,['refresh','csrf','health','auth/me']),'REFRESH_IDLE');
  demand(equal(p.jwtClaims?.required,CLAIMS),'JWT_CLAIMS');
  demand(equal(Object.keys(p.jwtClaims.properties).sort(),[...CLAIMS].sort()) && p.jwtClaims.additionalProperties===false,'JWT_CLAIMS');
  const claimTypes=Object.fromEntries(CLAIMS.map(k=>[k,['iat','nbf','exp','authVersion'].includes(k)?'integer':'string']));
  for(const [name,type] of Object.entries(claimTypes)) demand(p.jwtClaims.properties[name]?.type===type,'JWT_CLAIMS');
  demand(equal(p.sessionRevocation,['logout','disable','password-reset','grant-change']),'REVOCATION');
  demand(equal(p.password,{newMinimumCodePoints:15,maxUtf8Bytes:72,transform:'NONE'}) && p.usernameCanonicalization==='IDENTITY_EXACT_NO_TRANSFORM','CREDENTIAL_POLICY');
  demand(p.selfDisable===false && p.selfGrantChange===false && p.lastActiveIdentityAdminProtected===true,'ADMIN_POLICY');
  for(const [name,expected] of Object.entries(POLICY_STRUCTURES)) demand(equal(p[name],expected),'POLICY_'+name.toUpperCase());
  for(const [name,expected] of Object.entries(SHAPES)) demand(equal(wireShape(d.components.schemas[name]),expected),'SCHEMA_SHAPE');
  const logoutCookie=d.paths['/auth/logout'].post.parameters.find(x=>x.in==='cookie'&&x.name==='__Host-elogistics.refresh');
  demand(logoutCookie && logoutCookie.required===false && equal(logoutCookie.schema,{type:'string',minLength:32,maxLength:512}),'LOGOUT_OPTIONAL_COOKIE');
  const ss=d.components?.securitySchemes;
  demand(ss?.productionBearer?.type==='http'&&ss.productionBearer.scheme==='bearer'&&ss.csrfState?.in==='cookie'&&ss.csrfState.name==='__Host-elogistics.csrf'&&ss.refreshCookie?.in==='cookie'&&ss.refreshCookie.name==='__Host-elogistics.refresh','COOKIE_BINDING');
  demand(d.components.schemas.NewPassword.minLength===15 && d.components.schemas.NewPassword['x-maxUtf8Bytes']===72 && d.components.schemas.LoginPassword.minLength===1 && d.components.schemas.LoginPassword['x-maxUtf8Bytes']===72,'CREDENTIAL_POLICY');
  demand(d.components.schemas.Account.additionalProperties===false && !('passwordHash' in d.components.schemas.Account.properties) && !('refreshToken' in d.components.schemas.AccessSession.properties),'SECRET_SHAPE');
  walk(d, n=>{if(n.$ref) {
    demand(n.$ref.startsWith('#/'),'LOCAL_REFS');
    let target=d; for(const token of n.$ref.slice(2).split('/')) target=target?.[token.replace(/~1/g,'/').replace(/~0/g,'~')];
    demand(target!==undefined,'LOCAL_REFS');
  }});
  return true;
}
let passed=0,failed=0,mutations=0;
function test(id, fn) { try { fn();passed++; } catch(e) { failed++; console.error('FAIL '+id+': '+(e instanceof ContractError?e.category:e.message)); } }
test('contract.inventory.security.headers.status.policy.refs',()=>inspect(doc));
const ajv=new Ajv({strict:true,allErrors:true,coerceTypes:false,useDefaults:false,removeAdditional:false});
addFormats(ajv);
function schemaOnly(value) {
  if(Array.isArray(value))return value.map(schemaOnly);
  if(!value||typeof value!=='object')return value;
  return Object.fromEntries(Object.entries(value).filter(([k])=>!k.startsWith('x-')&&k!=='example').map(([k,v])=>[k,k==='$ref'?v.replace('#/components/schemas/','#/$defs/'):schemaOnly(v)]));
}
ajv.addSchema({$id:'https://identity.example.invalid/schemas',$defs:schemaOnly(doc.components.schemas)});
const validators={};
for(const name of Object.keys(doc.components.schemas)) test('compile.'+name,()=>{validators[name]=ajv.compile({$ref:'https://identity.example.invalid/schemas#/$defs/'+name});});
function valid(name,value){const v=validators[name];assert(v,'Schema not compiled: '+name);assert(v(value),name+': '+JSON.stringify(v.errors));}
function invalid(name,value){const v=validators[name];assert(v,'Schema not compiled: '+name);assert.equal(v(value),false,'Invalid fixture accepted: '+name);}
for(const [name,s] of Object.entries(doc.components.schemas)) if(s.example!==undefined) test('example.'+name,()=>valid(name,s.example));
test('fixture.independent.inventory',()=>assert.deepEqual(fixtures.operations.map(x=>x.id).sort(),EXPECTED.map(x=>x[0]).sort()));
for(const f of fixtures.operations){
 const r=EXPECTED.find(x=>x[0]===f.id); if(!r){test('unknown.fixture',()=>assert.fail('Unknown fixture'));continue;}
 const [id,method,route,req,res,status]=r;
 test('wire-position.'+id,()=>{assert.equal(f.method,method);assert.equal(f.path,route);assert.equal(f.status,status);});
 if(req){
  test('request.'+id,()=>valid(req,f.request));
  test('negative.request-extra.'+id,()=>invalid(req,{...f.request,forgedRole:'ADMIN'}));
 }
 if(res){
  test('response.'+id,()=>valid(res,f.response));
  test('negative.response-secret.'+id,()=>invalid(res,{...f.response,refreshToken:'SYNTHETIC_NOT_A_CREDENTIAL'}));
 }else test('empty.'+id,()=>assert.equal(f.response,null));
 const op=doc.paths[route][method];
 for(const p of op.parameters.filter(x=>x.in==='header'&&x.required)) {
   test('request-header.'+id+'.'+p.name,()=>assert(ajv.compile(schemaOnly(p.schema))(f.requestHeaders[p.name])));
   test('negative.request-header-missing.'+id+'.'+p.name,()=>assert.equal(ajv.compile(schemaOnly(p.schema))(undefined),false));
 }
 for(const [h,v]of Object.entries(op.responses[status].headers)) test('response-header.'+id+'.'+h,()=>assert(ajv.compile(schemaOnly(v.schema))(f.responseHeaders[h])));
 for(const [s,codes]of Object.entries(r[9])) {
  const name=id+'Error'+s;
  for(const code of codes) test('error.'+id+'.'+s+'.'+code,()=>valid(name,op.responses[s].content['application/json'].examples[code].value));
  test('negative.error-binding.'+id+'.'+s,()=>invalid(name,{code:'UNRELATED_ERROR',message:'Denied',requestId:'10000000-0000-4000-8000-000000000001'}));
 }
}
function credentialAccept(value,kind) { return typeof value==='string'&&value.isWellFormed()&&Array.from(value).length>=(kind==='new'?15:1)&&Buffer.byteLength(value,'utf8')<=72; }
for(const f of fixtures.credentialCases)test('credential.policy.'+f.id,()=>{const v=f.codeUnits?String.fromCharCode(...f.codeUnits):f.value;assert.equal(credentialAccept(v,f.kind),f.valid);});
test('credential.schema-byte-gap.explicit',()=>{assert(validators.NewPassword('€'.repeat(25)));assert.equal(credentialAccept('€'.repeat(25),'new'),false);});
const jwtValidator=ajv.compile(schemaOnly(doc['x-identity-policy'].jwtClaims));
const jwtFixture={iss:'https://identity.example.invalid',aud:'identity-production',sub:'10000000-0000-4000-8000-000000000002',jti:'10000000-0000-4000-8000-000000000003',sid:'10000000-0000-4000-8000-000000000004',iat:100,nbf:100,exp:1000,authVersion:1};
test('jwt.claims.schema-only',()=>assert(jwtValidator(jwtFixture)));
for(const c of CLAIMS)test('negative.jwt.missing.'+c,()=>{const v={...jwtFixture};delete v[c];assert.equal(jwtValidator(v),false);});
for(const f of fixtures.operations.filter(x=>['provisionIdentityAccount','disableIdentityAccount','reactivateIdentityAccount'].includes(x.id)))test('fixture.lifecycle-shape.'+f.id,()=>assert.equal(f.response.status,f.id==='provisionIdentityAccount'?'PENDING_ACTIVATION':f.id==='disableIdentityAccount'?'DISABLED':'ACTIVE'));
const requiredExpectations=['csrf.binding','refresh.concurrent','refresh.lost-response','session.revoked','idle.polling','grant.scope-union','account.target-wide','last-admin.concurrent','self.disable','self.grants','reactivate.old-session','reactivate.pending','invitation.consumption','invitation.resend','recovery.enumeration','reset.sessions','version.replay','version.stale','owner.authority','audit.transaction','delivery.boundary'];
test('declared-behavior.coverage-only',()=>assert.deepEqual(fixtures.expectations.map(x=>x.id).sort(),[...requiredExpectations].sort()));
for(const f of fixtures.expectations)test('declaration-only.'+f.id,()=>{assert(EXPECTED.some(x=>x[0]===f.operation));assert.equal(f.verification,'DECLARATIVE_NOT_RUNTIME_EXECUTED');assert(f.expected.length>20);});
const mutation=(id,category,change)=>{test('document-mutant.'+id,()=>{const d=structuredClone(doc);change(d);assert.throws(()=>inspect(d),e=>e instanceof ContractError&&e.category===category);});mutations++;};
mutation('missing-operation','INVENTORY',d=>delete d.paths['/auth/me'].get);
mutation('wrong-method','INVENTORY',d=>{d.paths['/auth/me'].post=d.paths['/auth/me'].get;delete d.paths['/auth/me'].get;});
mutation('wrong-route','INVENTORY',d=>{d.paths['/auth/whoami']=d.paths['/auth/me'];delete d.paths['/auth/me'];});
mutation('wrong-operationId','INVENTORY',d=>d.paths['/auth/me'].get.operationId='other');
mutation('missing-security','SECURITY',d=>delete d.paths['/auth/me'].get.security);
mutation('cookie-or-not-and','SECURITY',d=>d.paths['/auth/refresh'].post.security=[{csrfState:[]},{refreshCookie:[]}]);
mutation('missing-csrf-header','HEADERS',d=>d.paths['/auth/login'].post.parameters=d.paths['/auth/login'].post.parameters.filter(x=>x.name!=='X-CSRF-Token'));
mutation('missing-origin','HEADERS',d=>d.paths['/auth/login'].post.parameters=d.paths['/auth/login'].post.parameters.filter(x=>x.name!=='Origin'));
mutation('missing-key','HEADERS',d=>d.paths['/admin/users'].post.parameters=d.paths['/admin/users'].post.parameters.filter(x=>x.name!=='Idempotency-Key'));
mutation('missing-if-match','HEADERS',d=>d.paths['/admin/users/{accountId}/grants'].put.parameters=d.paths['/admin/users/{accountId}/grants'].put.parameters.filter(x=>x.name!=='If-Match'));
mutation('missing-required-body','REQUEST_SCHEMA',d=>d.paths['/auth/login'].post.requestBody.required=false);
mutation('wrong-response','RESPONSE_SCHEMA',d=>d.paths['/auth/me'].get.responses[200].content['application/json'].schema.$ref='#/components/schemas/Error');
mutation('missing-status428','STATUSES',d=>delete d.paths['/admin/users/{accountId}/grants'].put.responses[428]);
mutation('generic-error','ERROR_BINDING',d=>d.components.schemas.loginIdentityError401.allOf[1].properties.code.enum=['SESSION_INVALID']);
mutation('missing-etag','RESPONSE_HEADERS',d=>delete d.paths['/auth/me'].get.responses[200].headers.ETag);
mutation('missing-cookie-response','COOKIE_BINDING',d=>delete d.paths['/auth/csrf'].get.responses[200].headers['Set-Cookie']);
mutation('missing-jwt-claim','JWT_CLAIMS',d=>d['x-identity-policy'].jwtClaims.required.pop());
mutation('missing-policy','LIFETIMES',d=>delete d['x-identity-policy']);
mutation('refresh-grace','REFRESH_IDLE',d=>d['x-identity-policy'].refreshReplay='GRACE');
mutation('idle-polling','REFRESH_IDLE',d=>d['x-identity-policy'].idleExcluded.pop());
mutation('password-minimum','SCHEMA_SHAPE',d=>d.components.schemas.NewPassword.minLength=8);
mutation('self-disable','ADMIN_POLICY',d=>d['x-identity-policy'].selfDisable=true);
mutation('revocation-removed','REVOCATION',d=>d['x-identity-policy'].sessionRevocation.pop());
mutation('secret-response','SCHEMA_SHAPE',d=>d.components.schemas.Account.properties.passwordHash={type:'string'});
mutation('bad-ref','LOCAL_REFS',d=>d['x-extra-ref']={$ref:'#/components/schemas/MISSING'});
mutation('target-any-match','POLICY_AUTHORIZATION',d=>d['x-identity-policy'].authorization.targetRead='ANY_MATCH');
mutation('remove-union-authority','POLICY_AUTHORIZATION',d=>delete d['x-identity-policy'].authorization.fullReplacement);
mutation('client-only-csrf','POLICY_CSRF',d=>d['x-identity-policy'].csrf.binding='COOKIE_EQUALS_HEADER');
mutation('csrf-get-rotation','POLICY_CSRF',d=>d['x-identity-policy'].csrf.challengeGetRotatesRefresh=true);
mutation('cookie-not-httpOnly','POLICY_CSRF',d=>d['x-identity-policy'].csrf.refreshCookie.httpOnly=false);
mutation('reactivate-pending','POLICY_REACTIVATION',d=>d['x-identity-policy'].reactivation.disabledPendingEligible=true);
mutation('stale409','POLICY_PRECONDITIONS',d=>d['x-identity-policy'].preconditions.staleNonReplay=409);
mutation('optional-grants','SCHEMA_SHAPE',d=>d.components.schemas.ProvisionRequest.required.pop());
mutation('unknown-account-status','SCHEMA_SHAPE',d=>d.components.schemas.Account.properties.status.enum.push('UNKNOWN'));
mutation('forgot-password-fields','SCHEMA_SHAPE',d=>delete d.components.schemas.RecoveryConfirmRequest.properties.token);
mutation('weak-if-match','HEADER_SCHEMA',d=>d.paths['/admin/users/{accountId}/grants'].put.parameters.find(x=>x.name==='If-Match').schema.pattern='.*');
mutation('logout-refresh-required','SECURITY',d=>d.paths['/auth/logout'].post.security=[{csrfState:[],refreshCookie:[]}]);
mutation('logout-optional-param-deleted','LOGOUT_OPTIONAL_COOKIE',d=>d.paths['/auth/logout'].post.parameters=d.paths['/auth/logout'].post.parameters.filter(x=>x.in!=='cookie'));
mutation('logout-csrf-disabled','POLICY_LOGOUT',d=>d['x-identity-policy'].logout.csrfRequired=false);
mutation('recovery-existence-scoped','POLICY_RECOVERY',d=>d['x-identity-policy'].recovery.receiptScope='ACCOUNT_ID_IF_EXISTS');
mutation('recovery-arbitrary-email','POLICY_RECOVERY',d=>d['x-identity-policy'].recovery.deliveryAddress='REQUEST_EMAIL');
mutation('recovery-receipt-only-existing','POLICY_RECOVERY',d=>d['x-identity-policy'].recovery.receiptForEligibleAndIneligible=false);
mutation('recovery-plaintext-receipt','POLICY_RECOVERY',d=>d['x-identity-policy'].recovery.receiptStoresPlaintextSecrets=true);
test('logout.case-declarations-covered',()=>assert.deepEqual(fixtures.logoutCases.map(x=>x.id).sort(),['initial-family','missing-csrf','repeat-after-clear','untrusted-origin'].sort()));
for(const f of fixtures.logoutCases)test('logout.declaration-only.'+f.id,()=>{assert.equal(f.classification,'DECLARATIVE_NOT_RUNTIME_EXECUTED');assert.equal(f.expectedStatus,f.originValid&&f.csrfValid?204:403);});
test('document-positive.nonpolicy-description',()=>{const d=structuredClone(doc);d.info.description+=' Documentation clarification.';assert.equal(inspect(d),true);});
console.log(JSON.stringify({classification:'CONTRACT_ONLY_NOT_RUNTIME',passed,failed,skipped:0,documentMutationControls:mutations,operations:EXPECTED.length,schemas:Object.keys(validators).length,behavioralDeclarations:fixtures.expectations.length}));
process.exitCode=failed?1:0;
