package com.android.attendanceapp.presentation.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.android.attendanceapp.MainViewModel
import com.android.attendanceapp.R
import com.android.attendanceapp.database.UserEntity
import com.android.attendanceapp.domain.Utils
import com.android.attendanceapp.presentation.navigation.NavigationScreens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun AddUserScreen(viewModel: MainViewModel, navController: NavController, padding: PaddingValues) {
    var showDialog by remember {
        mutableStateOf(false)
    }
    var name by remember {
        mutableStateOf("")
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val bitmap = viewModel.getNewPersonData()
    Scaffold(
        modifier= Modifier
            .fillMaxSize()
            .background(Color.White),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding()),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text(
                        text = "Register",
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
    ) {paddingValues->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (showDialog) {
                Dialog(
                    onDismissRequest = {},
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    RegisterCard(
                        name = name,
                        time = System.currentTimeMillis(),
                        image = bitmap?.asImageBitmap() ?: ImageBitmap(10, 10)
                    ) {
                        showDialog = false
                        navController.navigate(NavigationScreens.MainScreen) {
                            launchSingleTop = true
                            popUpTo<NavigationScreens.MainScreen> { inclusive = false }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Image(
                    bitmap = bitmap?.asImageBitmap() ?: ImageBitmap(10, 10),
                    contentScale = ContentScale.Crop,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .padding(vertical = 20.dp)
                        .size(150.dp)
                        .clip(CircleShape)
                )
                ProfileDetailsCard(showSnack = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Please fill all required fields.",
                            duration = SnackbarDuration.Short
                        )
                    }

                }) {
                    viewModel.registerEmployee(it) {
                        name = it.name
                        CoroutineScope(Dispatchers.Main).launch {
                            showDialog = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.ProfileDetailsCard(showSnack: ()->Unit, onSubmit: (UserEntity) -> Unit) {
    var name by remember {
        mutableStateOf("")
    }
    var gender by remember {
        mutableStateOf("")
    }
    var mobile by remember {
        mutableStateOf("")
    }
    val scrollState = rememberScrollState()
    var address by remember {
        mutableStateOf("")
    }
    var dob by remember {
        mutableLongStateOf(-1)
    }
    var showError by remember {
        mutableStateOf(false)
    }
    ElevatedCard(
        onClick = { }, modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        shape = AbsoluteRoundedCornerShape(
            topLeftPercent = 10,
            topRightPercent = 10,
            bottomLeftPercent = 0,
            bottomRightPercent = 0
        ),
        enabled = false,
        colors = CardDefaults.elevatedCardColors(
            disabledContainerColor = Color.White,
            disabledContentColor = Color.Black
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "Employee Details",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, bottom = 15.dp),
            )
            OutlinedTextField(
                maxLines = 1,
                value = name,
                onValueChange = { name = it },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                placeholder = { Text(text = "Name") },
                label = { Text(text = "Name") },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                isError = showError && name.trim().isEmpty()
            )
            DatePickerDocked(showError) {
                dob = it
            }
            OutlinedTextField(
                maxLines = 1,
                value = gender,
                onValueChange = { gender = it },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                placeholder = { Text(text = "Gender") },
                label = { Text(text = "Gender") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                isError = showError && gender.trim().isEmpty(),
            )
            OutlinedTextField(
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                maxLines = 1,
                value = mobile,
                onValueChange = {
                    if (it.length <= 10) {
                        mobile = it
                    }
                },
                placeholder = { Text(text = "Phone") },
                label = { Text(text = "Phone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                isError = showError && mobile.trim().isEmpty()
            )

            OutlinedTextField(
                minLines = 3,
                value = address,
                onValueChange = { address = it },
                placeholder = { Text(text = "Address") },
                label = { Text(text = "Address") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                isError = showError && address.trim().isEmpty()
            )

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = {

                var employee = Utils.getEmptyEmployee()
                employee = employee.copy(
                    name = name.trim(),
                    gender = gender.trim(),
                    phone = mobile.trim(),
                    address = address.trim(),
                    dob = dob
                )
                if (employee.validateData()) {
                    onSubmit(employee)
                } else {
                    showError = true
                    showSnack()
                }
            }) {
                Text(
                    text = "Submit",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun RegisterCard(name: String, time: Long, image: ImageBitmap, hideDialog: () -> Unit) {
    val duration = 2
    var progress by remember { mutableFloatStateOf(1f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing), label = ""
    )
    LaunchedEffect(Unit) {
        for (i in duration downTo 1) {
            progress -= 0.50f
            delay(1000)
        }
        hideDialog()
    }

    ElevatedCard(
        modifier = Modifier.cardModifier(),
        onClick = {},
        shape = RoundedCornerShape(5)
    ) {
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = CardDefaults.elevatedCardColors().containerColor
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Registered!",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .padding(top = 20.dp)
            )
            Image(
                bitmap = image,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(),
                    modifier = Modifier.padding(bottom = 5.dp)
                )
                Text(
                    text = Utils.getDateTime(time),
                    style = MaterialTheme.typography.bodyLarge.copy(),
                    modifier = Modifier.padding(bottom = 30.dp)
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDocked( showError: Boolean, onSubmit: (Long) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDate by remember {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 15.dp)
    ) {
        OutlinedTextField(
            value = selectedDate,
            onValueChange = { },
            placeholder = { Text(text = "Date Of Birth") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = !showDatePicker }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select date"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        showDatePicker = true
                        focusManager.clearFocus()
                    }
                },
            isError = showError && selectedDate.isEmpty()
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDate = Utils.getDate(
                            datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        )
                        onSubmit(datePickerState.selectedDateMillis ?: -1)
                        showDatePicker = false
                    }) {
                        Text(text = "OK")
                    }
                }) {
                DatePicker(state = datePickerState)
            }
        }
    }
}