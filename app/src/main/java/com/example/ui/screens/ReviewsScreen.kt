@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CoffeeShop
import com.example.model.PhotoReviewDoc
import com.example.ui.components.InteractiveStarSelector
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.components.StarRatingRow
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.util.BangladeshTimeUtil
import kotlinx.coroutines.launch

@Composable
fun ReviewsScreen(
    shop: CoffeeShop?,
    reviews: List<PhotoReviewDoc>,
    myVotedReviewIds: Set<String>,
    currentUserId: String?,
    isSignedIn: Boolean,
    isBusy: Boolean,
    onBack: () -> Unit,
    onRequireSignIn: () -> Unit,
    onSubmitReview: suspend (rating: Int, comment: String, uris: List<Uri>) -> Result<Unit>,
    onToggleHelpful: suspend (String) -> Result<Unit>,
    onReportReview: suspend (reviewId: String, reason: String) -> Result<Unit>,
    onDeleteReview: suspend (String) -> Result<Unit>
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }
    var reportingReviewId by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        if (uris.isNotEmpty()) {
            pickedUris = (pickedUris + uris).distinct().take(3)
        }
    }

    Scaffold(
        containerColor = EspressoBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(EspressoRich.copy(alpha = 0.25f), EspressoBlack, EspressoBlack)
                    )
                )
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                PlatformTopBar(
                    title = stringResource(R.string.reviews_screen_title),
                    subtitle = shop?.name ?: "Dhaka Community",
                    onBack = onBack
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("photo_reviews_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "photo_review_composer") {
                        LiquidGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            accentGlow = true
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Share Your Visit & Photos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WarmCream
                                )
                                InteractiveStarSelector(
                                    selectedRating = rating,
                                    onSelectRating = { rating = it }
                                )
                                OutlinedTextField(
                                    value = comment,
                                    onValueChange = { if (it.length <= 800) comment = it },
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.reviews_comment_hint),
                                            color = MutedCinnamon
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 5,
                                    shape = RoundedCornerShape(18.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DeepRoastSurface,
                                        unfocusedContainerColor = DeepRoastSurface,
                                        focusedBorderColor = CaramelGold.copy(alpha = 0.7f),
                                        unfocusedBorderColor = CocoaBorder,
                                        focusedTextColor = WarmCream,
                                        unfocusedTextColor = WarmCream
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("photo_review_comment_input")
                                )

                                if (pickedUris.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        pickedUris.forEach { uri ->
                                            Box {
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = "Selected review photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(76.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                )
                                                Surface(
                                                    shape = CircleShape,
                                                    color = EspressoBlack.copy(alpha = 0.85f),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(26.dp)
                                                        .clickable { pickedUris = pickedUris - uri }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Remove photo",
                                                        tint = WarmCream,
                                                        modifier = Modifier.padding(4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (!isSignedIn) {
                                                onRequireSignIn()
                                            } else {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        },
                                        enabled = !isBusy && pickedUris.size < 3,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("pick_review_photo_button")
                                    ) {
                                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = CaramelGold)
                                        Text(" ${stringResource(R.string.reviews_add_photo)} (${pickedUris.size}/3)", color = CaramelGold)
                                    }

                                    Button(
                                        onClick = {
                                            if (!isSignedIn) {
                                                onRequireSignIn()
                                            } else {
                                                scope.launch {
                                                    val res = onSubmitReview(rating, comment.trim(), pickedUris)
                                                    if (res.isSuccess) {
                                                        comment = ""
                                                        pickedUris = emptyList()
                                                        rating = 5
                                                        snackbarHostState.showSnackbar("Photo review published")
                                                    } else {
                                                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Upload failed")
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isBusy && (!isSignedIn || comment.isNotBlank()),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CaramelGold,
                                            contentColor = DarkEspressoInk
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("submit_photo_review_button")
                                    ) {
                                        if (isBusy) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkEspressoInk, strokeWidth = 2.dp)
                                        } else {
                                            Text(
                                                text = if (isSignedIn) "Post" else "Sign In",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (reviews.isEmpty()) {
                        item(key = "photo_reviews_empty") {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.reviews_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ToastedLatte,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(reviews, key = { it.id }) { rev ->
                            val isOwned = currentUserId != null && rev.userId == currentUserId
                            val isVoted = rev.id in myVotedReviewIds
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rev.authorName.ifBlank { "Dhaka Coffee Lover" },
                                                style = MaterialTheme.typography.titleSmall,
                                                color = WarmCream,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = BangladeshTimeUtil.formatRelativeTime(rev.createdAt?.toDate()?.time),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MutedCinnamon
                                            )
                                        }
                                        if (isOwned) {
                                            IconButton(
                                                onClick = { scope.launch { onDeleteReview(rev.id) } },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete review", tint = ClosedRed)
                                            }
                                        }
                                    }

                                    StarRatingRow(rating = rev.rating.toDouble())
                                    Text(text = rev.comment, style = MaterialTheme.typography.bodyMedium, color = ToastedLatte)

                                    if (rev.photoUrls.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rev.photoUrls.forEach { url ->
                                                AsyncImage(
                                                    model = url,
                                                    contentDescription = "Customer cafe photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(96.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable { previewPhotoUrl = url }
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                if (!isSignedIn) {
                                                    onRequireSignIn()
                                                } else {
                                                    scope.launch { onToggleHelpful(rev.id) }
                                                }
                                            },
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.ThumbUp,
                                                contentDescription = null,
                                                tint = if (isVoted) CaramelGold else ToastedLatte,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = " " + stringResource(R.string.reviews_helpful, rev.helpfulCount.toInt()),
                                                color = if (isVoted) CaramelGold else ToastedLatte
                                            )
                                        }

                                        if (!isOwned) {
                                            TextButton(
                                                onClick = {
                                                    if (!isSignedIn) {
                                                        onRequireSignIn()
                                                    } else {
                                                        reportReason = ""
                                                        reportingReviewId = rev.id
                                                    }
                                                },
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Flag, contentDescription = null, tint = MutedCinnamon, modifier = Modifier.size(15.dp))
                                                Text(" ${stringResource(R.string.reviews_report)}", color = MutedCinnamon)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }
    }

    previewPhotoUrl?.let { url ->
        Dialog(onDismissRequest = { previewPhotoUrl = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = EspressoBlack,
                border = BorderStroke(1.dp, CocoaBorder)
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Full photo preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    IconButton(
                        onClick = { previewPhotoUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close preview", tint = WarmCream)
                    }
                }
            }
        }
    }

    reportingReviewId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { reportingReviewId = null },
            containerColor = RoastedBeanElevated,
            title = { Text("Report Review", color = WarmCream) },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { if (it.length <= 200) reportReason = it },
                    label = { Text("Why is this review inappropriate or misleading?") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = reportReason.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                    onClick = {
                        scope.launch {
                            val res = onReportReview(targetId, reportReason)
                            reportingReviewId = null
                            snackbarHostState.showSnackbar(
                                res.fold(
                                    onSuccess = { "Review flagged for moderation" },
                                    onFailure = { it.message ?: "Unable to submit report" }
                                )
                            )
                        }
                    }
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportingReviewId = null }) { Text("Cancel", color = MutedCinnamon) }
            }
        )
    }
}
