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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppTeal

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
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = WhatsAppTeal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_lead")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_lead),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Search Bar & Sort Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_leads_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_leads_hint),
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
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
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .testTag("button_sort_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = WhatsAppTeal
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOrder.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(sort.titleRes),
                                        fontWeight = if (selectedSortOrder == sort) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSortOrder == sort) WhatsAppTeal else TextPrimary
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

            // Filter Chips Scrollable Row (Status filters)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All Statuses" Chip
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusFilterChange(null) },
                    label = {
                        Text(
                            text = "${stringResource(R.string.filter_all)} ($totalCount)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedStatus == null) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WhatsAppTeal,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_status_all")
                )

                // Individual Status Chips
                LeadStatus.entries.forEach { status ->
                    val isSelected = selectedStatus == status
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onStatusFilterChange(null) else onStatusFilterChange(status)
                        },
                        label = {
                            Text(
                                text = stringResource(status.titleRes),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(status.badgeTextColor),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_status_${status.name.lowercase()}")
                    )
                }
            }

            // Secondary Filter Row: Source & Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date filter dropdown or chips
                DateFilter.entries.forEach { dateFilter ->
                    val isSelected = selectedDateFilter == dateFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onDateFilterChange(dateFilter) },
                        label = {
                            Text(
                                text = stringResource(dateFilter.titleRes),
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F766E),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_date_${dateFilter.name.lowercase()}")
                    )
                }

                // Sources chips
                LeadSource.entries.forEach { source ->
                    val isSelected = selectedSource == source
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onSourceFilterChange(null) else onSourceFilterChange(source)
                        },
                        label = {
                            Text(
                                text = stringResource(source.titleRes),
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4338CA),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_source_${source.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Leads List or Empty State
            if (leads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedStatus != null || selectedSource != null || selectedDateFilter != DateFilter.ALL) {
                                "لا توجد نتائج مطابقة للبحث"
                            } else {
                                stringResource(R.string.empty_leads_title)
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedStatus != null || selectedSource != null || selectedDateFilter != DateFilter.ALL) {
                                "جرّب تغيير كلمات البحث أو إعادة ضبط الفلاتر"
                            } else {
                                stringResource(R.string.empty_leads_desc)
                            },
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (searchQuery.isNotEmpty() || selectedStatus != null || selectedSource != null || selectedDateFilter != DateFilter.ALL) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    onSearchChange("")
                                    onStatusFilterChange(null)
                                    onSourceFilterChange(null)
                                    onDateFilterChange(DateFilter.ALL)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("إعادة ضبط الفلاتر")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.add_lead))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("leads_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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

    if (showAddDialog) {
        AddLeadDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { phone, name, source, status, notes, tags ->
                showAddDialog = false
                onCreateLead(phone, name, source, status, notes, tags)
            }
        )
    }

    leadToChangeStatus?.let { lead ->
        ChangeStatusDialog(
            currentStatus = LeadStatus.fromString(lead.status),
            onDismiss = { leadToChangeStatus = null },
            onSelectStatus = { newStatus ->
                val id = lead.id
                leadToChangeStatus = null
                onUpdateStatus(id, newStatus)
            }
        )
    }
}
