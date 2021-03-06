/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.Settings
import me.zhanghai.android.files.app.appClassLoader
import me.zhanghai.android.files.app.application
import me.zhanghai.android.files.app.packageManager
import me.zhanghai.android.files.file.MimeType
import me.zhanghai.android.files.file.intentType
import kotlin.reflect.KClass

fun <T : Activity> KClass<T>.createIntent(): Intent = Intent(application, java)

// TODO: Use androidx.core.app.ShareCompat?
fun CharSequence.createSendTextIntent(htmlText: String? = null): Intent =
    Intent()
        .setAction(Intent.ACTION_SEND)
        .setType(MimeType.TEXT_PLAIN.value)
        .putExtra(Intent.EXTRA_TEXT, this)
        .apply { htmlText?.let { putExtra(Intent.EXTRA_HTML_TEXT, it) } }

fun KClass<Intent>.createLaunchApp(packageName: String): Intent? =
    packageManager.getLaunchIntentForPackage(packageName)

fun KClass<Intent>.createPickImage(allowMultiple: Boolean = false): Intent =
    MimeType.IMAGE_ANY.createPickFileIntent(allowMultiple)

fun KClass<Intent>.createPickOrCaptureImageWithChooser(
    allowPickMultiple: Boolean = false,
    captureOutputUri: Uri
): Intent =
    createPickImage(allowPickMultiple)
        .withChooser(captureOutputUri.createCaptureImage())

fun KClass<Intent>.createSyncSettings(
    authorities: Array<out String>? = null,
    accountTypes: Array<out String>? = null
): Intent =
    Intent(Settings.ACTION_SYNC_SETTINGS).apply {
        if (!authorities.isNullOrEmpty()) {
            putExtra(Settings.EXTRA_AUTHORITIES, authorities)
        }
        if (!accountTypes.isNullOrEmpty()) {
            putExtra(Settings.EXTRA_ACCOUNT_TYPES, accountTypes)
        }
    }

fun KClass<Intent>.createSyncSettingsWithAuthorities(vararg authorities: String) =
    createSyncSettings(authorities = authorities)

fun KClass<Intent>.createSyncSettingsWithAccountType(vararg accountTypes: String) =
    createSyncSettings(accountTypes = accountTypes)

fun KClass<Intent>.createViewAppInMarket(packageName: String): Intent =
    Uri.parse("market://details?id=$packageName").createViewIntent()

fun <T : Parcelable> Intent.getParcelableExtraSafe(key: String): T? {
    setExtrasClassLoader(appClassLoader)
    return getParcelableExtra(key)
}

fun Intent.getParcelableArrayExtraSafe(key: String): Array<Parcelable>? {
    setExtrasClassLoader(appClassLoader)
    return getParcelableArrayExtra(key)
}

fun <T : Parcelable?> Intent.getParcelableArrayListExtraSafe(key: String): ArrayList<T>? {
    setExtrasClassLoader(appClassLoader)
    return getParcelableArrayListExtra(key)
}

fun Intent.withChooser(title: CharSequence? = null, vararg initialIntents: Intent): Intent =
    Intent.createChooser(this, title).apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents)
    }

fun Intent.withChooser(vararg initialIntents: Intent) = withChooser(null, *initialIntents)

fun MimeType.createPickFileIntent(allowMultiple: Boolean = false) =
    Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType(value)
        .apply {
            if (allowMultiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

fun Collection<MimeType>.createPickFileIntent(allowMultiple: Boolean = false): Intent =
    (singleOrNull() ?: MimeType.ANY).createPickFileIntent(allowMultiple)
        .apply {
            if (size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, map { it.value }.toTypedArray())
            }
        }

fun Uri.createSendImageIntent(text: CharSequence? = null): Intent =
    createSendStreamIntent(MimeType.IMAGE_ANY).apply {
        text?.let {
            // For maximum compatibility.
            putExtra(Intent.EXTRA_TEXT, it)
            putExtra(Intent.EXTRA_TITLE, it)
            putExtra(Intent.EXTRA_SUBJECT, it)
            // HACK: WeChat moments respects this extra only.
            putExtra("Kdescription", it)
        }
    }

fun Uri.createSendStreamIntent(mimeType: MimeType): Intent =
    Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_STREAM, this)
        .setType(mimeType.intentType)

fun Collection<Uri>.createSendStreamIntent(types: Collection<MimeType>): Intent =
    if (size == 1) {
        first().createSendStreamIntent(types.single())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType(types.intentType)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(this))
    }

fun Uri.createViewIntent(): Intent = Intent(Intent.ACTION_VIEW, this)

fun Uri.createViewIntent(mimeType: MimeType): Intent =
    Intent(Intent.ACTION_VIEW)
        // Calling setType() will clear data.
        .setDataAndType(this, mimeType.intentType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

@Suppress("DEPRECATION")
fun Uri.createInstallPackageIntent(): Intent =
    Intent(Intent.ACTION_INSTALL_PACKAGE)
        .setDataAndType(this, MimeType.APK.value)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

fun Uri.createCaptureImage(): Intent =
    Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        .putExtra(MediaStore.EXTRA_OUTPUT, this)
