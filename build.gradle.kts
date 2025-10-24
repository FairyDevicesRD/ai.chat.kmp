plugins {
    // Gradleプラグイン
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.build.konfig) apply false
    alias(libs.plugins.ktorfit.plugin) apply false
    alias(libs.plugins.spotless) apply true
    alias(libs.plugins.metro) apply false
}

spotless {
    kotlin {
        target("**/*.kt")
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
