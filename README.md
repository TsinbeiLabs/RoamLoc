# RoamLoc

RoamLoc is an LSPosed-based virtual location module that hooks Android system services to provide location simulation. The companion app provides controls for configuring and running the module.

The module uses the modern libxposed API 102 and requires an Xposed framework implementation with system-process support. Legacy `XposedBridge` module loading is not supported.

## Features

- Simulates location across supported providers.
- Supports GNSS and NMEA simulation.
- Simulates sensor information.
- Supports joystick and route-based movement.
- Configures speed, altitude, accuracy, and bearing.
- Creates a notification while location simulation is running.

## Thanks

- [GoGoGo](https://github.com/ZCShou/GoGoGo)
- [Baidu Map SDK](https://lbsyun.baidu.com/faq/api?title=androidsdk)

## Build configuration

RoamLoc does not store service credentials in the repository. Configure these values in the untracked root `local.properties` file for local builds:

```properties
BAIDU_API_KEY=your_baidu_lbs_api_key
BUGLY_APP_ID=your_bugly_app_id
```

GitHub Actions reads the same values from repository secrets named `BAIDU_API_KEY` and `BUGLY_APP_ID`. Create and restrict the Baidu key for the `com.tsinbei.roamloc` package and the certificate fingerprint used to sign the APK. Client-side Android API keys remain extractable from APKs, so provider-side package and signing-certificate restrictions are required even when GitHub Secrets are used.

## License

This project is a fork of [Portal](https://github.com/ella8192/Portal) and is licensed under the GNU General Public License v3.0 or later (GPL-3.0-or-later). See [LICENSE](LICENSE).

The original Apache License 2.0 is retained in [LICENSE.Apache-2.0](LICENSE.Apache-2.0).
