package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import com.example.utils.DocumentGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.Debt
import com.example.ui.AddDebtDialog
import com.example.ui.AnalyticsTab
import com.example.ui.AppTab
import com.example.ui.ToolsTab
import com.example.ui.BudgetPlannerDialog
import com.example.ui.PayoffCalculatorDialog
import com.example.ui.DebtListScreen
import com.example.ui.ProfileDialog
import com.example.ui.DocumentPreviewDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.UserPreferences
import kotlinx.coroutines.launch

import com.example.ui.DebtProtectionDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 1. Инициализируем базу данных (защищенное хранилище на устройстве)
        val db = AppDatabase.getDatabase(this)
        
        // 2. Создаем фабрику для передачи базы данных в ViewModel
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DebtCalculatorViewModel(db.debtDao()) as T
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Подключаем ViewModel к жизненному циклу Compose
                    val viewModel: DebtCalculatorViewModel = viewModel(factory = factory)
                    
                    // Собираем реактивные состояния
                    val debts by viewModel.debts.collectAsStateWithLifecycle()
                    val totalFact by viewModel.totalFact.collectAsStateWithLifecycle()
                    val totalPlan by viewModel.totalPlan.collectAsStateWithLifecycle()
                    val totalPenalty by viewModel.totalPenalty.collectAsStateWithLifecycle()
                    
                    var currentTab by remember { mutableStateOf(AppTab.REGISTRY) }

                    // Управление всплывающим окном
                    var showDialog by remember { mutableStateOf(false) }
                    var showBkiDialog by remember { mutableStateOf(false) }
                    var showProfileDialog by remember { mutableStateOf(false) }
                    var showBudgetDialog by remember { mutableStateOf(false) }
                    var showCalculatorDialog by remember { mutableStateOf(false) }
                    var showSavingsDialog by remember { mutableStateOf(false) }
                    var showSavingsSliderDialog by remember { mutableStateOf(false) }
                    var showTemplatesDialog by remember { mutableStateOf(false) }
                    var showStrategiesDialog by remember { mutableStateOf(false) }
                    var showMfoCheckDialog by remember { mutableStateOf(false) }
                    var mfoCheckInitialQuery by remember { mutableStateOf("") }
                    var editingDebt by remember { mutableStateOf<Debt?>(null) }
                    var selectedDebtForProtection by remember { mutableStateOf<Debt?>(null) }
                    var lackOfFundsPreviewDebt by remember { mutableStateOf<Debt?>(null) }
                    var isParsingBki by remember { mutableStateOf(false) }
                    var isScanning by remember { mutableStateOf(false) }
                    var bkiError by remember { mutableStateOf<String?>(null) }
                    val context = LocalContext.current
                    val userPrefs = remember { UserPreferences(context) }
                    val scope = rememberCoroutineScope()
                    var isAppUnlocked by remember { mutableStateOf(userPrefs.getPin().isEmpty()) }

                    val authViewModel: com.example.ui.AuthViewModel = viewModel()
                    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
                    val isGuest by authViewModel.isGuest.collectAsStateWithLifecycle()
                    val isCustomAuth by authViewModel.isCustomAuthenticated.collectAsStateWithLifecycle()

                    if (currentUser == null && !isGuest && !isCustomAuth) {
                        com.example.ui.AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = { },
                            onContinueAsGuest = { authViewModel.continueAsGuest() }
                        )
                        return@Surface
                    }

                    if (!isAppUnlocked && userPrefs.getPin().isNotEmpty()) {
                        com.example.ui.PinScreen(
                            expectedPin = userPrefs.getPin(),
                            onUnlock = { isAppUnlocked = true }
                        )
                        return@Surface
                    }

                    val pdfPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            showBkiDialog = false
                            isParsingBki = true
                            bkiError = null
                            scope.launch {
                                try {
                                    val parsedDebts = com.example.utils.BkiParser.parseBkiReport(context, uri)
                                    parsedDebts.forEach { parsed ->
                                        viewModel.saveDebt(
                                            id = null,
                                            creditorName = parsed.creditorName,
                                            contractDate = parsed.contractDate,
                                            factAmount = parsed.factAmount,
                                            penaltyAmount = 0.0,
                                            insuranceAmount = 0.0,
                                            isMortgage = parsed.isMortgage
                                        )
                                    }
                                } catch (e: Exception) {
                                    bkiError = e.message ?: "Ошибка при анализе файла"
                                } finally {
                                    isParsingBki = false
                                }
                            }
                        }
                    }

                    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
                    ) { bitmap ->
                        if (bitmap != null) {
                            isScanning = true
                            bkiError = null
                            scope.launch {
                                try {
                                    val scannedData = com.example.utils.DocumentScanner.parseDebtFromBitmap(bitmap)
                                    // Открываем диалог добавления долга с предзаполненными данными
                                    editingDebt = Debt(
                                        id = "",
                                        creditorName = scannedData.creditorName,
                                        contractDate = scannedData.contractDate,
                                        factAmount = scannedData.factAmount,
                                        penaltyAmount = 0.0,
                                        insuranceAmount = 0.0,
                                        planAmount = scannedData.factAmount,
                                        status = "В работе",
                                        isMortgage = false
                                    )
                                    showDialog = true
                                } catch (e: Exception) {
                                    bkiError = "Не удалось распознать текст на фото."
                                } finally {
                                    isScanning = false
                                }
                            }
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                AppTab.values().forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                                        label = { Text(tab.title) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            when (currentTab) {
                                AppTab.REGISTRY -> {
                                    DebtListScreen(
                                        debts = debts,
                                        totalFact = totalFact,
                                        totalPlan = totalPlan,
                                        totalToCancel = totalPenalty,
                                        onAddClick = {
                                            editingDebt = null
                                            showDialog = true
                                        },
                                        onEditClick = { debt ->
                                            editingDebt = debt
                                            showDialog = true
                                        },
                                        onDeleteClick = { debt ->
                                            viewModel.deleteDebt(debt)
                                        },
                                        onProtectionClick = { debt ->
                                            selectedDebtForProtection = debt
                                        },
                                        onStrategiesClick = { showStrategiesDialog = true },
                                        onCheckMfoClick = { creditorName ->
                                            mfoCheckInitialQuery = creditorName
                                            showMfoCheckDialog = true
                                        }
                                    )
                                }
                                AppTab.ANALYTICS -> {
                                    AnalyticsTab(
                                        onBudgetClick = { showBudgetDialog = true },
                                        onCalculatorClick = { showCalculatorDialog = true },
                                        onSavingsClick = { showSavingsDialog = true },
                                        onSavingsSliderClick = { showSavingsSliderDialog = true },
                                        debts = debts
                                    )
                                }
                                AppTab.TOOLS -> {
                                    ToolsTab(
                                        onBkiClick = { showBkiDialog = true },
                                        onScanClick = { cameraLauncher.launch(null) },
                                        onTemplatesClick = { showTemplatesDialog = true },
                                        onStrategiesClick = { showStrategiesDialog = true },
                                        onCheckMfoClick = {
                                            mfoCheckInitialQuery = ""
                                            showMfoCheckDialog = true
                                        },
                                        debts = debts
                                    )
                                }
                                AppTab.CHAT -> {
                                    com.example.ui.ChatScreen()
                                }
                                AppTab.PROFILE -> {
                                    ProfileDialog(
                                        userPreferences = userPrefs,
                                        onLoadMockupData = { viewModel.loadMockupPresentationData() },
                                        onClearAllDebts = { viewModel.clearAllDebts() },
                                        onFullDataReset = {
                                            viewModel.clearAllDebts()
                                            scope.launch {
                                                try {
                                                    db.messageDao().clearHistory()
                                                    db.queryHistoryDao().clearHistory()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                        onDismiss = { currentTab = AppTab.REGISTRY }
                                    )
                                }
                            }
                        }
                    }

                    // Всплывающее окно добавления/редактирования
                    if (showDialog) {
                        AddDebtDialog(
                            initialDebt = editingDebt,
                            onDismiss = { showDialog = false },
                            onSave = { id, name, date, fact, penalty, insurance, isMortgage, deadlineDate, deadlineType ->
                                viewModel.saveDebt(id, name, date, fact, penalty, insurance, isMortgage, deadlineDate, deadlineType)
                                showDialog = false
                            }
                        )
                    }

                    if (showSavingsDialog) {
                        com.example.ui.SavingsCalculatorDialog(
                            onDismiss = { showSavingsDialog = false }
                        )
                    }
                    if (showSavingsSliderDialog) {
                        com.example.ui.SavingsSliderDialog(
                            onDismiss = { showSavingsSliderDialog = false }
                        )
                    }

                    if (showTemplatesDialog) {
                        com.example.ui.LegalTemplatesDialog(
                            debts = debts,
                            onDismiss = { showTemplatesDialog = false }
                        )
                    }

                    if (showStrategiesDialog) {
                        com.example.ui.LegalStrategiesDialog(
                            onDismiss = { showStrategiesDialog = false },
                            onOpenTemplates = {
                                showStrategiesDialog = false
                                showTemplatesDialog = true
                            }
                        )
                    }

                    if (showBudgetDialog) {
                        BudgetPlannerDialog(
                            userPreferences = userPrefs,
                            onDismiss = { showBudgetDialog = false }
                        )
                    }

                    if (showCalculatorDialog) {
                        PayoffCalculatorDialog(
                            debts = debts,
                            userPreferences = userPrefs,
                            onDismiss = { showCalculatorDialog = false }
                        )
                    }

                    selectedDebtForProtection?.let { debt ->
                        DebtProtectionDialog(
                            debt = debt,
                            onDismiss = { selectedDebtForProtection = null },
                            onGenerateObjection = {
                                val uri = DocumentGenerator.generateObjectionPdf(context, debt)
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Отправить Возражение"))
                                }
                            },
                            onGenerateAntiCollector = {
                                val uri = DocumentGenerator.generateAntiCollectorPdf(context, debt)
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Отправить Отзыв согласия"))
                                }
                            },
                            onGenerateRefusal = {
                                val uri = DocumentGenerator.generateRefusalOfInteractionPdf(context, debt)
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Отправить Отказ от взаимодействия"))
                                }
                            },
                            onGenerateLackOfFunds = {
                                lackOfFundsPreviewDebt = debt
                            },
                            onGenerateInsuranceReturn = {
                                val uri = DocumentGenerator.generateInsuranceReturnPdf(context, debt)
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Отправить Возврат страховки"))
                                }
                            }
                        )
                    }

                    lackOfFundsPreviewDebt?.let { debt ->
                        val documentText = DocumentGenerator.getLackOfFundsText(context, debt)
                        DocumentPreviewDialog(
                            title = "Оспаривание по безденежности",
                            documentText = documentText,
                            onDismiss = { lackOfFundsPreviewDebt = null },
                            onShareClick = { text ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Отправить Претензию (Текст)"))
                            }
                        )
                    }

                    if (showBkiDialog) {
                        AlertDialog(
                            onDismissRequest = { showBkiDialog = false },
                            title = { Text("Анализ БКИ (НБКИ/ОКБ)", style = MaterialTheme.typography.titleLarge) },
                            text = { 
                                Text("Загрузите PDF-отчет из Бюро кредитных историй. Искусственный интеллект автоматически расшифрует ваши договоры, найдет суммы долгов и добавит их в реестр ПЛАН/ФАКТ.")
                            },
                            confirmButton = {
                                Button(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                                    Text("Выбрать PDF-файл")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBkiDialog = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    if (isParsingBki) {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Анализ документа") },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("ИИ изучает кредитную историю, это займет несколько секунд...")
                                }
                            },
                            confirmButton = { }
                        )
                    }

                    if (isScanning) {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Сканирование договора") },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Распознавание текста с камеры (ML Kit)...")
                                }
                            },
                            confirmButton = { }
                        )
                    }

                    if (bkiError != null) {
                        AlertDialog(
                            onDismissRequest = { bkiError = null },
                            title = { Text("Ошибка анализа") },
                            text = { Text(bkiError ?: "Неизвестная ошибка. Проверьте GEMINI_API_KEY в настройках.") },
                            confirmButton = {
                                Button(onClick = { bkiError = null }) { Text("ОК") }
                            }
                        )
                    }

                    if (showMfoCheckDialog) {
                        com.example.ui.CbrMfoCheckDialog(
                            initialQuery = mfoCheckInitialQuery,
                            onDismiss = { showMfoCheckDialog = false },
                            onAddDebtFromMfo = { name ->
                                editingDebt = Debt(
                                    id = "",
                                    creditorName = name,
                                    contractDate = "",
                                    factAmount = 0.0,
                                    penaltyAmount = 0.0,
                                    insuranceAmount = 0.0,
                                    planAmount = 0.0,
                                    status = "В работе",
                                    isMortgage = false
                                )
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}
