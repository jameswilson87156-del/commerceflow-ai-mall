# P6B Idempotency Evidence

The confirm page creates one client idempotency key per pending submit and keeps the same key when a retry is needed. The key includes a random component and is sent in the `Idempotency-Key` header.

The existing Java order implementation remains the authority for:

- same key and same request returning the original order;
- same key and different request body returning `409`;
- no duplicate stock deduction or order write on replay;
- inventory shortage and transaction rollback.

P6B does not reinterpret a replay or conflict as a new mobile order. The mobile UI reports the existing error boundary and does not fabricate a success order.

The successful browser run created `CF1785264511123` with status `CREATED`. The existing Java order tests cover replay, conflict, shortage, rollback, atomic stock deduction and movement evidence.
