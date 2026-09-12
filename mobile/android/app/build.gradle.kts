import java.util.Properties
import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
}

val privyLocalProperties = Properties().apply {
    val localFile = rootProject.file("privy.local.properties")
    if (localFile.isFile) {
        localFile.inputStream().use(::load)
    }
}

val issuerSpaceConfig = JsonSlurper().parse(rootProject.file("../../config/issuer-space.json")) as Map<*, *>

fun issuerConfigString(name: String): String =
    "\"${(issuerSpaceConfig[name] ?: error("Missing issuer config: $name")).toString().replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun issuerRoleString(name: String): String =
    "\"${((issuerSpaceConfig["roles"] as? Map<*, *>)?.get(name) ?: error("Missing issuer role: $name"))}\""

fun localBuildConfigString(name: String): String {
    val value = privyLocalProperties.getProperty(name, "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$value\""
}

android {
    namespace = "io.github.nodexbit.ethonline2026"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "io.github.nodexbit.ethonline2026"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "PRIVY_APP_ID", localBuildConfigString("PRIVY_APP_ID"))
        buildConfigField("String", "PRIVY_APP_CLIENT_ID", localBuildConfigString("PRIVY_APP_CLIENT_ID"))
        buildConfigField("long", "ISSUER_CHAIN_ID", "${issuerSpaceConfig["chainId"]}L")
        buildConfigField("String", "ISSUER_NAMESPACE", issuerConfigString("namespace"))
        buildConfigField("String", "ISSUER_ADDRESS", issuerConfigString("issuerAddress"))
        buildConfigField("String", "ETH_REGISTRY", issuerConfigString("ethRegistry"))
        buildConfigField("String", "PARENT_REGISTRY", issuerConfigString("parentRegistry"))
        buildConfigField("String", "ISSUER_REGISTRY", issuerConfigString("issuerRegistry"))
        buildConfigField("String", "ISSUER_RESOLVER", issuerConfigString("issuerResolver"))
        buildConfigField("String", "VERIFIABLE_FACTORY", issuerConfigString("verifiableFactory"))
        buildConfigField("String", "USER_REGISTRY_IMPLEMENTATION", issuerConfigString("userRegistryImplementation"))
        buildConfigField("String", "PERMISSIONED_RESOLVER_IMPLEMENTATION", issuerConfigString("permissionedResolverImplementation"))
        buildConfigField("long", "ISSUER_NAMESPACE_EXPIRY", "${issuerSpaceConfig["namespaceExpiry"]}L")
        buildConfigField("String", "ISSUER_REGISTRY_ROOT_ROLES", issuerRoleString("issuerRegistryRoot"))
        buildConfigField("String", "ISSUER_RESOLVER_ROOT_ROLES", issuerRoleString("issuerResolverRoot"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/DISCLAIMER",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/io.netty.versions.properties",
            )
        }
    }
}

dependencies {
    implementation("io.privy:privy-core:0.14.0")
    // The 4.12.3-android AAR is published without its ABI/support-module metadata.
    // Keep the single pinned core dependency, but exclude every unused network/KMS stack.
    implementation("org.web3j:core:4.12.3") {
        exclude(group = "org.web3j", module = "crypto")
        exclude(group = "org.web3j", module = "tuples")
        exclude(group = "com.github.jnr")
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "io.reactivex.rxjava2")
        exclude(group = "org.java-websocket")
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "org.slf4j")
        exclude(group = "io.github.adraffy")
        exclude(group = "io.tmio")
        exclude(group = "software.amazon.awssdk")
    }
    testImplementation("junit:junit:4.13.2")
}
