package io.alwayszihan.zeroxpv.ui.browser.folderlist

import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.BoxWithConstraints
import io.alwayszihan.zeroxpv.ui.utils.calculateResponsiveGridSpans
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import io.alwayszihan.zeroxpv.preferences.preference.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import io.alwayszihan.zeroxpv.domain.browser.FileSystemItem
import io.alwayszihan.zeroxpv.domain.media.model.Video
import io.alwayszihan.zeroxpv.domain.media.model.VideoFolder
import io.alwayszihan.zeroxpv.preferences.AppearancePreferences
import io.alwayszihan.zeroxpv.preferences.BrowserPreferences
import io.alwayszihan.zeroxpv.preferences.FolderSortType
import io.alwayszihan.zeroxpv.preferences.FolderViewMode
import io.alwayszihan.zeroxpv.preferences.FoldersPreferences
import io.alwayszihan.zeroxpv.preferences.GesturePreferences
import io.alwayszihan.zeroxpv.preferences.MediaLayoutMode
import io.alwayszihan.zeroxpv.preferences.OnboardingPreferences
import io.alwayszihan.zeroxpv.preferences.SortOrder
import io.alwayszihan.zeroxpv.preferences.SecureFolderPreferences
import io.alwayszihan.zeroxpv.preferences.preference.collectAsState
import io.alwayszihan.zeroxpv.database.repository.SecureFolderRepository
import io.alwayszihan.zeroxpv.presentation.Screen
import io.alwayszihan.zeroxpv.ui.browser.videolist.VideoListScreen
import io.alwayszihan.zeroxpv.presentation.components.pullrefresh.PullRefreshBox
import io.alwayszihan.zeroxpv.ui.browser.LocalNavigationBarHeight
import io.alwayszihan.zeroxpv.ui.browser.cards.FolderCard
import io.alwayszihan.zeroxpv.ui.browser.cards.VideoCard
import io.alwayszihan.zeroxpv.ui.browser.cards.VideoCardUiConfig
import io.alwayszihan.zeroxpv.ui.browser.components.BrowserBottomBar
import io.alwayszihan.zeroxpv.ui.browser.components.BrowserTopBar
import io.alwayszihan.zeroxpv.ui.browser.components.ExpressiveScrollBar
import io.alwayszihan.zeroxpv.ui.browser.components.fastScrollGlyph
import io.alwayszihan.zeroxpv.ui.browser.dialogs.DeleteConfirmationDialog
import io.alwayszihan.zeroxpv.ui.browser.dialogs.FileOperationProgressDialog
import io.alwayszihan.zeroxpv.ui.browser.dialogs.FolderPickerDialog
import io.alwayszihan.zeroxpv.ui.browser.dialogs.RenameDialog
import io.alwayszihan.zeroxpv.utils.media.CopyPasteOps
import io.alwayszihan.zeroxpv.utils.media.OpenDocumentTreeContract
import io.alwayszihan.zeroxpv.ui.browser.dialogs.FolderSortDialog
import io.alwayszihan.zeroxpv.ui.browser.filesystem.FileSystemDirectoryScreen
import io.alwayszihan.zeroxpv.ui.browser.medialibrary.MediaLibraryContent
import io.alwayszihan.zeroxpv.ui.browser.filesystem.FileSystemBrowserRootScreen
import io.alwayszihan.zeroxpv.ui.browser.selection.rememberSelectionManager
import io.alwayszihan.zeroxpv.ui.browser.sheets.PlayLinkSheet
import io.alwayszihan.zeroxpv.ui.browser.states.EmptyState
import io.alwayszihan.zeroxpv.ui.browser.states.LoadingState
import io.alwayszihan.zeroxpv.ui.browser.states.PermissionDeniedState
import io.alwayszihan.zeroxpv.ui.securefolder.SecureConfirmDialog
import io.alwayszihan.zeroxpv.ui.securefolder.SecureFolderGateScreen
import io.alwayszihan.zeroxpv.ui.securefolder.SecureFolderProgressDialog
import io.alwayszihan.zeroxpv.ui.utils.LocalBackStack
import io.alwayszihan.zeroxpv.utils.clipboard.SafeClipboard
import io.alwayszihan.zeroxpv.utils.history.RecentlyPlayedOps
import io.alwayszihan.zeroxpv.utils.media.MediaUtils
import io.alwayszihan.zeroxpv.utils.media.MediaSearchEngine
import io.alwayszihan.zeroxpv.utils.permission.PermissionUtils
import io.alwayszihan.zeroxpv.utils.storage.FileTypeUtils
import io.alwayszihan.zeroxpv.utils.sort.SortUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.io.File

@Serializable
object FolderListScreen : Screen {
  @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val browserPreferences = koinInject<BrowserPreferences>()
    val folderViewMode by browserPreferences.folderViewMode.collectAsState()

