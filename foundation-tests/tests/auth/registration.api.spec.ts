import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, uniqueUser } from '../../src/helpers/auth-helper';

/**
 * Registration contract (POST /auth/register, multipart/form-data).
 * Happy path, duplicates, field validation, boundaries, error shape.
 */
test.describe('Auth · Registration API', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('AUTH-REG-001 — valid registration returns 201 with tokens and USER role', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);

    expect(auth.userId).toBeTruthy();
    expect(auth.tokenType).toBe('Bearer');
    expect(auth.accessToken).toMatch(/^eyJ/);
    expect(auth.refreshToken).toMatch(/^eyJ/);
    expect(auth.expiresIn).toBeGreaterThan(0);
    expect(auth.roles).toEqual(['USER']);
    expect(auth.twoFactorRequired).toBe(false);
  });

  test('AUTH-REG-002 — duplicate email → 409 Conflict', async ({ gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);

    const res = await gatewayApi.post('/auth/register', {
      multipart: { ...uniqueUser(), email: user.email },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(409);
    const body = await res.json();
    expect(body.error).toBe('Conflict');
    expect(body.message).toBe('An account with this email already exists');
  });

  test('AUTH-REG-003 — duplicate username → 409 Conflict', async ({ gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);

    const res = await gatewayApi.post('/auth/register', {
      multipart: { ...uniqueUser(), username: user.username },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(409);
    const body = await res.json();
    expect(body.message).toBe('This username is already taken');
  });

  test('AUTH-REG-004 — invalid email → 400 with field violation', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/register', {
      multipart: { ...uniqueUser(), email: 'not-an-email' },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.message).toBe('Validation failed for one or more fields');
    expect(body.details).toEqual(
      expect.arrayContaining([expect.objectContaining({ field: 'email' })]),
    );
  });

  test('AUTH-REG-005 — weak password (no symbol) → 400 password violation', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/register', {
      multipart: { ...uniqueUser(), password: 'Password1' },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.details.map((d: { field: string }) => d.field)).toContain('password');
  });

  test('AUTH-REG-006 — empty required fields → 400 with multiple violations', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/register', {
      multipart: { firstName: '', lastName: '', username: '', email: '', password: '' },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(Array.isArray(body.details)).toBe(true);
    expect(body.details.length).toBeGreaterThan(1);
  });

  test('AUTH-REG-007 — username with illegal characters → 400', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/register', {
      multipart: { ...uniqueUser(), username: 'bad name!' },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.details.map((d: { field: string }) => d.field)).toContain('username');
  });

  test('AUTH-REG-008 — boundary: 8-char complex password is accepted; 7-char rejected', async ({ gatewayApi }) => {
    const accepted = await registerViaApi(gatewayApi, uniqueUser({ password: 'Aa1!aaaa' })); // exactly 8
    expect(accepted.auth.accessToken).toBeTruthy();

    const res = await gatewayApi.post('/auth/register', {
      multipart: { ...uniqueUser(), password: 'Aa1!aaa' }, // 7 chars
      failOnStatusCode: false,
    });
    expect(res.status()).toBe(400);
  });
});
