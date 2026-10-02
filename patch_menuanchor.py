import os

files = [
    'app/src/main/java/com/example/ui/LegalTemplatesDialog.kt',
    'app/src/main/java/com/example/ui/SavingsCalculatorDialog.kt'
]

for file in files:
    with open(file, 'r') as f:
        text = f.read()
    text = text.replace('.menuAnchor()', '.menuAnchor(MenuAnchorType.PrimaryNotEditable)')
    with open(file, 'w') as f:
        f.write(text)
print("Patched menuAnchor")
