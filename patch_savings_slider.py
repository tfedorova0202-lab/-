import re

with open('app/src/main/java/com/example/ui/SavingsSliderDialog.kt', 'r') as f:
    text = f.read()

imports_to_add = """import androidx.compose.ui.platform.LocalContext
import com.example.utils.DocumentGenerator
import android.content.Intent
"""
text = text.replace('import androidx.compose.ui.unit.dp', 'import androidx.compose.ui.unit.dp\n' + imports_to_add)

confirm_button_old = """        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Понятно")
            }
        }"""

confirm_button_new = """        confirmButton = {
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
                
                Button(
                    onClick = {
                        val uri = DocumentGenerator.generateSavingsPdf(
                            context = context,
                            principal = principal,
                            penalty = penalty,
                            limitPercent = sliderValue.toInt(),
                            limitAmount = limit,
                            savings = savings
                        )
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Отправить расчет"))
                        }
                    },
                    enabled = principal > 0
                ) {
                    Text("В PDF")
                }
            }
        }"""

text = text.replace(confirm_button_old, confirm_button_new)

with open('app/src/main/java/com/example/ui/SavingsSliderDialog.kt', 'w') as f:
    f.write(text)
print("Patched SavingsSliderDialog.kt")
