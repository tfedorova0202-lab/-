sed -i 's/viewModel.sendMessage("\/АУДИТ_ДОК")/documentLauncher.launch(arrayOf("application\/pdf", "image\/*"))/g' app/src/main/java/com/example/ui/ChatScreen.kt
