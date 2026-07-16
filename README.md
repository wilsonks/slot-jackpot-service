# slot-jackpot-service

Progressive jackpot pool service for the `slot-central` microservices platform — owns progressive jackpot pool state, contribution/rollup logic, and win recording.

This service owns the jackpot pool's `currentAmount`, `baseAmount`, `incrementRate`, and win history. It is called by `slot-game-controller-service` (or `slot-game-engine-service`, via the controller) after a spin to contribute to the pool and to record wins, and in turn calls `slot-floor-management-service`'s jackpot-broadcast HTTP endpoints to relay pool events (win/reset/rollup) to physical EGMs — it does NOT talk to RabbitMQ or the machines directly.

This repository is part of a re-architecture of the `slot-central-server-express-rmq` Node.js EGM slot-floor backend into Spring Boot microservices.

Scaffolding in progress.
