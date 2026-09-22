package com.hjinlabs.sengkode.feature.generator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentType
import com.hjinlabs.sengkode.core.model.WifiEncryption
import com.hjinlabs.sengkode.feature.generator.R

/**
 * Type picker chip row. Selecting a type calls [onSelected] with the
 * new type; the caller is responsible for resetting content.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TypePicker(
    selected: QrContentType,
    onSelected: (QrContentType) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QrContentType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelected(type) },
                label = { Text(typeLabel(type)) },
            )
        }
    }
}

@Composable
private fun typeLabel(type: QrContentType): String =
    androidx.compose.ui.res.stringResource(
        when (type) {
            QrContentType.TEXT -> R.string.type_text
            QrContentType.URL -> R.string.type_url
            QrContentType.WIFI -> R.string.type_wifi
            QrContentType.VCARD -> R.string.type_vcard
            QrContentType.EMAIL -> R.string.type_email
            QrContentType.SMS -> R.string.type_sms
            QrContentType.PHONE -> R.string.type_phone
            QrContentType.GEO -> R.string.type_geo
        },
    )

/**
 * Returns the default [QrContent] instance for a given [QrContentType].
 * Used when the user switches type to reset the content fields.
 */
internal fun defaultContentFor(type: QrContentType): QrContent =
    when (type) {
        QrContentType.TEXT -> QrContent.Text("")
        QrContentType.URL -> QrContent.Url("")
        QrContentType.WIFI -> QrContent.Wifi("", "", WifiEncryption.WPA, false)
        QrContentType.VCARD -> QrContent.VCard("", null, null, null, null, null)
        QrContentType.EMAIL -> QrContent.Email("", null, null)
        QrContentType.SMS -> QrContent.Sms("", null)
        QrContentType.PHONE -> QrContent.Phone("")
        QrContentType.GEO -> QrContent.Geo(1.3521, 103.8198)
    }

/**
 * Dispatches to the correct per-type editor composable.
 * No business logic — purely structural.
 */
@Composable
internal fun ContentEditor(
    content: QrContent,
    onContentChange: (QrContent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (content) {
            is QrContent.Text -> {
                Field(stringResource(R.string.field_text), content.text, true) {
                    onContentChange(QrContent.Text(it))
                }
            }
            is QrContent.Url -> {
                Field(stringResource(R.string.field_url), content.url, true) {
                    onContentChange(QrContent.Url(it))
                }
            }
            is QrContent.Wifi -> WifiEditor(content, onContentChange)
            is QrContent.VCard -> VCardEditor(content, onContentChange)
            is QrContent.Email -> EmailEditor(content, onContentChange)
            is QrContent.Sms -> SmsEditor(content, onContentChange)
            is QrContent.Phone -> {
                Field(stringResource(R.string.field_phone), content.number, true) {
                    onContentChange(QrContent.Phone(it))
                }
            }
            is QrContent.Geo -> GeoEditor(content, onContentChange)
        }
    }
}

@Composable
private fun WifiEditor(content: QrContent.Wifi, onContentChange: (QrContent) -> Unit) {
    Field(stringResource(R.string.field_wifi_ssid), content.ssid, true) {
        onContentChange(content.copy(ssid = it))
    }
    EncryptionPicker(content.encryption) { encryption ->
        onContentChange(
            content.copy(
                encryption = encryption,
                password = if (encryption == WifiEncryption.NOPASS) "" else content.password,
            ),
        )
    }
    if (content.encryption != WifiEncryption.NOPASS) {
        Field(stringResource(R.string.field_wifi_password), content.password, true) {
            onContentChange(content.copy(password = it))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = content.hidden,
            onClick = { onContentChange(content.copy(hidden = !content.hidden)) },
            label = { Text(stringResource(R.string.field_wifi_hidden)) },
        )
    }
}

@Composable
private fun EncryptionPicker(selected: WifiEncryption, onSelected: (WifiEncryption) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            WifiEncryption.WPA to R.string.encryption_wpa,
            WifiEncryption.WEP to R.string.encryption_wep,
            WifiEncryption.NOPASS to R.string.encryption_nopass,
        ).forEach { (encryption, labelRes) ->
            FilterChip(
                selected = encryption == selected,
                onClick = { onSelected(encryption) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}

@Composable
private fun VCardEditor(content: QrContent.VCard, onContentChange: (QrContent) -> Unit) {
    Field(stringResource(R.string.field_name), content.fullName, true) {
        onContentChange(content.copy(fullName = it))
    }
    Field(stringResource(R.string.field_organization), content.organization.orEmpty()) {
        onContentChange(content.copy(organization = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_phone), content.phone.orEmpty()) {
        onContentChange(content.copy(phone = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_email), content.email.orEmpty()) {
        onContentChange(content.copy(email = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_website), content.website.orEmpty()) {
        onContentChange(content.copy(website = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_note), content.note.orEmpty()) {
        onContentChange(content.copy(note = it.ifBlank { null }))
    }
}

@Composable
private fun EmailEditor(content: QrContent.Email, onContentChange: (QrContent) -> Unit) {
    Field(stringResource(R.string.field_email), content.address, true) {
        onContentChange(content.copy(address = it))
    }
    Field(stringResource(R.string.field_subject), content.subject.orEmpty()) {
        onContentChange(content.copy(subject = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_body), content.body.orEmpty()) {
        onContentChange(content.copy(body = it.ifBlank { null }))
    }
}

@Composable
private fun SmsEditor(content: QrContent.Sms, onContentChange: (QrContent) -> Unit) {
    Field(stringResource(R.string.field_sms_number), content.number, true) {
        onContentChange(content.copy(number = it))
    }
    Field(stringResource(R.string.field_sms_message), content.message.orEmpty()) {
        onContentChange(content.copy(message = it.ifBlank { null }))
    }
}

@Composable
private fun GeoEditor(content: QrContent.Geo, onContentChange: (QrContent) -> Unit) {
    var latText by remember { mutableStateOf(content.latitude.toString()) }
    var lonText by remember { mutableStateOf(content.longitude.toString()) }
    Field(
        label = stringResource(R.string.field_latitude),
        value = latText,
        singleLine = true,
        onValueChange = { latText = it },
    )
    Field(
        label = stringResource(R.string.field_longitude),
        value = lonText,
        singleLine = true,
        onValueChange = { lonText = it },
    )
    LaunchedEffect(latText, lonText) {
        val lat = latText.toDoubleOrNull()
        val lon = lonText.toDoubleOrNull()
        if (lat != null && lon != null) {
            onContentChange(QrContent.Geo(lat, lon))
        }
    }
}

@Composable
internal fun Field(
    label: String,
    value: String,
    singleLine: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}
