package com.android.attendanceapp.presentation.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.attendanceapp.MainViewModel
import com.android.attendanceapp.R
import com.android.attendanceapp.database.TodaysEntryWithUser
import com.android.attendanceapp.domain.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: MainViewModel, padding: PaddingValues, navController: NavController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val dataList by viewModel.todayDataList.collectAsState()
    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var selectedDate by remember {
        mutableStateOf("${Utils.getDateTime(Utils.getDateRange().first)}   -   ${Utils.getDateTime(Utils.getDateRange().second)}")
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.getTodayReport(Utils.getDateRange())
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding()),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    Icon(
                        painter = painterResource(id = R.drawable.calendar),
                        contentDescription = "",
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .clip(CircleShape)
                            .clickable {
                                showDatePicker = !showDatePicker
                            }
                    )
                },
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text(
                        text = "Report",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 25.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_back),
                        contentDescription = "",
                        modifier = Modifier
                            .padding(start = 15.dp)
                            .clickable {
                                navController.popBackStack()
                            }
                    )
                })
        }
    ) { paddingValues ->
        if (showDatePicker) {
            DateRangePickerModal(onDateRangeSelected = {
                if (it.first != null) {
                    val date =
                        Utils.getDateRange(it.first!!, it.second ?: System.currentTimeMillis())
                    Log.e("Test", Utils.getDateTime(1718064360000))
                    Log.e("Test", Utils.getDateTime(date.second))
                    selectedDate =
                        "${Utils.getDateTime(date.first)}   -   ${Utils.getDateTime(date.second)}"
                    viewModel.getTodayReport(date)
                }

            }) {
                showDatePicker = !showDatePicker
            }
        }
        if (dataList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO DATA AVAILABLE",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight(600),
                        letterSpacing = 2.sp
                    )
                )
            }
        }
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            Text(
                text = selectedDate,
                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray
                )
            )
            LazyColumn(modifier = Modifier) {
                items(dataList) {
                    ReportCard(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerModal(
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(
                        Pair(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Select date range"
                )
            },
            showModeToggle = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp)
        )
    }
}

@Composable
fun ReportCard(todaysEntryWithUser: TodaysEntryWithUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = Utils.byteArrayToBitmap(todaysEntryWithUser.imageData)?.asImageBitmap()
                ?: ImageBitmap(10, 10),
            contentDescription = "",
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(15.dp))
        Text(text = todaysEntryWithUser.Name)
        Spacer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        Text(
            text = if (todaysEntryWithUser.isEntry) "IN" else "OUT",
            textAlign = TextAlign.Center,
            modifier = Modifier.width(100.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = Utils.getTime(todaysEntryWithUser.time))
        Spacer(modifier = Modifier.width(10.dp))
    }
}
