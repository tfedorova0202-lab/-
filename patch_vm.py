import re

with open('app/src/main/java/com/example/DebtCalculatorViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'fun saveDebt(id: String?, creditorName: String, contractDate: String, factAmount: Double, penaltyAmount: Double, insuranceAmount: Double, isMortgage: Boolean)',
    'fun saveDebt(id: String?, creditorName: String, contractDate: String, factAmount: Double, penaltyAmount: Double, insuranceAmount: Double, isMortgage: Boolean, deadlineDate: String = "", deadlineType: String = "")'
)

text = text.replace(
    'isMortgage = isMortgage\n        )',
    'isMortgage = isMortgage,\n            deadlineDate = deadlineDate,\n            deadlineType = deadlineType\n        )'
)

with open('app/src/main/java/com/example/DebtCalculatorViewModel.kt', 'w') as f:
    f.write(text)