    when (folderViewMode) {
      FolderViewMode.FileManager -> FileSystemBrowserRootScreen.Content()
      FolderViewMode.AlbumView -> MediaStoreFolderListContent()
      FolderViewMode.MediaLibrary -> MediaLibraryContent()
    }
  }

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  private fun MediaStoreFolderListContent() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // ViewModels and preferences
    val viewModel: FolderListViewModel = viewModel(
      factory = FolderListViewModel.factory(context.applicationContext as android.app.Application)
    )
    val browserPreferences = koinInject<BrowserPreferences>()
    val gesturePreferences = koinInject<GesturePreferences>()
    val foldersPreferences = koinInject<FoldersPreferences>()
    val advancedPreferences = koinInject<io.alwayszihan.zeroxpv.preferences.AdvancedPreferences>()
    val onboardingPreferences = koinInject<OnboardingPreferences>()
    val onboardingCompleted by onboardingPreferences.completed.collectAsState()

    // State collection
    val videoFolders by viewModel.videoFolders.collectAsState()
    val foldersWithNewCount by viewModel.foldersWithNewCount.collectAsState()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scanStatus by viewModel.scanStatus.collectAsState()
    val hasCompletedInitialLoad by viewModel.hasCompletedInitialLoad.collectAsState()
    val foldersWereDeleted by viewModel.foldersWereDeleted.collectAsState()

    // Preferences
    val mediaLayoutMode by browserPreferences.mediaLayoutMode.collectAsState()
    val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
    val folderSortType by browserPreferences.folderSortType.collectAsState()
    val folderSortOrder by browserPreferences.folderSortOrder.collectAsState()
    val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
    val enableRecentlyPlayed by advancedPreferences.enableRecentlyPlayed.collectAsState()
    val pinnedFolderPaths by foldersPreferences.pinnedFolders.collectAsState()
    val dualPaneForTablet by browserPreferences.dualPaneForTablet.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isDualPaneActive = isTablet && dualPaneForTablet

    var selectedFolderBucketId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFolderName by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(isDualPaneActive) {
      if (!isDualPaneActive) {
        selectedFolderBucketId = null
        selectedFolderName = null
      }
    }

    // UI state - use standalone states to avoid scroll issues with predictive back gesture
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val navigationBarHeight = LocalNavigationBarHeight.current
    val navBarState = io.alwayszihan.zeroxpv.ui.browser.NavigationBarState
    val isRefreshing = remember { mutableStateOf(false) }
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
    var pendingDeleteFolders by remember { mutableStateOf<List<VideoFolder>>(emptyList()) }
    val showLinkDialog = remember { mutableStateOf(false) }
    val folderPickerOpen = rememberSaveable { mutableStateOf(false) }
    val operationType = remember { mutableStateOf<CopyPasteOps.OperationType?>(null) }
    val progressDialogOpen = rememberSaveable { mutableStateOf(false) }
    var renameDialogOpen by rememberSaveable { mutableStateOf(false) }
    val operationProgress by CopyPasteOps.operationProgress.collectAsState()

    // Search state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<FileSystemItem>>(emptyList()) }
    var isSearchLoading by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val foldersBlacklistedMessage = stringResource(io.alwayszihan.zeroxpv.R.string.pref_folders_blacklisted)

    // Search logic
    LaunchedEffect(searchQuery, isSearching) {
      if (isSearching && searchQuery.isNotBlank()) {
        delay(250)
        isSearchLoading = true
        try {
          val results = searchFoldersAndVideos(context, searchQuery)
          searchResults = results
        } catch (e: Exception) {
          Log.e("FolderListScreen", "Error during search", e)
          searchResults = emptyList()
        } finally {
          isSearchLoading = false
        }
      } else {
        searchResults = emptyList()
        isSearchLoading = false
      }
    }

    // FAB state
    val isFabVisible = remember { mutableStateOf(true) }
    val isFabExpanded = remember { mutableStateOf(false) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
      uri?.let {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            it,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
        MediaUtils.playFile(it.toString(), context, "open_file")
      }
    }

    // Sorting and filtering
    val sortedFolders = remember(videoFolders, folderSortType, folderSortOrder, pinnedFolderPaths) {
      val sorted = SortUtils.sortFolders(videoFolders, folderSortType, folderSortOrder)
      val (pinned, unpinned) = sorted.partition { it.path in pinnedFolderPaths }
      pinned + unpinned
    }

    val filteredFolders = sortedFolders
    
    suspend fun deleteFolders(folders: List<VideoFolder>): Pair<Int, Int> {
      var deleted = 0
      var failed = 0
      val deleteAll = browserPreferences.deleteFolderAllContents.get()
      val includeAudio = browserPreferences.includeAudioBrowser.get()
      for (folder in folders) {
        try {
          if (deleteAll) {
            val ids = setOf(folder.bucketId)
            val videos = io.alwayszihan.zeroxpv.repository.MediaFileRepository.getVideosForBuckets(context, ids)
            if (videos.isNotEmpty()) {
              val (d, f) = viewModel.deleteVideos(videos)
              deleted += d
              failed += f
            }
            val dir = java.io.File(folder.path)
            if (dir.exists()) {
              if (dir.deleteRecursively()) {
                deleted++
              } else {
                failed++
              }
            }
          } else {
            var deletedAny = false
            val dir = java.io.File(folder.path)
            if (dir.exists()) {
              dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                  val ext = file.extension.lowercase()
                  val isVideo = ext in FileTypeUtils.VIDEO_EXTENSIONS
                  val isAudio = includeAudio && ext in FileTypeUtils.AUDIO_EXTENSIONS
                  if (isVideo || isAudio) {
                    if (file.delete()) deletedAny = true
                  }
                }
              }
            }
            if (deletedAny) {
              deleted++
            } else {
              failed++
            }
          }
        } catch (e: Exception) {
          Log.e("FolderListScreen", "Error deleting folder ${folder.path}", e)
          failed++
        }
      }
      return Pair(deleted, failed)
    }

    // Selection manager
    val selectionManager = rememberSelectionManager(
      items = sortedFolders,
      getId = { it.bucketId },
      onDeleteItems = { folders, _ -> deleteFolders(folders) },
      onOperationComplete = { viewModel.refresh() },
    )

    val treePickerLauncher = rememberLauncherForActivityResult(
      contract = OpenDocumentTreeContract(),
    ) { uri ->
      if (uri == null || operationType.value == null) return@rememberLauncherForActivityResult
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
      }
      progressDialogOpen.value = true
      coroutineScope.launch {
        val selectedFolders = selectionManager.getSelectedItems()
        val selectedVideos = selectedFolders.flatMap { folder ->
          io.alwayszihan.zeroxpv.repository.MediaFileRepository.getVideosForBuckets(context, setOf(folder.bucketId))
        }
        if (selectedVideos.isNotEmpty()) {
          when (operationType.value) {
            is CopyPasteOps.OperationType.Copy -> CopyPasteOps.copyFilesToTreeUri(context, selectedVideos, uri)
            is CopyPasteOps.OperationType.Move -> CopyPasteOps.moveFilesToTreeUri(context, selectedVideos, uri)
            else -> {}
          }
        }
      }
    }

    // Permissions
    val permissionState = PermissionUtils.handleStoragePermission(
      onPermissionGranted = { viewModel.refresh() },
    )

    // Update MainScreen about permission state
    LaunchedEffect(permissionState.status) {
      io.alwayszihan.zeroxpv.ui.browser.MainScreen.updatePermissionState(
        isDenied = permissionState.status is PermissionStatus.Denied
      )
    }

    // Update NavigationBarState when selection mode changes
    LaunchedEffect(selectionManager.isInSelectionMode) {
      navBarState.updateSelectionState(
        inSelectionMode = selectionManager.isInSelectionMode,
        onlyVideos = true,
      )
    }

    DisposableEffect(isDualPaneActive, selectedFolderBucketId) {
      navBarState.isDualPaneFolderSelected = isDualPaneActive && selectedFolderBucketId != null
      onDispose {
        navBarState.isDualPaneFolderSelected = false
      }
    }

    // Lifecycle observer for refresh
    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          viewModel.recalculateNewVideoCounts()
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Move-to-Secure-Folder state
    val secureFolderRepository = koinInject<SecureFolderRepository>()
    val secureFolderPreferences = koinInject<SecureFolderPreferences>()
    val moveToSecureConfirmOpen = rememberSaveable { mutableStateOf(false) }
    val moveToSecureProgressOpen = rememberSaveable { mutableStateOf(false) }
    val secureFolderProgress by secureFolderRepository.progress.collectAsState()

    fun moveSelectedFoldersToSecureFolder() {
      coroutineScope.launch {
        val selectedIds = selectionManager.getSelectedItems().map { it.bucketId }.toSet()
        val allVideos = io.alwayszihan.zeroxpv.repository.MediaFileRepository
          .getVideosForBuckets(context, selectedIds)
        if (allVideos.isEmpty()) return@launch
        moveToSecureProgressOpen.value = true
        val result = secureFolderRepository.moveIn(context, allVideos)
        moveToSecureProgressOpen.value = false
        selectionManager.clear()
        viewModel.refresh()
        result
          .onSuccess { batch ->
            val message =
              if (batch.failedIds.isEmpty()) {
                context.getString(io.alwayszihan.zeroxpv.R.string.secure_folder_moved_success, batch.succeededIds.size)
              } else {
                context.getString(
                  io.alwayszihan.zeroxpv.R.string.secure_folder_moved_partial,
                  batch.succeededIds.size,
                  batch.failedIds.size,
                )
              }
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
          }.onFailure {
            android.widget.Toast.makeText(context, context.getString(io.alwayszihan.zeroxpv.R.string.secure_folder_move_failed), android.widget.Toast.LENGTH_SHORT).show()
          }
      }
    }

    // Optimized back handler for immediate response
    val shouldHandleBack = selectionManager.isInSelectionMode || isSearching || isFabExpanded.value || (isDualPaneActive && selectedFolderBucketId != null)
    androidx.activity.compose.BackHandler(enabled = shouldHandleBack) {
      when {
        isFabExpanded.value -> isFabExpanded.value = false
        selectionManager.isInSelectionMode -> selectionManager.clear()
        isSearching -> {
          isSearching = false
          searchQuery = ""
        }
        isDualPaneActive && selectedFolderBucketId != null -> {
          selectedFolderBucketId = null
          selectedFolderName = null
        }
      }
    }

    // FAB scroll tracking
    io.alwayszihan.zeroxpv.ui.browser.fab.FabScrollHelper.trackScrollForFabVisibility(
      listState = listState,
      gridState = if (mediaLayoutMode == MediaLayoutMode.GRID) gridState else null,
      isFabVisible = isFabVisible,
      expanded = isFabExpanded.value,
      onExpandedChange = { isFabExpanded.value = it },
    )

    @Composable
    fun FoldersPane() {
      Scaffold(
        topBar = {
          if (isSearching) {
            SearchBar(
              inputField = {
                SearchBarDefaults.InputField(
                  query = searchQuery,
                  onQueryChange = { searchQuery = it },
                  onSearch = { },
                  expanded = false,
                  onExpandedChange = { },
                  placeholder = { Text("Search folders and videos...") },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Filled.Search,
                      contentDescription = "Search",
                    )
                  },
                  trailingIcon = {
                    IconButton(
                      onClick = {
                        isSearching = false
                        searchQuery = ""
                      },
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                      )
                    }
                  },
                  modifier = Modifier.focusRequester(focusRequester),
                )
              },
              expanded = false,
              onExpandedChange = { },
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              shape = RoundedCornerShape(28.dp),
              tonalElevation = 6.dp,
            ) {
              // Empty content for SearchBar
            }
          } else {
            BrowserTopBar(
              title = stringResource(io.alwayszihan.zeroxpv.R.string.app_name),
              isInSelectionMode = selectionManager.isInSelectionMode,
              selectedCount = selectionManager.selectedCount,
              totalCount = videoFolders.size,
              onBackClick = null,
              onCancelSelection = { selectionManager.clear() },
              onSortClick = { sortDialogOpen.value = true },
              onSearchClick = {
                isSearching = !isSearching
                coroutineScope.launch {
                  buildSearchIndex(context)
                }
              },
              onSettingsClick = {
                backstack.add(io.alwayszihan.zeroxpv.ui.preferences.PreferencesScreen)
              },
              onSecureFolderClick = {
                backstack.add(io.alwayszihan.zeroxpv.ui.securefolder.SecureFolderGateScreen)
              },
              onMoveToSecureClick = {
                if (!secureFolderPreferences.isPinSet()) {
                  backstack.add(SecureFolderGateScreen)
                } else if (secureFolderPreferences.dontAskBeforeMove.get()) {
                  moveSelectedFoldersToSecureFolder()
                } else {
                  moveToSecureConfirmOpen.value = true
                }
              },
              onRenameClick = null,
              isSingleSelection = selectionManager.isSingleSelection,
              onInfoClick = null,
              onShareClick = {
                coroutineScope.launch {
                  val selectedIds = selectionManager.getSelectedItems().map { it.bucketId }.toSet()
                  val allVideos = io.alwayszihan.zeroxpv.repository.MediaFileRepository
                    .getVideosForBuckets(context, selectedIds)
                  if (allVideos.isNotEmpty()) {
                    MediaUtils.shareVideos(context, allVideos)
                  }
                }
              },
              onCopyClick = {
                val selectedPaths = selectionManager.getSelectedItems().map { it.path }.distinct()
                if (selectedPaths.isNotEmpty()) {
                  SafeClipboard.copyPlainText(context, "Selected folder paths", selectedPaths.joinToString("\n"))
                }
              },
              onPlayClick = {
                coroutineScope.launch {
                  val selectedIds = selectionManager.getSelectedItems().map { it.bucketId }.toSet()
                  val allVideos = io.alwayszihan.zeroxpv.repository.MediaFileRepository
                    .getVideosForBuckets(context, selectedIds)
                  if (allVideos.isNotEmpty()) {
                    if (allVideos.size == 1) {
                      MediaUtils.playFile(allVideos.first(), context)
                    } else {
                      val intent = Intent(Intent.ACTION_VIEW, allVideos.first().uri)
                      intent.setClass(context, io.alwayszihan.zeroxpv.ui.player.PlayerActivity::class.java)
                      intent.putExtra("internal_launch", true)
                      intent.putParcelableArrayListExtra("playlist", ArrayList(allVideos.map { it.uri }))
                      intent.putExtra("playlist_index", 0)
                      intent.putExtra("launch_source", "playlist")
                      runCatching {
                        context.startActivity(intent)
                      }.onFailure { e ->
                        android.util.Log.e("FolderListScreen", "Failed to open player playlist", e)
                      }
                    }
                    selectionManager.clear()
                  }
                }
              },
              onPinClick = {
                coroutineScope.launch {
                  val selectedFolders = selectionManager.getSelectedItems()
                  if (selectedFolders.isEmpty()) return@launch
                  val updated = foldersPreferences.pinnedFolders.get().toMutableSet()
                  val shouldUnpinAll = selectedFolders.all { it.path in updated }
                  selectedFolders.forEach { folder ->
                    if (shouldUnpinAll) {
                      updated.remove(folder.path)
                    } else {
                      updated.add(folder.path)
                    }
                  }
                  foldersPreferences.pinnedFolders.set(updated)
                  selectionManager.clear()
                }
              },
              onBlacklistClick = {
                coroutineScope.launch {
                  val selectedFolders = selectionManager.getSelectedItems()
                  val blacklistedFolders = foldersPreferences.blacklistedFolders.get().toMutableSet()
                  selectedFolders.forEach { folder ->
                    blacklistedFolders.add(folder.path)
                  }
                  foldersPreferences.blacklistedFolders.set(blacklistedFolders)
                  selectionManager.clear()
                  viewModel.refresh()
                  android.widget.Toast.makeText(
                    context,
                    foldersBlacklistedMessage,
                    android.widget.Toast.LENGTH_SHORT,
                  ).show()
                }
              },
              onDeleteClick = null,
              onSelectAll = { selectionManager.selectAll() },
              onInvertSelection = { selectionManager.invertSelection() },
              onDeselectAll = { selectionManager.clear() },
            )
          }
        },
        floatingActionButton = {
          FloatingActionButtonMenu(
            modifier = Modifier.padding(bottom = navigationBarHeight + 8.dp),
            expanded = isFabExpanded.value,
            button = {
              TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                  if (isFabExpanded.value) {
                    TooltipAnchorPosition.Start
                  } else {
                    TooltipAnchorPosition.Above
                  }
                ),
                tooltip = { PlainTooltip { Text("Toggle menu") } },
                state = rememberTooltipState(),
              ) {
                ToggleFloatingActionButton(
                  modifier = Modifier.animateFloatingActionButton(
                    visible = !selectionManager.isInSelectionMode && isFabVisible.value && !io.alwayszihan.zeroxpv.ui.browser.MainScreen.getPermissionDeniedState() && !(isDualPaneActive && selectedFolderBucketId != null),
                    alignment = Alignment.BottomEnd,
                  ),
                  checked = isFabExpanded.value,
                  onCheckedChange = { isFabExpanded.value = !isFabExpanded.value },
                ) {
                  val imageVector by remember {
                    derivedStateOf {
                      if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.PlayArrow
                    }
                  }
                  Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }),
                  )
                }
              }
            },
          ) {
            FloatingActionButtonMenuItem(
              onClick = {
                isFabExpanded.value = false
                filePicker.launch(arrayOf("video/*"))
              },
              icon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
              text = { Text(text = "Open File") },
            )

            FloatingActionButtonMenuItem(
              onClick = {
                isFabExpanded.value = false
                coroutineScope.launch {
                  val recentlyPlayedVideos = RecentlyPlayedOps.getRecentlyPlayed(limit = 1)
                  val lastPlayed = recentlyPlayedVideos.firstOrNull()
                  if (lastPlayed != null) {
                    MediaUtils.playFile(lastPlayed.filePath, context, "recently_played_button")
                  }
                }
              },
              icon = { Icon(Icons.Filled.History, contentDescription = null) },
              text = { Text(text = "Recently Played") },
            )

            FloatingActionButtonMenuItem(
              onClick = {
                isFabExpanded.value = false
                showLinkDialog.value = true
              },
              icon = { Icon(Icons.Filled.Link, contentDescription = null) },
              text = { Text(text = "Open Link") },
            )
          }
        },
      ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
          when (permissionState.status) {
            PermissionStatus.Granted -> {
              if (isSearching) {
                // Show search results
                Box(modifier = Modifier.fillMaxSize()) {
                  if (isSearchLoading) {
                    // Loading state
                    Box(
                      modifier = Modifier.fillMaxSize(),
                      contentAlignment = Alignment.Center,
                    ) {
                      CircularProgressIndicator()
                    }
                  } else if (searchResults.isEmpty()) {
                    // No results
                    EmptyState(
                      icon = Icons.Filled.Search,
                      title = "No results found",
                      message = "No folders or videos match your search query",
                      modifier = Modifier.fillMaxSize(),
                    )
                  } else {
                    // Show search results
                    SearchResultsContent(
                      searchResults = searchResults,
                      navigationBarHeight = navigationBarHeight,
                      onFolderClick = { folder ->
                        if (isDualPaneActive) {
                          selectedFolderBucketId = folder.bucketId
                          selectedFolderName = folder.name
                          isSearching = false
                          searchQuery = ""
                        } else {
                          backstack.add(io.alwayszihan.zeroxpv.ui.browser.videolist.VideoListScreen(folder.bucketId, folder.name))
                        }
                      },
                      onVideoClick = { video ->
                        MediaUtils.playFile(video, context)
                      },
                      mediaLayoutMode = mediaLayoutMode,
                    )
                  }
                }
              } else {
                FolderListContent(
                  folders = filteredFolders,
                  foldersWithNewCount = foldersWithNewCount,
                  pinnedFolderPaths = pinnedFolderPaths,
                  recentlyPlayedFilePath = recentlyPlayedFilePath,
                  isLoading = isLoading,
                  scanStatus = scanStatus,
                  hasCompletedInitialLoad = hasCompletedInitialLoad,
                  foldersWereDeleted = foldersWereDeleted,
                  mediaLayoutMode = mediaLayoutMode,
                  tapThumbnailToSelect = tapThumbnailToSelect,
                  navigationBarHeight = navigationBarHeight,
                  listState = listState,
                  gridState = gridState,
                  isRefreshing = isRefreshing,
                  selectionManager = selectionManager,
                  onRefresh = { viewModel.refresh() },
                  onFolderClick = { folder ->
                    if (selectionManager.isInSelectionMode) {
                      selectionManager.toggle(folder)
                    } else {
                      if (isDualPaneActive) {
                        selectedFolderBucketId = folder.bucketId
                        selectedFolderName = folder.name
                      } else {
                        backstack.add(io.alwayszihan.zeroxpv.ui.browser.videolist.VideoListScreen(folder.bucketId, folder.name))
                      }
                    }
                  },
                  onFolderLongClick = { folder ->
                    selectionManager.handleLongClick(folder)
                  },
                  onTogglePin = { folder ->
                    coroutineScope.launch {
                      val updated = foldersPreferences.pinnedFolders.get().toMutableSet()
                      if (!updated.add(folder.path)) {
                        updated.remove(folder.path)
                      }
                      foldersPreferences.pinnedFolders.set(updated)
                    }
                  },
                  selectedFolderBucketId = selectedFolderBucketId,
                )
              }
            }

            is PermissionStatus.Denied -> {
              // First-run onboarding (Files & Media, "Next") owns the permission flow until the
              // user completes it; only show the in-place prompt afterwards.
              if (onboardingCompleted) {
                PermissionDeniedState(
                  onRequestPermission = { permissionState.launchPermissionRequest() },
                  modifier = Modifier,
                )
              }
            }
          }

          if (selectionManager.isInSelectionMode) {
            BrowserBottomBar(
              isSelectionMode = true,
              onCopyClick = {
                operationType.value = CopyPasteOps.OperationType.Copy
                if (CopyPasteOps.canUseDirectFileOperations()) {
                  folderPickerOpen.value = true
                } else {
                  treePickerLauncher.launch(null)
                }
              },
              onMoveClick = {
                operationType.value = CopyPasteOps.OperationType.Move
                if (CopyPasteOps.canUseDirectFileOperations()) {
                  folderPickerOpen.value = true
                } else {
                  treePickerLauncher.launch(null)
                }
              },
              onRenameClick = { renameDialogOpen = true },
              onDeleteClick = { pendingDeleteFolders = selectionManager.getSelectedItems() },
              onAddToPlaylistClick = { },
              showCopy = true,
              showMove = true,
              showRename = selectionManager.isSingleSelection,
              showDownscale = false,
              showAddToPlaylist = false,
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (navBarState.shouldHideNavigationBar) 0.dp else navigationBarHeight),
            )
          }
        }
      }
    }

    if (isDualPaneActive && selectedFolderBucketId != null) {
      Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.4f)) {
          FoldersPane()
        }
        VerticalDivider(
          modifier = Modifier.fillMaxHeight(),
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          thickness = 1.dp
        )
        Box(modifier = Modifier.weight(0.6f)) {
          key(selectedFolderBucketId) {
            CompositionLocalProvider(
              io.alwayszihan.zeroxpv.ui.browser.LocalNavigationBarHeight provides 0.dp
            ) {
              VideoListScreen(
                bucketId = selectedFolderBucketId!!,
                folderName = selectedFolderName.orEmpty(),
                isDualPane = true,
                onBack = {
                  selectedFolderBucketId = null
                  selectedFolderName = null
                }
              ).Content()
            }
          }
        }
      }
    } else {
      FoldersPane()
    }

      // Dialogs
      PlayLinkSheet(
        isOpen = showLinkDialog.value,
        onDismiss = { showLinkDialog.value = false },
        onPlayLink = { url -> MediaUtils.playFile(url, context, "play_link") },
      )

      FolderPickerDialog(
        isOpen = folderPickerOpen.value,
        currentPath = "",
        onDismiss = { folderPickerOpen.value = false },
        onFolderSelected = { destinationPath ->
          folderPickerOpen.value = false
          val op = operationType.value
          if (op != null) {
            coroutineScope.launch {
              val selectedFolders = selectionManager.getSelectedItems()
              if (selectedFolders.isNotEmpty()) {
                when (op) {
                  is CopyPasteOps.OperationType.Move -> {
                    val needFallback = mutableListOf<VideoFolder>()
                    for (folder in selectedFolders) {
                      val dst = File(destinationPath, folder.name)
                      if (!File(folder.path).renameTo(dst)) needFallback.add(folder)
                    }
                    if (needFallback.isNotEmpty()) {
                      progressDialogOpen.value = true
                      for (folder in needFallback) {
                        val videos = io.alwayszihan.zeroxpv.repository.MediaFileRepository.getVideosForBuckets(context, setOf(folder.bucketId))
                        if (videos.isNotEmpty()) {
                          val subDest = File(destinationPath, folder.name).also { it.mkdirs() }.absolutePath
                          CopyPasteOps.moveFiles(context, videos, subDest)
                        }
                      }
                    } else {
                      selectionManager.clear()
                      viewModel.refresh()
                    }
                  }
                  is CopyPasteOps.OperationType.Copy -> {
                    progressDialogOpen.value = true
                    for (folder in selectedFolders) {
                      val videos = io.alwayszihan.zeroxpv.repository.MediaFileRepository.getVideosForBuckets(context, setOf(folder.bucketId))
                      if (videos.isNotEmpty()) {
                        val subDest = File(destinationPath, folder.name).also { it.mkdirs() }.absolutePath
                        CopyPasteOps.copyFiles(context, videos, subDest)
                      }
                    }
                  }
                }
              }
            }
          }
        },
      )

      if (operationType.value != null) {
        FileOperationProgressDialog(
          isOpen = progressDialogOpen.value,
          operationType = operationType.value!!,
          progress = operationProgress,
          onCancel = { CopyPasteOps.cancelOperation() },
          onDismiss = {
            progressDialogOpen.value = false
            operationType.value = null
            selectionManager.clear()
            viewModel.refresh()
          },
        )
      }

      if (renameDialogOpen && selectionManager.isSingleSelection) {
        val folder = selectionManager.getSelectedItems().firstOrNull()
        if (folder != null) {
          RenameDialog(
            isOpen = true,
            onDismiss = { renameDialogOpen = false },
            onConfirm = { newName ->
              renameDialogOpen = false
              coroutineScope.launch {
                val ok = viewModel.renameFolder(folder, newName)
                if (!ok) {
                  android.widget.Toast.makeText(context, "Rename failed", android.widget.Toast.LENGTH_SHORT).show()
                }
                selectionManager.clear()
                viewModel.refresh()
              }
            },
            currentName = folder.name,
            itemType = "folder",
          )
        }
      }

      FolderSortDialog(
        isOpen = sortDialogOpen.value,
        onDismiss = { sortDialogOpen.value = false },
        sortType = folderSortType,
        sortOrder = folderSortOrder,
        onSortTypeChange = { browserPreferences.folderSortType.set(it) },
        onSortOrderChange = { browserPreferences.folderSortOrder.set(it) },
        isDualPane = isDualPaneActive && selectedFolderBucketId != null,
      )

      if (pendingDeleteFolders.isNotEmpty()) {
        DeleteConfirmationDialog(
          isOpen = true,
          onDismiss = { pendingDeleteFolders = emptyList() },
          onConfirm = {
            val foldersToDelete = pendingDeleteFolders
            pendingDeleteFolders = emptyList()
            coroutineScope.launch {
              runCatching {
                val (deleted, failed) = deleteFolders(foldersToDelete)
                if (deleted > 0) {
                  android.widget.Toast.makeText(context, "Deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                } else if (failed > 0) {
                  android.widget.Toast.makeText(context, "Failed to delete", android.widget.Toast.LENGTH_SHORT).show()
                }
              }.onFailure {
                android.widget.Toast.makeText(context, "Failed to delete: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
              }
              selectionManager.clear()
              viewModel.refresh()
            }
          },
          itemType = "folder",
          itemCount = pendingDeleteFolders.size,
          itemNames = pendingDeleteFolders.map { it.name },
        )
      }

      // Move to Secure Folder — confirm (skippable via "don't ask again"), then progress
      SecureConfirmDialog(
        isOpen = moveToSecureConfirmOpen.value,
        title = context.getString(io.alwayszihan.zeroxpv.R.string.secure_folder_move_folders_title, selectionManager.selectedCount),
        subtitle = context.getString(io.alwayszihan.zeroxpv.R.string.secure_folder_move_folders_subtitle),
        dontAskAgain = secureFolderPreferences.dontAskBeforeMove,
        onConfirm = {
          moveToSecureConfirmOpen.value = false
          moveSelectedFoldersToSecureFolder()
        },
        onDismiss = { moveToSecureConfirmOpen.value = false },
      )

      SecureFolderProgressDialog(
        isOpen = moveToSecureProgressOpen.value,
        progress = secureFolderProgress,
        label = context.getString(io.alwayszihan.zeroxpv.R.string.secure_folder_moving_progress),
        onCancel = { secureFolderRepository.cancelOperation() },
      )
    }
  }

