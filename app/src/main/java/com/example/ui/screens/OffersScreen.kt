@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CoffeeShop
import com.example.model.CouponDiscountType
import com.example.model.CouponDoc
import com.example.model.CouponRedemptionDoc
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.theme.CaramelContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.util.BangladeshTimeUtil
import com.example.util.PlatformLogic
import kotlinx.coroutines.launch

@Composable
fun OffersScreen(
    coupons: List<CouponDoc>,
    redemptions: Map<String, CouponRedemptionDoc>,
    isOfflineCached: Boolean,
    isSignedIn: Boolean,
    canCreateOffers: Boolean,
    shops: List<CoffeeShop>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onRequireSignIn: () -> Unit,
    onSaveCoupon: suspend (CouponDoc) -> Result<Unit>,
    onRedeemCoupon: suspend (String) -> Result<String>,
    onCreateCoupon: suspend (
        shopId: String,
        shopName: String,
        code: String,
        title: String,
        description: String,
        type: CouponDiscountType,
        value: Long,
        minSpend: Long,
        maxLimit: Long,
        terms: String,
        validDays: Int
    ) -> Result<Unit>,
    onOpenShop: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var showActiveTab by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val (activeCoupons, expiredCoupons) = remember(coupons) {
        PlatformLogic.splitCoupons(coupons)
    }
    val displayed = if (showActiveTab) activeCoupons else expiredCoupons

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
                    title = stringResource(R.string.offers_title),
                    subtitle = if (isOfflineCached) "Showing offline cached offers" else stringResource(R.string.offers_subtitle),
                    onBack = onBack,
                    trailingAction = {
                        if (canCreateOffers && shops.isNotEmpty()) {
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CaramelGold,
                                    contentColor = DarkEspressoInk
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("offers_create_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("New", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = "${stringResource(R.string.offers_tab_active)} (${activeCoupons.size})",
                        selected = showActiveTab,
                        onClick = { showActiveTab = true },
                        modifier = Modifier.testTag("offers_tab_active")
                    )
                    LiquidGlassChip(
                        label = "${stringResource(R.string.offers_tab_expired)} (${expiredCoupons.size})",
                        selected = !showActiveTab,
                        onClick = { showActiveTab = false },
                        modifier = Modifier.testTag("offers_tab_expired")
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("offers_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (displayed.isEmpty()) {
                        item(key = "offers_empty") {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalOffer,
                                        contentDescription = null,
                                        tint = CaramelGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (showActiveTab) {
                                            stringResource(R.string.offers_empty)
                                        } else {
                                            "No expired coupons."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ToastedLatte
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayed, key = { it.id }) { coupon ->
                            val redemption = redemptions[coupon.id]
                            val isRedeemed = redemption?.status == "REDEEMED"
                            val isSaved = redemption != null
                            CouponCard(
                                coupon = coupon,
                                isActiveTab = showActiveTab,
                                isSaved = isSaved,
                                isRedeemed = isRedeemed,
                                isBusy = isBusy,
                                onCopyCode = {
                                    clipboard.setText(AnnotatedString(coupon.code))
                                    scope.launch { snackbarHostState.showSnackbar("Copied ${coupon.code}") }
                                },
                                onSave = {
                                    if (!isSignedIn) {
                                        onRequireSignIn()
                                    } else {
                                        scope.launch {
                                            val res = onSaveCoupon(coupon)
                                            snackbarHostState.showSnackbar(
                                                res.fold(
                                                    onSuccess = { "Offer saved to your account" },
                                                    onFailure = { it.message ?: "Failed to save offer" }
                                                )
                                            )
                                        }
                                    }
                                },
                                onRedeem = {
                                    if (!isSignedIn) {
                                        onRequireSignIn()
                                    } else {
                                        scope.launch {
                                            val res = onRedeemCoupon(coupon.id)
                                            snackbarHostState.showSnackbar(
                                                res.fold(
                                                    onSuccess = { it },
                                                    onFailure = { it.message ?: "Redemption failed" }
                                                )
                                            )
                                        }
                                    }
                                },
                                onOpenShop = { onOpenShop(coupon.shopId) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }
    }

    if (showCreateDialog && shops.isNotEmpty()) {
        CreateCouponDialog(
            shops = shops,
            isBusy = isBusy,
            onDismiss = { showCreateDialog = false },
            onSubmit = { shopId, shopName, code, title, desc, type, value, minSpend, maxLimit, terms, days ->
                scope.launch {
                    val res = onCreateCoupon(
                        shopId, shopName, code, title, desc, type, value, minSpend, maxLimit, terms, days
                    )
                    if (res.isSuccess) {
                        showCreateDialog = false
                        snackbarHostState.showSnackbar("Coupon published")
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to publish coupon")
                    }
                }
            }
        )
    }
}

@Composable
private fun CouponCard(
    coupon: CouponDoc,
    isActiveTab: Boolean,
    isSaved: Boolean,
    isRedeemed: Boolean,
    isBusy: Boolean,
    onCopyCode: () -> Unit,
    onSave: () -> Unit,
    onRedeem: () -> Unit,
    onOpenShop: () -> Unit
) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        accentGlow = isActiveTab
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CaramelContainer,
                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = coupon.formattedDiscount(),
                        style = MaterialTheme.typography.labelLarge,
                        color = CaramelGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                if (isRedeemed) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = OpenGreenContainer,
                        border = BorderStroke(1.dp, OpenGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = OpenGreen, modifier = Modifier.size(14.dp))
                            Text(stringResource(R.string.offers_redeemed), style = MaterialTheme.typography.labelSmall, color = OpenGreen)
                        }
                    }
                } else if (!isActiveTab) {
                    Text("Expired / Exhausted", style = MaterialTheme.typography.labelSmall, color = ClosedRed)
                }
            }

            Text(
                text = coupon.title,
                style = MaterialTheme.typography.titleMedium,
                color = WarmCream,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = onOpenShop,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = coupon.shopName, color = CaramelGold, fontWeight = FontWeight.SemiBold)
            }

            if (coupon.description.isNotBlank()) {
                Text(
                    text = coupon.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ToastedLatte
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DeepRoastSurface,
                border = BorderStroke(1.dp, CocoaBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PROMO CODE", style = MaterialTheme.typography.labelSmall, color = MutedCinnamon)
                        Text(coupon.code, style = MaterialTheme.typography.titleMedium, color = WarmCream, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = onCopyCode,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy code", tint = CaramelGold)
                    }
                }
            }

            val expiryText = BangladeshTimeUtil.formatDhakaDateTime(coupon.expiresAt?.toDate()?.time)
            Text(
                text = buildString {
                    append("Uses: ${coupon.redeemedCount}/${coupon.maxUsageLimit}")
                    if (coupon.minSpendBdt > 0) append(" · Min spend ৳${coupon.minSpendBdt}")
                    if (expiryText.isNotBlank()) append("\nExpires: $expiryText")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MutedCinnamon
            )

            if (coupon.terms.isNotBlank()) {
                HorizontalDivider(color = CocoaBorder)
                Text(
                    text = "Terms: ${coupon.terms}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ToastedLatte
                )
            }

            if (isActiveTab && !isRedeemed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSave,
                        enabled = !isBusy && !isSaved,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CocoaBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("coupon_save_${coupon.id}")
                    ) {
                        Icon(Icons.Rounded.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(if (isSaved) " Saved" else " ${stringResource(R.string.offers_save)}")
                    }
                    Button(
                        onClick = onRedeem,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CaramelGold,
                            contentColor = DarkEspressoInk
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("coupon_redeem_${coupon.id}")
                    ) {
                        Text(stringResource(R.string.offers_redeem), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCouponDialog(
    shops: List<CoffeeShop>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, CouponDiscountType, Long, Long, Long, String, Int) -> Unit
) {
    var selectedShop by remember { mutableStateOf(shops.first()) }
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf(CouponDiscountType.PERCENTAGE) }
    var valueText by remember { mutableStateOf("15") }
    var minSpendText by remember { mutableStateOf("300") }
    var limitText by remember { mutableStateOf("100") }
    var terms by remember { mutableStateOf("Valid for dine-in orders after 8 PM.") }
    var validDays by remember { mutableIntStateOf(14) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoastedBeanElevated,
        title = { Text("Publish Cafe Offer", color = WarmCream) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cafe: ${selectedShop.name}", style = MaterialTheme.typography.labelLarge, color = CaramelGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = discountType == CouponDiscountType.PERCENTAGE,
                        onClick = { discountType = CouponDiscountType.PERCENTAGE },
                        label = { Text("% Off") }
                    )
                    FilterChip(
                        selected = discountType == CouponDiscountType.FIXED_BDT,
                        onClick = { discountType = CouponDiscountType.FIXED_BDT },
                        label = { Text("৳ Fixed") }
                    )
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 24) code = it.uppercase() },
                    label = { Text("Code (e.g. MIDNIGHT15) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = { Text("Offer Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 300) description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Discount *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minSpendText,
                        onValueChange = { minSpendText = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Min ৳") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Limit *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = terms,
                    onValueChange = { if (it.length <= 500) terms = it },
                    label = { Text("Terms & Conditions") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && code.length >= 3 && title.length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                onClick = {
                    onSubmit(
                        selectedShop.id,
                        selectedShop.name,
                        code,
                        title,
                        description,
                        discountType,
                        valueText.toLongOrNull() ?: 10L,
                        minSpendText.toLongOrNull() ?: 0L,
                        limitText.toLongOrNull() ?: 50L,
                        terms,
                        validDays
                    )
                }
            ) {
                Text("Publish", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MutedCinnamon) }
        }
    )
}
