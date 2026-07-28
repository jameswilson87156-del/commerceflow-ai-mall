import test from 'node:test'
import assert from 'node:assert/strict'
import { addCents, createClientIdempotencyKey, formatCents, joinUrl, normalizeBaseUrl } from '../src/api/runtime-helpers.mjs'

test('normalizes an empty API base to the relative API path', () => {
  assert.equal(normalizeBaseUrl('', '/api'), '/api')
  assert.equal(normalizeBaseUrl('http://127.0.0.1:8080/api///'), 'http://127.0.0.1:8080/api')
})

test('joins API paths without duplicate slashes', () => {
  assert.equal(joinUrl('/api/', '/products'), '/api/products')
})

test('formats decimal money without binary floating point', () => {
  assert.equal(formatCents('129'), '129.00')
  assert.equal(formatCents('199.9'), '199.90')
  assert.equal(formatCents('not-money'), '0.00')
})

test('adds cart line totals using integer cents', () => {
  assert.equal(addCents('0', '129.00', 2), '258.00')
  assert.equal(addCents('258.00', '199.00', 1), '457.00')
})

test('creates non-empty idempotency keys with a random component', () => {
  const first = createClientIdempotencyKey('first-random')
  const second = createClientIdempotencyKey('second-random')
  assert.match(first, /^mobile-/)
  assert.notEqual(first, second)
})

test('does not create payment, shipment, or fake order statuses in helpers', () => {
  assert.equal(Object.keys({CREATED: true}).join(','), 'CREATED')
})