@Composable
private fun FolderListContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<io.alwayszihan.zeroxpv.ui.browser.folderlist.FolderWithNewCount>,
  pinnedFolderPaths: Set<String>,
  recentlyPlayedFilePath: String?,
  isLoading: Boolean,
  scanStatus: String?,
  hasCompletedInitialLoad: Boolean,
  foldersWereDeleted: Boolean,
  mediaLayoutMode: MediaLayoutMode,
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  listState: LazyListState,
  gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  selectionManager: io.alwayszihan.zeroxpv.ui.browser.selection.SelectionManager<VideoFolder, String>,
  onRefresh: suspend () -> Unit,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
  onTogglePin: (VideoFolder) -> Unit,
  selectedFolderBucketId: String? = null,
) {
  val isGridMode = mediaLayoutMode == MediaLayoutMode.GRID
  val showLoading = isLoading && !hasCompletedInitialLoad
  val showEmpty = folders.isEmpty() && hasCompletedInitialLoad && !foldersWereDeleted

  val hasEnoughItems = folders.size > 20
  val scrollbarAlpha by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (hasEnoughItems) 1f else 0f,
    animationSpec = androidx.compose.animation.core.spring(
      dampingRatio = io.alwayszihan.zeroxpv.ui.theme.AppMotion.Effect.Alpha.dampingRatio,
      stiffness = io.alwayszihan.zeroxpv.ui.theme.AppMotion.Effect.Alpha.stiffness,
    ),
    label = "scrollbarAlpha",
  )

  PullRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    listState = listState,
    modifier = Modifier.fillMaxSize(),
  ) {
    if (showLoading || showEmpty) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        if (showLoading) {
          LoadingState(
            icon = Icons.Filled.Folder,
            title = "Scanning for videos...",
            message = scanStatus ?: "Please wait while we search your device",
          )
        } else if (showEmpty) {
          EmptyState(
            icon = Icons.Filled.Folder,
            title = "No video folders found",
            message = "Add some video files to your device to see them here",
          )
        }
      }
    } else {
      if (isGridMode) {
        GridContent(
          folders = folders,
          foldersWithNewCount = foldersWithNewCount,
          pinnedFolderPaths = pinnedFolderPaths,
          recentlyPlayedFilePath = recentlyPlayedFilePath,
          tapThumbnailToSelect = tapThumbnailToSelect,
          navigationBarHeight = navigationBarHeight,
          gridState = gridState,
          scrollbarAlpha = scrollbarAlpha,
          selectionManager = selectionManager,
          onFolderClick = onFolderClick,
          onFolderLongClick = onFolderLongClick,
          onTogglePin = onTogglePin,
          selectedFolderBucketId = selectedFolderBucketId,
        )
      } else {
        ListContent(
          folders = folders,
          foldersWithNewCount = foldersWithNewCount,
          pinnedFolderPaths = pinnedFolderPaths,
          recentlyPlayedFilePath = recentlyPlayedFilePath,
          tapThumbnailToSelect = tapThumbnailToSelect,
          navigationBarHeight = navigationBarHeight,
          listState = listState,
          scrollbarAlpha = scrollbarAlpha,
          selectionManager = selectionManager,
          onFolderClick = onFolderClick,
          onFolderLongClick = onFolderLongClick,
          onTogglePin = onTogglePin,
          selectedFolderBucketId = selectedFolderBucketId,
        )
      }
    }
  }
}

