sed -i 's/val responseModalities: List<String>? = null/val responseModalities: List<String>? = null,\n    val safetySettings: List<SafetySetting>? = null/g' app/src/main/java/com/example/api/GeminiApi.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/api/GeminiApi.kt

@Serializable
data class SafetySetting(
    val category: String,
    val threshold: String
)
INNER_EOF
