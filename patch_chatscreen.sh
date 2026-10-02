sed -i '/val context = LocalContext.current/a\
\
    val documentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(\
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()\
    ) { uri ->\
        if (uri != null) {\
            val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"\
            if (mimeType.startsWith("image/") || mimeType == "application/pdf") {\
                try {\
                    val inputStream = context.contentResolver.openInputStream(uri)\
                    val bytes = inputStream?.readBytes()\
                    inputStream?.close()\
                    if (bytes != null) {\
                        val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)\
                        var fileName = "Document"\
                        val cursor = context.contentResolver.query(uri, null, null, null, null)\
                        cursor?.use {\
                            if (it.moveToFirst()) {\
                                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)\
                                if (nameIndex != -1) {\
                                    fileName = it.getString(nameIndex)\
                                }\
                            }\
                        }\
                        viewModel.sendDocument(base64Data, mimeType, fileName)\
                    } else {\
                        Toast.makeText(context, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()\
                    }\
                } catch (e: Exception) {\
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()\
                }\
            } else {\
                Toast.makeText(context, "Поддерживаются только изображения и PDF", Toast.LENGTH_SHORT).show()\
            }\
        }\
    }' app/src/main/java/com/example/ui/ChatScreen.kt