@Composable
private fun GridContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<io.alwayszihan.zeroxpv.ui.browser.folderlist.FolderWithNewCount>,
  pinnedFolderPaths: Set<String>,
  recentlyPlayedFilePath: String?,
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
  scrollbarAlpha: Float,
  selectionManager: io.alwayszihan.zeroxpv.ui.browser.selection.SelectionManager<VideoFolder, String>,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
  onTogglePin: (VideoFolder) -> Unit,
  selectedFolderBucketId: String? = null,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val browserPreferences = org.koin.compose.koinInject<io.alwayszihan.zeroxpv.preferences.BrowserPreferences>()
    val manualGridColumnsEnabled by browserPreferences.manualGridColumnsEnabled.collectAsState()
    val folderGridColumnsPortrait by browserPreferences.folderGridColumnsPortrait.collectAsState()
    val folderGridColumnsLandscape by browserPreferences.folderGridColumnsLandscape.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val folderGridColumnsPref = if (isLandscape) folderGridColumnsLandscape else folderGridColumnsPortrait

    val isTablet = configuration.smallestScreenWidthDp >= 600
    val dualPaneForTablet by browserPreferences.dualPaneForTablet.collectAsState()
    val isDualPaneActive = isTablet && dualPaneForTablet
    val isDualPane = isDualPaneActive && selectedFolderBucketId != null

    val computedColumns = if (manualGridColumnsEnabled) {
      folderGridColumnsPref.coerceAtLeast(1)
    } else {
      val contentHorizontalPadding = 8.dp
      val itemSpacing = 2.dp
      val usableWidth = maxWidth - (contentHorizontalPadding * 2) - itemSpacing
      val folderMinWidth = 100.dp
      (usableWidth / folderMinWidth).toInt().coerceAtLeast(1)
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(computedColumns),
      state = gridState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        bottom = navigationBarHeight
      ),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      items(count = folders.size, key = { index -> folders[index].bucketId }) { index ->
        val folder = folders[index]
        val isRecentlyPlayed = recentlyPlayedFilePath?.let { filePath ->
          val file = File(filePath)
          file.parent == folder.path
        } ?: false

        val newCount = foldersWithNewCount
          .find { it.folder.bucketId == folder.bucketId }
          ?.newVideoCount ?: 0

        val isActive = isDualPaneActive && folder.bucketId == selectedFolderBucketId

        FolderCard(
          folder = folder,
          isSelected = selectionManager.isSelected(folder),
          isRecentlyPlayed = isRecentlyPlayed,
          onClick = { onFolderClick(folder) },
          onLongClick = { onFolderLongClick(folder) },
          onThumbClick = if (tapThumbnailToSelect) {
            { selectionManager.toggle(folder) }
          } else {
            { onFolderClick(folder) }
          },
          newVideoCount = newCount,
          isGridMode = true,
          isPinned = folder.path in pinnedFolderPaths,
          onPinClick =
            if (!selectionManager.isInSelectionMode) {
              { onTogglePin(folder) }
            } else {
              null
            },
          isDualPane = isDualPane,
          isActive = isActive,
        )
      }
    }

    // Scrollbar with bottom padding
    if (folders.isNotEmpty() && scrollbarAlpha > 0.01f) {
      ExpressiveScrollBar(
        gridState = gridState,
        dragLabelProvider = { index ->
          fastScrollGlyph(folders.getOrNull(index)?.name)
        },
        modifier =
          Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 2.dp, top = 6.dp, bottom = navigationBarHeight + 6.dp)
            .graphicsLayer { alpha = scrollbarAlpha },
      )
    }
  }
}

