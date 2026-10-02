import re

with open('app/src/main/java/com/example/ui/AddDebtDialog.kt', 'r') as f:
    text = f.read()

# I will find the badly formatted block and replace it
bad_block = """                Row(                Divider(modifier = Modifier.padding(vertical = 4.dp))                Text("Критический срок (Юридический календарь)", style = MaterialTheme.typography.titleSmall)                OutlinedTextField(                    value = deadlineDate,                    onValueChange = { deadlineDate = it },                    label = { Text("Дата (ДД.ММ.ГГГГ)") },                    modifier = Modifier.fillMaxWidth(),                    singleLine = true                )                OutlinedTextField(                    value = deadlineType,                    onValueChange = { deadlineType = it },                    label = { Text("Тип (например: Отмена приказа)") },                    modifier = Modifier.fillMaxWidth(),                    singleLine = true                )                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,                    modifier = Modifier.fillMaxWidth()                ) {"""
bad_block_clean = bad_block.replace(" ", "").replace("\n", "")

def clean_text(s):
    return s.replace(" ", "").replace("\n", "")

text_clean = clean_text(text)

# We will just rewrite the file from scratch with correct structure if it's too broken, but maybe simpler:
