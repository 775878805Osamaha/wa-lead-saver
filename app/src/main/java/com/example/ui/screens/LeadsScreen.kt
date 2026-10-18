package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.LeadEntity
import com.example.data.model.DateFilter
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.data.model.SortOrder
import com.example.ui.components.CrmLeadCard
import com.example.ui.dialogs.AddLeadDialog
import com.example.ui.dialogs.ChangeStatusDialog
import com.example.ui.theme.BrandGreenGradient
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorCardBackground
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorScreenBackground
import com.example.ui.theme.ColorTextPrimary
import com.example.ui.theme.ColorTextSecondary

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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .padding(bottom = 68.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrandGreenGradient)
                        .clickable { showAddDialog = true }
                        .testTag("fab_add_lead"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة عميل",
                        tint = Color(0xFF0C1512),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            containerColor = ColorScreenBackground,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(ColorScreenBackground)
            ) {
                // Header: Title "العملاء (X)" + Filter Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العملاء ($totalCount)",
                        color = ColorTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorCardBackground)
                            .border(1.dp, ColorCardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                // Toggle sort order or cycle
                                onSortOrderChange(
                                    if (selectedSortOrder == SortOrder.NEWEST) SortOrder.OLDEST else SortOrder.NEWEST
                                )
                            }
                            .testTag("leads_sort_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "فرز",
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Search Box (Card background, border #223029)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ColorCardBackground)
                        .border(1.dp, ColorCardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "بحث بالاسم أو الرقم...",
                                    color = ColorTextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                textStyle = TextStyle(
                                    color = ColorTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(ColorBrandGreen),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("leads_search_input")
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح",
                                tint = ColorTextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onSearchChange("") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal Filter Chips Row (Screenshot 2)
                val filterList = listOf(
                    null to "الكل ($totalCount)",
                    LeadStatus.NEW to "جديد",
                    LeadStatus.FOLLOW_UP to "متابعة",
                    LeadStatus.INTERESTED to "مهتم",
                    LeadStatus.CONTACTED to "تم التواصل",
                    LeadStatus.SOLD to "تم البيع"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    filterList.forEach { (status, label) ->
                        val isSelected = selectedStatus == status
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) ColorBrandGreen else ColorCardBackground
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) ColorBrandGreen else ColorCardBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { onStatusFilterChange(status) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("filter_chip_${status?.name ?: "all"}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF0C1512) else ColorTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clients List (LazyColumn with stable keys)
                if (leads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة للبحث" else "لا يوجد عملاء حالياً",
                            color = ColorTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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

            // Dialogs
            if (showAddDialog) {
                AddLeadDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { phone, name, source, status, notes, tags ->
                        onCreateLead(phone, name, source, status, notes, tags)
                        showAddDialog = false
                    }
                )
            }

            leadToChangeStatus?.let { targetLead ->
                ChangeStatusDialog(
                    currentStatus = LeadStatus.fromString(targetLead.status),
                    onDismiss = { leadToChangeStatus = null },
                    onSelectStatus = { newStatus ->
                        onUpdateStatus(targetLead.id, newStatus)
                        leadToChangeStatus = null
                    }
                )
            }
        }
    }
}