@Composable
private fun ListContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<FolderWithNewCount>,
  pinnedFolderPaths: Set<String>,
  recentlyPlayedFilePath: String?,
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  listState: LazyListState,
  scrollbarAlpha: Float,
  selectionManager: io.alwayszihan.zeroxpv.ui.browser.selection.SelectionManager<VideoFolder, String>,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
  onTogglePin: (VideoFolder) -> Unit,
  selectedFolderBucketId: String? = null,
) {
  val configuration = androidx.compose.ui.platform.LocalConfiguration.current
  val isTablet = configuration.smallestScreenWidthDp >= 600
  val browserPreferences = org.koin.compose.koinInject<io.alwayszihan.zeroxpv.preferences.BrowserPreferences>()
  val dualPaneForTablet by browserPreferences.dualPaneForTablet.collectAsState()
  val isDualPaneActive = isTablet && dualPaneForTablet

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        bottom = navigationBarHeight
      ),
    ) {
      items(folders) { folder ->
        val isRecentlyPlayed = recentlyPlayedFilePath?.let { filePath ->
          val file = File(filePath)
          file.parent == folder.path
        } ?: false

        val newCount = foldersWithNewCount
          .find { it.folder.bucketId == folder.bucketId }
          ?.newVideoCount ?: 0

        val isActive = isDualPaneActive && folder.bucketId == selectedFolderBucketId

        FolderCard(
          folder = folder,
          isSelected = selectionManager.isSelected(folder),
          isRecentlyPlayed = isRecentlyPlayed,
          onClick = { onFolderClick(folder) },
          onLongClick = { onFolderLongClick(folder) },
          onThumbClick = if (tapThumbnailToSelect) {
            { selectionManager.toggle(folder) }
          } else {
            { onFolderClick(folder) }
          },
          newVideoCount = newCount,
          isGridMode = false,
          isPinned = folder.path in pinnedFolderPaths,
          onPinClick =
            if (!selectionManager.isInSelectionMode) {
              { onTogglePin(folder) }
            } else {
              null
            },
          customChipContent =
            if (folder.path in pinnedFolderPaths) {
              {
                Text(
                  "Pinned",
                  style = MaterialTheme.typography.labelSmall,
                  modifier =
                    Modifier
                      .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp),
                      )
                      .padding(horizontal = 8.dp, vertical = 4.dp),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            } else {
              null
            },
          isDualPane = isDualPaneActive && selectedFolderBucketId != null,
          isActive = isActive,
        )
      }
    }

    // Scrollbar with bottom padding
    if (folders.isNotEmpty() && scrollbarAlpha > 0.01f) {
      ExpressiveScrollBar(
        listState = listState,
        dragLabelProvider = { index ->
          fastScrollGlyph(folders.getOrNull(index)?.name)
        },
        modifier =
          Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 2.dp, top = 6.dp, bottom = navigationBarHeight + 6.dp)
            .graphicsLayer { alpha = scrollbarAlpha },
      )
    }
  }
}



