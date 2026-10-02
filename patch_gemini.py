import re

with open('app/src/main/java/com/example/api/GeminiApi.kt', 'r') as f:
    text = f.read()

# 1. Add Tool definitions
tools_classes = """@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null,
    val tools: List<Tool>? = null
)

@Serializable
data class Tool(
    val googleSearch: GoogleSearch? = null
)

@Serializable
class GoogleSearch"""

text = re.sub(r'@Serializable\s+data class GenerateContentRequest\([^\)]+\)', tools_classes, text)

with open('app/src/main/java/com/example/api/GeminiApi.kt', 'w') as f:
    f.write(text)
print("Patched GeminiApi.kt")
