import re

with open('app/src/main/java/com/example/ui/AppTabs.kt', 'r') as f:
    text = f.read()

old_block = """@Composable
fun AnalyticsTab(
    onBudgetClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onSavingsClick: () -> Unit,
    debts: List<Debt>
) {"""

new_block = """@Composable
fun AnalyticsTab(
    onBudgetClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onSavingsClick: () -> Unit,
    onSavingsSliderClick: () -> Unit,
    debts: List<Debt>
) {"""

text = text.replace(old_block, new_block)

card_old = """            Card(onClick = onSavingsClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Проверка переплат (МФО)", style = MaterialTheme.typography.titleMedium)
                    Text("Калькулятор списания незаконных начислений по ФЗ-353.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }"""

card_new = card_old + """
        
        Card(onClick = onSavingsSliderClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Интерактивная экономия", style = MaterialTheme.typography.titleMedium)
                    Text("Слайдер процентных ставок для расчета выгоды.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }"""

text = text.replace(card_old, card_new)

with open('app/src/main/java/com/example/ui/AppTabs.kt', 'w') as f:
    f.write(text)
print("patched AppTabs")