/**
 * Displays search results based on the user's layout preference (grid or list)
 */
@Composable
private fun SearchResultsContent(
  searchResults: List<FileSystemItem>,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  onFolderClick: (io.alwayszihan.zeroxpv.domain.media.model.VideoFolder) -> Unit,
  onVideoClick: (io.alwayszihan.zeroxpv.domain.media.model.Video) -> Unit,
  mediaLayoutMode: io.alwayszihan.zeroxpv.preferences.MediaLayoutMode,
) {
  val folders = searchResults.filterIsInstance<FileSystemItem.Folder>().map { folder ->
    io.alwayszihan.zeroxpv.domain.media.model.VideoFolder(
      bucketId = folder.path,  // Use path as bucketId since FileSystemItem.Folder doesn't have bucketId
      name = folder.name,
      path = folder.path,
      videoCount = folder.videoCount,
      totalSize = folder.totalSize,
      totalDuration = folder.totalDuration,
      lastModified = folder.lastModified
    )
  }
  val videos = searchResults.filterIsInstance<FileSystemItem.VideoFile>().map { it.video }
  val browserPreferences = koinInject<BrowserPreferences>()
  val appearancePreferences = koinInject<AppearancePreferences>()
  val showVideoThumbnails by browserPreferences.showVideoThumbnails.collectAsState()
  val showSizeChip by browserPreferences.showSizeChip.collectAsState()
  val showResolutionChip by browserPreferences.showResolutionChip.collectAsState()
  val showFramerateInResolution by browserPreferences.showFramerateInResolution.collectAsState()
  val showProgressBar by browserPreferences.showProgressBar.collectAsState()
  val showDateChip by browserPreferences.showDateChip.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val showUnplayedOldVideoLabel by appearancePreferences.showUnplayedOldVideoLabel.collectAsState()
  val unplayedOldVideoDays by appearancePreferences.unplayedOldVideoDays.collectAsState()
  val showExtensionField by browserPreferences.showExtensionField.collectAsState()
  val showDurationField by browserPreferences.showDurationField.collectAsState()
  val centerGridTitles by browserPreferences.centerGridTitles.collectAsState()
  val videoCardUiConfig =
    remember(
      unlimitedNameLines,
      showVideoThumbnails,
      showSizeChip,
      showResolutionChip,
      showFramerateInResolution,
      showProgressBar,
      showDateChip,
      showUnplayedOldVideoLabel,
      unplayedOldVideoDays,
      showExtensionField,
      showDurationField,
      centerGridTitles,
    ) {
      VideoCardUiConfig(
        unlimitedNameLines = unlimitedNameLines,
        showThumbnails = showVideoThumbnails,
        showSizeChip = showSizeChip,
        showResolutionChip = showResolutionChip,
        showFramerateInResolution = showFramerateInResolution,
        showProgressBar = showProgressBar,
        showDateChip = showDateChip,
        showUnplayedOldVideoLabel = showUnplayedOldVideoLabel,
        unplayedOldVideoDays = unplayedOldVideoDays,
        showExtensionField = showExtensionField,
        showDurationField = showDurationField,
        centerGridTitles = centerGridTitles,
      )
    }
  
  val isGridMode = mediaLayoutMode == io.alwayszihan.zeroxpv.preferences.MediaLayoutMode.GRID
  
  Box(modifier = Modifier.fillMaxSize()) {
    if (isGridMode) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val spansInfo = calculateResponsiveGridSpans(
          maxWidth = maxWidth,
          isGridMode = true
        )
        LazyVerticalGrid(
          columns = GridCells.Fixed(spansInfo.spans),
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = navigationBarHeight + 8.dp
          ),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(
            count = folders.size,
            key = { index -> folders[index].bucketId },
            span = { GridItemSpan(spansInfo.folderSpan) }
          ) { index ->
            val folder = folders[index]
            FolderCard(
              folder = folder,
              isSelected = false,
              isRecentlyPlayed = false,
              onClick = { onFolderClick(folder) },
              onLongClick = {},
              onThumbClick = { onFolderClick(folder) },
              newVideoCount = 0,
              isGridMode = true,
            )
          }
          
          items(
            count = videos.size,
            key = { index -> videos[index].id },
            span = { GridItemSpan(spansInfo.videoSpan) }
          ) { index ->
            val video = videos[index]
            VideoCard(
              video = video,
              isSelected = false,
              onClick = { onVideoClick(video) },
              onLongClick = {},
              onThumbClick = { onVideoClick(video) },
              isGridMode = true,
              showSubtitleIndicator = showSubtitleIndicator,
              uiConfig = videoCardUiConfig,
            )
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = 8.dp,
          end = 8.dp,
          top = 8.dp,
          bottom = navigationBarHeight + 8.dp
        ),
      ) {
        items(count = folders.size, key = { index -> folders[index].bucketId }) { index ->
          val folder = folders[index]
          FolderCard(
            folder = folder,
            isSelected = false,
            isRecentlyPlayed = false,
            onClick = { onFolderClick(folder) },
            onLongClick = {},
            onThumbClick = { onFolderClick(folder) },
            newVideoCount = 0,
            isGridMode = false,
          )
        }
        
        items(count = videos.size, key = { index -> videos[index].id }) { index ->
          val video = videos[index]
          VideoCard(
            video = video,
            isSelected = false,
            onClick = { onVideoClick(video) },
            onLongClick = {},
            onThumbClick = { onVideoClick(video) },
            isGridMode = false,
            showSubtitleIndicator = showSubtitleIndicator,
            uiConfig = videoCardUiConfig,
          )
        }
      }
    }
  }
}


