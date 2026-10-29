# Releasing MealForge

Pushing a tag like `v0.5.0` runs `.github/workflows/release.yml`, which tests, builds a signed APK and Android App Bundle, verifies the signature, and publishes a GitHub Release with the matching `CHANGELOG.md` section. Versions below 1.0 are marked as pre-releases.

## One-time setup: the signing key

The signing key proves updates come from you. **If it is lost, existing installs can never be updated; if it leaks, someone else can publish "updates".** Keep it out of the repository and back it up offline (for example in your password manager).

1. Create the key (needs a JDK; Android Studio includes one at `/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool`, or run `brew install openjdk@17`):

   ```bash
   keytool -genkeypair -v -keystore ~/mealforge-release.jks -storetype PKCS12 -alias mealforge -keyalg RSA -keysize 4096 -validity 10000
   ```

   With PKCS12 the key password is the same as the store password.

2. Add the four repository secrets (each `gh secret set` without `-b` prompts for the value, so it never lands in shell history):

   ```bash
   base64 -i ~/mealforge-release.jks | gh secret set MEALFORGE_KEYSTORE_BASE64 -R BaconDisk22/MealForge
   gh secret set MEALFORGE_KEYSTORE_PASSWORD -R BaconDisk22/MealForge
   gh secret set MEALFORGE_KEY_PASSWORD -R BaconDisk22/MealForge
   gh secret set MEALFORGE_KEY_ALIAS -R BaconDisk22/MealForge -b mealforge
   ```

## Each release

1. In `app/build.gradle.kts`, increase `versionCode` by 1 and set `versionName` (e.g. `0.6.0`).
2. Add a `## 0.6.0 - YYYY-MM-DD` section at the top of `CHANGELOG.md`.
3. Merge those changes to `main` and wait for CI to pass.
4. Tag and push:

   ```bash
   git checkout main && git pull && git tag v0.6.0 && git push origin v0.6.0
   ```

The workflow fails without publishing anything if the tag doesn't match `versionName`, the changelog section is missing, or the signing secrets aren't set.
