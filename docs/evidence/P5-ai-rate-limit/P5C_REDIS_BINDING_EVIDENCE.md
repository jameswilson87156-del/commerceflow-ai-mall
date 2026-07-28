# P5C Redis Binding Evidence

`docker-compose.yml` now publishes only the P5 Redis port as:

```yaml
127.0.0.1:${REDIS_PORT:-6380}:6379
```

Actual Compose inspection reported `127.0.0.1:6380` for Redis. The MySQL mapping remained unchanged at `0.0.0.0:3307` because P5C only hardens the newly added Redis service.

Redis 8.0.2 remained healthy, Java tests connected on the local port, and the GitHub Actions Redis service remains its isolated CI service at port 6379. No Redis password was added for the local Showcase; this compose configuration must not be reused as a production network exposure.
