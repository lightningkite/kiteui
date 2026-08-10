// Chrome's own sandbox cannot initialize inside restricted environments (CI containers,
// sandboxed shells), which kills the GPU process and prevents ChromeHeadless from starting.
// Launching with the sandbox disabled is the standard workaround for those environments.
config.customLaunchers = {
    ChromeHeadlessNoSandbox: {
        base: "ChromeHeadless",
        // --autoplay-policy: without it Chrome keeps every AudioContext suspended until the page
        // has user activation, which a test has no way to produce. A suspended context's clock
        // never advances, so no sound ever ends and no `ended` event ever fires - audio behaviour
        // would be untestable rather than merely silent.
        flags: [
            "--no-sandbox",
            "--disable-gpu",
            "--disable-dev-shm-usage",
            "--autoplay-policy=no-user-gesture-required",
        ],
    },
};
config.browsers = ["ChromeHeadlessNoSandbox"];
