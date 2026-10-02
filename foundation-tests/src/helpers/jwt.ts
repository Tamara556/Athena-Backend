import { createHmac } from 'node:crypto';
import { CONFIG } from '../config/env';

/**
 * Minimal HS384 JWT minter for DETERMINISTIC negative auth tests only.
 *
 * The backend's JwtService derives the HMAC key from the raw UTF-8 secret bytes
 * (HS384 for the 48-byte dev secret) and validates issuer + `type` claim. We use
 * this to produce expired / wrong-signature / wrong-type tokens without waiting
 * for the real 15-minute access TTL. It is not used to fabricate valid identities.
 */
interface MintOptions {
  subject?: string;
  roles?: string[];
  type?: 'ACCESS' | 'REFRESH';
  issuer?: string;
  /** Seconds until expiry; negative = already expired. */
  expiresInSeconds?: number;
  secret?: string;
}

const base64Url = (input: Buffer | string): string =>
  Buffer.from(input)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');

export function mintToken(options: MintOptions = {}): string {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: 'HS384', typ: 'JWT' };
  const payload = {
    sub: options.subject ?? '00000000-0000-0000-0000-000000000000',
    iss: options.issuer ?? CONFIG.jwtIssuer,
    type: options.type ?? 'ACCESS',
    roles: options.roles ?? ['USER'],
    iat: now,
    exp: now + (options.expiresInSeconds ?? 900),
  };

  const signingInput = `${base64Url(JSON.stringify(header))}.${base64Url(JSON.stringify(payload))}`;
  const signature = base64Url(
    createHmac('sha384', options.secret ?? CONFIG.jwtSecret).update(signingInput).digest(),
  );
  return `${signingInput}.${signature}`;
}

/** Correctly signed ACCESS token whose `exp` is already in the past. */
export const mintExpiredToken = (options: MintOptions = {}): string =>
  mintToken({ ...options, expiresInSeconds: -60 });

/** Structurally valid ACCESS token signed with the WRONG secret. */
export const mintWrongSignatureToken = (options: MintOptions = {}): string =>
  mintToken({ ...options, secret: 'wrong-secret-value-that-differs-0123456789abcdef0' });
