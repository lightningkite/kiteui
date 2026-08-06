// Karma-Mocha's default per-test timeout is 2000ms, shorter than
// SoundEffectPoolVolumeTest's own 5s polling budget for onended to fire. Give Mocha enough
// room for tests with real async waits (audio decode/playback under headless Chrome) to run
// to completion instead of being killed by the framework before their own assertion budget
// is exhausted.
config.client = config.client || {};
config.client.mocha = { timeout: 10000 };
