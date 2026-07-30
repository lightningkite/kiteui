// Chrome's own sandbox cannot initialize inside restricted environments (CI containers,
// sandboxed shells), which kills the GPU process and prevents ChromeHeadless from starting.
// Launching with the sandbox disabled is the standard workaround for those environments.
config.customLaunchers = {
    ChromeHeadlessNoSandbox: {
        base: "ChromeHeadless",
        flags: ["--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage"],
    },
};
config.browsers = ["ChromeHeadlessNoSandbox"];
