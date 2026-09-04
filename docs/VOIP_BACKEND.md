# VoIP backend integration

The Android UI exposes an Internet call option through `VoipCallClient`, but it stays disabled until the existing backend contract is supplied. A real WebRTC implementation needs:

- An authenticated signaling URL and token acquisition flow.
- Call create, offer, answer, ICE-candidate, reject, cancel and hang-up payloads.
- Stable user/device identifiers that can be resolved from the chat address.
- STUN and authenticated TURN server URLs and credentials.
- Push notification payloads for incoming calls while the app is backgrounded.
- Call timeout, reconnect and error semantics.

Once these are available, `VoipCallClient.startCall()` is the integration boundary. The removed self-managed Telecom prototype must not be restored without a working media and signaling path.
