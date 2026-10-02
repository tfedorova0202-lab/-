import os
import re

files_icons = {
    'app/src/main/java/com/example/ui/AppTabs.kt': [
        ('Icons.Default.List', 'Icons.AutoMirrored.Filled.List'),
        ('Icons.Default.Chat', 'Icons.AutoMirrored.Filled.Chat'),
        ('Icons.Default.TrendingDown', 'Icons.AutoMirrored.Filled.TrendingDown'),
        ('import androidx.compose.material.icons.filled.List', 'import androidx.compose.material.icons.automirrored.filled.List'),
        ('import androidx.compose.material.icons.filled.Chat', 'import androidx.compose.material.icons.automirrored.filled.Chat'),
        ('import androidx.compose.material.icons.filled.TrendingDown', 'import androidx.compose.material.icons.automirrored.filled.TrendingDown'),
    ],
    'app/src/main/java/com/example/ui/ChatScreen.kt': [
        ('Icons.Default.Send', 'Icons.AutoMirrored.Filled.Send'),
        ('import androidx.compose.material.icons.filled.Send', 'import androidx.compose.material.icons.automirrored.filled.Send'),
    ]
}

for file, replacements in files_icons.items():
    if not os.path.exists(file):
        continue
    with open(file, 'r') as f:
        text = f.read()
    for old, new in replacements:
        text = text.replace(old, new)
    with open(file, 'w') as f:
        f.write(text)

print("Icons patched.")
