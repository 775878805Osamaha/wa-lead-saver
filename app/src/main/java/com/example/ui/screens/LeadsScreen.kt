package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.entity.LeadEntity
import com.example.data.model.DateFilter
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.data.model.SortOrder
import com.example.ui.components.CrmLeadCard
import com.example.ui.dialogs.AddLeadDialog
import com.example.ui.dialogs.ChangeStatusDialog
import com.example.ui.theme.CrmBackground
import com.example.ui.theme.CrmBorder
import com.example.ui.theme.CrmCardSurface
import com.example.ui.theme.CrmHeader
import com.example.ui.theme.CrmPaleEmerald
import com.example.ui.theme.CrmPrimary
import com.example.ui.theme.CrmTextMuted
import com.example.ui.theme.CrmTextPrimary
import com.example.ui.theme.CrmTextSecondary

/**
 * LeadsScreen - Section 4 Strict Specification
 * 1. Compact emerald app bar:
 *    - Title: "العملاء"
 *    - Subtitle: "إدارة العملاء المحتملين من واتساب"
 *    - Search & Filter action
 * 2. Below header:
 *    - White search field with visible #E3E9EF border
 *    - Compact horizontal row of rounded filter chips: "الكل", "جديد", "متابعة", "مهتم"
 *    - Small result count
 * 3. Separate white rounded customer cards with subtle border
 * 4. Floating emerald action button for adding leads
 */
@Composable
fun LeadsScreen(
    leads: List<LeadEntity>,
    totalCount: Int,
    searchQuery: String,
    selectedStatus: LeadStatus?,
    selectedSource: LeadSource?,
    selectedDateFilter: DateFilter,
    selectedSortOrder: SortOrder,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (LeadStatus?) -> Unit,
    onSourceFilterChange: (LeadSource?) -> Unit,
    onDateFilterChange: (DateFilter) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onLeadClick: (LeadEntity) -> Unit,
    onCallClick: (LeadEntity) -> Unit,
    onWhatsAppClick: (LeadEntity) -> Unit,
    onUpdateStatus: (id: Long, LeadStatus) -> Unit,
    onCreateLead: (phone: String, name: String, source: LeadSource, status: LeadStatus, notes: String, tags: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var leadToChangeStatus by remember { mutableStateOf<LeadEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CrmPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_lead")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_lead),
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        containerColor = CrmBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CrmBackground)
        ) {
            // Compact in-screen header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "العملاء",
                        color = CrmTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrmPaleEmerald)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$totalCount",
                            color = CrmPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sort Action Button
                Box {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("button_sort_menu"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CrmBorder),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = CrmCardSurface,
                            contentColor = CrmTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = CrmPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(selectedSortOrder.titleRes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CrmTextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(CrmCardSurface)
                    ) {
                        SortOrder.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(sort.titleRes),
                                        fontWeight = if (selectedSortOrder == sort) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSortOrder == sort) CrmPrimary else CrmTextPrimary
                                    )
                                },
                                onClick = {
                                    onSortOrderChange(sort)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 1. White search field with visible border #E3E9EF
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_leads_input"),
                    placeholder = {
                        Text(
                            text = "بحث بالاسم أو الرقم...",
                            fontSize = 13.sp,
                            color = CrmTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CrmPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.testTag("button_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = CrmTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CrmTextPrimary,
                        unfocusedTextColor = CrmTextPrimary,
                        focusedBorderColor = CrmPrimary,
                        unfocusedBorderColor = CrmBorder,
                        focusedContainerColor = CrmCardSurface,
                        unfocusedContainerColor = CrmCardSurface
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Compact horizontal row of rounded filter chips: "الكل", "جديد", "متابعة", "مهتم"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "الكل" Chip
                val isAllSelected = selectedStatus == null
                FilterChip(
                    selected = isAllSelected,
                    onClick = { onStatusFilterChange(null) },
                    label = {
                        Text(
                            text = "الكل ($totalCount)",
                            fontSize = 12.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFFF1F5F9),
                        labelColor = CrmTextPrimary,
                        selectedContainerColor = CrmPrimary,
                        selectedLabelColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isAllSelected) CrmPrimary else CrmBorder
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("filter_chip_status_all")
                )

                // Mandatory status chips: جديد, متابعة, مهتم
                val keyStatuses = listOf(
                    LeadStatus.NEW to "جديد",
                    LeadStatus.FOLLOW_UP to "متابعة",
                    LeadStatus.INTERESTED to "مهتم",
                    LeadStatus.SOLD to "تم البيع",
                    LeadStatus.CONTACTED to "تم التواصل"
                )

                keyStatuses.forEach { (status, label) ->
                    val isSelected = selectedStatus == status
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onStatusFilterChange(null) else onStatusFilterChange(status)
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = CrmTextPrimary,
                            selectedContainerColor = CrmPrimary,
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CrmPrimary else CrmBorder
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("filter_chip_status_${status.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Small result count row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عرض ${leads.size} عميل",
                    color = CrmTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                if (searchQuery.isNotEmpty() || selectedStatus != null) {
                    Text(
                        text = "فلترة نشطة",
                        color = CrmPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Customer List or Empty State
            if (leads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CrmCardSurface),
                        border = BorderStroke(1.dp, CrmBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(CrmPaleEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = CrmPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedStatus != null) {
                                    "لا توجد نتائج مطابقة للبحث"
                                } else {
                                    "قائمة العملاء فارغة حتى الآن"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrmTextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedStatus != null) {
                                    "جرّب تغيير كلمات البحث أو إعادة ضبط الفلتر"
                                } else {
                                    "سيتم إدراج العملاء تلقائياً عند استلام إشعارات واتساب أو عبر الإضافة اليدوية."
                                },
                                fontSize = 12.sp,
                                color = CrmTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = leads,
                        key = { it.id }
                    ) { lead ->
                        CrmLeadCard(
                            lead = lead,
                            onClick = { onLeadClick(lead) },
                            onCallClick = { onCallClick(lead) },
                            onWhatsAppClick = { onWhatsAppClick(lead) },
                            onChangeStatusClick = { leadToChangeStatus = lead }
                        )
                    }
                }
            }
        }
    }

    // Add Lead Dialog
    if (showAddDialog) {
        AddLeadDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { phone, name, source, status, notes, tags ->
                onCreateLead(phone, name, source, status, notes, tags)
                showAddDialog = false
            }
        )
    }

    // Change Status Dialog
    leadToChangeStatus?.let { lead ->
        ChangeStatusDialog(
            currentStatus = LeadStatus.fromString(lead.status),
            onDismiss = { leadToChangeStatus = null },
            onSelectStatus = { newStatus ->
                onUpdateStatus(lead.id, newStatus)
                leadToChangeStatus = null
            }
        )
    }
}