/**
 * Searches for folders and videos matching the query
 * Returns FileSystemItem results containing matching folders and videos
 */

object SearchManager {
  val engine = MediaSearchEngine
}

private suspend fun buildSearchIndex(context: Context) {
  val folders = io.alwayszihan.zeroxpv.repository.MediaFileRepository.getAllVideoFoldersFast(context)
  val videosByFolder = folders.associate { folder ->
    folder.bucketId to io.alwayszihan.zeroxpv.repository.MediaFileRepository.getVideosInFolder(context, folder.bucketId)
  }
  SearchManager.engine.buildIndex(folders, videosByFolder)
}

private suspend fun searchFoldersAndVideos(
  context: Context,
  query: String,
): List<FileSystemItem> {
  val results = mutableListOf<FileSystemItem>()
  try {
    Log.d("FolderListScreen", "Searching for: $query")
    
    // Get all search matches from the optimized engine
    val matches = SearchManager.engine.search(query, limit = 50)

    for (item in matches) {
      when (item) {
        // Kotlin smart-casts 'item' to your domain model VideoFolder
        is VideoFolder -> {
          results.add(
            FileSystemItem.Folder(
              name = item.name,
              path = item.path,
              lastModified = item.lastModified,
              videoCount = item.videoCount,
              totalSize = item.totalSize,
              totalDuration = item.totalDuration,
            )
          )
        }
        // Kotlin smart-casts 'item' to your domain model Video
        is Video -> {
          results.add(
            FileSystemItem.VideoFile(
              name = item.displayName,
              path = item.path,
              lastModified = item.dateModified,
              video = item,
            )
          )
        }
      }
    }

    Log.d("FolderListScreen", "Found ${results.size} results for: $query")
  } catch (e: Exception) {
    Log.e("FolderListScreen", "Error searching folders and videos", e)
  }
  return results
}



