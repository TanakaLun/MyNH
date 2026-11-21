package io.tl.mynhentai.ui.account

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.AccountProfile
import io.tl.mynhentai.data.repository.SyncPhase
import io.tl.mynhentai.data.repository.SyncProgress
import io.tl.mynhentai.ui.components.AppTopBar
import io.tl.mynhentai.ui.components.AppDialog
import io.tl.mynhentai.ui.components.AppDialogButton
import io.tl.mynhentai.ui.components.AppIconButton
import io.tl.mynhentai.ui.components.rememberSurfaceBackdrop
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onOpenGallery: (Long) -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    val bindError by viewModel.bindError.collectAsState()
    val openGallery by viewModel.openGallery.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showKeyDialog by remember { mutableStateOf(false) }
    var showUnbindDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(TextFieldValue("")) }

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberSurfaceBackdrop()

    LaunchedEffect(event?.id) {
        event?.let { current ->
            val message = buildString {
                append(context.getString(current.messageRes))
                current.detail?.takeIf { it.isNotBlank() }?.let { append(": $it") }
            }
            snackbarHostState.showSnackbar(message)
            if (current.messageRes == R.string.account_bound_success && showKeyDialog) {
                showKeyDialog = false
                keyInput = TextFieldValue("")
                viewModel.clearBindError()
            }
            viewModel.dismissEvent()
        }
    }

    LaunchedEffect(openGallery) {
        openGallery?.let {
            viewModel.consumeOpenGallery()
            onOpenGallery(it)
        }
    }

    // Close the key sheet if the bound account disappears (unbind) as a safety net.
    val boundUserId = state.account?.profile?.id
    LaunchedEffect(boundUserId) {
        if (boundUserId == null && showKeyDialog && !state.binding) {
            keyInput = TextFieldValue("")
        }
    }

    if (showKeyDialog) {
        AppDialog(
            show = true,
            title = stringResource(R.string.account_key_title),
            backdrop = backdrop,
            onDismissRequest = {
                showKeyDialog = false
                viewModel.clearBindError()
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.account_key_hint),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                TextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        if (bindError != null) viewModel.clearBindError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.account_key_label),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
                val error = bindError
                if (error != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MiuixTheme.colorScheme.errorContainer)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(error.messageRes),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onErrorContainer
                        )
                        error.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                            Text(
                                text = detail,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppDialogButton(
                        text = stringResource(R.string.account_paste),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val text = clipboard?.primaryClip
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                                ?.trim()
                            if (!text.isNullOrBlank()) keyInput = TextFieldValue(text)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AppDialogButton(
                        text = if (state.binding) {
                            stringResource(R.string.account_binding)
                        } else {
                            stringResource(R.string.account_verify_bind)
                        },
                        onClick = { viewModel.bind(keyInput.text) },
                        enabled = keyInput.text.isNotBlank() && !state.binding,
                        primary = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = stringResource(R.string.account_key_privacy),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                AppDialogButton(
                    text = stringResource(R.string.account_get_key),
                    onClick = { openApiKeyPage(context) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showUnbindDialog) {
        AppDialog(
            show = true,
            title = stringResource(R.string.account_unbind),
            backdrop = backdrop,
            onDismissRequest = { showUnbindDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.account_unbind_confirm),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                AppDialogButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showUnbindDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
                AppDialogButton(
                    text = stringResource(R.string.account_unbind),
                    onClick = {
                        showUnbindDialog = false
                        viewModel.unbind()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    destructive = true
                )
            }
        }
    }

    if (state.mergePrompt) {
        AppDialog(
            show = true,
            title = stringResource(R.string.account_merge_title),
            backdrop = backdrop,
            onDismissRequest = { viewModel.dismissMergePrompt() }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.account_merge_message, state.localFavorites),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                AppDialogButton(
                    text = stringResource(R.string.account_merge_upload),
                    onClick = { viewModel.confirmMerge(true) },
                    primary = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AppDialogButton(
                    text = stringResource(R.string.account_merge_pull),
                    onClick = { viewModel.confirmMerge(false) },
                    modifier = Modifier.fillMaxWidth()
                )
                AppDialogButton(
                    text = stringResource(R.string.account_merge_later),
                    onClick = { viewModel.dismissMergePrompt() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.account),
                backdrop = backdrop,
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    AppIconButton(
                        onClick = onBack,
                        icon = MiuixIcons.Back,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null) Modifier.layerBackdrop(backdrop)
                    else Modifier
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .overscroll(rememberOverscrollEffect())
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .scrollEndHaptic(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            ) {
                val account = state.account
                if (account == null) {
                    item {
                        UnboundSection(
                            onBind = {
                                viewModel.clearBindError()
                                showKeyDialog = true
                            },
                            onOpenApiKeyPage = { openApiKeyPage(context) }
                        )
                    }
                } else {
                    if (account.authInvalid) {
                        item {
                            Column {
                                SmallTitle(text = stringResource(R.string.account))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.account_invalid_key),
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.error
                                        )
                                        TextButton(
                                            text = stringResource(R.string.account_rebind),
                                            onClick = {
                                                viewModel.clearBindError()
                                                showKeyDialog = true
                                            },
                                            colors = ButtonDefaults.textButtonColorsPrimary()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            SmallTitle(text = stringResource(R.string.account_profile))
                            ProfileCard(
                                profile = account.profile,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            )
                        }
                    }

                    item {
                        Column {
                            SmallTitle(text = stringResource(R.string.account_sync))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                BasicComponent(
                                    title = stringResource(R.string.account_cloud_favorites),
                                    endActions = {
                                        Text(
                                            text = "${state.localFavorites}",
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                )
                                BasicComponent(
                                    title = stringResource(R.string.account_pending_changes),
                                    endActions = {
                                        Text(
                                            text = "${state.pendingCount}",
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                )
                                BasicComponent(
                                    title = stringResource(R.string.account_last_sync),
                                    endActions = {
                                        Text(
                                            text = if (account.lastSyncAt > 0L) {
                                                formatDateTime(account.lastSyncAt)
                                            } else {
                                                stringResource(R.string.account_never)
                                            },
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                )
                                ArrowPreference(
                                    title = if (state.syncing) {
                                        stringResource(R.string.account_syncing)
                                    } else {
                                        stringResource(R.string.account_sync_now)
                                    },
                                    summary = state.progress?.let { describeProgress(it) },
                                    enabled = !state.syncing,
                                    onClick = { viewModel.syncNow() }
                                )
                                ArrowPreference(
                                    title = stringResource(R.string.account_random_favorite),
                                    enabled = !state.syncing,
                                    onClick = { viewModel.randomFavorite() }
                                )
                                SwitchPreference(
                                    checked = state.favoritesSyncEnabled,
                                    onCheckedChange = { viewModel.setFavoritesSyncEnabled(it) },
                                    title = stringResource(R.string.account_favorites_sync_switch),
                                    summary = stringResource(R.string.account_favorites_sync_summary)
                                )
                                SwitchPreference(
                                    checked = state.blacklistSyncEnabled,
                                    onCheckedChange = { viewModel.setBlacklistSyncEnabled(it) },
                                    title = stringResource(R.string.account_blacklist_sync_switch),
                                    summary = stringResource(R.string.account_blacklist_sync_summary)
                                )
                            }
                        }
                    }

                    item {
                        Column {
                            SmallTitle(text = stringResource(R.string.account_key))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                ArrowPreference(
                                    title = stringResource(R.string.account_change_key),
                                    onClick = {
                                        viewModel.clearBindError()
                                        showKeyDialog = true
                                    }
                                )
                                ArrowPreference(
                                    title = stringResource(R.string.account_get_key),
                                    summary = stringResource(R.string.account_get_key_summary),
                                    onClick = { openApiKeyPage(context) }
                                )
                                ArrowPreference(
                                    title = stringResource(R.string.account_unbind),
                                    titleColor = BasicComponentDefaults.titleColor(
                                        color = MiuixTheme.colorScheme.error
                                    ),
                                    onClick = { showUnbindDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnboundSection(
    onBind: () -> Unit,
    onOpenApiKeyPage: () -> Unit,
) {
    Column {
        SmallTitle(text = stringResource(R.string.account))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            BasicComponent(
                title = stringResource(R.string.account_bind),
                summary = stringResource(R.string.account_bind_summary),
                onClick = onBind
            )
            BasicComponent(
                title = stringResource(R.string.account_get_key),
                summary = stringResource(R.string.account_get_key_summary),
                onClick = onOpenApiKeyPage
            )
        }
        Column(modifier = Modifier.padding(top = 12.dp)) {
            SmallTitle(text = stringResource(R.string.account_about_title))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.account_about_body),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(
    profile: AccountProfile,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccountAvatar(avatarUrl = profile.avatarUrl, size = 56.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = profile.username,
                        style = MiuixTheme.textStyles.title3,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.account_uid, profile.id),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            if (profile.about.isNotBlank()) {
                Text(
                    text = profile.about,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (profile.favoriteTags.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.account_favorite_tags),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Text(
                        text = profile.favoriteTags,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
            }
            if (profile.roles.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    profile.roles.forEach { role ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MiuixTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = role,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.account_bound_at,
                    if (profile.boundAt > 0L) formatDateTime(profile.boundAt) else "—"
                ),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

@Composable
private fun describeProgress(progress: SyncProgress): String = when (progress.phase) {
    SyncPhase.PUSH_FAVORITES -> stringResource(
        R.string.account_progress_push,
        progress.current,
        progress.total
    )
    SyncPhase.MERGE_FAVORITES -> stringResource(
        R.string.account_progress_merge,
        progress.current,
        progress.total
    )
    SyncPhase.PULL_FAVORITES -> stringResource(
        R.string.account_progress_pull,
        progress.current,
        progress.total
    )
    SyncPhase.PUSH_BLACKLIST -> stringResource(R.string.account_progress_blacklist_push)
    SyncPhase.PULL_BLACKLIST -> stringResource(R.string.account_progress_blacklist_pull)
    SyncPhase.DONE -> stringResource(R.string.account_sync_success)
}

private fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

private fun openApiKeyPage(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://nhentai.net/user/settings#apikeys"))
        )
    }
}
